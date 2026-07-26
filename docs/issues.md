# Issues Found During Testing - GateBridge Core & Ingress v1.4.7

This document maps out the technical debt, API design flaws, and runtime bugs discovered during system testing and performance analysis. Addressing these issues is critical for stabilizing the framework for enterprise grade workloads.

## 1. Initialization and Auto-Discovery Flow (Control Plane)

**Current Behavior:** The gateway is configured via a fluent API, executes a classpath scan via reflection to register `@RouteMapping` and `@Subscribe`, and restricts the search to the application's root package to keep startup times under 1ms.

### Architectural and DX Gaps

* **The Closed Package Scope Trap:** By restricting the classpath scanning strictly to the "root package" to optimize startup time, the framework assumes client code will never be modularized. If an enterprise developer imports GateBridge and places controllers in a Maven submodule (e.g., `com.enterprise.routes`), auto-discovery will silently fail to find the routes. The gateway starts successfully but returns `404 Not Found` because the scanner ignored the other directory.
* **Proposed Fix:** The builder must accept an explicit method to broaden the scanning scope, such as `.scanPackages("com.enterprise")`.
* **Lack of Fail-Fast for Route Conflicts:** If two controllers declare the same command endpoint (e.g., two methods annotated with `@RouteMapping("CONF")`), the auto-discovery engine silently overwrites one of them during initialization.
* **Proposed Fix:** The `RouteRegistry` initializer must throw a clear exception (`DuplicateRouteException`) during bootstrap if it detects command key collisions, preventing the gateway from running in an inconsistent state.

---

## 2. L7 Data Plane Flow (HTTP Reverse Proxy)

**Current Behavior:** The `HttpTransport` receives requests on `base_port + 1`, selects a healthy node via Round-Robin, forwards headers and payloads using an asynchronous `HttpClient`, and passively extracts telemetry from response headers (`X-Telemetry-CPU`).

### Architectural and DX Gaps

* **Passive Telemetry Log Flooding:** If the backend node lacks the `X-Telemetry-CPU` header injector, the passive extraction method fails silently. At scale, constant extraction attempts resulting in null values will flood the logs and generate unnecessary empty string allocations in the JVM.
* **Broken Traceability Headers (Missing `X-Forwarded-*`):** The L7 proxy currently forwards raw headers. Without injecting or handling control headers like `X-Forwarded-For` (original client IP), `X-Forwarded-Proto`, and `X-Forwarded-Host`, internal microservices will assume requests originate from the gateway itself (`127.0.0.1`), destroying audit logs and backend geolocation tools.
* **HTTP/2 Negotiation Conflicts with Legacy Nodes:** The internal `HttpClient` is hardcoded to force HTTP/2. If a cluster backend is a legacy server running older HTTP/1.1 protocols, the JVM will attempt a protocol downgrade. Depending on the server, this can result in connection timeouts or TLS negotiation overhead on every proxying request.

---

## 3. L4 Data Plane Flow (TCP Proxy Tunneling)

**Current Behavior:** The gateway listens on `base_port + 3` and performs direct bidirectional byte tunneling between the client socket and the node socket using Virtual Threads.

### Architectural and DX Gaps

* **OOM Risk due to Lack of Backpressure (Buffer Leak):** In TCP tunneling, if the client sends gigabytes of data at high speeds but the destination backend node is slow, the Virtual Threads reading from the client socket will continue allocating buffers in the JVM Heap without restriction. Without active flow control (backpressure), the gateway will quickly exhaust JVM memory under stress, resulting in an `OutOfMemoryError`.
* **Orphan Socket Leaks (Missing TCP Keep-Alive & Timeout):** If a TCP connection is abruptly dropped by the client without sending a `FIN` packet, the gateway's Virtual Threads and file descriptors associated with that tunnel can remain open indefinitely. This leads to socket leaks and exhaustion of the operating system's open port limits.
* **Proposed Fix:** The Builder configuration must expose properties to define TCP inactivity timeouts (`SO_TIMEOUT`) and enable `SO_KEEPALIVE` on created sockets.

---

## 4. Health Monitoring Flow (ThreadPingScheduler)

**Current Behavior:** A periodic scheduler scans registered nodes and triggers asynchronous tests using multiple adapters. If a state change occurs, it dispatches events.

### Architectural and DX Gaps

* **Flapping Effect (Sensitivity Instability):** If a backend server experiences a temporary micro-bottleneck, a single failed ping will change its status to `OFFLINE`. The L7 balancer immediately cuts traffic to it. A moment later, the next ping succeeds, and it returns to `ONLINE`. This frantic oscillation destroys topology stability.
* **Proposed Fix:** Implement a **Failure Threshold** concept. A node should only be marked `OFFLINE` if it fails \(X\) consecutive times (e.g., 3 ping failures), protecting the cluster against false positives from edge network fluctuations.

---

## 5. Operations and Observability Flow (DevOps TUI & Logging)

**Current Behavior:** The TUI console displays resources in real-time. To prevent screen corruption, it hijacks standard output channels, redirecting `System.out` and `System.err` into its own interactive log panel.

### Architectural and DX Gaps

* **Host Application Log Hijacking:** This is a severe library-level DX issue. If a developer imports the `.jar` into their application, the TUI's initialization method redirects all standard JVM output to the GateBridge log panel. Developers lose visibility of their own application logs, and enterprise log collection agents (e.g., in Kubernetes) will break because standard output has been rerouted to an interactive buffer.
* **Proposed Fix:** Log redirection must be strictly optional and disabled by default when the framework is used as an embedded library. It should only hijack the console if initialized by the native DevOps main application (`TerminalMain.java`).

---

## 6. Critical State Management (Container Environments)

**Current Behavior & Impact:** System state configuration files, specifically `*-state.properties`, fail to persist across container restarts. This ephemeral storage behavior causes catastrophic configuration loss whenever the container lifecycle cycles (e.g., pod evictions, deployments, or restarts).

### Proposed Fix

* **Externalized Volume Mounting:** The framework must be updated to strictly write `*-state.properties` files to a configurable, externalized directory path. This ensures the configuration can be successfully mounted to persistent volumes (PV/PVC) in containerized environments rather than residing in the ephemeral container filesystem layer.

---

## Pre-Enterprise Refactoring Checklist

* **Decouple standard logs (`System.out`):** Make TUI console redirection optional in the builder.
* **Health Ping Thresholds:** Allow configuration of consecutive ping attempts before triggering a physical status transition.
* **Auto-discovery Scope Configuration:** Enable dynamic passing of multiple packages for controller scanning.
* **Explicit TCP/L4 Socket Timeouts:** Prevent orphan connections from getting stuck in asynchronous read loops and hanging the operating system.
* **Fix Container State Persistence:** Reroute `*-state.properties` generation to support persistent volume mounts.