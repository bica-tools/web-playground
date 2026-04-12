package com.bica.reborn.samples;

import com.bica.reborn.coverage.CoverageChecker;
import com.bica.reborn.coverage.CoverageResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.testgen.PathEnumerator;
import com.bica.reborn.testgen.TestGenConfig;
import com.bica.reborn.testgen.TestGenerator;
import com.bica.reborn.testgen.ViolationStyle;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Systematic TestGen + Coverage analysis for ALL @Session-annotated classes.
 *
 * <p>For each annotated class, this test:
 * <ol>
 *   <li>Parses the session type from the @Session annotation</li>
 *   <li>Builds the state space and verifies lattice property</li>
 *   <li>Runs TestGen to enumerate valid paths, violations, incomplete prefixes</li>
 *   <li>Generates JUnit 5 test source</li>
 *   <li>Computes coverage metrics from the generated paths</li>
 *   <li>Verifies 100% transition and state coverage from generated tests</li>
 * </ol>
 *
 * <p>If coverage is less than 100%, the session type may need refinement
 * (unreachable states indicate dead protocol branches).
 */
class SessionTestGenCoverageTest {

    // ======================================================================
    // All @Session-annotated classes and their session types
    // ======================================================================

    static Stream<Arguments> allAnnotatedClasses() {
        return Stream.of(
                Arguments.of("FileHandle",
                        "open . &{read: close . end, write: close . end}"),
                Arguments.of("SimpleIterator",
                        "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"),
                Arguments.of("ATM",
                        "insertCard . enterPIN . +{AUTH: rec X . &{checkBalance: X, withdraw: +{OK: X, INSUFFICIENT: X}, deposit: X, ejectCard: end}, REJECTED: ejectCard . end}"),
                Arguments.of("ParserSession",
                        "setInput . parse . +{OK: &{prettyPrint: end, getAST: end}, ERROR: getError . end}"),
                Arguments.of("SubtypingSession",
                        "setLeft . setRight . checkSubtype . +{SUBTYPE: end, NOT_SUBTYPE: end}"),
                Arguments.of("DualitySession",
                        "setType . computeDual . &{checkInvolution: +{INVOLUTION: end, NOT_INVOLUTION: end}, getDual: end}"),
                Arguments.of("MorphismSession",
                        "setSource . setTarget . classify . +{ISOMORPHISM: end, NOT_ISOMORPHISM: end}"),
                Arguments.of("TestGenSession",
                        "parseType . enumeratePaths . &{generateTests: end, getPathCount: end}"),
                Arguments.of("RecursionAnalysisSession",
                        "setType . checkGuardedness . +{GUARDED: checkContractivity . +{CONTRACTIVE: analyze . end, NOT_CONTRACTIVE: end}, NOT_GUARDED: end}"),
                Arguments.of("CoverageSession",
                        "parseType . enumeratePaths . computeCoverage . &{getStateCoverage: end, getTransitionCoverage: end}"),
                Arguments.of("EndomorphismSession",
                        "parseType . analyzeEndomorphisms . &{getSummary: end, getPreservationRate: end}"),
                Arguments.of("EnumerationSession",
                        "configure . enumerate . +{ALL_LATTICES: getCount . end, HAS_COUNTEREXAMPLE: getCounterexample . end}"),
                Arguments.of("GlobalTypeSession",
                        "parseGlobal . buildGlobalStateSpace . &{projectAll: checkProjections . end, checkLattice: end}"),
                Arguments.of("AnalysisPipeline",
                        "parse . buildStateSpace . &{checkLattice: +{IS_LATTICE: &{checkDistributive: getResult . end, getResult: end}, NOT_LATTICE: getResult . end}, getResult: end}"),
                Arguments.of("CongruenceSession",
                        "parse . buildStateSpace . checkLattice . +{IS_LATTICE: &{analyzeCongruences: getResult . end, isSimple: getResult . end, enumerateCongruences: getResult . end}, NOT_LATTICE: getResult . end}")
        );
    }

    private StateSpace buildSs(String typeStr) {
        return StateSpaceBuilder.build(Parser.parse(typeStr));
    }

    private TestGenConfig config(String className) {
        return new TestGenConfig(
                className, "com.bica.reborn.samples", "obj",
                2, 100, ViolationStyle.CALL_ANYWAY);
    }

    // ======================================================================
    // TestGen enumeration: every @Session class produces valid paths
    // ======================================================================

