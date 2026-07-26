package com.watashi;

import hexacloud.core.ports.GatewayBuilderPort;
import hexacloud.core.ports.RunningGatewayPort;
import hexacloud.core.utils.common.DebugUtils;
import hexacloud.infra.gateway.GatewayFactory;
import hexacloud.core.server.route.RouteController;
import hexacloud.core.server.route.RouteMapping;
import hexacloud.core.server.HttpEngine;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.io.IOException;

public class GatewayApplication {

 private static boolean isPortAvailable(int port) {
    
        try (java.net.ServerSocket socket = new java.net.ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("0.0.0.0", port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void main(String[] args) {

        System.out.println("=== GatewayApplication Starting ===");

        int basePort = 8079;

        System.out.println(
            "Checking port " + basePort + "..."
        );

        if (!isPortAvailable(basePort)) {
            System.err.println(
                "ERROR: Port " + basePort + " is already in use."
            );
            System.exit(1);
        }

        System.out.println(
            "Port " + basePort + " is available."
        );

        DebugUtils.setDebugEnabled(true);

        System.out.println("Creating gateway builder...");

        GatewayBuilderPort builder = GatewayFactory.createGateway("benchmark-cluster")
            .createCluster("watata")
            .port(basePort)
            .enableTelnet(false)
            .enableHttp(true)
            .enableWs(true)
            .enableTcpProxy(false)
            .requireToken(false, null)
            .rateLimit(-1, 0)
            .httpEngine(HttpEngine.JDK_DEFAULT);

        System.out.println("Starting gateway listen()...");

        RunningGatewayPort runningGateway = builder.listen();

        System.out.println(
            "Gateway started successfully: " + runningGateway
        );
    }

    public static class HelloController implements RouteController {

        @RouteMapping("HELLO")
        public void sayHello(String args, PrintWriter out) {
            out.print("hello");
        }
    }
}