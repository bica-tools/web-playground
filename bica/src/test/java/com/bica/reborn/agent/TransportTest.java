package com.bica.reborn.agent;

import com.bica.reborn.agent.transport.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the transport layer: InProcess, MCP, and A2A.
 */
class TransportTest {

    // ======================================================================
    // TransportMessage
    // ======================================================================

    @Nested
    class MessageTests {

        @Test
        void createMessage() {
            var msg = TransportMessage.of("sender", "receiver", "callTool", Map.of("tool", "analyze"));
            assertEquals("sender", msg.fromAgent());
            assertEquals("receiver", msg.toAgent());
            assertEquals("callTool", msg.label());
            assertEquals("analyze", msg.payload().get("tool"));
            assertTrue(msg.timestampMs() > 0);
        }

        @Test
        void replySwapsSenderReceiver() {
            var msg = TransportMessage.of("A", "B", "request", Map.of());
            var reply = msg.reply("response", Map.of("data", "ok"));
            assertEquals("B", reply.fromAgent());
            assertEquals("A", reply.toAgent());
            assertEquals("response", reply.label());
        }

        @Test
        void payloadIsImmutable() {
            var payload = new java.util.HashMap<String, Object>();
            payload.put("key", "value");
            var msg = TransportMessage.of("A", "B", "m", payload);
            assertThrows(UnsupportedOperationException.class, () -> msg.payload().put("x", "y"));
        }
    }

    // ======================================================================
    // InProcessTransport
    // ======================================================================

    @Nested
    class InProcessTests {

        @Test
        void sendAndReceive() throws Exception {
            var transport = new InProcessTransport();
            var received = new ArrayList<TransportMessage>();

            transport.onMessage("agent1", received::add);
            assertEquals(1, transport.listenerCount());

            var msg = TransportMessage.of("caller", "agent1", "work", Map.of("task", "analyze"));
            var future = transport.send("agent1", msg);
            future.get(); // wait for delivery

            assertEquals(1, received.size());
            assertEquals("work", received.getFirst().label());
        }

        @Test
        void sendToUnregisteredFails() {
            var transport = new InProcessTransport();
            var msg = TransportMessage.of("A", "ghost", "m", Map.of());
            var future = transport.send("ghost", msg);
            assertThrows(ExecutionException.class, future::get);
        }

        @Test
        void transportId() {
            assertEquals("in-process", new InProcessTransport().id());
        }
    }

    // ======================================================================
    // McpTransport
    // ======================================================================

    @Nested
    class McpTests {

        @Test
        void simulationMode() {
            var mcp = McpTransport.simulation(Map.of(
                    "analyze", (tool, args) -> Map.of("result", "lattice=true", "states", 5)
            ));
            assertTrue(mcp.isSimulation());
            assertEquals("mcp-simulation", mcp.id());
        }

        @Test
        void callToolInSimulation() throws Exception {
            var mcp = McpTransport.simulation(Map.of(
                    "analyze", (tool, args) -> Map.of("isLattice", true)
            ));

            var msg = TransportMessage.of("host", "server", "analyze",
                    Map.of("type", "&{a: end}"));
            var response = mcp.send("server", msg).get();

            assertEquals("RESULT", response.label());
            assertEquals(true, response.payload().get("isLattice"));
        }

        @Test
        void unknownToolReturnsError() throws Exception {
            var mcp = McpTransport.simulation();
            var msg = TransportMessage.of("host", "server", "nonexistent", Map.of());
            var response = mcp.send("server", msg).get();
            assertEquals("ERROR", response.label());
        }

        @Test
        void listenerNotified() throws Exception {
            var received = new ArrayList<TransportMessage>();
            var mcp = McpTransport.simulation(Map.of(
                    "test", (tool, args) -> Map.of("ok", true)
            ));
            mcp.onMessage("server", received::add);

            var msg = TransportMessage.of("host", "server", "test", Map.of());
            mcp.send("server", msg).get();

            assertFalse(received.isEmpty());
        }
    }

    // ======================================================================
    // A2aTransport
    // ======================================================================

    @Nested
    class A2aTests {

        @Test
        void simulationMode() {
            var a2a = A2aTransport.simulation();
            assertTrue(a2a.isSimulation());
            assertEquals("a2a-simulation", a2a.id());
        }

        @Test
        void sendTaskCompletes() throws Exception {
            var a2a = A2aTransport.simulation();
            a2a.registerHandler("worker", (taskId, input) ->
                    Map.of("output", "processed-" + input.get("data")));

            var msg = TransportMessage.of("orchestrator", "worker", "process",
                    Map.of("data", "step23"));
            var response = a2a.send("worker", msg).get();

            assertEquals("COMPLETED", response.label());
            assertEquals("processed-step23", response.payload().get("output"));

            a2a.shutdown();
        }

        @Test
        void taskLifecycleTracked() throws Exception {
            var a2a = A2aTransport.simulation();
            a2a.registerHandler("agent", (taskId, input) -> Map.of("done", true));

            var msg = TransportMessage.of("caller", "agent", "work", Map.of());
            a2a.send("agent", msg).get();

            // At least one task should exist
            assertFalse(a2a.allTasks().isEmpty());
            var task = a2a.allTasks().values().iterator().next();
            assertEquals(A2aTransport.TaskStatus.COMPLETED, task.status());

            a2a.shutdown();
        }

        @Test
        void noHandlerReturnsFailed() throws Exception {
            var a2a = A2aTransport.simulation();
            // No handler registered for "ghost"

            var msg = TransportMessage.of("caller", "ghost", "work", Map.of());
            var response = a2a.send("ghost", msg).get();
            assertEquals("FAILED", response.label());

            a2a.shutdown();
        }

        @Test
        void handlerExceptionReturnsFailed() throws Exception {
            var a2a = A2aTransport.simulation();
            a2a.registerHandler("failing", (taskId, input) -> {
                throw new RuntimeException("boom");
            });

            var msg = TransportMessage.of("caller", "failing", "work", Map.of());
            var response = a2a.send("failing", msg).get();
            assertEquals("FAILED", response.label());
            assertEquals("boom", response.payload().get("error"));

            a2a.shutdown();
        }

        @Test
        void listenerReceivesUpdates() throws Exception {
            var received = new ArrayList<TransportMessage>();
            var a2a = A2aTransport.simulation();
            a2a.registerHandler("worker", (taskId, input) -> Map.of("result", "ok"));
            a2a.onMessage("worker", received::add);

            var msg = TransportMessage.of("caller", "worker", "task", Map.of());
            a2a.send("worker", msg).get();

            // Listener should have received WORKING and COMPLETED notifications
            assertTrue(received.size() >= 1);

            a2a.shutdown();
        }
    }
}
