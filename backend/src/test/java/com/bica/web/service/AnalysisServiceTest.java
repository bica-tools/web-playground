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
    void parallelBenchmarkHasHeroSvg() {
        service.initBenchmarkCache();
        var benchmarks = service.getBenchmarks();
        var parallel = benchmarks.stream()
                .filter(b -> b.usesParallel())
                .findFirst()
                .orElseThrow();
        assertNotNull(parallel.heroSvgHtml());
        assertFalse(parallel.heroSvgHtml().isEmpty());
        assertTrue(parallel.heroSvgHtml().contains("<svg"));
    }

    @Test
    void analyzeBadInput() {
        assertThrows(Exception.class, () -> service.analyze("not valid }{"));
    }
}
