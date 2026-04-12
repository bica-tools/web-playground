package com.bica.reborn.spectral_sync;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for spectral synchronization analysis (Step 31d).
 * Java port of Python {@code test_spectral_sync.py}.
 */
class SpectralSyncCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Role extraction
    // -----------------------------------------------------------------------

    @Test
    void extractRolesSingleMethod() {
        List<String> roles = SpectralSyncChecker.extractRoles(build("&{a: end}"));
        assertTrue(roles.contains("a"));
    }

    @Test
    void extractRolesMultipleMethods() {
        List<String> roles = SpectralSyncChecker.extractRoles(build("&{a: end, b: end}"));
        assertTrue(roles.contains("a"));
        assertTrue(roles.contains("b"));
    }

    @Test
    void extractRolesNested() {
        List<String> roles = SpectralSyncChecker.extractRoles(build("&{a: &{b: end}}"));
        assertTrue(roles.contains("a"));
        assertTrue(roles.contains("b"));
    }

    // -----------------------------------------------------------------------
    // Coupling matrix
    // -----------------------------------------------------------------------

    @Test
    void couplingMatrixSingleMethod() {
        var cr = SpectralSyncChecker.roleCouplingMatrix(build("&{a: end}"));
        assertEquals(1, cr.matrix().size(), "one role 'a'");
        assertTrue(cr.matrix().get(0).get(0) >= 1);
    }

    @Test
    void couplingMatrixTwoMethods() {
        var cr = SpectralSyncChecker.roleCouplingMatrix(build("&{a: end, b: end}"));
        assertEquals(2, cr.matrix().size());
    }

    @Test
    void couplingMatrixSymmetric() {
        var cr = SpectralSyncChecker.roleCouplingMatrix(build("&{a: end, b: end}"));
        List<List<Integer>> M = cr.matrix();
        int n = M.size();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                assertEquals(M.get(i).get(j), M.get(j).get(i),
                        "M[" + i + "][" + j + "] != M[" + j + "][" + i + "]");
            }
        }
    }

    // -----------------------------------------------------------------------
    // Sync spectrum
    // -----------------------------------------------------------------------

    @Test
    void spectrumSingleRole() {
        List<Double> spec = SpectralSyncChecker.syncSpectrum(build("&{a: end}"));
        assertEquals(1, spec.size());
    }

    @Test
    void spectrumTwoRoles() {
        List<Double> spec = SpectralSyncChecker.syncSpectrum(build("&{a: end, b: end}"));
        assertEquals(2, spec.size());
    }

    // -----------------------------------------------------------------------
    // Metrics
    // -----------------------------------------------------------------------

    @Test
    void maxCouplingNonNegative() {
        int mc = SpectralSyncChecker.maxCoupling(build("&{a: end}"));
        assertTrue(mc >= 0);
    }

    @Test
    void isolationScoreNonNegative() {
        int iso = SpectralSyncChecker.isolationScore(build("&{a: end}"));
        assertTrue(iso >= 0);
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Test
    void analyzeEnd() {
        SyncResult r = SpectralSyncChecker.analyze(build("end"));
        assertEquals(1, r.numStates());
    }

    @Test
    void analyzeBranch() {
        SyncResult r = SpectralSyncChecker.analyze(build("&{a: end, b: end}"));
        assertTrue(r.numRoles() >= 2);
    }

    @Test
    void analyzeParallel() {
        SyncResult r = SpectralSyncChecker.analyze(build("(&{a: end} || &{b: end})"));
        assertTrue(r.numRoles() >= 2);
    }

    // -----------------------------------------------------------------------
    // Benchmarks
    // -----------------------------------------------------------------------

    static Stream<org.junit.jupiter.params.provider.Arguments> benchmarks() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("File Object", "&{open: &{read: end, write: end}}"),
                org.junit.jupiter.params.provider.Arguments.of("SMTP", "&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}"),
                org.junit.jupiter.params.provider.Arguments.of("Two-choice", "&{a: end, b: end}"),
                org.junit.jupiter.params.provider.Arguments.of("Parallel", "(&{a: end} || &{b: end})")
        );
    }

    @ParameterizedTest
    @MethodSource("benchmarks")
    void benchmarkSyncRuns(String name, String type) {
        StateSpace ss = build(type);
        SyncResult r = SpectralSyncChecker.analyze(ss);
        assertEquals(ss.states().size(), r.numStates(), name);
        assertTrue(r.numRoles() >= 1, name + ": must have at least 1 role");
        assertEquals(r.numRoles(), r.syncSpectrum().size(),
                name + ": spectrum size must match role count");
    }
}
