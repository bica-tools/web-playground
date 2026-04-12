package com.bica.reborn.ast;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T2b {@link Chain} primitive — Java mirror of the reticulate Python
 * {@code TestChain} suite under {@code reticulate/tests/test_parser.py}
 * and {@code reticulate/tests/test_statespace.py}.
 *
 * <p>Parity obligation from {@code feedback_java_python_parity} and the
 * modularity-campaign charter's Instrument §4/§5: every T2b semantic
 * asserted for reticulate must be asserted identically for BICA Reborn.
 */
class ChainTest {

    // =========================================================================
    // Constructor / equality
    // =========================================================================

    @Nested
    class ConstructorAndEquality {

        @Test
        void constructsWithMethodAndCont() {
            Chain ch = new Chain("m", new End());
            assertEquals("m", ch.method());
            assertEquals(new End(), ch.cont());
        }

        @Test
        void rejectsNullMethod() {
            assertThrows(NullPointerException.class,
                    () -> new Chain(null, new End()));
        }

        @Test
        void rejectsNullCont() {
            assertThrows(NullPointerException.class,
                    () -> new Chain("m", null));
        }

        @Test
        void structuralEquality() {
            assertEquals(
                    new Chain("m", new End()),
                    new Chain("m", new End()));
        }

        @Test
        void inequalityOnMethod() {
            assertNotEquals(
                    new Chain("m", new End()),
                    new Chain("n", new End()));
        }

        @Test
        void inequalityOnCont() {
            assertNotEquals(
                    new Chain("m", new End()),
                    new Chain("m", new Var("X")));
        }

        @Test
        void notEqualToSingleArmBranch() {
            // T2b core invariant: Chain and single-arm Branch are distinct
            // syntactic categories, even though their state spaces are
            // lattice-isomorphic. This inequality is load-bearing for the
            // modularity classification (docs/specs/modularity-classification-spec.md §3.4).
            Chain chain = new Chain("m", new End());
            Branch single = new Branch(List.of(new Branch.Choice("m", new End())));
            assertNotEquals(chain, single);
            assertNotEquals(single, chain);
        }

        @Test
        void isSessionType() {
            assertInstanceOf(SessionType.class, new Chain("m", new End()));
        }
    }

    // =========================================================================
    // Parser
    // =========================================================================

    @Nested
    class ParsingChain {

        @Test
        void bareIdentDotEndParsesAsChain() {
            assertEquals(new Chain("m", new End()), Parser.parse("m . end"));
        }

        @Test
        void chainedMethodsNestRightAssociatively() {
            assertEquals(
                    new Chain("a", new Chain("b", new Chain("c", new End()))),
                    Parser.parse("a . b . c . end"));
        }

        @Test
        void chainInsideBranch() {
            SessionType parsed = Parser.parse("&{open: m . end, err: end}");
            SessionType expected = new Branch(List.of(
                    new Branch.Choice("open", new Chain("m", new End())),
                    new Branch.Choice("err", new End())));
            assertEquals(expected, parsed);
        }

        @Test
        void chainInsideSelect() {
            SessionType parsed = Parser.parse("+{OK: m . end, ERR: end}");
            SessionType expected = new Select(List.of(
                    new Branch.Choice("OK", new Chain("m", new End())),
                    new Branch.Choice("ERR", new End())));
            assertEquals(expected, parsed);
        }

        @Test
        void chainInsideRec() {
            SessionType parsed = Parser.parse("rec X . m . X");
            SessionType expected = new Rec("X", new Chain("m", new Var("X")));
            assertEquals(expected, parsed);
        }

        @Test
        void chainToBranchBody() {
            // m . &{a: end, b: end} — Chain followed by a multi-arm Branch
            SessionType parsed = Parser.parse("m . &{a: end, b: end}");
            SessionType expected = new Chain("m", new Branch(List.of(
                    new Branch.Choice("a", new End()),
                    new Branch.Choice("b", new End()))));
            assertEquals(expected, parsed);
        }

        @Test
        void parallelBranchesAreChains() {
            // (read . end || write . end) — both arms are Chain
            SessionType parsed = Parser.parse("(read . end || write . end)");
            SessionType expected = new Parallel(
                    new Chain("read", new End()),
                    new Chain("write", new End()));
            assertEquals(expected, parsed);
        }

