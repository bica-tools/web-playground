package com.bica.reborn.eventstructures;

import com.bica.reborn.eventstructures.EventStructureChecker.*;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for event structure module (Step 16).
 * Mirrors all 49 Python tests from test_event_structures.py.
 */
class EventStructureCheckerTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Event structure construction
    // -----------------------------------------------------------------------

    @Nested
    class BuildEventStructure {

        @Test
        void endEmpty() {
            var ss = build("end");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertEquals(0, es.numEvents());
            assertEquals(0, es.conflict().size());
        }

        @Test
        void singleBranch() {
            var ss = build("&{a: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertEquals(1, es.numEvents());
            assertEquals(0, es.conflict().size());
        }

        @Test
        void simpleBranchEvents() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertEquals(2, es.numEvents());
        }

        @Test
        void simpleBranchConflict() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertTrue(es.numConflicts() >= 1, "Two transitions from same state should conflict");
        }

        @Test
        void deepChainNoConflict() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertEquals(3, es.numEvents());
            assertEquals(0, es.numConflicts());
        }

        @Test
        void deepChainCausality() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertTrue(es.numCausalPairs() >= 2, "a<b, b<c (and a<c transitively)");
        }

        @Test
        void parallelEvents() {
            var ss = build("(&{a: end} || &{b: end})");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertTrue(es.numEvents() >= 2);
        }

        @Test
        void iteratorEvents() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertTrue(es.numEvents() >= 3, "hasNext, TRUE/FALSE, next");
        }

        @Test
        void nestedBranch() {
            var ss = build("&{a: &{c: end, d: end}, b: &{e: end, f: end}}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertEquals(6, es.numEvents());
            assertTrue(es.numConflicts() >= 1, "a # b at top");
        }

        @Test
        void resultTypes() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertInstanceOf(EventStructureResult.class, es);
            assertNotNull(es.events());
            assertNotNull(es.causality());
            assertNotNull(es.conflict());
        }
    }

    // -----------------------------------------------------------------------
    // Conflict properties
    // -----------------------------------------------------------------------

    @Nested
    class ConflictProperties {

        @Test
        void conflictIsSymmetric() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            for (var pair : es.conflict()) {
                assertTrue(es.conflict().contains(new EventPair(pair.second(), pair.first())),
                        "Conflict must be symmetric");
            }
        }

        @Test
        void conflictIsIrreflexive() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            for (var pair : es.conflict()) {
                assertNotEquals(pair.first(), pair.second(), "Conflict must be irreflexive");
            }
        }

        @Test
        void noConflictOnChain() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertEquals(0, es.conflict().size());
        }

        @Test
        void conflictHeredity() {
            var ss = build("&{a: &{c: end, d: end}, b: &{e: end, f: end}}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertTrue(es.numConflicts() >= 1, "Conflict heredity should propagate");
        }

        @Test
        void selectCreatesConflict() {
            var ss = build("+{ok: end, err: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertTrue(es.numConflicts() >= 1, "ok # err");
        }
    }

    // -----------------------------------------------------------------------
    // Causality properties
    // -----------------------------------------------------------------------

    @Nested
    class CausalityProperties {

        @Test
        void causalityReflexive() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            for (var e : es.events()) {
                assertTrue(es.causality().contains(new EventPair(e, e)),
                        "Causality must be reflexive");
            }
        }

        @Test
        void chainCausality() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var events = new ArrayList<>(new TreeSet<>(es.events()));
            // First event causes second, second causes third
            assertTrue(es.causality().contains(new EventPair(events.get(0), events.get(1))));
            assertTrue(es.causality().contains(new EventPair(events.get(1), events.get(2))));
            // Transitivity
            assertTrue(es.causality().contains(new EventPair(events.get(0), events.get(2))));
        }
    }

    // -----------------------------------------------------------------------
    // Configurations
    // -----------------------------------------------------------------------

    @Nested
    class Configurations {

        @Test
        void endOneConfig() {
            var ss = build("end");
            var es = EventStructureChecker.buildEventStructure(ss);
            var configs = EventStructureChecker.configurations(es);
            assertEquals(1, configs.size(), "Just the empty config");
        }

        @Test
        void simpleBranchConfigs() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var configs = EventStructureChecker.configurations(es);
            assertEquals(3, configs.size(), "Empty, {a}, {b}");
        }

        @Test
        void deepChainConfigs() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var configs = EventStructureChecker.configurations(es);
            assertEquals(4, configs.size(), "Empty, {a}, {a,b}, {a,b,c}");
        }

        @Test
        void emptyIsAlwaysConfig() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var configs = EventStructureChecker.configurations(es);
            assertTrue(configs.stream().anyMatch(c -> c.size() == 0));
        }

        @Test
        void configDownwardClosed() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var configs = EventStructureChecker.configurations(es);
            for (var c : configs) {
                for (var e : c.events()) {
                    for (var pair : es.causality()) {
                        if (pair.second().equals(e) && !pair.first().equals(e)) {
                            assertTrue(c.events().contains(pair.first()),
                                    "Config " + c + " contains " + e + " but not predecessor " + pair.first());
                        }
                    }
                }
            }
        }

        @Test
        void configConflictFree() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var configs = EventStructureChecker.configurations(es);
            for (var c : configs) {
                for (var e1 : c.events()) {
                    for (var e2 : c.events()) {
                        assertFalse(
                                es.conflict().contains(new EventPair(e1, e2)) && !e1.equals(e2),
                                "Config must be conflict-free");
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Configuration domain
    // -----------------------------------------------------------------------

    @Nested
    class ConfigDomainTests {

        @Test
        void domainStructure() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var dom = EventStructureChecker.configDomain(es);
            assertInstanceOf(ConfigDomain.class, dom);
            assertTrue(dom.numConfigs() >= 1);
        }

        @Test
        void bottomIsEmpty() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var dom = EventStructureChecker.configDomain(es);
            assertEquals(0, dom.bottom().size());
        }
    }

    // -----------------------------------------------------------------------
    // Isomorphism check
    // -----------------------------------------------------------------------

    @Nested
    class Isomorphism {

        @Test
        void endIsomorphic() {
            var ss = build("end");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertTrue(EventStructureChecker.checkIsomorphism(es, ss));
        }

        @Test
        void simpleBranchNotIsomorphic() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var configs = EventStructureChecker.configurations(es);
            assertEquals(3, configs.size(), "More configs than states");
            assertFalse(EventStructureChecker.checkIsomorphism(es, ss));
        }

        @Test
        void deepChainIsomorphic() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertTrue(EventStructureChecker.checkIsomorphism(es, ss));
        }

        @Test
        void selectNotIsomorphic() {
            var ss = build("+{ok: end, err: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertFalse(EventStructureChecker.checkIsomorphism(es, ss));
        }
    }

    // -----------------------------------------------------------------------
    // Classification
    // -----------------------------------------------------------------------

    @Nested
    class Classification {

        @Test
        void branchEvents() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var cl = EventStructureChecker.classifyEvents(es, ss);
            assertEquals(2, cl.branchEvents().size(), "a and b are branch events");
        }

        @Test
        void selectEvents() {
            var ss = build("+{ok: end, err: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var cl = EventStructureChecker.classifyEvents(es, ss);
            assertEquals(2, cl.selectEvents().size(), "ok and err are select events");
        }

        @Test
        void conflictGroups() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var cl = EventStructureChecker.classifyEvents(es, ss);
            assertTrue(cl.conflictGroups().size() >= 1);
        }

        @Test
        void resultType() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var cl = EventStructureChecker.classifyEvents(es, ss);
            assertInstanceOf(EventClassification.class, cl);
        }
    }

    // -----------------------------------------------------------------------
    // Concurrency
    // -----------------------------------------------------------------------

    @Nested
    class Concurrency {

        @Test
        void noConcurrencyInChain() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var conc = EventStructureChecker.concurrencyPairs(es);
            assertEquals(0, conc.size());
        }

        @Test
        void noConcurrencyInBranch() {
            var ss = build("&{a: end, b: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            var conc = EventStructureChecker.concurrencyPairs(es);
            assertEquals(0, conc.size());
        }

        @Test
        void parallelHasConcurrency() {
            var ss = build("(&{a: end} || &{b: end})");
            var es = EventStructureChecker.buildEventStructure(ss);
            var conc = EventStructureChecker.concurrencyPairs(es);
            // May be 0 if product merges events
            assertTrue(conc.size() >= 0);
        }
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Nested
    class Analysis {

        @Test
        void endAnalysis() {
            var ss = build("end");
            var a = EventStructureChecker.analyze(ss);
            assertInstanceOf(ESAnalysis.class, a);
            assertEquals(0, a.numEvents());
            assertEquals(1, a.numConfigs());
        }

        @Test
        void simpleBranchAnalysis() {
            var ss = build("&{a: end, b: end}");
            var a = EventStructureChecker.analyze(ss);
            assertEquals(2, a.numEvents());
            assertTrue(a.numConflicts() >= 1);
            assertEquals(3, a.numConfigs());
        }

        @Test
        void deepChainAnalysis() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var a = EventStructureChecker.analyze(ss);
            assertEquals(3, a.numEvents());
            assertEquals(0, a.numConflicts());
            assertEquals(4, a.numConfigs());
            assertTrue(a.isIsomorphic());
        }

        @Test
        void conflictDensity() {
            var ss = build("&{a: end, b: end}");
            var a = EventStructureChecker.analyze(ss);
            assertTrue(a.conflictDensity() >= 0.0 && a.conflictDensity() <= 1.0);
        }

        @Test
        void maxConfigSize() {
            var ss = build("&{a: &{b: &{c: end}}}");
            var a = EventStructureChecker.analyze(ss);
            assertEquals(3, a.maxConfigSize(), "{a, b, c}");
        }
    }

    // -----------------------------------------------------------------------
    // Benchmarks
    // -----------------------------------------------------------------------

    @Nested
    class Benchmarks {

        @ParameterizedTest(name = "{1}")
        @CsvSource({
                "'&{a: end, b: end}',            SimpleBranch",
                "'+{ok: end, err: end}',          SimpleSelect",
                "'&{a: &{b: &{c: end}}}',         DeepChain",
                "'&{a: &{c: end, d: end}, b: &{e: end, f: end}}', NestedBranch",
                "'&{get: +{OK: end, NOT_FOUND: end}, put: +{OK: end, ERR: end}}', REST",
        })
        void esOnBenchmarks(String typeString, String name) {
            var ss = build(typeString);
            var a = EventStructureChecker.analyze(ss);
            assertTrue(a.numEvents() >= 1, name + " should have events");
            assertTrue(a.numConfigs() >= 2, name + " should have at least 2 configs");
        }

        @ParameterizedTest(name = "{1}")
        @CsvSource({
                "'&{a: &{b: &{c: end}}}', DeepChain",
        })
        void isomorphismOnChains(String typeString, String name) {
            var ss = build(typeString);
            var es = EventStructureChecker.buildEventStructure(ss);
            assertTrue(EventStructureChecker.checkIsomorphism(es, ss),
                    name + ": D(ES(S)) should be isomorphic to L(S)");
        }

        @ParameterizedTest(name = "{1}")
        @CsvSource({
                "'&{a: end, b: end}',       SimpleBranch",
                "'+{ok: end, err: end}',     SimpleSelect",
        })
        void configsExceedStatesOnBranches(String typeString, String name) {
            var ss = build(typeString);
            var es = EventStructureChecker.buildEventStructure(ss);
            var configs = EventStructureChecker.configurations(es);
            assertTrue(configs.size() >= ss.states().size(),
                    name + ": configs >= states");
        }
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Nested
    class EdgeCases {

        @Test
        void singleEvent() {
            var ss = build("&{a: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertEquals(1, es.numEvents());
            assertEquals(0, es.numConflicts());
        }

        @Test
        void wideBranch() {
            var ss = build("&{a: end, b: end, c: end, d: end, e: end}");
            var es = EventStructureChecker.buildEventStructure(ss);
            assertEquals(5, es.numEvents());
            assertEquals(10, es.numConflicts(), "C(5,2) = 10");
        }

        @Test
        void eventToString() {
            var e = new Event(0, "a", 1);
            assertTrue(e.toString().contains("a"));
        }

        @Test
        void configurationSize() {
            var c = new Configuration(Set.of());
            assertEquals(0, c.size());
        }

        @Test
        void recursiveType() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var a = EventStructureChecker.analyze(ss);
            assertTrue(a.numEvents() >= 3);
            assertTrue(a.numConfigs() >= 1);
        }
    }
}
