package com.watashi;

import hexacloud.core.cluster.Cluster;
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
        GatewayBuilderPort builder = GatewayFactory.createGateway("gt-watashi")
            .createCluster("cl-watashi")
            .port(3000)
            .enableTelnet(false)
            .enableWs(false)
            .enableHttp(true)
            .enableTcpProxy(false)
            .requireToken(false, "null")
            .rateLimit(60, 10)
            .httpEngine(HttpEngine.UNDERTOW)
            .performanceProfile(PerformanceProfile.MAX_PERFORMANCE);

        builder.routeHost("localhost", "/auth/**", "cl-watashi");

        builder.getCluster().setRoutingMode(Cluster.RoutingMode.LOAD_BALANCER_ONLY);

        builder.registerNode("https://api-shiori.hexacloud.net.br", 443)
                .external(true)
                .pingPath("/health")
                .register();
        
        builder.registerNode("https://gatebridge.hexacloud.net.br", 443)
                .external(true)
                .pingPath("/public_telemetry")
                .register();

        var gateway = builder.listen();

        gateway.startPingScheduler();

        TerminalUiFactory.createTui("watashi00")
           .seedGateway(gateway)
           .startToggleMode();
    }
}