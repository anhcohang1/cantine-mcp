package fr.cantine;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    private static final Map<String, Object> NO_ARGUMENTS = Map.of(
            "type", "object",
            "properties", Map.of(),
            "additionalProperties", false
    );

    @Bean
    HttpServletStreamableServerTransportProvider mcpTransport() {
        return HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(McpJsonDefaults.getMapper())
                .mcpEndpoint("/mcp")
                .build();
    }

    @Bean
    ServletRegistrationBean<HttpServletStreamableServerTransportProvider> mcpServlet(
            HttpServletStreamableServerTransportProvider transport) {
        return new ServletRegistrationBean<>(transport, "/mcp");
    }

    @Bean(destroyMethod = "close")
    McpSyncServer mcpServer(HttpServletStreamableServerTransportProvider transport,
                            CantineService cantineService) {

        var balanceTool = SyncToolSpecification.builder()
                .tool(Tool.builder("get_canteen_balance", NO_ARGUMENTS)
                        .description("Retourne le solde actuel du compte de cantine en euros.")
                        .build())
                .callHandler((exchange, request) -> CallToolResult.builder()
                        .content(List.of(new TextContent(
                                "Solde du compte cantine : " + cantineService.getBalance() + " EUR")))
                        .build())
                .build();

        var maxDebtTool = SyncToolSpecification.builder()
                .tool(Tool.builder("get_canteen_max_debt", NO_ARGUMENTS)
                        .description("Retourne la dette maximale autorisée du compte de cantine en euros.")
                        .build())
                .callHandler((exchange, request) -> CallToolResult.builder()
                        .content(List.of(new TextContent(
                                "Dette maximale autorisée : " + cantineService.getMaxDebt() + " EUR")))
                        .build())
                .build();

        return McpServer.sync(transport)
                .serverInfo("cantine-mcp", "1.0.0")
                .capabilities(ServerCapabilities.builder().tools(false).build())
                .tools(balanceTool, maxDebtTool)
                .build();
    }
}
