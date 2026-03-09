package com.bica.web.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisServiceTest {

    private final AnalysisService service = new AnalysisService();

    @Test
    void analyzeSimpleType() {
        var result = service.analyze("end");
        assertEquals("end", result.pretty());
        assertEquals(1, result.numStates());
        assertTrue(result.isLattice());
        assertTrue(result.terminates());
    }

    @Test
    void analyzeIterator() {
        var result = service.analyze("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
        assertEquals(4, result.numStates());
        assertTrue(result.isLattice());
        assertTrue(result.terminates());
        assertTrue(result.wfParallel());
    }

    @Test
    void analyzeParallel() {
        var result = service.analyze(
                "lookup . getPrice . (proposeA . end || proposeB . +{ACCEPT: pay . end, REJECT: end})");
        assertTrue(result.numStates() > 1);
        assertTrue(result.isLattice());
        assertNotNull(result.dotSource());
        assertFalse(result.dotSource().isEmpty());
    }

    @Test
    void generateTests() {
        var result = service.generateTests(
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                "Iterator", null, null);
        assertNotNull(result.testSource());
        assertTrue(result.testSource().contains("class IteratorProtocolTest"));
    }

    @Test
    void benchmarksLoaded() {
        service.initBenchmarkCache();
        var benchmarks = service.getBenchmarks();
        assertEquals(34, benchmarks.size());
        assertTrue(benchmarks.stream().allMatch(b -> b.isLattice()));
    }

    @Test
    void analyzeBadInput() {
        assertThrows(Exception.class, () -> service.analyze("not valid }{"));
    }

    // --- New enriched field tests ---

    @Test
    void analyzeEndHasNoParallelNoRecursion() {
        var result = service.analyze("end");
        assertFalse(result.usesParallel());
        assertFalse(result.isRecursive());
        assertEquals(0, result.recDepth());
        assertTrue(result.methods().isEmpty());
        assertEquals(0, result.numMethods());
        assertTrue(result.threadSafe());
    }

    @Test
    void analyzeIteratorEnrichedFields() {
        var result = service.analyze("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
        assertFalse(result.usesParallel());
        assertTrue(result.isRecursive());
        assertEquals(1, result.recDepth());
        assertTrue(result.methods().contains("hasNext"));
        assertTrue(result.methods().contains("next"));
        assertEquals(2, result.numMethods());
        assertTrue(result.threadSafe());
        assertTrue(result.numValidPaths() > 0);
        assertTrue(result.numTests() > 0);
    }

    @Test
    void analyzeParallelEnrichedFields() {
        var result = service.analyze(
                "lookup . getPrice . (proposeA . end || proposeB . +{ACCEPT: pay . end, REJECT: end})");
        assertTrue(result.usesParallel());
        assertFalse(result.isRecursive());
        assertEquals(0, result.recDepth());
        assertTrue(result.numMethods() > 0);
        assertTrue(result.methods().contains("lookup"));
        assertTrue(result.methods().contains("proposeA"));
        assertTrue(result.methods().contains("proposeB"));
    }

    @Test
    void analyzeNestedRecDepth() {
        // rec X . +{A: rec Y . &{b: Y, c: X}, B: end}
        var result = service.analyze("rec X . +{A: rec Y . &{b: Y, c: X}, B: end}");
        assertTrue(result.isRecursive());
        assertEquals(2, result.recDepth());
    }

    @Test
    void analyzeTestCountsPresent() {
        var result = service.analyze("&{a: end, b: end}");
        assertTrue(result.numValidPaths() >= 0);
        assertTrue(result.numViolations() >= 0);
        assertTrue(result.numIncomplete() >= 0);
        assertEquals(result.numValidPaths() + result.numViolations() + result.numIncomplete(),
                result.numTests());
    }

    @Test
    void benchmarkEnrichedFields() {
        service.initBenchmarkCache();
        var benchmarks = service.getBenchmarks();
        // Check the Iterator benchmark has enriched fields
        var iterator = benchmarks.stream()
                .filter(b -> b.name().equals("Java Iterator"))
                .findFirst().orElseThrow();
        assertTrue(iterator.isRecursive());
        assertEquals(1, iterator.recDepth());
        assertEquals(2, iterator.numMethods());
        assertTrue(iterator.methods().contains("hasNext"));
        assertTrue(iterator.methods().contains("next"));
        assertTrue(iterator.numValidPaths() > 0);

        // Check a parallel benchmark
        var twoBuyer = benchmarks.stream()
                .filter(b -> b.name().equals("Two-Buyer"))
                .findFirst().orElseThrow();
        assertTrue(twoBuyer.usesParallel());
        assertFalse(twoBuyer.isRecursive());
        assertEquals(0, twoBuyer.recDepth());
        assertTrue(twoBuyer.numMethods() > 0);
    }
}
