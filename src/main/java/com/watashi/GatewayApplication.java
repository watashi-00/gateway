package com.watashi;

import hexacloud.core.model.RoutingProtocol;
import hexacloud.core.ports.GatewayBuilderPort;
import hexacloud.core.ports.RunningGatewayPort;
import hexacloud.core.utils.common.DebugUtils;
import hexacloud.infra.gateway.GatewayFactory;
import hexacloud.core.server.route.RouteController;
import hexacloud.core.server.route.RouteMapping;
import hexacloud.core.server.HttpEngine;
import java.io.PrintWriter;

public class GatewayApplication {
    public static void main(String[] args) {
        System.out.println("=== GatewayApplication Starting ===");
        DebugUtils.setDebugEnabled(true);
        System.out.println("Creating gateway builder...");

        GatewayBuilderPort builder = GatewayFactory.createGateway("benchmark-cluster")
                .createCluster("c1")
                    .registerNode("http://localhost", 3000)
                        .routingProtocol(RoutingProtocol.HTTP)
                        .pingEnabled(true)
                    .register()
                    .registerNode("http://localhost", 3001)
                        .routingProtocol(RoutingProtocol.HTTP)
                        .pingEnabled(true)
                    .register()
                    .registerNode("http://localhost", 3002)
                        .routingProtocol(RoutingProtocol.HTTP)
                        .pingEnabled(true)
                    .register()
                    .requireToken(false, null)
                    .rateLimit(-1, 0)
                .createCluster("c2")
                    .registerNode("http://localhost", 4000)
                        .routingProtocol(RoutingProtocol.HTTP)
                        .pingEnabled(true)
                    .register()
                    .registerNode("http://localhost", 4001)
                        .routingProtocol(RoutingProtocol.HTTP)
                        .pingEnabled(true)
                    .register()
                    .registerNode("http://localhost", 4002)
                        .routingProtocol(RoutingProtocol.HTTP)
                        .pingEnabled(true)
                    .register()
                    .requireToken(false, null)
                    .rateLimit(-1, 0)
                .createCluster("c3")
                    .registerNode("http://localhost", 5000)
                        .routingProtocol(RoutingProtocol.HTTP)
                        .pingEnabled(true)
                    .register()
                    .registerNode("http://localhost", 5001)
                        .routingProtocol(RoutingProtocol.HTTP)
                        .pingEnabled(true)
                    .register()
                    .registerNode("http://localhost", 5002)
                        .routingProtocol(RoutingProtocol.HTTP)
                        .pingEnabled(true)
                    .register()
                    .requireToken(false, null)
                    .rateLimit(-1, 0)
                .routeHost("localhost", "/api/c1/**", "c1", "/")
                .routeHost("localhost", "/api/c2/**", "c2", "/")
                .routeHost("localhost", "/api/c3/**", "c3", "/")

                .port(8079)
                .enableTelnet(false)
                .enableHttp(true)
                .enableWs(false)
                .enableTcpProxy(false)
                .requireToken(false, null)
                .httpEngine(HttpEngine.JDK_DEFAULT)
                .rateLimit(-1, 0);

        System.out.println("Starting gateway listen()...");
        RunningGatewayPort runningGateway = builder.listen();
        runningGateway.startPingScheduler();

        System.out.println("Gateway started successfully: " + runningGateway);
    }

    public static class HelloController implements RouteController {
        @RouteMapping("hello")
        public void sayHello(String args, PrintWriter out) {
            out.print("hello");
        }

        @RouteMapping(value = "fast", fastPath = true)
        public void sayHelloFast(String args, PrintWriter out) {
            out.print("hello");
        }

    }
}