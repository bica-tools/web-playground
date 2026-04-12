package com.bica.reborn.channel;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Bounded-buffer channel semantics (Step 158 port).
 *
 * <p>Models single-endpoint buffered channels where messages are enqueued into
 * a FIFO buffer of bounded capacity before being consumed. Selections enqueue,
 * branches dequeue.
 */
public final class BufferedChannelChecker {

    private BufferedChannelChecker() {}

    /**
     * Buffered state key: (protocol_state, buffer_contents).
     */
    record BufferedStateKey(int protoState, List<String> buffer) {
        BufferedStateKey {
            buffer = List.copyOf(buffer);
        }
    }

    // =========================================================================
    // Build buffered state space
    // =========================================================================

    /**
     * Build a buffered channel state space via BFS.
     *
     * @param ast      Session type AST.
     * @param capacity Maximum buffer size K (>= 0). K=0 means synchronous.
     * @return StateSpace with buffered states.
     */
    public static StateSpace buildBufferedChannel(SessionType ast, int capacity) {
        StateSpace baseSS = StateSpaceBuilder.build(ast);
        return buildBufferedFromSS(baseSS, capacity);
    }

    static StateSpace buildBufferedFromSS(StateSpace baseSS, int capacity) {
        Map<Integer, List<StateSpace.Transition>> adj = new HashMap<>();
        for (int s : baseSS.states()) adj.put(s, new ArrayList<>());
        for (var t : baseSS.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new ArrayList<>()).add(t);
        }

        Map<BufferedStateKey, Integer> keyToId = new LinkedHashMap<>();
        Map<Integer, String> idLabels = new HashMap<>();
        List<StateSpace.Transition> transitions = new ArrayList<>();
        int[] nextId = {0};

        java.util.function.Function<BufferedStateKey, Integer> getId = key -> {
            if (!keyToId.containsKey(key)) {
                int sid = nextId[0]++;
                keyToId.put(key, sid);
                String baseLabel = baseSS.labels().getOrDefault(key.protoState(), String.valueOf(key.protoState()));
                String bufStr = key.buffer().isEmpty() ? "[]" : "[" + String.join(",", key.buffer()) + "]";
                idLabels.put(sid, "(" + baseLabel + ", " + bufStr + ")");
            }
            return keyToId.get(key);
        };

        BufferedStateKey start = new BufferedStateKey(baseSS.top(), List.of());
        getId.apply(start);
        Deque<BufferedStateKey> queue = new ArrayDeque<>();
        queue.add(start);
        Set<BufferedStateKey> visited = new HashSet<>();

        while (!queue.isEmpty()) {
            BufferedStateKey key = queue.poll();
            if (!visited.add(key)) continue;
            int src = keyToId.get(key);

            for (var t : adj.getOrDefault(key.protoState(), List.of())) {
                boolean isSel = t.kind() == TransitionKind.SELECTION;
                if (isSel) {
                    if (key.buffer().size() < capacity) {
                        List<String> newBuf = new ArrayList<>(key.buffer());
                        newBuf.add(t.label());
                        BufferedStateKey newKey = new BufferedStateKey(t.target(), newBuf);
                        int tgtId = getId.apply(newKey);
                        transitions.add(new StateSpace.Transition(src, "enqueue:" + t.label(), tgtId, TransitionKind.SELECTION));
                        if (!visited.contains(newKey)) queue.add(newKey);
                    } else if (capacity == 0) {
                        BufferedStateKey newKey = new BufferedStateKey(t.target(), key.buffer());
                        int tgtId = getId.apply(newKey);
                        transitions.add(new StateSpace.Transition(src, t.label(), tgtId, TransitionKind.SELECTION));
                        if (!visited.contains(newKey)) queue.add(newKey);
                    }
                } else {
                    if (!key.buffer().isEmpty() && key.buffer().getFirst().equals(t.label())) {
                        List<String> newBuf = key.buffer().subList(1, key.buffer().size());
                        BufferedStateKey newKey = new BufferedStateKey(t.target(), newBuf);
                        int tgtId = getId.apply(newKey);
                        transitions.add(new StateSpace.Transition(src, "dequeue:" + t.label(), tgtId, TransitionKind.METHOD));
                        if (!visited.contains(newKey)) queue.add(newKey);
                    } else if (key.buffer().isEmpty() && capacity == 0) {
                        BufferedStateKey newKey = new BufferedStateKey(t.target(), key.buffer());
                        int tgtId = getId.apply(newKey);
                        transitions.add(new StateSpace.Transition(src, t.label(), tgtId, TransitionKind.METHOD));
                        if (!visited.contains(newKey)) queue.add(newKey);
                    }
                }
            }
        }

        int top = keyToId.get(start);
        BufferedStateKey bottomKey = new BufferedStateKey(baseSS.bottom(), List.of());
        int bottom = getId.apply(bottomKey);

