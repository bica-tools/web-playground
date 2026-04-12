package com.bica.reborn.fca;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Formal Concept Analysis (Step 30aa).
 * Mirrors the 38 Python tests in test_fca.py.
 */
class FcaCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    // =========================================================================
    // Formal context construction
    // =========================================================================

    @Nested
    class FormalContextTests {

        @Test
        void endContext() {
            var ctx = FcaChecker.formalContext(build("end"));
            assertEquals(1, ctx.objects().size());
            assertEquals(0, ctx.attributes().size());
        }

        @Test
        void singleBranch() {
            var ctx = FcaChecker.formalContext(build("&{a: end}"));
            assertEquals(2, ctx.objects().size());
            assertTrue(ctx.attributes().contains("a"));
            assertTrue(ctx.incidence().values().stream().anyMatch(s -> s.contains("a")));
        }

        @Test
        void twoBranch() {
            var ctx = FcaChecker.formalContext(build("&{a: end, b: end}"));
            assertTrue(ctx.attributes().contains("a"));
            assertTrue(ctx.attributes().contains("b"));
        }

        @Test
        void chain() {
            var ctx = FcaChecker.formalContext(build("&{a: &{b: end}}"));
            assertEquals(2, ctx.attributes().size());
            assertTrue(ctx.attributes().contains("a"));
            assertTrue(ctx.attributes().contains("b"));
        }

        @Test
        void selection() {
            var ctx = FcaChecker.formalContext(build("+{a: end, b: end}"));
            assertTrue(ctx.attributes().contains("a"));
            assertTrue(ctx.attributes().contains("b"));
        }

        @Test
        void parallel() {
            var ctx = FcaChecker.formalContext(build("(&{a: end} || &{b: end})"));
            assertTrue(ctx.attributes().contains("a"));
            assertTrue(ctx.attributes().contains("b"));
        }

        @Test
        void contextTypes() {
            var ctx = FcaChecker.formalContext(build("&{a: end}"));
            assertInstanceOf(FormalContext.class, ctx);
            assertNotNull(ctx.objects());
            assertNotNull(ctx.attributes());
            assertNotNull(ctx.incidence());
        }
    }

    // =========================================================================
    // Derivation operators
    // =========================================================================

    @Nested
    class DerivationOperatorTests {

        @Test
        void emptyAttrsExtent() {
            var ctx = FcaChecker.formalContext(build("&{a: end, b: end}"));
            var result = FcaChecker.extent(Set.of(), ctx);
            assertEquals(new HashSet<>(ctx.objects()), result);
        }

        @Test
        void emptyObjsIntent() {
            var ctx = FcaChecker.formalContext(build("&{a: end, b: end}"));
            var result = FcaChecker.intent(Set.of(), ctx);
            assertEquals(new HashSet<>(ctx.attributes()), result);
        }

        @Test
        void singleAttrExtent() {
            var ctx = FcaChecker.formalContext(build("&{a: &{b: end}}"));
            var aExtent = FcaChecker.extent(Set.of("a"), ctx);
            assertTrue(aExtent.size() >= 1);
        }

        @Test
        void singleObjIntent() {
            var ctx = FcaChecker.formalContext(build("&{a: end, b: end}"));
            // Find the state that has outgoing transitions (initial)
            int initial = ctx.objects().stream()
                    .filter(s -> !ctx.incidence().get(s).isEmpty())
                    .findFirst().orElseThrow();
            var result = FcaChecker.intent(Set.of(initial), ctx);
            assertTrue(result.contains("a"));
            assertTrue(result.contains("b"));
        }

        @Test
        void galoisConnection() {
            var ctx = FcaChecker.formalContext(build("&{a: end, b: end}"));
            for (int obj : ctx.objects()) {
                var objs = Set.of(obj);
                var b = FcaChecker.intent(objs, ctx);
                var a = FcaChecker.extent(b, ctx);
                // A'' must contain A (closure is extensive)
                assertTrue(a.containsAll(objs));
            }
        }
    }

    // =========================================================================
    // Concept closure
    // =========================================================================

    @Nested
    class ConceptClosureTests {

        @Test
        void closureIsConcept() {
            var ss = build("&{a: end, b: end}");
            var ctx = FcaChecker.formalContext(ss);
            int initial = ss.states().stream().min(Integer::compareTo).orElseThrow();
            var c = FcaChecker.conceptClosure(Set.of(initial), ctx);
            assertInstanceOf(FormalConcept.class, c);
            assertNotNull(c.extent());
            assertNotNull(c.intent());
        }

        @Test
        void closureIdempotent() {
            var ctx = FcaChecker.formalContext(build("&{a: &{b: end}}"));
            int initial = ctx.objects().stream().min(Integer::compareTo).orElseThrow();
            var c1 = FcaChecker.conceptClosure(Set.of(initial), ctx);
            var c2 = FcaChecker.conceptClosure(c1.extent(), ctx);
            assertEquals(c1.extent(), c2.extent());
            assertEquals(c1.intent(), c2.intent());
        }

        @Test
        void attributeClosure() {
            var ctx = FcaChecker.formalContext(build("&{a: end, b: end}"));
            var c = FcaChecker.attributeClosure(Set.of("a"), ctx);
            assertInstanceOf(FormalConcept.class, c);
            assertTrue(c.intent().contains("a"));
        }

        @Test
        void closuresAgree() {
            var ctx = FcaChecker.formalContext(build("&{a: &{b: end}}"));
            var c1 = FcaChecker.conceptClosure(new HashSet<>(ctx.objects()), ctx);
            var c2 = FcaChecker.attributeClosure(Set.of(), ctx);
            assertEquals(new HashSet<>(ctx.objects()), c1.extent());
            assertEquals(new HashSet<>(ctx.objects()), c2.extent());
        }
    }

    // =========================================================================
    // All concepts
    // =========================================================================

    @Nested
    class AllConceptsTests {

        @Test
        void endConcepts() {
            var ctx = FcaChecker.formalContext(build("end"));
            var concepts = FcaChecker.allConcepts(ctx);
            assertTrue(concepts.size() >= 1);
        }

        @Test
        void singleBranchConcepts() {
            var ctx = FcaChecker.formalContext(build("&{a: end}"));
            var concepts = FcaChecker.allConcepts(ctx);
            assertTrue(concepts.size() >= 2);
        }

        @Test
        void conceptsAreClosed() {
            var ctx = FcaChecker.formalContext(build("&{a: end, b: end}"));
            var concepts = FcaChecker.allConcepts(ctx);
            for (var c : concepts) {
                var c2 = FcaChecker.conceptClosure(c.extent(), ctx);
                assertEquals(c.extent(), c2.extent());
                assertEquals(c.intent(), c2.intent());
            }
        }

        @Test
        void conceptCountChain() {
            var ctx = FcaChecker.formalContext(build("&{a: &{b: end}}"));
            var concepts = FcaChecker.allConcepts(ctx);
            assertTrue(concepts.size() >= 3);
        }

        @Test
        void noDuplicateConcepts() {
            var ctx = FcaChecker.formalContext(build("&{a: end, b: end}"));
            var concepts = FcaChecker.allConcepts(ctx);
            var extents = new HashSet<Set<Integer>>();
            for (var c : concepts) {
                assertTrue(extents.add(c.extent()), "Duplicate extent found");
            }
        }
    }

    // =========================================================================
    // Concept lattice
    // =========================================================================

    @Nested
    class ConceptLatticeTests {

        @Test
        void latticeType() {
            var ctx = FcaChecker.formalContext(build("&{a: end}"));
            var lat = FcaChecker.conceptLattice(ctx);
            assertInstanceOf(ConceptLattice.class, lat);
            assertNotNull(lat.concepts());
            assertNotNull(lat.order());
        }

        @Test
        void hasTopAndBottom() {
            var ctx = FcaChecker.formalContext(build("&{a: end, b: end}"));
            var lat = FcaChecker.conceptLattice(ctx);
            var topExt = lat.concepts().get(lat.top()).extent();
            for (var c : lat.concepts()) {
                assertTrue(topExt.containsAll(c.extent()));
            }
        }

        @Test
        void orderIsExtentInclusion() {
            var ctx = FcaChecker.formalContext(build("&{a: end, b: end}"));
            var lat = FcaChecker.conceptLattice(ctx);
            for (var pair : lat.order()) {
                var ei = lat.concepts().get(pair[0]).extent();
                var ej = lat.concepts().get(pair[1]).extent();
                assertTrue(ej.containsAll(ei));
            }
        }

        @Test
        void orderIsAntisymmetric() {
            var ctx = FcaChecker.formalContext(build("&{a: end, b: end}"));
            var lat = FcaChecker.conceptLattice(ctx);
            // Build a set of order pairs as strings for lookup
            var orderSet = new HashSet<String>();
            for (var pair : lat.order()) {
                orderSet.add(pair[0] + "," + pair[1]);
            }
            for (var pair : lat.order()) {
                if (orderSet.contains(pair[1] + "," + pair[0])) {
                    assertEquals(
                            lat.concepts().get(pair[0]).extent(),
                            lat.concepts().get(pair[1]).extent());
                }
            }
        }

        @Test
        void parallelConceptCount() {
            var ctxSeq = FcaChecker.formalContext(build("&{a: end}"));
            var ctxPar = FcaChecker.formalContext(build("(&{a: end} || &{b: end})"));
            int cSeq = FcaChecker.allConcepts(ctxSeq).size();
            int cPar = FcaChecker.allConcepts(ctxPar).size();
            assertTrue(cPar >= cSeq);
        }
    }

    // =========================================================================
    // Context properties
    // =========================================================================

    @Nested
    class ContextPropertyTests {

        @Test
        void densityEnd() {
            var ctx = FcaChecker.formalContext(build("end"));
            assertEquals(0.0, FcaChecker.contextDensity(ctx));
        }

        @Test
        void densityRange() {
            var ctx = FcaChecker.formalContext(build("&{a: end, b: end}"));
            double d = FcaChecker.contextDensity(ctx);
            assertTrue(d >= 0.0 && d <= 1.0);
        }

        @Test
        void densityChain() {
            var ctx = FcaChecker.formalContext(build("&{a: &{b: &{c: end}}}"));
            double d = FcaChecker.contextDensity(ctx);
            assertTrue(d > 0.0 && d < 1.0);
        }

        @Test
        void clarifiedEnd() {
            var ctx = FcaChecker.formalContext(build("end"));
            assertTrue(FcaChecker.isClarified(ctx));
        }

        @Test
        void clarifiedChain() {
            var ctx = FcaChecker.formalContext(build("&{a: &{b: end}}"));
            assertTrue(FcaChecker.isClarified(ctx));
        }
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    @Nested
    class AnalyzeFcaTests {

        @Test
        void endAnalysis() {
            var result = FcaChecker.analyzeFca(build("end"));
            assertInstanceOf(FCAResult.class, result);
            assertEquals(1, result.numObjects());
            assertEquals(0, result.numAttributes());
            assertTrue(result.numConcepts() >= 1);
        }

        @Test
        void branchAnalysis() {
            var result = FcaChecker.analyzeFca(build("&{a: end, b: end}"));
            assertEquals(2, result.numAttributes());
            assertTrue(result.numConcepts() >= 2);
        }

        @Test
        void chainAnalysis() {
            var result = FcaChecker.analyzeFca(build("&{a: &{b: end}}"));
            assertEquals(3, result.numObjects());
            assertEquals(2, result.numAttributes());
            assertTrue(result.latticeSizeRatio() > 0);
        }

        @Test
        void parallelAnalysis() {
            var result = FcaChecker.analyzeFca(build("(&{a: end} || &{b: end})"));
            assertTrue(result.numConcepts() >= 3);
        }

        @Test
        void recursiveAnalysis() {
            var result = FcaChecker.analyzeFca(build("rec X . &{a: X, b: end}"));
            assertTrue(result.numConcepts() >= 2);
        }

        @Test
        void resultConsistency() {
            var result = FcaChecker.analyzeFca(build("&{a: &{b: end}, c: end}"));
            assertEquals(result.numObjects(), result.context().objects().size());
            assertEquals(result.numAttributes(), result.context().attributes().size());
            assertEquals(result.numConcepts(), result.conceptLattice().concepts().size());
        }

        @Test
        void densityMatches() {
            var result = FcaChecker.analyzeFca(build("&{a: end, b: end}"));
            assertEquals(result.density(), FcaChecker.contextDensity(result.context()));
        }

        @Test
        void selectionAnalysis() {
            var result = FcaChecker.analyzeFca(build("+{a: end, b: end}"));
            assertTrue(result.numConcepts() >= 2);
        }
    }

    // =========================================================================
    // Benchmarks
    // =========================================================================

    @Nested
    class BenchmarkTests {

        @Test
        void smtpLike() {
            var result = FcaChecker.analyzeFca(build(
                    "&{ehlo: &{auth: +{OK: &{mail: &{rcpt: &{data: end}}}, FAIL: end}}}"));
            assertTrue(result.numConcepts() >= 2);
            assertTrue(result.density() > 0);
        }

        @Test
        void iteratorLike() {
            var result = FcaChecker.analyzeFca(build(
                    "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
            assertTrue(result.numConcepts() >= 2);
        }

        @Test
        void parallel3voice() {
            var result = FcaChecker.analyzeFca(build(
                    "(&{a: end} || (&{b: end} || &{c: end}))"));
            assertTrue(result.numConcepts() >= 4);
        }
    }

    // =========================================================================
    // Parameterized sweep
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "end",
            "&{a: end}",
            "&{a: end, b: end}",
            "+{a: end, b: end}",
            "&{a: &{b: end}}",
            "(&{a: end} || &{b: end})",
            "rec X . &{a: X, b: end}",
            "&{a: &{b: end}, c: end}",
            "&{a: &{b: &{c: end}}}",
    })
    void analysisSanity(String typeStr) {
        var result = FcaChecker.analyzeFca(build(typeStr));
        assertTrue(result.numObjects() >= 1);
        assertTrue(result.numConcepts() >= 1);
        assertTrue(result.latticeSizeRatio() > 0);
    }
}
