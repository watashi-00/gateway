package com.watashi;

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

    private final String[] clusters = {
        "knowledge_service",
        "ai_service",
    };

    public static void main(String[] args) {
        System.out.println("Gateway application started");
        DebugUtils.setDebugEnabled(true);
        new GatewayApplication().start();
    }

    void start() {
        GatewayBuilderPort builder = GatewayFactory.createGateway("gt-watashi") // gateway name
            .createCluster("auth_service")  // cluster name
            .port(8079) // default port http = 8080
            .enableTelnet(false)
            .enableWs(false) 
            .enableHttp(true)
            .enableTcpProxy(false)
            .requireToken(false, null)
            .rateLimit(60, 10)
            .httpEngine(HttpEngine.UNDERTOW)
            .performanceProfile(PerformanceProfile.MAX_PERFORMANCE);

        for(String cl : clusters) {
            builder.createCluster(cl);
        }

        builder.routeHost("localhost", "/auth/**", "auth_service");
        builder.routeHost("localhost", "/knowledge/**", "knowledge_service");
        builder.routeHost("localhost", "/ai/**", "ai_service");

        var gateway = builder.listen();

        gateway.startPingScheduler();

        TerminalUiFactory.createTui("watashi00")
           .seedGateway(gateway)
           .startToggleMode();
    }
}