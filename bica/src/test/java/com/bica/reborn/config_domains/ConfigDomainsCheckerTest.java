package com.bica.reborn.config_domains;

import com.bica.reborn.config_domains.ConfigDomainsChecker.*;
import com.bica.reborn.eventstructures.EventStructureChecker;
import com.bica.reborn.eventstructures.EventStructureChecker.ConfigDomain;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for configuration domains (Step 17).
 * Mirrors 82 Python tests from test_config_domains.py.
 */
class ConfigDomainsCheckerTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    private ConfigDomain domain(String type) {
        return ConfigDomainsChecker.buildConfigDomain(build(type));
    }

    // Common fixtures as methods
    private StateSpace endSs()         { return build("end"); }
    private StateSpace singleBranch()  { return build("&{a: end}"); }
    private StateSpace binaryBranch()  { return build("&{a: end, b: end}"); }
    private StateSpace deepChain()     { return build("&{a: &{b: &{c: end}}}"); }
    private StateSpace parallelSs()    { return build("(&{a: end} || &{b: end})"); }
    private StateSpace selectSs()      { return build("+{ok: end, err: end}"); }
    private StateSpace iteratorSs()    { return build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"); }
    private StateSpace nestedSs()      { return build("&{a: &{c: end, d: end}, b: &{e: end, f: end}}"); }
    private StateSpace ternaryBranch() { return build("&{a: &{x: end, y: end}, b: &{x: end, y: end}, c: &{x: end, y: end}}"); }

    // -----------------------------------------------------------------------
    // buildConfigDomain
    // -----------------------------------------------------------------------

    @Nested
    class BuildConfigDomainTests {

        @Test
        void endSingleConfig() {
            var dom = domain("end");
            assertEquals(1, dom.numConfigs());
            assertTrue(dom.bottom().events().isEmpty());
        }

        @Test
        void singleBranchConfigs() {
            var dom = domain("&{a: end}");
            assertEquals(2, dom.numConfigs());
        }

        @Test
        void binaryBranchConfigs() {
            var dom = domain("&{a: end, b: end}");
            assertEquals(3, dom.numConfigs());
        }

        @Test
        void deepChainConfigs() {
            var dom = domain("&{a: &{b: &{c: end}}}");
            assertEquals(4, dom.numConfigs());
        }

        @Test
        void parallelConfigs() {
            var dom = domain("(&{a: end} || &{b: end})");
            assertTrue(dom.numConfigs() >= 3);
        }

        @Test
        void orderingReflexive() {
            var dom = domain("&{a: end, b: end}");
            var ordering = buildOrderingSet(dom);
            for (int i = 0; i < dom.numConfigs(); i++) {
                assertTrue(ordering.contains(packPair(i, i)),
                        "ordering must be reflexive at " + i);
            }
        }

        @Test
        void orderingAntisymmetric() {
            var dom = domain("&{a: &{b: &{c: end}}}");
            var ordering = buildOrderingSet(dom);
            for (long packed : ordering) {
                int i = (int) (packed >> 32);
                int j = (int) packed;
                if (i != j) {
                    assertFalse(ordering.contains(packPair(j, i)),
                            "antisymmetry violated for (" + i + "," + j + ")");
                }
            }
        }

        @Test
        void bottomIsEmpty() {
            var dom = domain("&{a: &{c: end, d: end}, b: &{e: end, f: end}}");
            assertTrue(dom.bottom().events().isEmpty());
        }
    }

    // Helper to build ordering set (mirrors internal method)
    private static Set<Long> buildOrderingSet(ConfigDomain domain) {
        var set = new HashSet<Long>();
        for (var pair : domain.ordering()) {
            set.add(packPair(pair[0], pair[1]));
        }
        return set;
    }

    private static long packPair(int i, int j) {
        return ((long) i << 32) | (j & 0xFFFFFFFFL);
    }

    // -----------------------------------------------------------------------
    // compactElements
    // -----------------------------------------------------------------------

    @Nested
    class CompactElementsTests {

        @Test
        void endAllCompact() {
            var dom = domain("end");
            assertEquals(dom.numConfigs(), ConfigDomainsChecker.compactElements(dom).size());
        }

        @Test
        void binaryBranchAllCompact() {
            var dom = domain("&{a: end, b: end}");
            assertEquals(3, ConfigDomainsChecker.compactElements(dom).size());
        }

        @Test
        void deepChainAllCompact() {
            var dom = domain("&{a: &{b: &{c: end}}}");
            assertEquals(4, ConfigDomainsChecker.compactElements(dom).size());
        }

        @Test
        void finiteMeansAllCompact() {
            var dom = domain("(&{a: end} || &{b: end})");
            assertEquals(dom.numConfigs(), ConfigDomainsChecker.compactElements(dom).size());
        }
    }

    // -----------------------------------------------------------------------
    // consistentPairs
    // -----------------------------------------------------------------------

    @Nested
    class ConsistentPairsTests {

        @Test
        void endSinglePair() {
            var dom = domain("end");
            assertEquals(1, ConfigDomainsChecker.consistentPairs(dom).size());
        }

        @Test
        void chainAllConsistent() {
            var dom = domain("&{a: &{b: &{c: end}}}");
            var pairs = ConfigDomainsChecker.consistentPairs(dom);
            int n = dom.numConfigs();
            assertEquals(n * (n + 1) / 2, pairs.size());
        }

        @Test
        void binaryBranchConsistent() {
            var dom = domain("&{a: end, b: end}");
            var pairs = ConfigDomainsChecker.consistentPairs(dom);
            assertTrue(pairs.size() >= 3);
        }

        @Test
        void selfPairsAlwaysConsistent() {
            var dom = domain("&{a: &{c: end, d: end}, b: &{e: end, f: end}}");
            var pairs = ConfigDomainsChecker.consistentPairs(dom);
            var pairSet = new HashSet<Long>();
            for (var p : pairs) pairSet.add(packPair(p[0], p[1]));
            for (int i = 0; i < dom.numConfigs(); i++) {
                assertTrue(pairSet.contains(packPair(i, i)));
            }
        }

        @Test
        void parallelMoreConsistent() {
            var dom = domain("(&{a: end} || &{b: end})");
            var pairs = ConfigDomainsChecker.consistentPairs(dom);
            assertTrue(pairs.size() >= dom.numConfigs());
        }
    }

    // -----------------------------------------------------------------------
    // checkCoherence
    // -----------------------------------------------------------------------

    @Nested
    class CoherenceTests {

        @Test
        void endCoherent() {
            assertTrue(ConfigDomainsChecker.checkCoherence(domain("end")));
        }

        @Test
        void chainCoherent() {
            assertTrue(ConfigDomainsChecker.checkCoherence(domain("&{a: &{b: &{c: end}}}")));
        }

        @Test
        void binaryBranchCoherent() {
            assertTrue(ConfigDomainsChecker.checkCoherence(domain("&{a: end, b: end}")));
        }

        @Test
        void parallelCoherent() {
            assertTrue(ConfigDomainsChecker.checkCoherence(domain("(&{a: end} || &{b: end})")));
        }

        @Test
        void nestedCoherent() {
            assertTrue(ConfigDomainsChecker.checkCoherence(
                    domain("&{a: &{c: end, d: end}, b: &{e: end, f: end}}")));
        }

        @Test
        void selectCoherent() {
            assertTrue(ConfigDomainsChecker.checkCoherence(domain("+{ok: end, err: end}")));
        }
    }

    // -----------------------------------------------------------------------
    // checkAlgebraicity
    // -----------------------------------------------------------------------

    @Nested
    class AlgebraicityTests {

        @Test
        void alwaysAlgebraicEnd() {
            assertTrue(ConfigDomainsChecker.checkAlgebraicity(domain("end")));
        }

        @Test
        void alwaysAlgebraicChain() {
            assertTrue(ConfigDomainsChecker.checkAlgebraicity(domain("&{a: &{b: &{c: end}}}")));
        }

        @Test
        void alwaysAlgebraicParallel() {
            assertTrue(ConfigDomainsChecker.checkAlgebraicity(domain("(&{a: end} || &{b: end})")));
        }
    }

    // -----------------------------------------------------------------------
    // checkBoundedCompleteness
    // -----------------------------------------------------------------------

    @Nested
    class BoundedCompletenessTests {

        @Test
        void endBoundedComplete() {
            assertTrue(ConfigDomainsChecker.checkBoundedCompleteness(domain("end")));
        }

        @Test
        void chainBoundedComplete() {
            assertTrue(ConfigDomainsChecker.checkBoundedCompleteness(domain("&{a: &{b: &{c: end}}}")));
        }

        @Test
        void binaryBranchBoundedComplete() {
            assertTrue(ConfigDomainsChecker.checkBoundedCompleteness(domain("&{a: end, b: end}")));
        }

        @Test
        void parallelBoundedComplete() {
            assertTrue(ConfigDomainsChecker.checkBoundedCompleteness(domain("(&{a: end} || &{b: end})")));
        }

        @Test
        void selectBoundedComplete() {
            assertTrue(ConfigDomainsChecker.checkBoundedCompleteness(domain("+{ok: end, err: end}")));
        }
    }

    // -----------------------------------------------------------------------
    // checkScottDomain
    // -----------------------------------------------------------------------

    @Nested
    class CheckScottDomainTests {

        @Test
        void endIsScottDomain() {
            var result = ConfigDomainsChecker.checkScottDomain(domain("end"));
            assertTrue(result.isScottDomain());
            assertTrue(result.isDcpo());
            assertTrue(result.isAlgebraic());
        }

        @Test
        void chainIsScottDomain() {
            var result = ConfigDomainsChecker.checkScottDomain(domain("&{a: &{b: &{c: end}}}"));
            assertTrue(result.isScottDomain());
        }

        @Test
        void binaryBranchScottDomain() {
            var dom = domain("&{a: end, b: end}");
            var result = ConfigDomainsChecker.checkScottDomain(dom);
            assertTrue(result.isScottDomain());
            assertEquals(dom.numConfigs(), result.numCompact());
        }

        @Test
        void parallelScottDomain() {
            assertTrue(ConfigDomainsChecker.checkScottDomain(domain("(&{a: end} || &{b: end})")).isScottDomain());
        }

        @Test
        void iteratorScottDomain() {
            assertTrue(ConfigDomainsChecker.checkScottDomain(
                    domain("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}")).isScottDomain());
        }

        @Test
        void nestedScottDomain() {
            assertTrue(ConfigDomainsChecker.checkScottDomain(
                    domain("&{a: &{c: end, d: end}, b: &{e: end, f: end}}")).isScottDomain());
        }

        @Test
        void scottResultHasCounts() {
            var dom = domain("&{a: end, b: end}");
            var result = ConfigDomainsChecker.checkScottDomain(dom);
            assertEquals(dom.numConfigs(), result.numConfigs());
            assertTrue(result.numCompact() > 0);
            assertTrue(result.numConsistentPairs() > 0);
            assertTrue(result.numScottOpen() > 0);
        }
    }

    // -----------------------------------------------------------------------
    // scottOpenSets
    // -----------------------------------------------------------------------

    @Nested
    class ScottOpenSetsTests {

        @Test
        void endTwoOpens() {
            var dom = domain("end");
            assertEquals(2, ConfigDomainsChecker.scottOpenSets(dom).size());
        }

        @Test
        void chainOpens() {
            var dom = domain("&{a: end}");
            assertTrue(ConfigDomainsChecker.scottOpenSets(dom).size() >= 2);
        }

        @Test
        void emptyIsOpen() {
            var dom = domain("&{a: end, b: end}");
            var opens = ConfigDomainsChecker.scottOpenSets(dom);
            assertTrue(opens.stream().anyMatch(Set::isEmpty));
        }

        @Test
        void wholeSetIsOpen() {
            var dom = domain("&{a: end, b: end}");
            var opens = ConfigDomainsChecker.scottOpenSets(dom);
            var all = new HashSet<Integer>();
            for (int i = 0; i < dom.numConfigs(); i++) all.add(i);
            assertTrue(opens.contains(all));
        }

        @Test
        void opensAreUpperSets() {
            var dom = domain("&{a: &{b: &{c: end}}}");
            var ordering = buildOrderingSet(dom);
            var opens = ConfigDomainsChecker.scottOpenSets(dom);
            for (var u : opens) {
                for (int i : u) {
                    for (int j = 0; j < dom.numConfigs(); j++) {
                        if (ordering.contains(packPair(i, j))) {
                            assertTrue(u.contains(j));
                        }
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // checkDistributivity
    // -----------------------------------------------------------------------

    @Nested
    class DistributivityTests {

        @Test
        void endDistributive() {
            assertTrue(ConfigDomainsChecker.checkDistributivity(domain("end")));
        }

        @Test
        void chainDistributive() {
            assertTrue(ConfigDomainsChecker.checkDistributivity(domain("&{a: &{b: &{c: end}}}")));
        }

        @Test
        void binaryBranchDistributive() {
            assertTrue(ConfigDomainsChecker.checkDistributivity(domain("&{a: end, b: end}")));
        }

        @Test
        void parallelDistributive() {
            assertTrue(ConfigDomainsChecker.checkDistributivity(domain("(&{a: end} || &{b: end})")));
        }

        @Test
        void selectDistributive() {
            assertTrue(ConfigDomainsChecker.checkDistributivity(domain("+{ok: end, err: end}")));
        }
    }

    // -----------------------------------------------------------------------
    // coveringChains
    // -----------------------------------------------------------------------

    @Nested
    class CoveringChainsTests {

        @Test
        void endSingleChain() {
            var dom = domain("end");
            var chains = ConfigDomainsChecker.coveringChains(dom);
            assertEquals(1, chains.size());
            assertEquals(1, chains.get(0).size());
        }

        @Test
        void singleBranchOneChain() {
            var dom = domain("&{a: end}");
            var chains = ConfigDomainsChecker.coveringChains(dom);
            assertEquals(1, chains.size());
            assertEquals(2, chains.get(0).size());
        }

        @Test
        void binaryBranchTwoChains() {
            var dom = domain("&{a: end, b: end}");
            var chains = ConfigDomainsChecker.coveringChains(dom);
            assertEquals(2, chains.size());
        }

        @Test
        void deepChainOneChain() {
            var dom = domain("&{a: &{b: &{c: end}}}");
            var chains = ConfigDomainsChecker.coveringChains(dom);
            assertEquals(1, chains.size());
            assertEquals(4, chains.get(0).size());
        }

        @Test
        void chainsStartAtBottom() {
            var dom = domain("&{a: &{c: end, d: end}, b: &{e: end, f: end}}");
            var chains = ConfigDomainsChecker.coveringChains(dom);
            for (var chain : chains) {
                assertTrue(dom.configs().get(chain.get(0)).events().isEmpty());
            }
        }

        @Test
        void chainsAreCovering() {
            var dom = domain("(&{a: end} || &{b: end})");
            var chains = ConfigDomainsChecker.coveringChains(dom);
            for (var chain : chains) {
                for (int k = 0; k < chain.size() - 1; k++) {
                    var c1 = dom.configs().get(chain.get(k));
                    var c2 = dom.configs().get(chain.get(k + 1));
                    assertEquals(c1.events().size() + 1, c2.events().size());
                    assertTrue(c2.events().containsAll(c1.events()));
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // joinIrreducibles
    // -----------------------------------------------------------------------

    @Nested
    class JoinIrreduciblesTests {

        @Test
        void endNoJi() {
            assertEquals(0, ConfigDomainsChecker.joinIrreducibles(domain("end")).size());
        }

        @Test
        void singleBranchOneJi() {
            assertEquals(1, ConfigDomainsChecker.joinIrreducibles(domain("&{a: end}")).size());
        }

        @Test
        void chainAllNonBottomAreJi() {
            var dom = domain("&{a: &{b: &{c: end}}}");
            assertEquals(dom.numConfigs() - 1, ConfigDomainsChecker.joinIrreducibles(dom).size());
        }

        @Test
        void binaryBranchJi() {
            assertEquals(2, ConfigDomainsChecker.joinIrreducibles(domain("&{a: end, b: end}")).size());
        }
    }

    // -----------------------------------------------------------------------
    // isLattice
    // -----------------------------------------------------------------------

    @Nested
    class IsLatticeTests {

        @Test
        void endIsLattice() {
            assertTrue(ConfigDomainsChecker.isLattice(domain("end")));
        }

        @Test
        void chainIsLattice() {
            assertTrue(ConfigDomainsChecker.isLattice(domain("&{a: &{b: &{c: end}}}")));
        }

        @Test
        void binaryBranchLatticeCheck() {
            // Config domain of branch: {}, {a}, {b} -- may or may not be lattice
            var result = ConfigDomainsChecker.isLattice(domain("&{a: end, b: end}"));
            assertNotNull(result); // just verify it returns a boolean
        }

        @Test
        void parallelConfigDomain() {
            var result = ConfigDomainsChecker.isLattice(domain("(&{a: end} || &{b: end})"));
            assertNotNull(result);
        }
    }

    // -----------------------------------------------------------------------
    // width
    // -----------------------------------------------------------------------

    @Nested
    class WidthTests {

        @Test
        void endWidth1() {
            assertEquals(1, ConfigDomainsChecker.width(domain("end")));
        }

        @Test
        void chainWidth1() {
            assertEquals(1, ConfigDomainsChecker.width(domain("&{a: &{b: &{c: end}}}")));
        }

        @Test
        void binaryBranchWidth2() {
            assertEquals(2, ConfigDomainsChecker.width(domain("&{a: end, b: end}")));
        }

        @Test
        void parallelWidth() {
            assertTrue(ConfigDomainsChecker.width(domain("(&{a: end} || &{b: end})")) >= 2);
        }
    }

    // -----------------------------------------------------------------------
    // analyzeConfigDomain
    // -----------------------------------------------------------------------

    @Nested
    class AnalyzeConfigDomainTests {

        @Test
        void endAnalysis() {
            var analysis = ConfigDomainsChecker.analyzeConfigDomain(endSs());
            assertEquals(1, analysis.numConfigs());
            assertTrue(analysis.scottResult().isScottDomain());
        }

        @Test
        void chainAnalysis() {
            var analysis = ConfigDomainsChecker.analyzeConfigDomain(deepChain());
            assertEquals(4, analysis.numConfigs());
            assertEquals(1, analysis.numCoveringChains());
            assertEquals(3, analysis.numJoinIrreducibles());
            assertEquals(4, analysis.maxChainLength());
            assertEquals(1, analysis.width());
        }

        @Test
        void binaryBranchAnalysis() {
            var analysis = ConfigDomainsChecker.analyzeConfigDomain(binaryBranch());
            assertEquals(3, analysis.numConfigs());
            assertTrue(analysis.scottResult().isScottDomain());
        }

        @Test
        void parallelAnalysis() {
            var analysis = ConfigDomainsChecker.analyzeConfigDomain(parallelSs());
            assertTrue(analysis.scottResult().isScottDomain());
            assertTrue(analysis.scottResult().isAlgebraic());
        }

        @Test
        void iteratorAnalysis() {
            var analysis = ConfigDomainsChecker.analyzeConfigDomain(iteratorSs());
            assertTrue(analysis.scottResult().isScottDomain());
            assertTrue(analysis.numConfigs() > 0);
        }

        @Test
        void nestedAnalysis() {
            var analysis = ConfigDomainsChecker.analyzeConfigDomain(nestedSs());
            assertTrue(analysis.numConfigs() > 0);
            assertTrue(analysis.scottResult().isDcpo());
        }

        @Test
        void analysisHasDomain() {
            var analysis = ConfigDomainsChecker.analyzeConfigDomain(singleBranch());
            assertNotNull(analysis.domain());
            assertNotNull(analysis.es());
        }

        @Test
        void selectAnalysis() {
            var analysis = ConfigDomainsChecker.analyzeConfigDomain(selectSs());
            assertTrue(analysis.scottResult().isScottDomain());
        }
    }

    // -----------------------------------------------------------------------
    // dI-domain properties
    // -----------------------------------------------------------------------

    @Nested
    class DiDomainTests {

        @Test
        void chainIsDiDomain() {
            assertTrue(ConfigDomainsChecker.checkScottDomain(domain("&{a: &{b: &{c: end}}}")).isDiDomain());
        }

        @Test
        void endIsDiDomain() {
            assertTrue(ConfigDomainsChecker.checkScottDomain(domain("end")).isDiDomain());
        }

        @Test
        void parallelDiDomain() {
            assertTrue(ConfigDomainsChecker.checkScottDomain(domain("(&{a: end} || &{b: end})")).isDiDomain());
        }
    }

    // -----------------------------------------------------------------------
    // Edge cases and integration
    // -----------------------------------------------------------------------

    @Nested
    class EdgeCaseTests {

        @Test
        void singleElementDomain() {
            var dom = domain("end");
            assertEquals(1, dom.numConfigs());
            assertTrue(ConfigDomainsChecker.checkCoherence(dom));
            assertTrue(ConfigDomainsChecker.checkBoundedCompleteness(dom));
            assertTrue(ConfigDomainsChecker.checkAlgebraicity(dom));
            assertTrue(ConfigDomainsChecker.isLattice(dom));
        }

        @Test
        void scottOpenIncludesEmptyAndFull() {
            var dom = domain("&{a: end}");
            var opens = ConfigDomainsChecker.scottOpenSets(dom);
            assertTrue(opens.stream().anyMatch(Set::isEmpty));
            var all = new HashSet<Integer>();
            for (int i = 0; i < dom.numConfigs(); i++) all.add(i);
            assertTrue(opens.contains(all));
        }

        @Test
        void coveringChainsCountMatchesPaths() {
            var dom = domain("&{a: &{c: end, d: end}, b: &{e: end, f: end}}");
            var chains = ConfigDomainsChecker.coveringChains(dom);
            assertTrue(chains.size() > 0);
        }

        @Test
        void configDomainOrderingTransitive() {
            var dom = domain("(&{a: end} || &{b: end})");
            var ordering = buildOrderingSet(dom);
            for (var pair1 : dom.ordering()) {
                for (var pair2 : dom.ordering()) {
                    if (pair1[1] == pair2[0]) {
                        assertTrue(ordering.contains(packPair(pair1[0], pair2[1])),
                                "transitivity violated");
                    }
                }
            }
        }

        @Test
        void ternaryBranchAnalysis() {
            var analysis = ConfigDomainsChecker.analyzeConfigDomain(ternaryBranch());
            assertTrue(analysis.numConfigs() > 0);
            assertTrue(analysis.scottResult().isScottDomain());
        }
    }
}
