package com.bica.reborn.domain.l3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for L3 protocol analysis — Java port of test_l3_protocols.py.
 */
class L3ProtocolsTest {

    // -----------------------------------------------------------------------
    // Catalog completeness
    // -----------------------------------------------------------------------

    @Test
    void catalogHas16Protocols() {
        assertEquals(16, L3Protocols.CATALOG.size());
    }

    @Test
    void catalogContainsJavaIterator() {
        assertTrue(L3Protocols.CATALOG.containsKey("java_iterator"));
    }

    @Test
    void catalogContainsPythonIterator() {
        assertTrue(L3Protocols.CATALOG.containsKey("python_iterator"));
    }

    @Test
    void everyEntryHasL1AndL3() {
        for (var entry : L3Protocols.CATALOG.entrySet()) {
            assertNotNull(entry.getValue().l1(), entry.getKey() + " missing L1");
            assertNotNull(entry.getValue().l3(), entry.getKey() + " missing L3");
        }
    }

    @Test
    void everyEntryHasDescription() {
        for (var entry : L3Protocols.CATALOG.entrySet()) {
            assertNotNull(entry.getValue().description(), entry.getKey() + " missing description");
            assertFalse(entry.getValue().description().isEmpty(), entry.getKey() + " empty description");
        }
    }

    // -----------------------------------------------------------------------
    // L3 individual protocol analysis
    // -----------------------------------------------------------------------

    static Stream<String> protocolNames() {
        return L3Protocols.CATALOG.keySet().stream();
    }

    @ParameterizedTest
    @MethodSource("protocolNames")
    void l3IsLattice(String name) {
        var spec = L3Protocols.CATALOG.get(name);
        L3Result r = L3Analyzer.analyze(name, spec.l3(), "L3");
        assertTrue(r.isLattice(), name + " L3 should be a lattice");
    }

    @ParameterizedTest
    @MethodSource("protocolNames")
    void l1IsLattice(String name) {
        var spec = L3Protocols.CATALOG.get(name);
        L3Result r = L3Analyzer.analyze(name, spec.l1(), "L1");
        assertTrue(r.isLattice(), name + " L1 should be a lattice");
    }

    @ParameterizedTest
    @MethodSource("protocolNames")
    void l3HasPositiveStateCount(String name) {
        var spec = L3Protocols.CATALOG.get(name);
        L3Result r = L3Analyzer.analyze(name, spec.l3(), "L3");
        assertTrue(r.numStates() > 0, name + " should have states");
        assertTrue(r.numTransitions() > 0, name + " should have transitions");
    }

    @ParameterizedTest
    @MethodSource("protocolNames")
    void l3ClassificationNotNull(String name) {
        var spec = L3Protocols.CATALOG.get(name);
        L3Result r = L3Analyzer.analyze(name, spec.l3(), "L3");
        assertNotNull(r.classification());
        assertFalse(r.classification().isEmpty());
    }

    // -----------------------------------------------------------------------
    // Specific known results
    // -----------------------------------------------------------------------

    @Test
    void javaIteratorL3IsDistributive() {
        L3Result r = L3Analyzer.analyze("java_iterator",
                L3Protocols.CATALOG.get("java_iterator").l3(), "L3");
        assertTrue(r.isDistributive(), "java_iterator L3 should be distributive");
    }

    @Test
    void pythonIteratorL3IsDistributive() {
        L3Result r = L3Analyzer.analyze("python_iterator",
                L3Protocols.CATALOG.get("python_iterator").l3(), "L3");
        assertTrue(r.isDistributive(), "python_iterator L3 should be distributive");
    }

    @Test
    void javaOutputstreamL3IsDistributive() {
        L3Result r = L3Analyzer.analyze("java_outputstream",
                L3Protocols.CATALOG.get("java_outputstream").l3(), "L3");
        assertTrue(r.isDistributive(), "java_outputstream L3 should be distributive");
    }

    // -----------------------------------------------------------------------
    // Batch analysis
    // -----------------------------------------------------------------------

    @Test
    void analyzeAllL3Returns16Results() {
        List<L3Result> results = L3Analyzer.analyzeAllL3();
        assertEquals(16, results.size());
    }

    @Test
    void analyzeAllLevelsReturns32Results() {
        List<L3Result> results = L3Analyzer.analyzeAllLevels();
        assertEquals(32, results.size());
    }

    @Test
    void allL3AreLattices() {
        List<L3Result> results = L3Analyzer.analyzeAllL3();
        for (L3Result r : results) {
            assertTrue(r.isLattice(), r.name() + " L3 must be a lattice");
        }
    }

    @Test
    void allL1AreLattices() {
        List<L3Result> results = L3Analyzer.analyzeAllLevels();
        for (L3Result r : results) {
            if ("L1".equals(r.level())) {
                assertTrue(r.isLattice(), r.name() + " L1 must be a lattice");
            }
        }
    }

    // -----------------------------------------------------------------------
    // Reports
    // -----------------------------------------------------------------------

    @Test
    void formatReportNotEmpty() {
        List<L3Result> results = L3Analyzer.analyzeAllL3();
        String report = L3Analyzer.formatReport(results);
        assertFalse(report.isEmpty());
        assertTrue(report.contains("L3 PROTOCOL FIDELITY ANALYSIS"));
        assertTrue(report.contains("java_iterator"));
    }

    @Test
    void formatComparisonNotEmpty() {
        List<L3Result> results = L3Analyzer.analyzeAllLevels();
        String report = L3Analyzer.formatComparison(results);
        assertFalse(report.isEmpty());
        assertTrue(report.contains("L1 vs L3 FIDELITY COMPARISON"));
    }

    @Test
    void l3HasMoreStatesThanL1() {
        // L3 models are more detailed, so they typically have more states
        List<L3Result> results = L3Analyzer.analyzeAllLevels();
        int l3MoreStates = 0;
        var byName = new java.util.LinkedHashMap<String, java.util.Map<String, L3Result>>();
        for (L3Result r : results) {
            byName.computeIfAbsent(r.name(), k -> new java.util.LinkedHashMap<>())
                    .put(r.level(), r);
        }
        for (var entry : byName.entrySet()) {
            L3Result l1 = entry.getValue().get("L1");
            L3Result l3 = entry.getValue().get("L3");
            if (l1 != null && l3 != null && l3.numStates() >= l1.numStates()) {
                l3MoreStates++;
            }
        }
        // Most L3 models should have >= states as L1
        assertTrue(l3MoreStates > byName.size() / 2,
                "Most L3 models should have at least as many states as L1");
    }
}
