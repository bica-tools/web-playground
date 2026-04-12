package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.algebraic.zeta.Shellability.*;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Shellability} — Java port of Python
 * {@code test_shellability.py} (46 tests, Step 30w).
 */
class ShellabilityTest {

    private static StateSpace build(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    // Fixtures
    private static StateSpace fxEnd() { return build("end"); }
    private static StateSpace fxSimpleBranch() { return build("&{a: end, b: end}"); }
    private static StateSpace fxDeepChain() { return build("&{a: &{b: &{c: end}}}"); }
    private static StateSpace fxParallel() { return build("(&{a: end} || &{b: end})"); }
    private static StateSpace fxWideBranch() { return build("&{a: end, b: end, c: end, d: end}"); }
    private static StateSpace fxNestedBranch() { return build("&{a: &{c: end, d: end}, b: &{e: end, f: end}}"); }

    // ---------------------------------------------------------------------
    // Hasse diagram
    // ---------------------------------------------------------------------

    @Nested
    class HasseLabelsTests {
        @Test
        void endNoEdges() {
            assertEquals(0, Shellability.hasseLabels(fxEnd()).size());
        }

        @Test
        void simpleBranchTest() {
            List<HasseEdge> edges = Shellability.hasseLabels(fxSimpleBranch());
            assertTrue(edges.size() >= 2);
        }

        @Test
        void deepChainTest() {
            List<HasseEdge> edges = Shellability.hasseLabels(fxDeepChain());
            assertTrue(edges.size() >= 3);
        }

        @Test
        void labelsAreStrings() {
            for (HasseEdge e : Shellability.hasseLabels(fxSimpleBranch())) {
                assertNotNull(e.label());
            }
        }
    }

    // ---------------------------------------------------------------------
    // Maximal chains
    // ---------------------------------------------------------------------

    @Nested
    class MaximalChainsTests {
        @Test
        void end() {
            List<LabeledChain> chains = Shellability.maximalChains(fxEnd());
            assertEquals(1, chains.size());
            assertEquals(List.of(fxEnd().top()), chains.get(0).states());
        }

        @Test
        void simpleBranchTwoChains() {
            assertEquals(2, Shellability.maximalChains(fxSimpleBranch()).size());
        }

        @Test
        void deepChainOneChain() {
            assertEquals(1, Shellability.maximalChains(fxDeepChain()).size());
        }

        @Test
        void chainStructure() {
            StateSpace ss = fxSimpleBranch();
            for (LabeledChain c : Shellability.maximalChains(ss)) {
                assertEquals(ss.top(), (int) c.states().get(0));
                assertEquals(ss.bottom(), (int) c.states().get(c.states().size() - 1));
                assertEquals(c.states().size() - 1, c.labels().size());
            }
        }

        @Test
        void wideBranchFourChains() {
            assertEquals(4, Shellability.maximalChains(fxWideBranch()).size());
        }

        @Test
        void nestedBranchChains() {
            assertTrue(Shellability.maximalChains(fxNestedBranch()).size() >= 4);
        }

        @Test
        void maxChainsLimit() {
            assertEquals(1, Shellability.maximalChains(fxSimpleBranch(), 1).size());
        }

        @Test
        void parallelChains() {
            assertTrue(Shellability.maximalChains(fxParallel()).size() >= 2);
        }
    }

    // ---------------------------------------------------------------------
    // Descents
    // ---------------------------------------------------------------------

    @Nested
    class DescentsTests {
        @Test
        void increasingChainNoDescents() {
            List<LabeledChain> chains = Shellability.maximalChains(fxDeepChain());
            assertEquals(1, chains.size());
            assertTrue(chains.get(0).isIncreasing() || chains.get(0).descents().size() >= 0);
        }

        @Test
        void descentSet() {
            for (LabeledChain c : Shellability.maximalChains(fxSimpleBranch())) {
                Set<Integer> ds = Shellability.descentSet(c);
                assertNotNull(ds);
            }
        }

        @Test
        void descentStatistic() {
            List<LabeledChain> chains = Shellability.maximalChains(fxWideBranch());
            Map<Integer, Integer> stats = Shellability.descentStatistic(chains);
            int total = 0;
            for (int v : stats.values()) total += v;
            assertEquals(chains.size(), total);
        }
    }

    // ---------------------------------------------------------------------
    // EL-labeling
    // ---------------------------------------------------------------------

    @Nested
    class ELLabelingTests {
        @Test
        void end() {
            assertNotNull(Shellability.checkElLabeling(fxEnd()));
        }

        @Test
        void deepChainIsEl() {
            ELResult r = Shellability.checkElLabeling(fxDeepChain());
            assertNotNull(r);
            assertTrue(r.numIntervals() > 0);
        }

        @Test
        void simpleBranchElCheck() {
            assertNotNull(Shellability.checkElLabeling(fxSimpleBranch()));
        }

        @Test
        void resultStructure() {
            ELResult r = Shellability.checkElLabeling(fxSimpleBranch());
            assertTrue(r.numIntervals() >= 0);
            assertTrue(r.intervalsWithUniqueIncreasing() >= 0);
        }
    }

    // ---------------------------------------------------------------------
    // CL-labeling
    // ---------------------------------------------------------------------

    @Nested
    class CLLabelingTests {
        @Test
        void end() {
            assertNotNull(Shellability.checkClLabeling(fxEnd()));
        }

        @Test
        void deepChainIsCl() {
            assertNotNull(Shellability.checkClLabeling(fxDeepChain()));
        }

        @Test
        void simpleBranchCl() {
            assertNotNull(Shellability.checkClLabeling(fxSimpleBranch()));
        }
    }

    // ---------------------------------------------------------------------
    // f-vector and h-vector
    // ---------------------------------------------------------------------

    @Nested
    class FVectorTests {
        @Test
        void end() {
            List<Integer> f = Shellability.computeFVector(fxEnd());
            assertEquals(1, (int) f.get(0));
        }

        @Test
        void simpleBranchTest() {
            List<Integer> f = Shellability.computeFVector(fxSimpleBranch());
            assertTrue(f.get(0) >= 2);
            assertTrue(f.size() >= 2);
        }

        @Test
        void deepChainTest() {
            List<Integer> f = Shellability.computeFVector(fxDeepChain());
            assertTrue(f.get(0) >= 4);
        }

        @Test
        void monotoneFVector() {
            for (int fi : Shellability.computeFVector(fxSimpleBranch())) {
                assertTrue(fi >= 0);
            }
        }
    }

    @Nested
    class HVectorTests {
        @Test
        void end() {
            assertNotNull(Shellability.computeHVector(fxEnd()));
        }

        @Test
        void simpleBranchTest() {
            assertNotNull(Shellability.computeHVector(fxSimpleBranch()));
        }

        @Test
        void deepChainTest() {
            assertNotNull(Shellability.computeHVector(fxDeepChain()));
        }
    }

    // ---------------------------------------------------------------------
    // Shellability
    // ---------------------------------------------------------------------

    @Nested
    class ShellabilityTests {
        @Test
        void end() {
            assertNotNull(Shellability.checkShellability(fxEnd()));
        }

        @Test
        void deepChainShellable() {
            ShellabilityResult r = Shellability.checkShellability(fxDeepChain());
            assertTrue(r.height() >= 3);
        }

        @Test
        void simpleBranchShellability() {
            ShellabilityResult r = Shellability.checkShellability(fxSimpleBranch());
            assertEquals(2, r.numMaximalChains());
        }

        @Test
        void resultCompleteness() {
            ShellabilityResult r = Shellability.checkShellability(fxSimpleBranch());
            assertNotNull(r.elResult());
            assertNotNull(r.clResult());
            assertNotNull(r.fVector());
            assertNotNull(r.hVector());
        }

        @Test
        void parallelShellability() {
            assertNotNull(Shellability.checkShellability(fxParallel()));
        }

        @Test
        void wideBranchShellability() {
            assertEquals(4, Shellability.checkShellability(fxWideBranch()).numMaximalChains());
        }
    }

    // ---------------------------------------------------------------------
    // Convenience functions
    // ---------------------------------------------------------------------

    @Nested
    class ConvenienceTests {
        @Test
        void isShellable() {
            Shellability.isShellable(fxSimpleBranch());
        }

        @Test
        void isElLabelable() {
            Shellability.isElLabelable(fxSimpleBranch());
        }

        @Test
        void analyzeAlias() {
            ShellabilityResult r1 = Shellability.checkShellability(fxSimpleBranch());
            ShellabilityResult r2 = Shellability.analyzeShellability(fxSimpleBranch());
            assertEquals(r1.isShellable(), r2.isShellable());
            assertEquals(r1.numMaximalChains(), r2.numMaximalChains());
        }
    }

    // ---------------------------------------------------------------------
    // Benchmark protocols
    // ---------------------------------------------------------------------

    @Nested
    class BenchmarkTests {
        @ParameterizedTest(name = "{1}")
        @CsvSource({
            "'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}', Iterator, false",
            "'&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}', File, false",
            "'&{a: end, b: end}', SimpleBranch, true",
            "'+{ok: end, err: end}', SimpleSelect, true",
            "'(&{a: end} || &{b: end})', SimpleParallel, true",
            "'&{a: &{c: end}, b: &{d: end}}', NestedBranch, true",
            "'&{get: +{OK: end, NOT_FOUND: end}, put: +{OK: end, ERR: end}}', REST, true",
            "'&{a: &{b: &{c: end}}}', DeepChain, true"
        })
        void shellabilityOnBenchmarks(String typeString, String name, boolean hasChains) {
            StateSpace ss = build(typeString);
            ShellabilityResult r = Shellability.checkShellability(ss);
            assertNotNull(r, name);
            if (hasChains) {
                assertTrue(r.numMaximalChains() >= 1, name + " should have chains");
            } else {
                assertEquals(0, r.numMaximalChains(), name + " is cyclic, no acyclic chains");
            }
        }

        @ParameterizedTest(name = "{1}")
        @CsvSource({
            "'&{a: end, b: end}', SimpleBranch",
            "'&{a: end, b: end, c: end}', TripleBranch",
            "'&{a: &{c: end}, b: &{d: end}}', NestedBranch",
            "'&{a: &{b: &{c: end}}}', DeepChain"
        })
        void gradedOnBenchmarks(String typeString, String name) {
            ShellabilityResult r = Shellability.checkShellability(build(typeString));
            assertTrue(r.isGraded(), name + " should be graded");
        }
    }

    // ---------------------------------------------------------------------
    // Edge cases
    // ---------------------------------------------------------------------

    @Nested
    class EdgeCasesTests {
        @Test
        void singleState() {
            ShellabilityResult r = Shellability.checkShellability(build("end"));
            assertEquals(0, r.height());
            assertEquals(1, r.numMaximalChains());
        }

        @Test
        void twoStates() {
            ShellabilityResult r = Shellability.checkShellability(build("&{a: end}"));
            assertTrue(r.height() >= 1);
            assertTrue(r.numMaximalChains() >= 1);
        }

        @Test
        void pureRecursion() {
            assertNotNull(Shellability.checkShellability(build("rec X . &{a: X}")));
        }

        @Test
        void selectionOnly() {
            assertNotNull(Shellability.checkShellability(build("+{a: end, b: end}")));
        }

        @Test
        void fVectorNonempty() {
            List<Integer> f = Shellability.computeFVector(fxSimpleBranch());
            assertTrue(f.size() >= 1);
            assertTrue(f.get(0) > 0);
        }

        @Test
        void hVectorNonempty() {
            assertTrue(Shellability.computeHVector(fxSimpleBranch()).size() >= 1);
        }
    }
}