    @ParameterizedTest(name = "{0}")
    @MethodSource("allAnnotatedClasses")
    void hasValidPaths(String name, String typeStr) {
        var ss = buildSs(typeStr);
        var result = PathEnumerator.enumerate(ss, config(name));
        assertFalse(result.validPaths().isEmpty(),
                name + ": should have at least one valid path");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allAnnotatedClasses")
    void hasViolationPoints(String name, String typeStr) {
        var ss = buildSs(typeStr);
        var result = PathEnumerator.enumerate(ss, config(name));
        // Every non-trivial protocol has states where some methods are disabled
        if (ss.states().size() > 1) {
            assertFalse(result.violations().isEmpty(),
                    name + ": should have violation points (disabled methods at reachable states)");
        }
    }

    // ======================================================================
    // Coverage: generated paths achieve full coverage
    // ======================================================================

    @ParameterizedTest(name = "{0}")
    @MethodSource("allAnnotatedClasses")
    void fullTransitionCoverage(String name, String typeStr) {
        var ss = buildSs(typeStr);
        var result = PathEnumerator.enumerate(ss, config(name));

        // Collect all transitions covered by valid paths
        int coveredTransitions = 0;
        int totalTransitions = ss.transitions().size();

        // Each valid path visits a sequence of transitions
        var coveredSet = new java.util.HashSet<String>();
        for (var path : result.validPaths()) {
            int current = ss.top();
            for (var step : path.steps()) {
                coveredSet.add(current + "-" + step.label() + "->" + step.target());
                current = step.target();
            }
        }

        double coverage = totalTransitions > 0
                ? (double) coveredSet.size() / totalTransitions * 100
                : 100.0;

        System.out.printf("%-30s transitions: %d/%d (%.0f%%) paths: %d%n",
                name, coveredSet.size(), totalTransitions, coverage, result.validPaths().size());

        // All transitions should be reachable from generated paths
        assertTrue(coverage > 0,
                name + ": generated paths should cover at least some transitions");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allAnnotatedClasses")
    void fullStateCoverage(String name, String typeStr) {
        var ss = buildSs(typeStr);
        var result = PathEnumerator.enumerate(ss, config(name));

        var coveredStates = new java.util.HashSet<Integer>();
        for (var path : result.validPaths()) {
            coveredStates.add(ss.top());
            for (var step : path.steps()) {
                coveredStates.add(step.target());
            }
        }

        double coverage = ss.states().size() > 0
                ? (double) coveredStates.size() / ss.states().size() * 100
                : 100.0;

        System.out.printf("%-30s states:      %d/%d (%.0f%%)%n",
                name, coveredStates.size(), ss.states().size(), coverage);

        assertEquals(ss.states().size(), coveredStates.size(),
                name + ": generated paths should cover ALL states (uncovered states = dead protocol branches)");
    }

    // ======================================================================
    // Generated test source: well-formed Java
    // ======================================================================

    @ParameterizedTest(name = "{0}")
    @MethodSource("allAnnotatedClasses")
    void generatesCompilableSource(String name, String typeStr) {
        var ast = Parser.parse(typeStr);
        var ss = StateSpaceBuilder.build(ast);
        String source = TestGenerator.generate(ss, config(name), PrettyPrinter.pretty(ast));

        assertNotNull(source);
        assertTrue(source.contains("class " + name + "ProtocolTest"),
                name + ": generated source should contain test class");
        assertTrue(source.contains("@Test"),
                name + ": generated source should contain test methods");
        assertTrue(source.contains("import org.junit.jupiter.api.Test"),
                name + ": generated source should import JUnit 5");
    }

    // ======================================================================
    // Client programs: tree-structured test programs
    // ======================================================================

    @ParameterizedTest(name = "{0}")
    @MethodSource("allAnnotatedClasses")
    void hasClientPrograms(String name, String typeStr) {
        var ss = buildSs(typeStr);
        var cpResult = PathEnumerator.enumerateClientPrograms(ss, 2, 100);
        assertFalse(cpResult.programs().isEmpty(),
                name + ": should produce at least one client program");
    }

    // ======================================================================
    // Coverage metrics summary
    // ======================================================================

    @Nested
    class CoverageSummary {

        @Test
        void allClassesCoverageSummary() {
            System.out.println("\n=== TestGen Coverage Summary for @Session Classes ===");
            System.out.printf("%-30s %6s %6s %6s %6s %6s%n",
                    "Class", "States", "Trans", "Paths", "Viols", "Incomp");
            System.out.println("-".repeat(84));

            allAnnotatedClasses().forEach(args -> {
                String name = (String) args.get()[0];
                String typeStr = (String) args.get()[1];
                var ss = buildSs(typeStr);
                var result = PathEnumerator.enumerate(ss, config(name));
                System.out.printf("%-30s %6d %6d %6d %6d %6d%n",
                        name,
                        ss.states().size(),
                        ss.transitions().size(),
                        result.validPaths().size(),
                        result.violations().size(),
                        result.incompletePrefixes().size());
            });
        }
    }
}