        return new StateSpace(
                keyToId.values().stream().collect(Collectors.toUnmodifiableSet()),
                transitions, top, bottom, idLabels);
    }

    // =========================================================================
    // Buffer safety check
    // =========================================================================

    /**
     * Check buffer safety and compute analysis.
     *
     * @param ast      Session type AST.
     * @param capacity Buffer bound K.
     * @return BufferedChannelResult with safety analysis.
     */
    public static BufferedChannelResult checkBufferBounds(SessionType ast, int capacity) {
        StateSpace baseSS = StateSpaceBuilder.build(ast);
        StateSpace bufferedSS = buildBufferedFromSS(baseSS, capacity);
        LatticeResult lr = LatticeChecker.checkLattice(bufferedSS);

        // Classify base states
        Map<Integer, List<StateSpace.Transition>> baseAdj = new HashMap<>();
        for (var t : baseSS.transitions()) {
            baseAdj.computeIfAbsent(t.source(), k -> new ArrayList<>()).add(t);
        }

        // Rebuild key mapping for analysis
        Map<BufferedStateKey, Integer> keyToId = rebuildKeyMapping(baseSS, capacity);

        int overflowCount = 0;
        int underflowCount = 0;
        Map<Integer, Integer> bufDist = new HashMap<>();
        int maxOcc = 0;

        for (var entry : keyToId.entrySet()) {
            BufferedStateKey key = entry.getKey();
            int bufLen = key.buffer().size();
            bufDist.merge(bufLen, 1, Integer::sum);
            maxOcc = Math.max(maxOcc, bufLen);

            if (capacity > 0) {
                boolean hasSel = baseAdj.getOrDefault(key.protoState(), List.of()).stream()
                        .anyMatch(t -> t.kind() == TransitionKind.SELECTION);
                if (hasSel && bufLen >= capacity) overflowCount++;

                boolean hasBranch = baseAdj.getOrDefault(key.protoState(), List.of()).stream()
                        .anyMatch(t -> t.kind() == TransitionKind.METHOD);
                if (hasBranch && bufLen == 0) underflowCount++;
            }
        }

        // Deadlock detection
        Map<Integer, Integer> outDeg = new HashMap<>();
        for (int s : bufferedSS.states()) outDeg.put(s, 0);
        for (var t : bufferedSS.transitions()) outDeg.merge(t.source(), 1, Integer::sum);
        int deadlocks = 0;
        for (var e : outDeg.entrySet()) {
            if (e.getValue() == 0 && e.getKey() != bufferedSS.bottom()) deadlocks++;
        }

        return new BufferedChannelResult(
                PrettyPrinter.pretty(ast),
                capacity,
                baseSS.states().size(),
                bufferedSS.states().size(),
                bufferedSS.transitions().size(),
                lr.isLattice(),
                overflowCount == 0 && underflowCount == 0,
                overflowCount,
                underflowCount,
                maxOcc,
                bufDist,
                deadlocks,
                lr);
    }

    private static Map<BufferedStateKey, Integer> rebuildKeyMapping(StateSpace baseSS, int capacity) {
        Map<Integer, List<StateSpace.Transition>> adj = new HashMap<>();
        for (var t : baseSS.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new ArrayList<>()).add(t);
        }

        Map<BufferedStateKey, Integer> keyToId = new LinkedHashMap<>();
        int[] nextId = {0};

        java.util.function.Function<BufferedStateKey, Integer> getId = key -> {
            if (!keyToId.containsKey(key)) {
                keyToId.put(key, nextId[0]++);
            }
            return keyToId.get(key);
        };

        BufferedStateKey start = new BufferedStateKey(baseSS.top(), List.of());
        getId.apply(start);
        Deque<BufferedStateKey> queue = new ArrayDeque<>();
        queue.add(start);
        Set<BufferedStateKey> visited = new HashSet<>();

        while (!queue.isEmpty()) {
            BufferedStateKey key = queue.poll();
            if (!visited.add(key)) continue;

            for (var t : adj.getOrDefault(key.protoState(), List.of())) {
                boolean isSel = t.kind() == TransitionKind.SELECTION;
                if (isSel) {
                    if (key.buffer().size() < capacity) {
                        List<String> newBuf = new ArrayList<>(key.buffer());
                        newBuf.add(t.label());
                        BufferedStateKey nk = new BufferedStateKey(t.target(), newBuf);
                        getId.apply(nk);
                        if (!visited.contains(nk)) queue.add(nk);
                    } else if (capacity == 0) {
                        BufferedStateKey nk = new BufferedStateKey(t.target(), key.buffer());
                        getId.apply(nk);
                        if (!visited.contains(nk)) queue.add(nk);
                    }
                } else {
                    if (!key.buffer().isEmpty() && key.buffer().getFirst().equals(t.label())) {
                        BufferedStateKey nk = new BufferedStateKey(t.target(), key.buffer().subList(1, key.buffer().size()));
                        getId.apply(nk);
                        if (!visited.contains(nk)) queue.add(nk);
                    } else if (key.buffer().isEmpty() && capacity == 0) {
                        BufferedStateKey nk = new BufferedStateKey(t.target(), key.buffer());
                        getId.apply(nk);
                        if (!visited.contains(nk)) queue.add(nk);
                    }
                }
            }
        }

        BufferedStateKey bottomKey = new BufferedStateKey(baseSS.bottom(), List.of());
        getId.apply(bottomKey);

        return keyToId;
    }
}
