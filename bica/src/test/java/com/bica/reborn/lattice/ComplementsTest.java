package com.bica.reborn.lattice;

import com.bica.reborn.lattice.Complements.ComplementAnalysis;
import com.bica.reborn.lattice.Complements.ComplementInfo;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for complement analysis on session type lattices.
 *
 * <p>Ported from {@code reticulate/tests/test_complements.py} (Step 363c).
 */
class ComplementsTest {

    // -- helpers ---------------------------------------------------------------

    private record Built(StateSpace ss, LatticeResult lr) {}

    private static Built build(String source) {
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(source));
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        return new Built(ss, lr);
    }

    // =========================================================================
    // findComplements
    // =========================================================================

    @Nested
    class FindComplements {

        @Test
        void endSelfComplement() {
            var b = build("end");
            assertTrue(b.lr.isLattice());
            List<Integer> comps = Complements.findComplements(b.ss, b.ss.top(), b.lr);
            assertTrue(comps.contains(b.ss.top()));
        }

        @Test
        void twoElementChain() {
            var b = build("&{a: end}");
            assertTrue(b.lr.isLattice());
            List<Integer> comps = Complements.findComplements(b.ss, b.ss.top(), b.lr);
            assertTrue(comps.size() >= 1);
        }

        @Test
        void branchComplement() {
            var b = build("&{a: end, b: end}");
            assertTrue(b.lr.isLattice());
            List<Integer> comps = Complements.findComplements(b.ss, b.ss.top(), b.lr);
            assertTrue(comps.size() >= 1);
        }

        @Test
        void parallelBoolean() {
            var b = build("(&{a: end} || &{b: end})");
            assertTrue(b.lr.isLattice());
            for (int s : b.ss.states()) {
                List<Integer> comps = Complements.findComplements(b.ss, s, b.lr);
                assertFalse(comps.isEmpty(), "State " + s + " has no complement");
            }
        }

        @Test
        void nonLatticeReturnsEmpty() {
            var b = build("&{a: end, b: rec X . &{a: X}}");
            if (!b.lr.isLattice()) {
                List<Integer> comps = Complements.findComplements(b.ss, b.ss.top(), b.lr);
                assertTrue(comps.isEmpty());
            }
        }
    }

    // =========================================================================
    // complementInfo
    // =========================================================================

    @Nested
    class ComplementInfoTests {

        @Test
        void returnsComplementInfo() {
            var b = build("&{a: end}");
            ComplementInfo info = Complements.complementInfo(b.ss, b.ss.top(), b.lr);
            assertEquals(b.ss.top(), info.element());
        }

        @Test
        void uniquenessFlag() {
            var b = build("end");
            ComplementInfo info = Complements.complementInfo(b.ss, b.ss.top(), b.lr);
            assertTrue(info.hasComplement());
            assertTrue(info.isUnique());
        }
    }

    // =========================================================================
    // isComplemented
    // =========================================================================

    @Nested
    class IsComplemented {

        @Test
        void trivialLatticeComplemented() {
            var b = build("end");
            assertTrue(Complements.isComplemented(b.ss, b.lr));
        }

        @Test
        void twoChainComplemented() {
            var b = build("&{a: end}");
            assertTrue(Complements.isComplemented(b.ss, b.lr));
        }

        @Test
        void threeChainNotComplemented() {
            var b = build("&{a: &{b: end}}");
            assertTrue(b.lr.isLattice());
            assertFalse(Complements.isComplemented(b.ss, b.lr));
        }

        @Test
        void diamondM3Complemented() {
            var b = build("+{a: end, b: end}");
            if (b.lr.isLattice() && b.ss.states().size() >= 3) {
                assertTrue(Complements.isComplemented(b.ss, b.lr));
            }
        }

        @Test
        void parallelComplemented() {
            var b = build("(&{a: end} || &{b: end})");
            assertTrue(b.lr.isLattice());
            assertTrue(Complements.isComplemented(b.ss, b.lr));
        }
    }

    // =========================================================================
    // isUniquelyComplemented
    // =========================================================================

    @Nested
    class IsUniquelyComplemented {

        @Test
        void trivial() {
            var b = build("end");
            assertTrue(Complements.isUniquelyComplemented(b.ss, b.lr));
        }

        @Test
        void twoChainUnique() {
            var b = build("&{a: end}");
            assertTrue(Complements.isUniquelyComplemented(b.ss, b.lr));
        }

        @Test
        void booleanUnique() {
            var b = build("(&{a: end} || &{b: end})");
            assertTrue(b.lr.isLattice());
            DistributivityResult dr = DistributivityChecker.checkDistributive(b.ss);
            if (dr.isDistributive()) {
                assertTrue(Complements.isUniquelyComplemented(b.ss, b.lr));
            }
        }
    }

    // =========================================================================
    // isBoolean
    // =========================================================================

    @Nested
    class IsBoolean {

        @Test
        void trivialBoolean() {
            var b = build("end");
            assertTrue(Complements.isBoolean(b.ss, b.lr));
        }

        @Test
        void twoChainBoolean() {
            var b = build("&{a: end}");
            assertTrue(Complements.isBoolean(b.ss, b.lr));
        }

        @Test
        void threeChainNotBoolean() {
            var b = build("&{a: &{b: end}}");
            assertFalse(Complements.isBoolean(b.ss, b.lr));
        }

        @Test
        void parallelBinaryBoolean() {
            var b = build("(&{a: end} || &{b: end})");
            assertTrue(b.lr.isLattice());
            DistributivityResult dr = DistributivityChecker.checkDistributive(b.ss);
            if (dr.isDistributive() && b.ss.states().size() == 4) {
                assertTrue(Complements.isBoolean(b.ss, b.lr));
            }
        }

        @Test
        void nonDistributiveNotBoolean() {
            var b = build("&{a: end, b: end, c: end}");
            DistributivityResult dr = DistributivityChecker.checkDistributive(b.ss);
            if (b.lr.isLattice() && !dr.isDistributive()) {
                assertFalse(Complements.isBoolean(b.ss, b.lr));
            }
        }
    }

    // =========================================================================
    // booleanRank
    // =========================================================================

    @Nested
    class BooleanRank {

        @Test
        void rank0() {
            var b = build("end");
            assertEquals(Integer.valueOf(0), Complements.booleanRank(b.ss, b.lr));
        }

        @Test
        void rank1() {
            var b = build("&{a: end}");
            assertEquals(Integer.valueOf(1), Complements.booleanRank(b.ss, b.lr));
        }

        @Test
        void nonBooleanNull() {
            var b = build("&{a: &{b: end}}");
            assertNull(Complements.booleanRank(b.ss, b.lr));
        }
    }

    // =========================================================================
    // isRelativelyComplemented
    // =========================================================================

    @Nested
    class IsRelativelyComplemented {

        @Test
        void trivial() {
            var b = build("end");
            assertTrue(Complements.isRelativelyComplemented(b.ss, b.lr));
        }

        @Test
        void twoChain() {
            var b = build("&{a: end}");
            assertTrue(Complements.isRelativelyComplemented(b.ss, b.lr));
        }

        @Test
        void booleanRelativelyComplemented() {
            var b = build("(&{a: end} || &{b: end})");
            if (Complements.isBoolean(b.ss, b.lr)) {
                assertTrue(Complements.isRelativelyComplemented(b.ss, b.lr));
            }
        }
    }

    // =========================================================================
    // findRelativeComplement
    // =========================================================================

    @Nested
    class FindRelativeComplement {

        @Test
        void fullInterval() {
            var b = build("&{a: end}");
            int top = b.ss.top();
            int bottom = b.ss.bottom();
            List<Integer> comps = Complements.findRelativeComplement(
                    b.ss, top, bottom, top, b.lr);
            assertTrue(comps.contains(bottom));
        }

        @Test
        void elementOutsideInterval() {
            var b = build("&{a: &{b: end}}");
            assertTrue(b.lr.isLattice());
        }
    }

    // =========================================================================
    // analyzeComplements
    // =========================================================================

    @Nested
    class AnalyzeComplements {

        @Test
        void returnsAnalysis() {
            var b = build("&{a: end}");
            ComplementAnalysis result = Complements.analyzeComplements(b.ss, b.lr);
            assertNotNull(result);
        }

        @Test
        void trivialAnalysis() {
            var b = build("end");
            ComplementAnalysis result = Complements.analyzeComplements(b.ss, b.lr);
            assertTrue(result.isComplemented());
            assertTrue(result.isUniquelyComplemented());
            assertTrue(result.isBoolean());
            assertEquals(Integer.valueOf(0), result.booleanRank());
            assertEquals(0, result.uncomplementedElements().size());
        }

        @Test
        void twoChainAnalysis() {
            var b = build("&{a: end}");
            ComplementAnalysis result = Complements.analyzeComplements(b.ss, b.lr);
            assertTrue(result.isComplemented());
            assertTrue(result.isBoolean());
            assertEquals(Integer.valueOf(1), result.booleanRank());
            assertEquals(2, result.elementCount());
        }

        @Test
        void threeChainAnalysis() {
            var b = build("&{a: &{b: end}}");
            ComplementAnalysis result = Complements.analyzeComplements(b.ss, b.lr);
            assertFalse(result.isComplemented());
            assertFalse(result.isBoolean());
            assertTrue(result.uncomplementedElements().size() > 0);
        }

        @Test
        void nonLatticeAnalysis() {
            var b = build("&{a: end, b: rec X . &{a: X}}");
            if (!b.lr.isLattice()) {
                ComplementAnalysis result = Complements.analyzeComplements(b.ss, b.lr);
                assertFalse(result.isComplemented());
                assertFalse(result.isBoolean());
                assertEquals(0, result.complementCount());
            }
        }

        @Test
        void complementMapPopulated() {
            var b = build("&{a: end}");
            ComplementAnalysis result = Complements.analyzeComplements(b.ss, b.lr);
            assertEquals(result.elementCount(), result.complementMap().size());
        }

        @Test
        void parallelAnalysis() {
            var b = build("(&{a: end} || &{b: end})");
            ComplementAnalysis result = Complements.analyzeComplements(b.ss, b.lr);
            assertTrue(result.elementCount() > 0);
        }

        @Test
        void complementCountSymmetric() {
            var b = build("&{a: end}");
            ComplementAnalysis result = Complements.analyzeComplements(b.ss, b.lr);
            for (var e : result.complementMap().entrySet()) {
                int a = e.getKey();
                for (int c : e.getValue()) {
                    List<Integer> rev = result.complementMap().getOrDefault(c, List.of());
                    assertTrue(rev.contains(a),
                            "Symmetry violated: " + c + " does not complement " + a);
                }
            }
        }
    }

    // =========================================================================
    // Benchmark protocols
    // =========================================================================

    static Stream<Arguments> benchmarkAnalysisCases() {
        return Stream.of(
                Arguments.of("iterator",
                        "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"),
                Arguments.of("simple_branch", "&{a: end, b: end}"),
                Arguments.of("binary_select", "+{ok: end, err: end}"),
                Arguments.of("chain_4", "&{a: &{b: &{c: end}}}"),
                Arguments.of("parallel_simple", "(&{a: end} || &{b: end})"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("benchmarkAnalysisCases")
    void benchmarkAnalysis(String name, String source) {
        var b = build(source);
        if (b.lr.isLattice()) {
            ComplementAnalysis result = Complements.analyzeComplements(b.ss, b.lr);
            assertNotNull(result);
            assertTrue(result.elementCount() > 0);
            if (result.isBoolean()) {
                assertTrue(result.isDistributive());
            }
            if (result.isUniquelyComplemented()) {
                assertTrue(result.isComplemented());
            }
        }
    }

    static Stream<Arguments> booleanClassificationCases() {
        return Stream.of(
                Arguments.of("end", "end", true),
                Arguments.of("single_branch", "&{a: end}", true),
                Arguments.of("chain_3", "&{a: &{b: end}}", false),
                Arguments.of("chain_4", "&{a: &{b: &{c: end}}}", false));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("booleanClassificationCases")
    void booleanClassification(String name, String source, boolean expected) {
        var b = build(source);
        assertTrue(b.lr.isLattice());
        assertEquals(expected, Complements.isBoolean(b.ss, b.lr));
    }

    // =========================================================================
    // Properties and invariants
    // =========================================================================

    @Nested
    class Properties {

        @Test
        void topBottomAlwaysComplement() {
            var b = build("&{a: end, b: end}");
            if (b.lr.isLattice()) {
                int top = b.ss.top();
                int bottom = b.ss.bottom();
                List<Integer> compsTop = Complements.findComplements(b.ss, top, b.lr);
                List<Integer> compsBot = Complements.findComplements(b.ss, bottom, b.lr);
                assertTrue(compsTop.contains(bottom));
                assertTrue(compsBot.contains(top));
            }
        }

        @Test
        void distributiveImpliesUniqueComplements() {
            var b = build("(&{a: end} || &{b: end})");
            DistributivityResult dr = DistributivityChecker.checkDistributive(b.ss);
            if (b.lr.isLattice() && dr.isDistributive()) {
                ComplementAnalysis result = Complements.analyzeComplements(b.ss, b.lr);
                if (result.isComplemented()) {
                    assertTrue(result.isUniquelyComplemented());
                }
            }
        }

        @Test
        void booleanImpliesRelativelyComplemented() {
            var b = build("end");
            if (Complements.isBoolean(b.ss, b.lr)) {
                assertTrue(Complements.isRelativelyComplemented(b.ss, b.lr));
            }
        }

        @Test
        void complementInvolution() {
            var b = build("&{a: end}");
            if (b.lr.isLattice()) {
                for (int s : b.ss.states()) {
                    List<Integer> comps = Complements.findComplements(b.ss, s, b.lr);
                    for (int c : comps) {
                        List<Integer> rev = Complements.findComplements(b.ss, c, b.lr);
                        // Rev contains the rep of s
                        int sRep = LatticeChecker.checkLattice(b.ss).sccMap().getOrDefault(s, s);
                        assertTrue(rev.contains(sRep) || rev.contains(s),
                                "Involution violated for s=" + s + " c=" + c);
                    }
                }
            }
        }
    }
}