        @Test
        void complexLeftIsSequenceNotChain() {
            // Only a BARE IDENT on the left of `.` triggers Chain.
            // Compound expressions still produce Sequence.
            SessionType parsed = Parser.parse("(a || b) . end");
            assertInstanceOf(Sequence.class, parsed);
        }
    }

    // =========================================================================
    // Pretty printer
    // =========================================================================

    @Nested
    class PrettyPrintingChain {

        @Test
        void chainPrintsAsMethodDotBody() {
            assertEquals("m . end", PrettyPrinter.pretty(new Chain("m", new End())));
        }

        @Test
        void nestedChainUnrollsInfix() {
            assertEquals("a . b . c . end",
                    PrettyPrinter.pretty(
                            new Chain("a", new Chain("b", new Chain("c", new End())))));
        }

        @Test
        void chainPrintsCoreForm() {
            // prettyCore uses the same rendering for Chain (no sugar flag).
            assertEquals("m . end",
                    PrettyPrinter.prettyCore(new Chain("m", new End())));
        }

        @Test
        void roundTripSimple() {
            SessionType chain = new Chain("m", new End());
            assertEquals(chain, Parser.parse(PrettyPrinter.pretty(chain)));
        }

        @Test
        void roundTripInsideBranch() {
            SessionType ast = Parser.parse("&{open: m . end, err: end}");
            assertEquals(ast, Parser.parse(PrettyPrinter.pretty(ast)));
        }

        @Test
        void roundTripComplexProtocol() {
            // Iterator protocol — single-arm branches inside rec should
            // survive a pretty/parse round trip unchanged.
            String src = "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}";
            SessionType first = Parser.parse(src);
            SessionType second = Parser.parse(PrettyPrinter.pretty(first));
            assertEquals(first, second);
        }
    }

    // =========================================================================
    // State space
    // =========================================================================

    @Nested
    class StateSpaceOfChain {

        @Test
        void chainBuildsTwoStatesAndOneTransition() {
            // Chain(m, End) should have: entry state, end state, one m-transition.
            StateSpace ss = StateSpaceBuilder.build(new Chain("m", new End()));
            assertEquals(2, ss.states().size());
            assertEquals(1, ss.transitions().size());
            assertEquals("m", ss.transitions().getFirst().label());
            assertEquals(ss.top(), ss.transitions().getFirst().source());
            assertEquals(ss.bottom(), ss.transitions().getFirst().target());
        }

        @Test
        void chainHasSameStateSpaceAsSingleArmBranch() {
            // T2b invariant: Chain and single-arm Branch produce isomorphic
            // state spaces (same state count, same transition shape) — the
            // syntactic distinction is AST-level only. See
            // lean4-formalization/Reticulate/ChainAndSeq.lean
            // chain_branch_iso_lattice_level.
            StateSpace chainSs = StateSpaceBuilder.build(
                    new Chain("m", new End()));
            StateSpace branchSs = StateSpaceBuilder.build(
                    new Branch(List.of(new Branch.Choice("m", new End()))));
            assertEquals(branchSs.states().size(), chainSs.states().size());
            assertEquals(branchSs.transitions().size(), chainSs.transitions().size());
            assertEquals(
                    branchSs.transitions().getFirst().label(),
                    chainSs.transitions().getFirst().label());
        }

        @Test
        void nestedChainBuildsLinearChain() {
            // Chain(a, Chain(b, Chain(c, End))) → 4 states, 3 transitions.
            StateSpace ss = StateSpaceBuilder.build(
                    new Chain("a", new Chain("b", new Chain("c", new End()))));
            assertEquals(4, ss.states().size());
            assertEquals(3, ss.transitions().size());
        }

        @Test
        void chainToVarBuildsCyclicStructure() {
            // rec X . m . X — Chain body is a Var(X); state space should
            // cycle (same state space as rec X . &{m: X}).
            StateSpace ss = StateSpaceBuilder.build(
                    Parser.parse("rec X . m . X"));
            // Cyclic recursive type: finite state space with a loop.
            assertFalse(ss.states().isEmpty());
            assertFalse(ss.transitions().isEmpty());
        }
    }
}
