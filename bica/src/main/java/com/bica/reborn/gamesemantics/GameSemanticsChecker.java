package com.bica.reborn.gamesemantics;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Game-semantic interpretation of session types (Step 94).
 *
 * <p>Interprets session types as two-player games between Proponent (the process)
 * and Opponent (the environment/client).
 *
 * <p>Key correspondences:
 * <ul>
 *   <li>Branch states   -> Opponent moves (external choice: environment decides)</li>
 *   <li>Selection states -> Proponent moves (internal choice: process decides)</li>
 *   <li>Terminal state   -> game over (protocol completed)</li>
 *   <li>Winning = reaching the terminal state (deadlock-free implementation)</li>
 *   <li>Parallel composition -> product (tensor) of games</li>
 * </ul>
 */
public final class GameSemanticsChecker {

    private GameSemanticsChecker() {}

    // -----------------------------------------------------------------------
    // Enums
    // -----------------------------------------------------------------------

    /** The two players in a session game. */
    public enum Player {
        PROPONENT("P"),
        OPPONENT("O");

        private final String code;

        Player(String code) { this.code = code; }

        public String code() { return code; }
    }

    /** Who has a winning strategy. */
    public enum GameOutcome {
        PROPONENT_WINS("P_wins"),
        OPPONENT_WINS("O_wins"),
        DRAW("draw");

        private final String code;

        GameOutcome(String code) { this.code = code; }

        public String code() { return code; }
    }

    // -----------------------------------------------------------------------
    // Result records
    // -----------------------------------------------------------------------

    /**
     * A move in the game arena.
     *
     * @param source state the move originates from
     * @param label  transition label (method name / selection label)
     * @param target state the move leads to
     * @param player which player makes this move
     */
    public record Move(int source, String label, int target, Player player) {
        public Move {
            Objects.requireNonNull(label, "label must not be null");
            Objects.requireNonNull(player, "player must not be null");
        }
    }

    /**
     * Game arena derived from a session-type state space.
     *
     * @param positions set of game positions (= state-space states)
     * @param moves     list of all moves
     * @param initial   starting position (= state-space top)
     * @param terminal  set of terminal positions (at minimum, bottom)
     * @param polarity  mapping from position to the player who moves there
     */
    public record Arena(
            Set<Integer> positions,
            List<Move> moves,
            int initial,
            Set<Integer> terminal,
            Map<Integer, Player> polarity) {

        public Arena {
            Objects.requireNonNull(positions, "positions must not be null");
            Objects.requireNonNull(moves, "moves must not be null");
            Objects.requireNonNull(terminal, "terminal must not be null");
            Objects.requireNonNull(polarity, "polarity must not be null");
            positions = Set.copyOf(positions);
            moves = List.copyOf(moves);
            terminal = Set.copyOf(terminal);
            polarity = Map.copyOf(polarity);
        }

        /** Return all moves available from the given position. */
        public List<Move> movesFrom(int position) {
            return moves.stream()
                    .filter(m -> m.source() == position)
                    .collect(Collectors.toUnmodifiableList());
        }

        /** True if the position is terminal (no outgoing moves or is bottom). */
        public boolean isTerminal(int position) {
            return terminal.contains(position);
        }

        /** Positions where Opponent moves. */
        public Set<Integer> opponentPositions() {
            return polarity.entrySet().stream()
                    .filter(e -> e.getValue() == Player.OPPONENT)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toUnmodifiableSet());
        }

