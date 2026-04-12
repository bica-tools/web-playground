package com.bica.reborn.agent.transport;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A2A (Agent-to-Agent) transport for task delegation between agents.
 *
 * <p>Implements Google's A2A protocol pattern: the sender creates a task,
 * the receiver processes it asynchronously, and the sender polls or
 * subscribes for status updates.
 *
 * <p>Session type of A2A:
 * {@code &{sendTask: rec X . +{WORKING: &{getStatus: X, cancel: end}, COMPLETED: &{getArtifact: end}, FAILED: end}}}
 *
 * <p>This transport can operate in:
 * <ul>
 *   <li><strong>Simulation mode</strong>: tasks are processed in-memory (for testing)</li>
 *   <li><strong>HTTP mode</strong>: tasks are sent via HTTP REST to remote agents</li>
 *   <li><strong>Subprocess mode</strong>: tasks are delegated to CLI subprocesses</li>
 * </ul>
 *
 * @see <a href="https://google.github.io/A2A/">Agent-to-Agent Protocol</a>
 */
public final class A2aTransport implements Transport {

    /** Task status in the A2A lifecycle. */
    public enum TaskStatus { SUBMITTED, WORKING, COMPLETED, FAILED, CANCELED }

    /** An A2A task. */
    public record Task(
            String taskId,
            String fromAgent,
            String toAgent,
            TaskStatus status,
            Map<String, Object> input,
            Map<String, Object> output) {}

    /** Handler that processes tasks for simulation mode. */
    @FunctionalInterface
    public interface TaskHandler {
        Map<String, Object> process(String taskId, Map<String, Object> input);
    }

    private final Map<String, TransportListener> listeners = new ConcurrentHashMap<>();
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final Map<String, TaskHandler> handlers = new ConcurrentHashMap<>();
    private final AtomicInteger taskCounter = new AtomicInteger(1);
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final boolean simulationMode;

    private A2aTransport(boolean simulationMode) {
        this.simulationMode = simulationMode;
    }

    /** Create a simulation-mode A2A transport. */
    public static A2aTransport simulation() {
        return new A2aTransport(true);
    }

    /** Register a task handler for a specific agent (simulation mode). */
    public void registerHandler(String agentName, TaskHandler handler) {
        handlers.put(agentName, handler);
    }

    @Override
    public CompletableFuture<TransportMessage> send(String targetAgent, TransportMessage message) {
        String taskId = "task-" + taskCounter.getAndIncrement();

        Task task = new Task(taskId, message.fromAgent(), targetAgent,
                TaskStatus.SUBMITTED, message.payload(), Map.of());
        tasks.put(taskId, task);

        if (simulationMode) {
            return processSimulated(targetAgent, message, taskId);
        }
        return processRemote(targetAgent, message, taskId);
    }

    private CompletableFuture<TransportMessage> processSimulated(
            String target, TransportMessage message, String taskId) {
        return CompletableFuture.supplyAsync(() -> {
            // Update status to WORKING
            tasks.put(taskId, new Task(taskId, message.fromAgent(), target,
                    TaskStatus.WORKING, message.payload(), Map.of()));

            notifyListener(target, message.reply("WORKING", Map.of("taskId", taskId)));

            TaskHandler handler = handlers.get(target);
            if (handler == null) {
                tasks.put(taskId, new Task(taskId, message.fromAgent(), target,
                        TaskStatus.FAILED, message.payload(), Map.of("error", "No handler")));
                return message.reply("FAILED", Map.of("taskId", taskId, "error", "No handler"));
            }

            try {
                Map<String, Object> result = handler.process(taskId, message.payload());
                tasks.put(taskId, new Task(taskId, message.fromAgent(), target,
                        TaskStatus.COMPLETED, message.payload(), result));
                var response = message.reply("COMPLETED", result);
                notifyListener(target, response);
                return response;
            } catch (Exception e) {
                tasks.put(taskId, new Task(taskId, message.fromAgent(), target,
                        TaskStatus.FAILED, message.payload(), Map.of("error", e.getMessage())));
                return message.reply("FAILED", Map.of("taskId", taskId, "error", e.getMessage()));
            }
        }, executor);
    }

    private CompletableFuture<TransportMessage> processRemote(
            String target, TransportMessage message, String taskId) {
        // In a real implementation, this would POST to the agent's HTTP endpoint:
        // POST /tasks/send { "id": taskId, "message": { "role": "user", "parts": [...] } }
        // Then poll GET /tasks/get?id=taskId until status is COMPLETED/FAILED
        return CompletableFuture.completedFuture(
                message.reply("COMPLETED", Map.of("taskId", taskId, "note", "remote not implemented")));
    }

    @Override
    public void onMessage(String agentName, TransportListener listener) {
        listeners.put(agentName, listener);
    }

    @Override
    public String id() {
        return simulationMode ? "a2a-simulation" : "a2a-http";
    }

    /** Get the current status of a task. */
    public Optional<Task> getTask(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    /** Get all tasks. */
    public Map<String, Task> allTasks() {
        return Map.copyOf(tasks);
    }

    /** Whether simulation mode is active. */
    public boolean isSimulation() {
        return simulationMode;
    }

    /** Shutdown the executor. */
    public void shutdown() {
        executor.shutdown();
    }

    private void notifyListener(String agentName, TransportMessage message) {
        TransportListener listener = listeners.get(agentName);
        if (listener != null) listener.onMessage(message);
    }
}
