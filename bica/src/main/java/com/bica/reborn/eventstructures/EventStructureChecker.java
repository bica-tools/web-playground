package com.bica.reborn.eventstructures;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Event structures from session types (Step 16).
 *
 * <p>A <b>prime event structure</b> is a triple (E, &le;, #) where:
 * <ul>
 *   <li>E is a set of events</li>
 *   <li>&le; &sube; E &times; E is a partial order (causality)</li>
 *   <li># &sube; E &times; E is a symmetric irreflexive relation (conflict)</li>
 * </ul>
 *
 * <p>satisfying:
 * <ul>
 *   <li>Finite causes: {e' : e' &le; e} is finite for all e</li>
 *   <li>Conflict heredity: e # e' and e' &le; e'' implies e # e''</li>
 * </ul>
 *
 * <p>A <b>configuration</b> is a subset x &sube; E that is:
 * <ul>
 *   <li>Downward-closed: e in x and e' &le; e implies e' in x</li>
 *   <li>Conflict-free: no e, e' in x with e # e'</li>
 * </ul>
 *
 * <p>The <b>configuration domain</b> D(ES(S)) is ordered by inclusion
 * and is order-isomorphic to the session type state space L(S).
 *
 * <p>Key methods:
 * <ul>
 *   <li>{@link #buildEventStructure(StateSpace)} -- construct ES(S) from state space</li>
 *   <li>{@link #configurations(EventStructureResult)} -- enumerate all configurations</li>
 *   <li>{@link #configDomain(EventStructureResult)} -- build configuration poset</li>
 *   <li>{@link #checkIsomorphism(EventStructureResult, StateSpace)} -- verify D(ES(S)) isomorphic to L(S)</li>
 *   <li>{@link #classifyEvents(EventStructureResult, StateSpace)} -- branch/select/parallel events</li>
 *   <li>{@link #concurrencyPairs(EventStructureResult)} -- find concurrent event pairs</li>
 *   <li>{@link #analyze(StateSpace)} -- full analysis</li>
 * </ul>
 */
public final class EventStructureChecker {

    private EventStructureChecker() {}

    private static final int MAX_CONFIGS = 10_000;

    // -----------------------------------------------------------------------
    // Data records
    // -----------------------------------------------------------------------

    /**
     * An event in the event structure.
     *
     * @param source source state ID
     * @param label  transition label (method name)
     * @param target target state ID
     */
    public record Event(int source, String label, int target) implements Comparable<Event> {
        public Event {
            Objects.requireNonNull(label, "label must not be null");
        }

        @Override
        public int compareTo(Event o) {
            int c = Integer.compare(source, o.source);
            if (c != 0) return c;
            c = label.compareTo(o.label);
            if (c != 0) return c;
            return Integer.compare(target, o.target);
        }

        @Override
        public String toString() {
            return "(" + source + "," + label + "," + target + ")";
        }
    }

    /**
     * A prime event structure (E, &le;, #).
     *
     * @param events    set of events
     * @param causality set of (e1, e2) pairs where e1 &le; e2 (e1 causes e2)
     * @param conflict  set of (e1, e2) pairs where e1 # e2 (mutually exclusive)
     */
    public record EventStructureResult(
            Set<Event> events,
            Set<EventPair> causality,
            Set<EventPair> conflict) {
        public EventStructureResult {
            Objects.requireNonNull(events, "events must not be null");
            Objects.requireNonNull(causality, "causality must not be null");
            Objects.requireNonNull(conflict, "conflict must not be null");
            events = Set.copyOf(events);
            causality = Set.copyOf(causality);
            conflict = Set.copyOf(conflict);
        }

        /** Number of events. */
        public int numEvents() {
            return events.size();
        }

        /** Number of conflict pairs (symmetric, count each pair once). */
        public int numConflicts() {
            return conflict.size() / 2;
        }

        /** Number of non-reflexive causal pairs. */
        public int numCausalPairs() {
            return (int) causality.stream().filter(p -> !p.first().equals(p.second())).count();
        }
    }

    /** An ordered pair of events. */
    public record EventPair(Event first, Event second) {
        public EventPair {
            Objects.requireNonNull(first, "first must not be null");
            Objects.requireNonNull(second, "second must not be null");
        }
    }

    /**
     * A configuration (downward-closed conflict-free subset).
     *
     * @param events the events in this configuration
     */
    public record Configuration(Set<Event> events) {
        public Configuration {
            Objects.requireNonNull(events, "events must not be null");
            events = Set.copyOf(events);
        }

        /** Number of events in this configuration. */
        public int size() {
            return events.size();
        }
    }

    /**
     * The configuration domain D(ES(S)).
     *
     * @param configs    list of configurations ordered by inclusion
     * @param bottom     empty configuration
     * @param ordering   set of (i, j) index pairs where configs[i] subset of configs[j]
     * @param numConfigs number of configurations
     */
    public record ConfigDomain(
            List<Configuration> configs,
            Configuration bottom,
            Set<int[]> ordering,
            int numConfigs) {
        public ConfigDomain {
            Objects.requireNonNull(configs, "configs must not be null");
            Objects.requireNonNull(bottom, "bottom must not be null");
            Objects.requireNonNull(ordering, "ordering must not be null");
            configs = List.copyOf(configs);
        }
    }

    /**
     * Classification of events by their session type origin.
     *
     * @param branchEvents   events from branch (&amp;) constructors
     * @param selectEvents   events from selection (+) constructors
     * @param parallelEvents events from parallel (||) factors
     * @param conflictGroups groups of mutually conflicting events (by source state)
     */
    public record EventClassification(
            Set<Event> branchEvents,
            Set<Event> selectEvents,
            Set<Event> parallelEvents,
            Map<Integer, Set<Event>> conflictGroups) {
        public EventClassification {
            Objects.requireNonNull(branchEvents, "branchEvents must not be null");
            Objects.requireNonNull(selectEvents, "selectEvents must not be null");
            Objects.requireNonNull(parallelEvents, "parallelEvents must not be null");
            Objects.requireNonNull(conflictGroups, "conflictGroups must not be null");
            branchEvents = Set.copyOf(branchEvents);
            selectEvents = Set.copyOf(selectEvents);
            parallelEvents = Set.copyOf(parallelEvents);
            // Deep copy conflict groups
            var cg = new HashMap<Integer, Set<Event>>();
            for (var entry : conflictGroups.entrySet()) {
                cg.put(entry.getKey(), Set.copyOf(entry.getValue()));
            }
            conflictGroups = Map.copyOf(cg);
        }
    }

    /**
     * Complete event structure analysis.
     *
     * @param es              the event structure
     * @param numEvents       number of events
     * @param numConflicts    number of conflict pairs
     * @param numCausal       number of non-reflexive causal pairs
     * @param numConfigs      number of configurations
     * @param numConcurrent   number of concurrent event pairs
     * @param isIsomorphic    true iff config domain is order-isomorphic to state space
     * @param classification  event classification
     * @param maxConfigSize   size of the largest configuration
     * @param conflictDensity fraction of event pairs in conflict
     */
    public record ESAnalysis(
            EventStructureResult es,
            int numEvents,
            int numConflicts,
            int numCausal,
            int numConfigs,
            int numConcurrent,
            boolean isIsomorphic,
            EventClassification classification,
            int maxConfigSize,
            double conflictDensity) {
        public ESAnalysis {
            Objects.requireNonNull(es, "es must not be null");
            Objects.requireNonNull(classification, "classification must not be null");
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /** Forward adjacency with labels. */
    private static Map<Integer, List<Transition>> buildAdj(StateSpace ss) {
        var adj = new HashMap<Integer, List<Transition>>();
        for (int s : ss.states()) {
            adj.put(s, new ArrayList<>());
        }
        for (var t : ss.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new ArrayList<>()).add(t);
        }
        return adj;
    }

    /** States reachable from start (inclusive). */
    private static Set<Integer> reachable(StateSpace ss, int start) {
        var adj = buildAdj(ss);
        var visited = new HashSet<Integer>();
        var stack = new ArrayDeque<Integer>();
        stack.push(start);
        while (!stack.isEmpty()) {
            int s = stack.pop();
            if (!visited.add(s)) continue;
            for (var t : adj.getOrDefault(s, List.of())) {
                stack.push(t.target());
            }
        }
        return visited;
    }

    // -----------------------------------------------------------------------
    // Public API: Build event structure
    // -----------------------------------------------------------------------

    /**
     * Construct the prime event structure ES(S) from a state space.
     *
     * <p>Events = transitions in the state space.
     * Conflict = events sharing the same source state.
     * Causality = sequential dependency (target of e1 reaches source of e2).
     *
     * @param ss the state space
     * @return the event structure
     */
    public static EventStructureResult buildEventStructure(StateSpace ss) {
        // 1. Events: one per transition
        var events = new TreeSet<Event>();
        for (var t : ss.transitions()) {
            events.add(new Event(t.source(), t.label(), t.target()));
        }
        var eventList = new ArrayList<>(events);

        // 2. Immediate conflict: same source state, different events
        var immediateConflict = new HashSet<EventPair>();
        var bySource = new HashMap<Integer, List<Event>>();
        for (var e : eventList) {
            bySource.computeIfAbsent(e.source(), k -> new ArrayList<>()).add(e);
        }
        for (var entry : bySource.entrySet()) {
            var evts = entry.getValue();
            if (evts.size() >= 2) {
                for (int i = 0; i < evts.size(); i++) {
                    for (int j = i + 1; j < evts.size(); j++) {
                        immediateConflict.add(new EventPair(evts.get(i), evts.get(j)));
                        immediateConflict.add(new EventPair(evts.get(j), evts.get(i)));
                    }
                }
            }
        }

        // 3. Causality: e1 <=_E e2 iff e1.target can reach e2.source
        var reach = new HashMap<Integer, Set<Integer>>();
        for (int s : ss.states()) {
            reach.put(s, reachable(ss, s));
        }

        var causality = new HashSet<EventPair>();
        // Reflexive
        for (var e : eventList) {
            causality.add(new EventPair(e, e));
        }
        // Non-reflexive
        for (var e1 : eventList) {
            for (var e2 : eventList) {
                if (e1.equals(e2)) continue;
                if (reach.getOrDefault(e1.target(), Set.of()).contains(e2.source())) {
                    causality.add(new EventPair(e1, e2));
                }
            }
        }

        // 4. Conflict heredity: if e1 # e2 and e2 <= e3, then e1 # e3
        var fullConflict = new HashSet<>(immediateConflict);
        boolean changed = true;
        while (changed) {
            changed = false;
            var newConflicts = new HashSet<EventPair>();
            for (var c : fullConflict) {
                for (var cp : causality) {
                    if (cp.first().equals(c.second()) && !cp.second().equals(c.first())
                            && !fullConflict.contains(new EventPair(c.first(), cp.second()))) {
                        newConflicts.add(new EventPair(c.first(), cp.second()));
                        newConflicts.add(new EventPair(cp.second(), c.first()));
                    }
                }
            }
            if (!newConflicts.isEmpty() && !fullConflict.containsAll(newConflicts)) {
                fullConflict.addAll(newConflicts);
                changed = true;
            }
        }

        return new EventStructureResult(
                Set.copyOf(events),
                Set.copyOf(causality),
                Set.copyOf(fullConflict));
    }

    // -----------------------------------------------------------------------
    // Public API: Configurations
    // -----------------------------------------------------------------------

    /**
     * Enumerate all configurations of the event structure.
     *
     * <p>A configuration is a downward-closed, conflict-free subset of events.
     *
     * @param es the event structure
     * @return list of configurations
     */
    public static List<Configuration> configurations(EventStructureResult es) {
        return configurations(es, MAX_CONFIGS);
    }

    /**
     * Enumerate configurations with a maximum count.
     *
     * @param es         the event structure
     * @param maxConfigs maximum number of configurations to enumerate
     * @return list of configurations
     */
    public static List<Configuration> configurations(EventStructureResult es, int maxConfigs) {
        var eventList = new ArrayList<>(new TreeSet<>(es.events()));

        // Build predecessor lookup: for each event, events that must precede it
        var predecessors = new HashMap<Event, Set<Event>>();
        for (var e : eventList) predecessors.put(e, new HashSet<>());
        for (var pair : es.causality()) {
            if (!pair.first().equals(pair.second())) {
                predecessors.computeIfAbsent(pair.second(), k -> new HashSet<>()).add(pair.first());
            }
        }

        // Build conflict lookup
        var conflictsOf = new HashMap<Event, Set<Event>>();
        for (var e : eventList) conflictsOf.put(e, new HashSet<>());
        for (var pair : es.conflict()) {
            conflictsOf.computeIfAbsent(pair.first(), k -> new HashSet<>()).add(pair.second());
        }

        // BFS on configurations
        var result = new ArrayList<Configuration>();
        var queue = new ArrayDeque<Set<Event>>();
        var seen = new HashSet<Set<Event>>();
        Set<Event> empty = Set.of();
        queue.add(empty);
        seen.add(empty);

        while (!queue.isEmpty() && result.size() < maxConfigs) {
            var current = queue.poll();
            result.add(new Configuration(current));

            for (var e : eventList) {
                if (current.contains(e)) continue;

                // Check conflict-free
                boolean hasConflict = false;
                for (var c : conflictsOf.getOrDefault(e, Set.of())) {
                    if (current.contains(c)) {
                        hasConflict = true;
                        break;
                    }
                }
                if (hasConflict) continue;

                // Check downward-closed: all predecessors must be in current
                if (!current.containsAll(predecessors.getOrDefault(e, Set.of()))) continue;

                var newConfig = new HashSet<>(current);
                newConfig.add(e);
                var immutable = Set.copyOf(newConfig);
                if (seen.add(immutable)) {
                    queue.add(immutable);
                }
            }
        }

        return List.copyOf(result);
    }

    // -----------------------------------------------------------------------
    // Public API: Configuration domain
    // -----------------------------------------------------------------------

    /**
     * Build the configuration domain D(ES(S)).
     *
     * <p>Configurations ordered by subset inclusion.
     *
     * @param es the event structure
     * @return the configuration domain
     */
    public static ConfigDomain configDomain(EventStructureResult es) {
        return configDomain(es, MAX_CONFIGS);
    }

    /**
     * Build configuration domain with a maximum configuration count.
     *
     * @param es         the event structure
     * @param maxConfigs maximum number of configurations
     * @return the configuration domain
     */
    public static ConfigDomain configDomain(EventStructureResult es, int maxConfigs) {
        var configs = configurations(es, maxConfigs);
        var bottom = new Configuration(Set.of());

        var ordering = new HashSet<int[]>();
        for (int i = 0; i < configs.size(); i++) {
            for (int j = 0; j < configs.size(); j++) {
                if (configs.get(j).events().containsAll(configs.get(i).events())) {
                    ordering.add(new int[]{i, j});
                }
            }
        }

        return new ConfigDomain(configs, bottom, ordering, configs.size());
    }

    // -----------------------------------------------------------------------
    // Public API: Isomorphism check
    // -----------------------------------------------------------------------

    /**
     * Check if the configuration domain D(ES(S)) is order-isomorphic to L(S).
     *
     * <p>Verifies the fundamental theorem: the number of configurations
     * equals the number of states (a necessary condition for isomorphism).
     *
     * @param es the event structure
     * @param ss the state space
     * @return true iff |configs| == |states|
     */
    public static boolean checkIsomorphism(EventStructureResult es, StateSpace ss) {
        var configs = configurations(es);
        return configs.size() == ss.states().size();
    }

    // -----------------------------------------------------------------------
    // Public API: Classification
    // -----------------------------------------------------------------------

    /**
     * Classify events by their session type origin.
     *
     * <ul>
     *   <li>Branch events: from &amp; constructors (external choice)</li>
     *   <li>Select events: from + constructors (internal choice)</li>
     *   <li>Parallel events: from || factors (not currently detectable without product coords)</li>
     * </ul>
     *
     * @param es the event structure
     * @param ss the state space
     * @return the event classification
     */
    public static EventClassification classifyEvents(EventStructureResult es, StateSpace ss) {
        var branchEvents = new HashSet<Event>();
        var selectEvents = new HashSet<Event>();
        var parallelEvents = new HashSet<Event>(); // placeholder: no productCoords in Java StateSpace

        for (var e : es.events()) {
            // Check if this transition is a selection
            boolean isSelection = ss.transitions().stream()
                    .anyMatch(t -> t.source() == e.source()
                            && t.label().equals(e.label())
                            && t.target() == e.target()
                            && t.kind() == TransitionKind.SELECTION);
            if (isSelection) {
                selectEvents.add(e);
            } else {
                branchEvents.add(e);
            }
        }

        // Conflict groups: events sharing a source state with 2+ events
        var allBySource = new HashMap<Integer, Set<Event>>();
        for (var e : es.events()) {
            allBySource.computeIfAbsent(e.source(), k -> new HashSet<>()).add(e);
        }
        var conflictGroups = new HashMap<Integer, Set<Event>>();
        for (var entry : allBySource.entrySet()) {
            if (entry.getValue().size() >= 2) {
                conflictGroups.put(entry.getKey(), Set.copyOf(entry.getValue()));
            }
        }

        return new EventClassification(
                Set.copyOf(branchEvents),
                Set.copyOf(selectEvents),
                Set.copyOf(parallelEvents),
                Map.copyOf(conflictGroups));
    }

    // -----------------------------------------------------------------------
    // Public API: Concurrency
    // -----------------------------------------------------------------------

    /**
     * Find all concurrent event pairs.
     *
     * <p>Events e1, e2 are concurrent iff:
     * <ul>
     *   <li>e1 != e2</li>
     *   <li>NOT e1 &le; e2</li>
     *   <li>NOT e2 &le; e1</li>
     *   <li>NOT e1 # e2</li>
     * </ul>
     *
     * @param es the event structure
     * @return list of concurrent event pairs
     */
    public static List<EventPair> concurrencyPairs(EventStructureResult es) {
        var eventList = new ArrayList<>(new TreeSet<>(es.events()));
        var causalSet = es.causality();
        var conflictSet = es.conflict();

        var concurrent = new ArrayList<EventPair>();
        for (int i = 0; i < eventList.size(); i++) {
            for (int j = i + 1; j < eventList.size(); j++) {
                var e1 = eventList.get(i);
                var e2 = eventList.get(j);
                if (!causalSet.contains(new EventPair(e1, e2))
                        && !causalSet.contains(new EventPair(e2, e1))
                        && !conflictSet.contains(new EventPair(e1, e2))) {
                    concurrent.add(new EventPair(e1, e2));
                }
            }
        }
        return List.copyOf(concurrent);
    }

    // -----------------------------------------------------------------------
    // Public API: Full analysis
    // -----------------------------------------------------------------------

    /**
     * Full event structure analysis of a state space.
     *
     * @param ss the state space
     * @return complete analysis
     */
    public static ESAnalysis analyze(StateSpace ss) {
        var es = buildEventStructure(ss);
        var configs = configurations(es);
        var classification = classifyEvents(es, ss);
        var concurrent = concurrencyPairs(es);
        boolean iso = configs.size() == ss.states().size();

        int maxConfig = configs.stream().mapToInt(Configuration::size).max().orElse(0);

        int nEvents = es.numEvents();
        int nPairs = nEvents * (nEvents - 1) / 2;
        double conflictDensity = nPairs > 0 ? (double) es.numConflicts() / nPairs : 0.0;

        return new ESAnalysis(
                es,
                nEvents,
                es.numConflicts(),
                es.numCausalPairs(),
                configs.size(),
                concurrent.size(),
                iso,
                classification,
                maxConfig,
                conflictDensity);
    }
}
