package com.bica.reborn.persistenthomology;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.persistenthomology.PersistentHomologyChecker.*;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for persistent homology of session type lattices (Step 33c).
 * Mirrors all 62 Python tests from test_persistent_homology.py.
 */
class PersistentHomologyCheckerTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Test: PersistencePair
    // -----------------------------------------------------------------------

    @Nested
    class TestPersistencePair {

        @Test
        void finitePair() {
            var p = new PersistencePair(1.0, 3.0, 0);
            assertEquals(2.0, p.persistence(), 1e-12);
            assertFalse(p.isInfinite());
        }

        @Test
        void infinitePair() {
            var p = new PersistencePair(0.0, Double.POSITIVE_INFINITY, 0);
            assertEquals(Double.POSITIVE_INFINITY, p.persistence());
            assertTrue(p.isInfinite());
        }

        @Test
        void zeroPersistence() {
            var p = new PersistencePair(2.0, 2.0, 1);
            assertEquals(0.0, p.persistence(), 1e-12);
            assertFalse(p.isInfinite());
        }

        @Test
        void dimension() {
            var p0 = new PersistencePair(0, 1, 0);
            var p1 = new PersistencePair(0, 1, 1);
            assertEquals(0, p0.dimension());
            assertEquals(1, p1.dimension());
        }
    }

    // -----------------------------------------------------------------------
    // Test: PersistenceDiagram
    // -----------------------------------------------------------------------

    @Nested
    class TestPersistenceDiagram {

        @Test
        void emptyDiagram() {
            var pd = new PersistenceDiagram(List.of(), List.of(), 0.0);
            assertEquals(0, pd.numPairs());
            assertTrue(pd.allPairs().isEmpty());
        }

        @Test
        void mixedDiagram() {
            var fp = new PersistencePair(0, 2, 0);
            var ip = new PersistencePair(0, Double.POSITIVE_INFINITY, 0);
            var pd = new PersistenceDiagram(List.of(fp), List.of(ip), 3.0);
            assertEquals(2, pd.numPairs());
            assertEquals(2, pd.allPairs().size());
        }

        @Test
        void pairsInDim() {
            var p0 = new PersistencePair(0, 1, 0);
            var p1 = new PersistencePair(1, 2, 1);
            var pd = new PersistenceDiagram(List.of(p0, p1), List.of(), 2.0);
            assertEquals(1, pd.pairsInDim(0).size());
            assertEquals(1, pd.pairsInDim(1).size());
            assertEquals(0, pd.pairsInDim(2).size());
        }
    }

    // -----------------------------------------------------------------------
    // Test: Filtration construction
    // -----------------------------------------------------------------------

    @Nested
    class TestRankFiltration {

        @Test
        void endType() {
            var ss = build("end");
            var filt = PersistentHomologyChecker.rankFiltration(ss);
            assertTrue(filt.numSimplices() >= 1);
            assertTrue(filt.maxLevel() >= 0);
        }

        @Test
        void simpleBranch() {
            var ss = build("&{a: end}");
            var filt = PersistentHomologyChecker.rankFiltration(ss);
            assertTrue(filt.numSimplices() >= 2);
            assertTrue(filt.maxLevel() >= 0);
        }

        @Test
        void chainOrdering() {
            var ss = build("&{a: &{b: end}}");
            var filt = PersistentHomologyChecker.rankFiltration(ss);
            long vertices = filt.simplices().stream().filter(s -> s.size() == 1).count();
            long edges = filt.simplices().stream().filter(s -> s.size() == 2).count();
            assertTrue(vertices >= 2);
            assertTrue(edges >= 1);
        }

        @Test
        void flatBranch() {
            var ss = build("&{a: end, b: end}");
            var filt = PersistentHomologyChecker.rankFiltration(ss);
            assertTrue(filt.numSimplices() >= 3);
        }

        @Test
        void parallel() {
            var ss = build("(&{a: end} || &{b: end})");
            var filt = PersistentHomologyChecker.rankFiltration(ss);
            assertTrue(filt.numSimplices() >= 4);
        }
    }

    @Nested
    class TestReverseRankFiltration {

        @Test
        void basic() {
            var ss = build("&{a: &{b: end}}");
            var filt = PersistentHomologyChecker.reverseRankFiltration(ss);
            assertTrue(filt.numSimplices() >= 2);
            // Top should have filtration value 0
            for (int i = 0; i < filt.simplices().size(); i++) {
                var s = filt.simplices().get(i);
                if (s.size() == 1 && s.get(0) == ss.top()) {
                    assertEquals(0.0, filt.filtrationValues().get(i), 1e-12);
                }
            }
        }

        @Test
        void matchesRankSimplexCount() {
            var ss = build("&{a: end, b: end}");
            var rFilt = PersistentHomologyChecker.rankFiltration(ss);
            var rrFilt = PersistentHomologyChecker.reverseRankFiltration(ss);
            assertEquals(rFilt.numSimplices(), rrFilt.numSimplices());
        }
    }

    @Nested
    class TestSublevelFiltration {

        @Test
        void fromTop() {
            var ss = build("&{a: &{b: end}}");
            var filt = PersistentHomologyChecker.sublevelFiltration(ss, ss.top());
            for (int i = 0; i < filt.simplices().size(); i++) {
                var s = filt.simplices().get(i);
                if (s.size() == 1 && s.get(0) == ss.top()) {
                    assertEquals(0.0, filt.filtrationValues().get(i), 1e-12);
                }
            }
        }

        @Test
        void fromBottom() {
            var ss = build("&{a: &{b: end}}");
            var filt = PersistentHomologyChecker.sublevelFiltration(ss, ss.bottom());
            for (int i = 0; i < filt.simplices().size(); i++) {
                var s = filt.simplices().get(i);
                if (s.size() == 1 && s.get(0) == ss.bottom()) {
                    assertEquals(0.0, filt.filtrationValues().get(i), 1e-12);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Test: Persistence computation
    // -----------------------------------------------------------------------

    @Nested
    class TestComputePersistence {

        @Test
        void emptyFiltration() {
            var filt = new Filtration(List.of(), List.of(), 0.0, 0);
            var pd = PersistentHomologyChecker.computePersistence(filt);
            assertEquals(0, pd.numPairs());
        }

        @Test
        void singleVertex() {
            var filt = new Filtration(
                    List.of(List.of(0)),
                    List.of(0.0),
                    0.0, 1);
            var pd = PersistentHomologyChecker.computePersistence(filt);
            assertEquals(1, pd.infinitePairs().size());
            assertEquals(0, pd.infinitePairs().get(0).dimension());
        }

        @Test
        void twoVerticesOneEdge() {
            var filt = new Filtration(
                    List.of(List.of(0), List.of(1), List.of(0, 1)),
                    List.of(0.0, 1.0, 1.0),
                    1.0, 3);
            var pd = PersistentHomologyChecker.computePersistence(filt);
            assertTrue(pd.numPairs() >= 1);
        }

        @Test
        void chainType() {
            var ss = build("&{a: &{b: end}}");
            var filt = PersistentHomologyChecker.rankFiltration(ss);
            var pd = PersistentHomologyChecker.computePersistence(filt);
            long infDim0 = pd.infinitePairs().stream().filter(p -> p.dimension() == 0).count();
            assertTrue(infDim0 >= 1);
        }

        @Test
        void branchType() {
            var ss = build("&{a: end, b: end}");
            var filt = PersistentHomologyChecker.rankFiltration(ss);
            var pd = PersistentHomologyChecker.computePersistence(filt);
            assertTrue(pd.numPairs() >= 1);
        }

        @Test
        void parallelType() {
            var ss = build("(&{a: end} || &{b: end})");
            var filt = PersistentHomologyChecker.rankFiltration(ss);
            var pd = PersistentHomologyChecker.computePersistence(filt);
            assertTrue(pd.numPairs() >= 1);
        }
    }

    // -----------------------------------------------------------------------
    // Test: Persistence invariants
    // -----------------------------------------------------------------------

    @Nested
    class TestPersistenceInvariants {

        @Test
        void totalPersistenceEmpty() {
            var pd = new PersistenceDiagram(List.of(), List.of(), 0.0);
            assertEquals(0.0, PersistentHomologyChecker.totalPersistence(pd), 1e-12);
        }

        @Test
        void totalPersistenceFinite() {
            var p1 = new PersistencePair(0, 2, 0);
            var p2 = new PersistencePair(1, 4, 0);
            var pd = new PersistenceDiagram(List.of(p1, p2), List.of(), 4.0);
            assertEquals(5.0, PersistentHomologyChecker.totalPersistence(pd), 1e-12);
        }

        @Test
        void maxPersistenceEmpty() {
            var pd = new PersistenceDiagram(List.of(), List.of(), 0.0);
            assertEquals(0.0, PersistentHomologyChecker.maxPersistence(pd), 1e-12);
        }

        @Test
        void maxPersistenceFinite() {
            var p1 = new PersistencePair(0, 2, 0);
            var p2 = new PersistencePair(1, 5, 0);
            var pd = new PersistenceDiagram(List.of(p1, p2), List.of(), 5.0);
            assertEquals(4.0, PersistentHomologyChecker.maxPersistence(pd), 1e-12);
        }

        @Test
        void entropyEmpty() {
            var pd = new PersistenceDiagram(List.of(), List.of(), 0.0);
            assertEquals(0.0, PersistentHomologyChecker.persistenceEntropy(pd), 1e-12);
        }

        @Test
        void entropySingleBar() {
            var p = new PersistencePair(0, 3, 0);
            var pd = new PersistenceDiagram(List.of(p), List.of(), 3.0);
            assertEquals(0.0, PersistentHomologyChecker.persistenceEntropy(pd), 1e-12);
        }

        @Test
        void entropyUniform() {
            var p1 = new PersistencePair(0, 2, 0);
            var p2 = new PersistencePair(1, 3, 0);
            var pd = new PersistenceDiagram(List.of(p1, p2), List.of(), 3.0);
            assertEquals(1.0, PersistentHomologyChecker.persistenceEntropy(pd), 1e-10);
        }

        @Test
        void entropyNonuniform() {
            var p1 = new PersistencePair(0, 1, 0);
            var p2 = new PersistencePair(0, 3, 0);
            var pd = new PersistenceDiagram(List.of(p1, p2), List.of(), 3.0);
            double ent = PersistentHomologyChecker.persistenceEntropy(pd);
            assertTrue(ent > 0);
            assertTrue(ent < 1.0);
        }
    }

    // -----------------------------------------------------------------------
    // Test: Distances
    // -----------------------------------------------------------------------

    @Nested
    class TestBottleneckDistance {

        @Test
        void identicalDiagrams() {
            var p = new PersistencePair(0, 2, 0);
            var pd = new PersistenceDiagram(List.of(p), List.of(), 2.0);
            assertEquals(0.0, PersistentHomologyChecker.bottleneckDistance(pd, pd), 1e-12);
        }

        @Test
        void emptyDiagrams() {
            var pd1 = new PersistenceDiagram(List.of(), List.of(), 0.0);
            var pd2 = new PersistenceDiagram(List.of(), List.of(), 0.0);
            assertEquals(0.0, PersistentHomologyChecker.bottleneckDistance(pd1, pd2), 1e-12);
        }

        @Test
        void oneEmpty() {
            var p = new PersistencePair(0, 4, 0);
            var pd1 = new PersistenceDiagram(List.of(p), List.of(), 4.0);
            var pd2 = new PersistenceDiagram(List.of(), List.of(), 0.0);
            double dist = PersistentHomologyChecker.bottleneckDistance(pd1, pd2);
            assertTrue(dist > 0);
        }

        @Test
        void symmetry() {
            var p1 = new PersistencePair(0, 2, 0);
            var p2 = new PersistencePair(0, 3, 0);
            var pd1 = new PersistenceDiagram(List.of(p1), List.of(), 2.0);
            var pd2 = new PersistenceDiagram(List.of(p2), List.of(), 3.0);
            assertEquals(
                    PersistentHomologyChecker.bottleneckDistance(pd1, pd2),
                    PersistentHomologyChecker.bottleneckDistance(pd2, pd1),
                    1e-12);
        }

        @Test
        void nonnegative() {
            var p1 = new PersistencePair(0, 2, 0);
            var p2 = new PersistencePair(1, 5, 0);
            var pd1 = new PersistenceDiagram(List.of(p1), List.of(), 2.0);
            var pd2 = new PersistenceDiagram(List.of(p2), List.of(), 5.0);
            assertTrue(PersistentHomologyChecker.bottleneckDistance(pd1, pd2) >= 0);
        }
    }

    @Nested
    class TestWassersteinDistance {

        @Test
        void identical() {
            var p = new PersistencePair(0, 2, 0);
            var pd = new PersistenceDiagram(List.of(p), List.of(), 2.0);
            assertEquals(0.0, PersistentHomologyChecker.wassersteinDistance(pd, pd), 1e-12);
        }

        @Test
        void empty() {
            var pd = new PersistenceDiagram(List.of(), List.of(), 0.0);
            assertEquals(0.0, PersistentHomologyChecker.wassersteinDistance(pd, pd), 1e-12);
        }

        @Test
        void symmetry() {
            var p1 = new PersistencePair(0, 2, 0);
            var p2 = new PersistencePair(0, 4, 0);
            var pd1 = new PersistenceDiagram(List.of(p1), List.of(), 2.0);
            var pd2 = new PersistenceDiagram(List.of(p2), List.of(), 4.0);
            assertEquals(
                    PersistentHomologyChecker.wassersteinDistance(pd1, pd2),
                    PersistentHomologyChecker.wassersteinDistance(pd2, pd1),
                    1e-12);
        }

        @Test
        void nonnegative() {
            var p1 = new PersistencePair(0, 1, 0);
            var p2 = new PersistencePair(2, 5, 0);
            var pd1 = new PersistenceDiagram(List.of(p1), List.of(), 1.0);
            var pd2 = new PersistenceDiagram(List.of(p2), List.of(), 5.0);
            assertTrue(PersistentHomologyChecker.wassersteinDistance(pd1, pd2) >= 0);
        }
    }

    // -----------------------------------------------------------------------
    // Test: Convenience functions
    // -----------------------------------------------------------------------

    @Nested
    class TestConvenienceFunctions {

        @Test
        void persistencePairsChain() {
            var ss = build("&{a: &{b: end}}");
            var pairs = PersistentHomologyChecker.persistencePairs(ss);
            assertTrue(pairs.size() >= 1);
        }

        @Test
        void persistencePairsBranch() {
            var ss = build("&{a: end, b: end}");
            var pairs = PersistentHomologyChecker.persistencePairs(ss);
            assertTrue(pairs.size() >= 1);
        }

        @Test
        void bettiBarcodes() {
            var ss = build("&{a: &{b: end}}");
            var bc = PersistentHomologyChecker.bettiBarcodes(ss);
            assertTrue(bc.containsKey(0));
            assertTrue(bc.get(0).size() >= 1);
        }
    }

    // -----------------------------------------------------------------------
    // Test: Full analysis
    // -----------------------------------------------------------------------

    @Nested
    class TestAnalyzePersistence {

        @Test
        void end() {
            var ss = build("end");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertNotNull(result);
            assertEquals(ss.states().size(), result.numStates());
        }

        @Test
        void chain() {
            var ss = build("&{a: &{b: end}}");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertTrue(result.betti0() >= 1);
            assertTrue(result.totalPersistence() >= 0);
            assertTrue(result.maxPersistence() >= 0);
            assertTrue(result.persistenceEntropy() >= 0);
        }

        @Test
        void branch() {
            var ss = build("&{a: end, b: end}");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertTrue(result.betti0() >= 1);
        }

        @Test
        void parallel() {
            var ss = build("(&{a: end} || &{b: end})");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertTrue(result.betti0() >= 1);
            assertTrue(result.numStates() >= 4);
        }

        @Test
        void recursive() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertTrue(result.betti0() >= 1);
        }

        @Test
        void selection() {
            var ss = build("+{ok: end, err: end}");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertTrue(result.betti0() >= 1);
        }
    }

    // -----------------------------------------------------------------------
    // Test: Benchmark protocols
    // -----------------------------------------------------------------------

    @Nested
    class TestBenchmarkProtocols {

        @Test
        void javaIterator() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertTrue(result.betti0() >= 1);
            assertEquals(4, result.numStates());
        }

        @Test
        void fileObject() {
            var ss = build("&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertTrue(result.betti0() >= 1);
            assertEquals(5, result.numStates());
        }

        @Test
        void smtp() {
            var ss = build(
                    "&{connect: &{ehlo: rec X . &{mail: &{rcpt: &{data: "
                    + "+{OK: X, ERR: X}}}, quit: end}}}");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertTrue(result.betti0() >= 1);
            assertEquals(7, result.numStates());
        }

        @Test
        void http() {
            var ss = build(
                    "&{connect: rec X . &{request: +{OK200: &{readBody: X}, "
                    + "ERR4xx: X, ERR5xx: X}, close: end}}");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertTrue(result.betti0() >= 1);
        }

        @Test
        void simpleParallel() {
            var ss = build("(&{a: end} || &{b: end})");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertTrue(result.betti0() >= 1);
        }

        @Test
        void deepChain() {
            var ss = build("&{a: &{b: &{c: &{d: end}}}}");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertTrue(result.betti0() >= 1);
            assertEquals(5, result.numStates());
        }

        @Test
        void wideBranch() {
            var ss = build("&{a: end, b: end, c: end, d: end}");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertTrue(result.betti0() >= 1);
        }

        @Test
        void nestedSelection() {
            var ss = build("&{init: +{ok: &{use: end}, err: end}}");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertTrue(result.betti0() >= 1);
        }
    }

    // -----------------------------------------------------------------------
    // Test: Distance between benchmarks
    // -----------------------------------------------------------------------

    @Nested
    class TestProtocolDistances {

        @Test
        void sameProtocolDistanceZero() {
            var ss = build("&{a: end, b: end}");
            var filt = PersistentHomologyChecker.rankFiltration(ss);
            var pd = PersistentHomologyChecker.computePersistence(filt);
            assertEquals(0.0, PersistentHomologyChecker.bottleneckDistance(pd, pd), 1e-12);
        }

        @Test
        void differentProtocolsNonnegative() {
            var ss1 = build("&{a: end}");
            var ss2 = build("&{a: end, b: end}");
            var pd1 = PersistentHomologyChecker.computePersistence(
                    PersistentHomologyChecker.rankFiltration(ss1));
            var pd2 = PersistentHomologyChecker.computePersistence(
                    PersistentHomologyChecker.rankFiltration(ss2));
            assertTrue(PersistentHomologyChecker.bottleneckDistance(pd1, pd2) >= 0.0);
        }

        @Test
        void wassersteinBetweenProtocols() {
            var ss1 = build("&{a: &{b: end}}");
            var ss2 = build("&{a: end, b: end}");
            var pd1 = PersistentHomologyChecker.computePersistence(
                    PersistentHomologyChecker.rankFiltration(ss1));
            var pd2 = PersistentHomologyChecker.computePersistence(
                    PersistentHomologyChecker.rankFiltration(ss2));
            assertTrue(PersistentHomologyChecker.wassersteinDistance(pd1, pd2) >= 0.0);
        }
    }

    // -----------------------------------------------------------------------
    // Test: Edge cases
    // -----------------------------------------------------------------------

    @Nested
    class TestEdgeCases {

        @Test
        void singleState() {
            var ss = build("end");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertTrue(result.numStates() >= 1);
        }

        @Test
        void twoStateChain() {
            var ss = build("&{a: end}");
            var result = PersistentHomologyChecker.analyzePersistence(ss);
            assertEquals(2, result.numStates());
        }

        @Test
        void entropyNoFiniteBars() {
            var pd = new PersistenceDiagram(
                    List.of(),
                    List.of(new PersistencePair(0, Double.POSITIVE_INFINITY, 0)),
                    5.0);
            assertEquals(0.0, PersistentHomologyChecker.persistenceEntropy(pd), 1e-12);
        }
    }
}
