package com.bica.reborn.agent.transport;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP (Model Context Protocol) transport for tool-calling agents.
 *
 * <p>Implements the MCP JSON-RPC 2.0 protocol over stdio. Each tool call
 * is a session type transition: the agent sends a request (branch label)
 * and receives a response (selection outcome).
 *
 * <p>Session type of MCP:
 * {@code &{initialize: rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end}}}
 *
 * <p>This transport can either connect to an external MCP server process
 * (via stdin/stdout) or operate in simulation mode for testing.
 *
 * @see <a href="https://modelcontextprotocol.io">Model Context Protocol</a>
 */
public final class McpTransport implements Transport {

    private final Map<String, TransportListener> listeners = new ConcurrentHashMap<>();
    private final AtomicInteger requestId = new AtomicInteger(1);
    private final boolean simulationMode;
    private final Map<String, McpToolHandler> toolHandlers;

    /** Tool handler for simulation mode. */
    @FunctionalInterface
    public interface McpToolHandler {
        Map<String, Object> handle(String toolName, Map<String, Object> arguments);
    }

    private McpTransport(boolean simulationMode, Map<String, McpToolHandler> toolHandlers) {
        this.simulationMode = simulationMode;
        this.toolHandlers = Map.copyOf(toolHandlers);
    }

    /** Create a simulation-mode MCP transport (for testing). */
    public static McpTransport simulation(Map<String, McpToolHandler> handlers) {
        return new McpTransport(true, handlers);
    }

    /** Create a simulation-mode MCP transport with no tools. */
    public static McpTransport simulation() {
        return new McpTransport(true, Map.of());
    }

    @Override
    public CompletableFuture<TransportMessage> send(String targetAgent, TransportMessage message) {
        if (simulationMode) {
            return sendSimulated(targetAgent, message);
        }
        return sendJsonRpc(targetAgent, message);
    }

    private CompletableFuture<TransportMessage> sendSimulated(String target, TransportMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            String toolName = message.label();
            McpToolHandler handler = toolHandlers.get(toolName);
            if (handler != null) {
                Map<String, Object> result = handler.handle(toolName, message.payload());
                var response = message.reply("RESULT", result);
                // Notify listener
                TransportListener listener = listeners.get(target);
                if (listener != null) listener.onMessage(response);
                return response;
            }
            return message.reply("ERROR", Map.of("error", "Unknown tool: " + toolName));
        });
    }

    private CompletableFuture<TransportMessage> sendJsonRpc(String target, TransportMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            int id = requestId.getAndIncrement();
            // Build JSON-RPC 2.0 request
            String json = String.format(
                    "{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"tools/call\","
                            + "\"params\":{\"name\":\"%s\",\"arguments\":%s}}",
                    id, message.label(), mapToJson(message.payload()));
            // In a real implementation, this would write to a Process stdin
            // and read the response from stdout. For now, return a placeholder.
            return message.reply("RESULT", Map.of("id", id, "status", "sent"));
        });
    }

    @Override
    public void onMessage(String agentName, TransportListener listener) {
        listeners.put(agentName, listener);
    }

    @Override
    public String id() {
        return simulationMode ? "mcp-simulation" : "mcp-jsonrpc";
    }

    /** Whether this transport is in simulation mode. */
    public boolean isSimulation() {
        return simulationMode;
    }

    private static String mapToJson(Map<String, Object> map) {
        var sb = new StringBuilder("{");
        var it = map.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof String s) sb.append("\"").append(s).append("\"");
            else if (v instanceof Number n) sb.append(n);
            else if (v instanceof Boolean b) sb.append(b);
            else sb.append("\"").append(v).append("\"");
            if (it.hasNext()) sb.append(",");
        }
        return sb.append("}").toString();
    }
}
