package com.watashi;

import hexacloud.core.cluster.Cluster.RoutingMode;
import hexacloud.core.ports.GatewayBuilderPort;
import hexacloud.core.server.HttpEngine;
import hexacloud.core.server.PerformanceProfile;
import hexacloud.core.tui.TerminalUiFactory;
import hexacloud.core.utils.common.DebugUtils;
import hexacloud.infra.gateway.GatewayFactory;

/**
 * Main entry point for the Gateway application.
 */
public class GatewayApplication {

    public static void main(String[] args) {
        System.out.println("Gateway application started");
        DebugUtils.setDebugEnabled(true);
        new GatewayApplication().start();
    }

    void start() {
        final String secret = System.getenv("SECRET_KEY");

        GatewayBuilderPort builder = GatewayFactory.createGateway("gt-watashi")
            .createCluster("cl-auth")
                .registerNode("node-auth-1", "http://localhost", 8085)
                .pingEnabled(true)
                .pingPath("/api/health")
                .register()
            .port(8079)                                
            .enableHttp(true)
            .enableTcpProxy(true)
            .requireToken(false, secret)
            .rateLimit(60, 10)
            .httpEngine(HttpEngine.JDK_DEFAULT)
            .performanceProfile(PerformanceProfile.MAX_PERFORMANCE);

        builder.createCluster("cl-knowledge")
            .registerNode("node-knowledge-1", "http://localhost", 8086)
                .pingEnabled(true)
                .pingPath("/api/health")
                .register();

        builder.createCluster("cl-ai")
            .registerNode("node-ai-1", "http://localhost", 8087)
                .pingEnabled(true)
                .pingPath("/api/health")
                .register();

        builder.createCluster("cl-worker")
            .registerNode("node-worker-1", "localhost", 8088)
                .pingEnabled(true)
                .register();

        builder.routeHost("localhost", "/auth/**", "cl-auth");
        builder.routeHost("localhost", "/knowledge/**", "cl-knowledge");
        builder.routeHost("localhost", "/ai/**", "cl-ai");
        builder.routeHost("localhost", "/worker/**", "cl-worker");

        var cl = builder.getCluster();
        cl.setRoutingMode(RoutingMode.LOAD_BALANCER_ONLY);
        
        var gateway = builder.listen();

        TerminalUiFactory.createTui("watashi00")
           .seedGateway(gateway)
           .startToggleMode();                                    

        gateway.startPingScheduler();                             
    }
}