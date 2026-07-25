package com.watashi;

import hexacloud.core.cluster.Cluster.RoutingMode;
import hexacloud.core.model.NodeStatus;
import hexacloud.core.model.PingProtocol;
import hexacloud.core.model.ServerNode;
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
        GatewayBuilderPort builder = GatewayFactory.createGateway("gt-watashi") // gateway name
            .createCluster("cl-watashi")                         // cluster name
            .port(8079)                                          // base port
            .enableHttp(true)                                    // enable http requests http port = base + 1;
            .requireToken(false, System.getenv("SECRET_KEY"))     // enable token authentication
            .rateLimit(60, 10).httpEngine(HttpEngine.JDK_DEFAULT)// JDK_DEFAULT | UNDERTOW 
            .performanceProfile(PerformanceProfile.MAX_PERFORMANCE);// MAX_PERFORMANCE | STANDARD


        builder.registerServer(new ServerNode(
                "node-http-1", "http://localhost", 8081, NodeStatus.OFFLINE, false,
                PingProtocol.HTTP, "/api/health", "X-Cluster-Token", "watashi_secretKey"
            ));

        builder.routeHost("localhost", "/auth/**", "cl-watashi");

        var cl = builder.getCluster();
        cl.setRoutingMode(RoutingMode.LOAD_BALANCER_ONLY);       // LOAD_BALANCER_ONLY | TELEMETRY_ONLY | HYBRID
        
        var gateway = builder.listen();

        TerminalUiFactory.createTui("watashi00")
           .seedGateway(gateway)
           .startToggleMode();                                    // create a decoupled TUI that is active when Enter key is pressed

        gateway.startPingScheduler();                             // active ping scheduler

    }
}