        /** Positions where Proponent moves. */
        public Set<Integer> proponentPositions() {
            return polarity.entrySet().stream()
                    .filter(e -> e.getValue() == Player.PROPONENT)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toUnmodifiableSet());
        }

        /**
         * Longest path from initial to any terminal position.
         * Uses DFS with cycle detection.
         */
        public int depth() {
            var memo = new HashMap<Integer, Integer>();
            return longestPath(initial, new HashSet<>(), memo);
        }

        private int longestPath(int pos, Set<Integer> onPath, Map<Integer, Integer> memo) {
            if (terminal.contains(pos)) return 0;
            if (memo.containsKey(pos) && !onPath.contains(pos)) return memo.get(pos);
            var moves = movesFrom(pos);
            if (moves.isEmpty()) return 0;
            int best = 0;
            onPath.add(pos);
            for (var m : moves) {
                if (onPath.contains(m.target())) continue; // skip cycles
                int val = 1 + longestPath(m.target(), onPath, memo);
                if (val > best) best = val;
            }
            onPath.remove(pos);
            memo.put(pos, best);
            return best;
        }
    }

    /**
     * A deterministic Proponent strategy.
     * Maps each reachable Proponent position to the chosen move.
     *
     * @param choices mapping from Proponent position to the Move chosen
     */
    public record Strategy(Map<Integer, Move> choices) {
        public Strategy {
            Objects.requireNonNull(choices, "choices must not be null");
            choices = Map.copyOf(choices);
        }

        /** Return the move prescribed at the position, or null if not a P-position. */
        public Move apply(int position) {
            return choices.getOrDefault(position, null);
        }

        /** Position to label chosen. */
        public Map<Integer, String> labels() {
            var result = new HashMap<Integer, String>();
            for (var e : choices.entrySet()) {
                result.put(e.getKey(), e.getValue().label());
            }
            return Collections.unmodifiableMap(result);
        }
    }

    /**
     * A deterministic Opponent counter-strategy.
     * Maps each reachable Opponent position to the chosen move.
     *
     * @param choices mapping from Opponent position to the Move chosen
     */
    public record CounterStrategy(Map<Integer, Move> choices) {
        public CounterStrategy {
            Objects.requireNonNull(choices, "choices must not be null");
            choices = Map.copyOf(choices);
        }

        /** Return the move prescribed at the position, or null. */
        public Move apply(int position) {
            return choices.getOrDefault(position, null);
        }
    }

    /**
     * Full game-semantic analysis of a session type.
     */
    public record GameAnalysis(
            Arena arena,
            GameOutcome outcome,
            int numProponentPositions,
            int numOpponentPositions,
            int numTerminalPositions,
            int numWinningStrategies,
            int numTotalStrategies,
            boolean isDeterminate,
            Strategy exampleWinningStrategy) {
        public GameAnalysis {
            Objects.requireNonNull(arena, "arena must not be null");
            Objects.requireNonNull(outcome, "outcome must not be null");
            // exampleWinningStrategy may be null
        }
    }

    // -----------------------------------------------------------------------
    // Arena construction
    // -----------------------------------------------------------------------

    /**
     * Construct a game arena from a session-type state space.
     * Branch transitions become Opponent moves, selection transitions become Proponent moves.
     */
    public static Arena buildArena(StateSpace ss) {
        var moves = new ArrayList<Move>();
        var polarity = new HashMap<Integer, Player>();

        for (var t : ss.transitions()) {
            Player player = (t.kind() == TransitionKind.SELECTION)
                    ? Player.PROPONENT
                    : Player.OPPONENT;
            moves.add(new Move(t.source(), t.label(), t.target(), player));
        }

        // Determine polarity for each non-terminal position
        for (int state : ss.states()) {
            var outgoing = ss.transitionsFrom(state);
            if (outgoing.isEmpty()) continue; // terminal

            boolean allSel = outgoing.stream().allMatch(t -> t.kind() == TransitionKind.SELECTION);
            if (allSel) {
                polarity.put(state, Player.PROPONENT);
            } else {
                polarity.put(state, Player.OPPONENT);
            }
        }

        // Terminal positions: states with no outgoing transitions
        var statesWithOutgoing = new HashSet<Integer>();
        for (var t : ss.transitions()) {
            statesWithOutgoing.add(t.source());
        }
        var terminal = new HashSet<Integer>();
        for (int state : ss.states()) {
            if (!statesWithOutgoing.contains(state)) {
                terminal.add(state);
            }
        }

        return new Arena(
                new HashSet<>(ss.states()),
                moves,
                ss.top(),
                terminal,
                polarity);
    }

    // -----------------------------------------------------------------------
    // Strategy enumeration and checking
    // -----------------------------------------------------------------------

    /** Find all Proponent positions reachable from initial. */
    private static Set<Integer> reachableProponentPositions(Arena arena) {
        var reachable = new HashSet<Integer>();
        var visited = new HashSet<Integer>();
        var stack = new ArrayDeque<Integer>();
        stack.push(arena.initial());
        while (!stack.isEmpty()) {
            int pos = stack.pop();
            if (!visited.add(pos)) continue;
            if (arena.polarity().get(pos) == Player.PROPONENT) {
                reachable.add(pos);
            }
            for (var m : arena.movesFrom(pos)) {
                stack.push(m.target());
            }
        }
        return reachable;
    }

    /** Find all Opponent positions reachable from initial. */
    private static Set<Integer> reachableOpponentPositions(Arena arena) {
        var reachable = new HashSet<Integer>();
        var visited = new HashSet<Integer>();
        var stack = new ArrayDeque<Integer>();
        stack.push(arena.initial());
        while (!stack.isEmpty()) {
            int pos = stack.pop();
            if (!visited.add(pos)) continue;
            if (arena.polarity().get(pos) == Player.OPPONENT) {
                reachable.add(pos);
            }
            for (var m : arena.movesFrom(pos)) {
                stack.push(m.target());
            }
        }
        return reachable;
    }

    /**
     * Enumerate all deterministic Proponent strategies.
     * A strategy assigns one move at each reachable Proponent position.
     */
    public static List<Strategy> enumerateStrategies(Arena arena) {
        var pPositions = new TreeSet<>(reachableProponentPositions(arena));
        if (pPositions.isEmpty()) {
            return List.of(new Strategy(Map.of()));
        }

        // For each P-position, collect available moves
        var choicesPerPos = new LinkedHashMap<Integer, List<Move>>();
        for (int pos : pPositions) {
            var moves = arena.movesFrom(pos);
            if (!moves.isEmpty()) {
                choicesPerPos.put(pos, moves);
            }
        }

        if (choicesPerPos.isEmpty()) {
            return List.of(new Strategy(Map.of()));
        }

        // Cartesian product of choices
        var positions = new ArrayList<>(choicesPerPos.keySet());
        var moveLists = new ArrayList<List<Move>>();
        for (int p : positions) {
            moveLists.add(choicesPerPos.get(p));
        }

        var strategies = new ArrayList<Strategy>();
        cartesianProduct(moveLists, 0, new Move[positions.size()], combo -> {
            var choices = new HashMap<Integer, Move>();
            for (int i = 0; i < positions.size(); i++) {
                choices.put(positions.get(i), combo[i]);
            }
            strategies.add(new Strategy(choices));
        });

        return strategies;
    }

    /**
     * Enumerate all deterministic Opponent counter-strategies.
     */
    public static List<CounterStrategy> enumerateCounterStrategies(Arena arena) {
        var oPositions = new TreeSet<>(reachableOpponentPositions(arena));
        if (oPositions.isEmpty()) {
            return List.of(new CounterStrategy(Map.of()));
        }

        var choicesPerPos = new LinkedHashMap<Integer, List<Move>>();
        for (int pos : oPositions) {
            var moves = arena.movesFrom(pos);
            if (!moves.isEmpty()) {
                choicesPerPos.put(pos, moves);
            }
        }

        if (choicesPerPos.isEmpty()) {
            return List.of(new CounterStrategy(Map.of()));
        }

        var positions = new ArrayList<>(choicesPerPos.keySet());
        var moveLists = new ArrayList<List<Move>>();
        for (int p : positions) {
            moveLists.add(choicesPerPos.get(p));
        }

        var counterStrategies = new ArrayList<CounterStrategy>();
        cartesianProduct(moveLists, 0, new Move[positions.size()], combo -> {
            var choices = new HashMap<Integer, Move>();
            for (int i = 0; i < positions.size(); i++) {
                choices.put(positions.get(i), combo[i]);
            }
            counterStrategies.add(new CounterStrategy(choices));
        });

        return counterStrategies;
    }

    /** Recursive cartesian product helper. */
    private static void cartesianProduct(
            List<List<Move>> lists, int depth, Move[] current,
            java.util.function.Consumer<Move[]> consumer) {
        if (depth == lists.size()) {
            consumer.accept(current.clone());
            return;
        }
        for (var move : lists.get(depth)) {
            current[depth] = move;
            cartesianProduct(lists, depth + 1, current, consumer);
        }
    }

    /**
     * Execute a play given both strategies, returning the sequence of moves.
     * Stops when a terminal position is reached or after maxSteps.
     */
    public static List<Move> playGame(
            Arena arena, Strategy strategy, CounterStrategy counterStrategy, int maxSteps) {
        var moves = new ArrayList<Move>();
        int position = arena.initial();
        var visitedPositions = new ArrayList<Integer>();
        visitedPositions.add(position);

        for (int step = 0; step < maxSteps; step++) {
            if (arena.isTerminal(position)) break;

            Player player = arena.polarity().get(position);
            Move move;
            if (player == Player.PROPONENT) {
                move = strategy.apply(position);
            } else if (player == Player.OPPONENT) {
                move = counterStrategy.apply(position);
            } else {
                break; // no polarity -> stuck
            }

            if (move == null) break; // strategy doesn't cover this position

            moves.add(move);
            position = move.target();

            // Detect infinite loop (cycle)
            if (visitedPositions.contains(position)) {
                break;
            }
            visitedPositions.add(position);
        }

        return Collections.unmodifiableList(moves);
    }

    /** Overload with default maxSteps = 1000. */
    public static List<Move> playGame(
            Arena arena, Strategy strategy, CounterStrategy counterStrategy) {
        return playGame(arena, strategy, counterStrategy, 1000);
    }

    /**
     * Check if a Proponent strategy is winning against ALL Opponent counter-strategies.
     * A strategy is winning iff every possible play reaches a terminal position.
     */
    public static boolean isWinning(Strategy strategy, Arena arena, int maxSteps) {
        var counterStrategies = enumerateCounterStrategies(arena);
        for (var cs : counterStrategies) {
            var play = playGame(arena, strategy, cs, maxSteps);
            if (play.isEmpty()) {
                // No moves played -> initial must be terminal
                if (!arena.isTerminal(arena.initial())) return false;
                continue;
            }
            int finalPos = play.get(play.size() - 1).target();
            if (!arena.isTerminal(finalPos)) return false;
        }
        return true;
    }

    /** Overload with default maxSteps = 1000. */
    public static boolean isWinning(Strategy strategy, Arena arena) {
        return isWinning(strategy, arena, 1000);
    }

    /** Return all winning Proponent strategies. */
    public static List<Strategy> winningStrategies(Arena arena) {
        var allStrats = enumerateStrategies(arena);
        return allStrats.stream()
                .filter(s -> isWinning(s, arena))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Determine who has a winning strategy.
     *
     * <p>Returns PROPONENT_WINS if any winning strategy exists for P,
     * OPPONENT_WINS if O can prevent P from winning regardless of P's strategy,
     * DRAW if neither (possible with infinite games from recursion).
     */
    public static GameOutcome gameValue(Arena arena) {
        var allStrats = enumerateStrategies(arena);
        boolean pWins = allStrats.stream().anyMatch(s -> isWinning(s, arena));

        if (pWins) return GameOutcome.PROPONENT_WINS;

        // Check if Opponent can win against all P strategies
        boolean oWins = true;
        var counterStrats = enumerateCounterStrategies(arena);
        for (var strat : allStrats) {
            boolean allReachTerminal = true;
            for (var cs : counterStrats) {
                var play = playGame(arena, strat, cs);
                if (!play.isEmpty()) {
                    int finalPos = play.get(play.size() - 1).target();
                    if (!arena.isTerminal(finalPos)) {
                        allReachTerminal = false;
                        break;
                    }
                } else if (!arena.isTerminal(arena.initial())) {
                    allReachTerminal = false;
                    break;
                }
            }
            if (allReachTerminal) {
                oWins = false;
                break;
            }
        }

        if (oWins) return GameOutcome.OPPONENT_WINS;

        return GameOutcome.DRAW;
    }

    // -----------------------------------------------------------------------
    // Arena composition (tensor product for parallel)
    // -----------------------------------------------------------------------

    /**
     * Compute the tensor product of two arenas (parallel composition).
     * Positions are pairs (p1, p2). Moves interleave: at each position,
     * either component can make a move independently.
     */
    public static Arena composeArenas(Arena a1, Arena a2) {
        var positions = new HashSet<Integer>();
        var posMap = new HashMap<long[], Integer>();
        // Use a more reliable key: sorted pair encoding
        var pairToId = new LinkedHashMap<Long, Integer>();
        int nextId = 0;

        var sortedP1 = new TreeSet<>(a1.positions());
        var sortedP2 = new TreeSet<>(a2.positions());

        for (int p1 : sortedP1) {
            for (int p2 : sortedP2) {
                long key = pairKey(p1, p2);
                pairToId.put(key, nextId);
                positions.add(nextId);
                nextId++;
            }
        }

        int initial = pairToId.get(pairKey(a1.initial(), a2.initial()));

        // Terminal: both components terminal
        var terminal = new HashSet<Integer>();
        for (int p1 : a1.terminal()) {
            for (int p2 : a2.terminal()) {
                terminal.add(pairToId.get(pairKey(p1, p2)));
            }
        }

        // Moves: interleaving from both components
        var moves = new ArrayList<Move>();

        for (var entry : pairToId.entrySet()) {
            long key = entry.getKey();
            int pid = entry.getValue();
            int p1 = (int) (key >> 32);
            int p2 = (int) (key & 0xFFFFFFFFL);

            // Moves from component 1
            for (var m : a1.movesFrom(p1)) {
                int target = pairToId.get(pairKey(m.target(), p2));
                moves.add(new Move(pid, m.label(), target, m.player()));
            }

            // Moves from component 2
            for (var m : a2.movesFrom(p2)) {
                int target = pairToId.get(pairKey(p1, m.target()));
                moves.add(new Move(pid, m.label(), target, m.player()));
            }
        }

        // Compute polarity after all moves are built
        var polarity = new HashMap<Integer, Player>();
        for (int pid : positions) {
            var outgoing = moves.stream()
                    .filter(m -> m.source() == pid)
                    .collect(Collectors.toList());
            if (outgoing.isEmpty()) continue;
            boolean allP = outgoing.stream()
                    .allMatch(m -> m.player() == Player.PROPONENT);
            polarity.put(pid, allP ? Player.PROPONENT : Player.OPPONENT);
        }

        return new Arena(positions, moves, initial, terminal, polarity);
    }

    /** Encode a pair of ints into a long key for the position map. */
    private static long pairKey(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    /**
     * Perform a complete game-semantic analysis of a session-type state space.
     */
    public static GameAnalysis analyzeGame(StateSpace ss) {
        var arena = buildArena(ss);
        var outcome = gameValue(arena);

        var allStrats = enumerateStrategies(arena);
        var winStrats = allStrats.stream()
                .filter(s -> isWinning(s, arena))
                .collect(Collectors.toList());

        return new GameAnalysis(
                arena,
                outcome,
                arena.proponentPositions().size(),
                arena.opponentPositions().size(),
                arena.terminal().size(),
                winStrats.size(),
                allStrats.size(),
                outcome == GameOutcome.PROPONENT_WINS,
                winStrats.isEmpty() ? null : winStrats.get(0));
    }
}
