package com.bica.reborn.gamesemantics;

import com.bica.reborn.gamesemantics.GameSemanticsChecker.*;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for game-semantic interpretation of session types (Step 94).
 * Mirrors all 71 Python tests from test_game_semantics.py.
 */
class GameSemanticsCheckerTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    private Arena arena(String type) {
        return GameSemanticsChecker.buildArena(build(type));
    }

    // =========================================================================
    // Section 1: Arena construction
    // =========================================================================

    @Nested
    class BuildArenaTests {

        @Test
        void endArena() {
            var a = arena("end");
            assertEquals(1, a.positions().size());
            assertEquals(0, a.moves().size());
            assertTrue(a.terminal().contains(a.initial()));
        }

        @Test
        void simpleBranch() {
            var a = arena("&{a: end, b: end}");
            assertFalse(a.terminal().contains(a.initial()));
            var moves = a.movesFrom(a.initial());
            assertEquals(2, moves.size());
            assertTrue(moves.stream().allMatch(m -> m.player() == Player.OPPONENT));
            assertEquals(Player.OPPONENT, a.polarity().get(a.initial()));
        }

        @Test
        void simpleSelection() {
            var a = arena("+{x: end, y: end}");
            var moves = a.movesFrom(a.initial());
            assertEquals(2, moves.size());
            assertTrue(moves.stream().allMatch(m -> m.player() == Player.PROPONENT));
            assertEquals(Player.PROPONENT, a.polarity().get(a.initial()));
        }

        @Test
        void branchThenSelect() {
            var a = arena("&{a: +{ok: end, err: end}, b: end}");
            assertEquals(Player.OPPONENT, a.polarity().get(a.initial()));
            assertTrue(a.proponentPositions().size() >= 1);
        }

        @Test
        void terminalPositions() {
            var a = arena("&{a: end, b: end}");
            for (int t : a.terminal()) {
                assertTrue(a.movesFrom(t).isEmpty());
            }
        }

        @Test
        void positionsMatchStateSpace() {
            var ss = build("&{a: +{x: end}, b: end}");
            var a = GameSemanticsChecker.buildArena(ss);
            assertEquals(ss.states(), a.positions());
        }

        @Test
        void initialIsTop() {
            var ss = build("+{a: end, b: end}");
            var a = GameSemanticsChecker.buildArena(ss);
            assertEquals(ss.top(), a.initial());
        }

        @Test
        void movesMatchTransitions() {
            var ss = build("&{a: end, b: +{x: end, y: end}}");
            var a = GameSemanticsChecker.buildArena(ss);
            assertEquals(ss.transitions().size(), a.moves().size());
        }

        @Test
        void singleBranchForced() {
            var a = arena("&{a: end}");
            var moves = a.movesFrom(a.initial());
            assertEquals(1, moves.size());
            assertEquals(Player.OPPONENT, moves.get(0).player());
            assertEquals("a", moves.get(0).label());
        }

        @Test
        void nestedBranchesAllOpponent() {
            var a = arena("&{a: &{b: end}}");
            assertEquals(2, a.opponentPositions().size());
        }

        @Test
        void nestedSelectionsAllProponent() {
            var a = arena("+{a: +{b: end}}");
            assertEquals(2, a.proponentPositions().size());
        }

        @Test
        void depthSimple() {
            var a = arena("&{a: end}");
            assertEquals(1, a.depth());
        }

        @Test
        void depthNested() {
            var a = arena("&{a: +{ok: end, err: end}, b: end}");
            assertEquals(2, a.depth());
        }
    }

    // =========================================================================
    // Section 2: Strategy enumeration
    // =========================================================================

    @Nested
    class EnumerateStrategiesTests {

        @Test
        void noSelectionOneStrategy() {
            var a = arena("&{a: end}");
            var strats = GameSemanticsChecker.enumerateStrategies(a);
            assertEquals(1, strats.size());
            assertTrue(strats.get(0).choices().isEmpty());
        }

        @Test
        void singleSelectionTwoStrategies() {
            var a = arena("+{a: end, b: end}");
            var strats = GameSemanticsChecker.enumerateStrategies(a);
            assertEquals(2, strats.size());
        }

        @Test
        void singleSelectionThreeChoices() {
            var a = arena("+{a: end, b: end, c: end}");
            var strats = GameSemanticsChecker.enumerateStrategies(a);
            assertEquals(3, strats.size());
        }

        @Test
        void twoSelectionsProduct() {
            var a = arena("&{a: +{x: end, y: end}, b: +{u: end, v: end}}");
            var strats = GameSemanticsChecker.enumerateStrategies(a);
            assertEquals(4, strats.size());
        }

        @Test
        void endTypeOneStrategy() {
            var a = arena("end");
            var strats = GameSemanticsChecker.enumerateStrategies(a);
            assertEquals(1, strats.size());
        }

        @Test
        void strategyCoversAllPPositions() {
            var a = arena("&{a: +{x: end, y: end}, b: +{u: end, v: end}}");
            for (var strat : GameSemanticsChecker.enumerateStrategies(a)) {
                for (int pos : a.proponentPositions()) {
                    assertTrue(strat.choices().containsKey(pos));
                }
            }
        }

        @Test
        void strategyLabels() {
            var a = arena("+{a: end, b: end}");
            var strats = GameSemanticsChecker.enumerateStrategies(a);
            for (var s : strats) {
                var labels = s.labels();
                assertEquals(1, labels.size());
                assertTrue(Set.of("a", "b").contains(labels.values().iterator().next()));
            }
        }
    }

    // =========================================================================
    // Section 3: Counter-strategy enumeration
    // =========================================================================

    @Nested
    class EnumerateCounterStrategiesTests {

        @Test
        void noBranchOneCounter() {
            var a = arena("+{a: end}");
            var cs = GameSemanticsChecker.enumerateCounterStrategies(a);
            assertEquals(1, cs.size());
        }

        @Test
        void singleBranchTwoCounters() {
            var a = arena("&{a: end, b: end}");
            var cs = GameSemanticsChecker.enumerateCounterStrategies(a);
            assertEquals(2, cs.size());
        }

        @Test
        void branchThreeChoices() {
            var a = arena("&{a: end, b: end, c: end}");
            var cs = GameSemanticsChecker.enumerateCounterStrategies(a);
            assertEquals(3, cs.size());
        }
    }

    // =========================================================================
    // Section 4: Playing games
    // =========================================================================

    @Nested
    class PlayGameTests {

        @Test
        void endEmptyPlay() {
            var a = arena("end");
            var s = new Strategy(Map.of());
            var cs = new CounterStrategy(Map.of());
            var play = GameSemanticsChecker.playGame(a, s, cs);
            assertTrue(play.isEmpty());
        }

        @Test
        void branchOneStep() {
            var a = arena("&{a: end}");
            var s = new Strategy(Map.of());
            var moves = a.movesFrom(a.initial());
            var cs = new CounterStrategy(Map.of(a.initial(), moves.get(0)));
            var play = GameSemanticsChecker.playGame(a, s, cs);
            assertEquals(1, play.size());
            assertEquals("a", play.get(0).label());
        }

        @Test
        void selectionOneStep() {
            var a = arena("+{x: end, y: end}");
            var moves = a.movesFrom(a.initial());
            var s = new Strategy(Map.of(a.initial(), moves.get(0)));
            var cs = new CounterStrategy(Map.of());
            var play = GameSemanticsChecker.playGame(a, s, cs);
            assertEquals(1, play.size());
        }

        @Test
        void branchThenSelectTwoSteps() {
            var a = arena("&{a: +{ok: end, err: end}}");
            var oMoves = a.movesFrom(a.initial());
            assertEquals(1, oMoves.size());
            int selPos = oMoves.get(0).target();
            var pMoves = a.movesFrom(selPos);
            assertEquals(2, pMoves.size());

            var s = new Strategy(Map.of(selPos, pMoves.get(0)));
            var cs = new CounterStrategy(Map.of(a.initial(), oMoves.get(0)));
            var play = GameSemanticsChecker.playGame(a, s, cs);
            assertEquals(2, play.size());
            assertEquals(Player.OPPONENT, play.get(0).player());
            assertEquals(Player.PROPONENT, play.get(1).player());
        }

        @Test
        void playReachesTerminal() {
            var a = arena("&{a: end, b: end}");
            var oMoves = a.movesFrom(a.initial());
            var cs = new CounterStrategy(Map.of(a.initial(), oMoves.get(0)));
            var s = new Strategy(Map.of());
            var play = GameSemanticsChecker.playGame(a, s, cs);
            assertTrue(a.isTerminal(play.get(play.size() - 1).target()));
        }
    }

    // =========================================================================
    // Section 5: Winning strategy check
    // =========================================================================

    @Nested
    class IsWinningTests {

        @Test
        void endTriviallyWinning() {
            var a = arena("end");
            var s = new Strategy(Map.of());
            assertTrue(GameSemanticsChecker.isWinning(s, a));
        }

        @Test
        void branchOnlyWinning() {
            var a = arena("&{a: end, b: end}");
            var s = new Strategy(Map.of());
            assertTrue(GameSemanticsChecker.isWinning(s, a));
        }

        @Test
        void selectionBothWinning() {
            var a = arena("+{x: end, y: end}");
            var strats = GameSemanticsChecker.enumerateStrategies(a);
            assertEquals(2, strats.size());
            assertTrue(strats.stream().allMatch(s -> GameSemanticsChecker.isWinning(s, a)));
        }

        @Test
        void branchSelectWinning() {
            var a = arena("&{a: +{ok: end, err: end}, b: end}");
            var strats = GameSemanticsChecker.enumerateStrategies(a);
            assertTrue(strats.stream().allMatch(s -> GameSemanticsChecker.isWinning(s, a)));
        }

        @Test
        void nestedSelectionWinning() {
            var a = arena("+{a: +{b: end}}");
            var strats = GameSemanticsChecker.enumerateStrategies(a);
            assertEquals(1, strats.size());
            assertTrue(GameSemanticsChecker.isWinning(strats.get(0), a));
        }
    }

    // =========================================================================
    // Section 6: Winning strategies enumeration
    // =========================================================================

    @Nested
    class WinningStrategiesTests {

        @Test
        void endOneWinning() {
            var a = arena("end");
            assertEquals(1, GameSemanticsChecker.winningStrategies(a).size());
        }

        @Test
        void allBranchOneWinning() {
            var a = arena("&{a: end, b: end}");
            assertEquals(1, GameSemanticsChecker.winningStrategies(a).size());
        }

        @Test
        void selectionAllWinning() {
            var a = arena("+{a: end, b: end}");
            assertEquals(2, GameSemanticsChecker.winningStrategies(a).size());
        }

        @Test
        void complexAllWinning() {
            var a = arena("&{a: +{ok: end, err: end}, b: +{x: end, y: end}}");
            var ws = GameSemanticsChecker.winningStrategies(a);
            var allStrats = GameSemanticsChecker.enumerateStrategies(a);
            assertEquals(4, ws.size());
            assertEquals(allStrats.size(), ws.size());
        }
    }

    // =========================================================================
    // Section 7: Game value (outcome)
    // =========================================================================

    @Nested
    class GameValueTests {

        @Test
        void endProponentWins() {
            assertEquals(GameOutcome.PROPONENT_WINS,
                    GameSemanticsChecker.gameValue(arena("end")));
        }

        @Test
        void branchProponentWins() {
            assertEquals(GameOutcome.PROPONENT_WINS,
                    GameSemanticsChecker.gameValue(arena("&{a: end}")));
        }

        @Test
        void selectionProponentWins() {
            assertEquals(GameOutcome.PROPONENT_WINS,
                    GameSemanticsChecker.gameValue(arena("+{a: end, b: end}")));
        }

        @Test
        void nestedProponentWins() {
            assertEquals(GameOutcome.PROPONENT_WINS,
                    GameSemanticsChecker.gameValue(arena("&{a: +{ok: end, err: end}, b: end}")));
        }

        @Test
        void deepNestingProponentWins() {
            assertEquals(GameOutcome.PROPONENT_WINS,
                    GameSemanticsChecker.gameValue(arena("&{a: &{b: +{x: end, y: end}}}")));
        }
    }

    // =========================================================================
    // Section 8: Arena composition (tensor product)
    // =========================================================================

    @Nested
    class ComposeArenasTests {

        @Test
        void composeTwoEnds() {
            var a1 = arena("end");
            var a2 = arena("end");
            var composed = GameSemanticsChecker.composeArenas(a1, a2);
            assertEquals(1, composed.positions().size());
            assertTrue(composed.terminal().contains(composed.initial()));
        }

        @Test
        void composePositionsProduct() {
            var a1 = arena("&{a: end}"); // 2 positions
            var a2 = arena("&{b: end}"); // 2 positions
            var composed = GameSemanticsChecker.composeArenas(a1, a2);
            assertEquals(4, composed.positions().size());
        }

        @Test
        void composeTerminal() {
            var a1 = arena("&{a: end}");
            var a2 = arena("&{b: end}");
            var composed = GameSemanticsChecker.composeArenas(a1, a2);
            assertEquals(1, composed.terminal().size()); // (end, end)
        }

        @Test
        void composeInterleavingMoves() {
            var a1 = arena("&{a: end}");
            var a2 = arena("&{b: end}");
            var composed = GameSemanticsChecker.composeArenas(a1, a2);
            var labels = composed.moves().stream()
                    .map(Move::label).collect(java.util.stream.Collectors.toSet());
            assertTrue(labels.contains("a"));
            assertTrue(labels.contains("b"));
        }

        @Test
        void composePreservesDeterminacy() {
            var a1 = arena("&{a: end, b: end}");
            var a2 = arena("+{x: end, y: end}");
            var composed = GameSemanticsChecker.composeArenas(a1, a2);
            assertEquals(GameOutcome.PROPONENT_WINS,
                    GameSemanticsChecker.gameValue(composed));
        }

        @Test
        void composeInitialNotTerminal() {
            var a1 = arena("&{a: end}");
            var a2 = arena("&{b: end}");
            var composed = GameSemanticsChecker.composeArenas(a1, a2);
            assertFalse(composed.terminal().contains(composed.initial()));
        }
    }

    // =========================================================================
    // Section 9: Full analysis
    // =========================================================================

    @Nested
    class AnalyzeGameTests {

        @Test
        void endAnalysis() {
            var analysis = GameSemanticsChecker.analyzeGame(build("end"));
            assertTrue(analysis.isDeterminate());
            assertEquals(GameOutcome.PROPONENT_WINS, analysis.outcome());
            assertEquals(0, analysis.numProponentPositions());
            assertEquals(0, analysis.numOpponentPositions());
            assertEquals(1, analysis.numTerminalPositions());
            assertEquals(1, analysis.numWinningStrategies());
            assertEquals(1, analysis.numTotalStrategies());
        }

        @Test
        void branchAnalysis() {
            var analysis = GameSemanticsChecker.analyzeGame(build("&{a: end, b: end}"));
            assertTrue(analysis.isDeterminate());
            assertEquals(1, analysis.numOpponentPositions());
            assertEquals(0, analysis.numProponentPositions());
        }

        @Test
        void selectAnalysis() {
            var analysis = GameSemanticsChecker.analyzeGame(build("+{x: end, y: end}"));
            assertTrue(analysis.isDeterminate());
            assertEquals(1, analysis.numProponentPositions());
            assertEquals(0, analysis.numOpponentPositions());
            assertEquals(2, analysis.numWinningStrategies());
            assertEquals(2, analysis.numTotalStrategies());
        }

        @Test
        void mixedAnalysis() {
            var analysis = GameSemanticsChecker.analyzeGame(
                    build("&{a: +{ok: end, err: end}, b: end}"));
            assertTrue(analysis.isDeterminate());
            assertTrue(analysis.numProponentPositions() >= 1);
            assertTrue(analysis.numOpponentPositions() >= 1);
            assertNotNull(analysis.exampleWinningStrategy());
        }

        @Test
        void analysisHasArena() {
            var analysis = GameSemanticsChecker.analyzeGame(build("&{a: end}"));
            assertNotNull(analysis.arena());
        }

        @Test
        void analysisWinningStrategyIsValid() {
            var analysis = GameSemanticsChecker.analyzeGame(
                    build("&{a: +{ok: end, err: end}, b: end}"));
            var ws = analysis.exampleWinningStrategy();
            assertNotNull(ws);
            assertTrue(GameSemanticsChecker.isWinning(ws, analysis.arena()));
        }
    }

    // =========================================================================
    // Section 10: Recursive types
    // =========================================================================

    @Nested
    class RecursiveTypesTests {

        @Test
        void simpleRecursiveBranch() {
            var a = arena("rec X . &{a: X, b: end}");
            var strats = GameSemanticsChecker.enumerateStrategies(a);
            assertEquals(1, strats.size());
        }

        @Test
        void recursiveWithSelection() {
            var a = arena("rec X . &{next: +{TRUE: X, FALSE: end}}");
            assertTrue(a.proponentPositions().size() >= 1);
            var strats = GameSemanticsChecker.enumerateStrategies(a);
            assertTrue(strats.size() >= 2);
        }
    }

    // =========================================================================
    // Section 11: Polarity and player assignment
    // =========================================================================

    @Nested
    class PolarityTests {

        @Test
        void branchIsOpponent() {
            var a = arena("&{a: end}");
            assertEquals(Player.OPPONENT, a.polarity().get(a.initial()));
        }

        @Test
        void selectionIsProponent() {
            var a = arena("+{a: end}");
            assertEquals(Player.PROPONENT, a.polarity().get(a.initial()));
        }

        @Test
        void terminalHasNoPolarity() {
            var a = arena("&{a: end}");
            for (int t : a.terminal()) {
                assertFalse(a.polarity().containsKey(t));
            }
        }

        @Test
        void alternationBranchSelect() {
            var a = arena("&{a: +{ok: end}}");
            assertEquals(Player.OPPONENT, a.polarity().get(a.initial()));
            var m = a.movesFrom(a.initial()).get(0);
            assertEquals(Player.PROPONENT, a.polarity().get(m.target()));
        }
    }

    // =========================================================================
    // Section 12: Edge cases and Move record
    // =========================================================================

    @Nested
    class MoveAndEdgeCaseTests {

        @Test
        void moveIsRecord() {
            var m = new Move(0, "a", 1, Player.OPPONENT);
            assertEquals(0, m.source());
            assertEquals("a", m.label());
            assertEquals(1, m.target());
            assertEquals(Player.OPPONENT, m.player());
        }

        @Test
        void strategyIsRecord() {
            var s = new Strategy(Map.of());
            assertTrue(s.choices().isEmpty());
        }

        @Test
        void playerEnumValues() {
            assertEquals("P", Player.PROPONENT.code());
            assertEquals("O", Player.OPPONENT.code());
        }

        @Test
        void gameOutcomeValues() {
            assertEquals("P_wins", GameOutcome.PROPONENT_WINS.code());
            assertEquals("O_wins", GameOutcome.OPPONENT_WINS.code());
        }

        @Test
        void arenaIsTerminal() {
            var a = arena("&{a: end}");
            assertFalse(a.isTerminal(a.initial()));
            for (int t : a.terminal()) {
                assertTrue(a.isTerminal(t));
            }
        }

        @Test
        void strategyApply() {
            var a = arena("+{a: end}");
            var strats = GameSemanticsChecker.enumerateStrategies(a);
            var s = strats.get(0);
            assertNotNull(s.apply(a.initial()));
            assertNull(s.apply(9999));
        }

        @Test
        void counterStrategyApply() {
            var a = arena("&{a: end}");
            var csList = GameSemanticsChecker.enumerateCounterStrategies(a);
            var cs = csList.get(0);
            assertNotNull(cs.apply(a.initial()));
            assertNull(cs.apply(9999));
        }
    }

    // =========================================================================
    // Section 13: Benchmark protocols
    // =========================================================================

    @Nested
    class BenchmarkProtocolTests {

        @Test
        void atmProtocol() {
            var ss = build("&{insertCard: &{enterPIN: +{ok: &{withdraw: end, balance: end}, reject: end}}}");
            var analysis = GameSemanticsChecker.analyzeGame(ss);
            assertTrue(analysis.isDeterminate());
            assertTrue(analysis.numProponentPositions() >= 1);
        }

        @Test
        void simpleAuthFlow() {
            var ss = build("&{login: +{grant: &{action: end}, deny: end}}");
            var analysis = GameSemanticsChecker.analyzeGame(ss);
            assertTrue(analysis.isDeterminate());
            assertTrue(analysis.numWinningStrategies() >= 1);
        }

        @Test
        void iteratorPattern() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var analysis = GameSemanticsChecker.analyzeGame(ss);
            assertTrue(analysis.numProponentPositions() >= 1);
            assertTrue(analysis.numTotalStrategies() >= 2);
        }
    }
}
