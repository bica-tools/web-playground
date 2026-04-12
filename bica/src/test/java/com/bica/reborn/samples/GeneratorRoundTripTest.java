package com.bica.reborn.samples;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.testgen.PathEnumerator;
import com.bica.reborn.testgen.TestGenConfig;
import com.bica.reborn.testgen.TestGenerator;
import com.bica.reborn.testgen.ViolationStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Meta-test: regenerates test source for each sample implementation
 * and verifies the output structure matches expectations.
 */
class GeneratorRoundTripTest {

    private static final String FILE_HANDLE_TYPE =
            "open . &{read: close . end, write: close . end}";
    private static final String SIMPLE_ITERATOR_TYPE =
            "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}";
    private static final String ATM_TYPE =
            "insertCard . enterPIN . +{AUTH: rec X . &{checkBalance: X, "
            + "withdraw: +{OK: X, INSUFFICIENT: X}, deposit: X, "
            + "ejectCard: end}, REJECTED: ejectCard . end}";

    private TestGenConfig config(String className) {
        return new TestGenConfig(
                className, "com.bica.reborn.samples", "obj",
                2, 100, ViolationStyle.CALL_ANYWAY);
    }

    private PathEnumerator.EnumerationResult enumerate(String sessionType, String className) {
        var ast = Parser.parse(sessionType);
        var ss = StateSpaceBuilder.build(ast);
        return PathEnumerator.enumerate(ss, config(className));
    }

    private PathEnumerator.ClientProgramResult clientPrograms(String sessionType) {
        var ast = Parser.parse(sessionType);
        var ss = StateSpaceBuilder.build(ast);
        return PathEnumerator.enumerateClientPrograms(ss, 2, 100);
    }

    private String generate(String sessionType, String className) {
        var ast = Parser.parse(sessionType);
        var ss = StateSpaceBuilder.build(ast);
        return TestGenerator.generate(ss, config(className), PrettyPrinter.pretty(ast));
    }

    // =========================================================================
    // FileHandle round-trip
    // =========================================================================

    @Test
    void fileHandle_validPathCount() {
        var result = enumerate(FILE_HANDLE_TYPE, "FileHandle");
        assertEquals(2, result.validPaths().size());
    }

    @Test
    void fileHandle_violationCount() {
        var result = enumerate(FILE_HANDLE_TYPE, "FileHandle");
        assertEquals(11, result.violations().size());
    }

    @Test
    void fileHandle_incompletePrefixCount() {
        var result = enumerate(FILE_HANDLE_TYPE, "FileHandle");
        assertEquals(3, result.incompletePrefixes().size());
    }

    @Test
    void fileHandle_notTruncated() {
        var result = enumerate(FILE_HANDLE_TYPE, "FileHandle");
        assertFalse(result.truncated());
    }

    @Test
    void fileHandle_generatedSourceContainsClassName() {
        var source = generate(FILE_HANDLE_TYPE, "FileHandle");
        assertTrue(source.contains("class FileHandleProtocolTest"));
    }

    @Test
    void fileHandle_generatedSourceContainsPackage() {
        var source = generate(FILE_HANDLE_TYPE, "FileHandle");
        assertTrue(source.contains("package com.bica.reborn.samples;"));
    }

    @Test
    void fileHandle_generatedSourceContainsAssertThrows() {
        var source = generate(FILE_HANDLE_TYPE, "FileHandle");
        assertTrue(source.contains("assertThrows(ProtocolViolationException.class"));
    }

    @Test
    void fileHandle_generatedSourceIsStable() {
        var first = generate(FILE_HANDLE_TYPE, "FileHandle");
        var second = generate(FILE_HANDLE_TYPE, "FileHandle");
        assertEquals(first, second, "Generated source must be deterministic");
    }

    @Test
    void fileHandle_clientProgramCount() {
        var cp = clientPrograms(FILE_HANDLE_TYPE);
        assertEquals(2, cp.programs().size(), "FileHandle has no selections, same as flat paths");
    }

    // =========================================================================
    // SimpleIterator round-trip
    // =========================================================================

    @Test
    void simpleIterator_validPathCount() {
        var result = enumerate(SIMPLE_ITERATOR_TYPE, "SimpleIterator");
        assertEquals(3, result.validPaths().size());
    }

    @Test
    void simpleIterator_violationCount() {
        var result = enumerate(SIMPLE_ITERATOR_TYPE, "SimpleIterator");
        assertEquals(2, result.violations().size());
    }

    @Test
    void simpleIterator_incompletePrefixCount() {
        var result = enumerate(SIMPLE_ITERATOR_TYPE, "SimpleIterator");
        assertEquals(7, result.incompletePrefixes().size());
    }

    @Test
    void simpleIterator_notTruncated() {
        var result = enumerate(SIMPLE_ITERATOR_TYPE, "SimpleIterator");
        assertFalse(result.truncated());
    }

    @Test
    void simpleIterator_generatedSourceContainsClassName() {
        var source = generate(SIMPLE_ITERATOR_TYPE, "SimpleIterator");
        assertTrue(source.contains("class SimpleIteratorProtocolTest"));
    }

    @Test
    void simpleIterator_generatedSourceIsStable() {
        var first = generate(SIMPLE_ITERATOR_TYPE, "SimpleIterator");
        var second = generate(SIMPLE_ITERATOR_TYPE, "SimpleIterator");
        assertEquals(first, second, "Generated source must be deterministic");
    }

    @Test
    void simpleIterator_clientProgramCount() {
        var cp = clientPrograms(SIMPLE_ITERATOR_TYPE);
        assertEquals(1, cp.programs().size(), "SimpleIterator folds hasNext selection into switch");
    }

    @Test
    void simpleIterator_generatedSourceContainsSwitch() {
        var source = generate(SIMPLE_ITERATOR_TYPE, "SimpleIterator");
        assertTrue(source.contains("var hasNextResult = obj.hasNext()"));
        assertTrue(source.contains("switch (hasNextResult)"));
        assertTrue(source.contains("case TRUE ->"));
        assertTrue(source.contains("case FALSE ->"));
    }

    // =========================================================================
    // ATM round-trip
    // =========================================================================

    @Test
    void atm_validPathCount() {
        var result = enumerate(ATM_TYPE, "ATM");
        assertEquals(22, result.validPaths().size());
    }

    @Test
    void atm_violationCount() {
        var result = enumerate(ATM_TYPE, "ATM");
        assertEquals(17, result.violations().size());
    }

    @Test
    void atm_incompletePrefixCount() {
        var result = enumerate(ATM_TYPE, "ATM");
        assertEquals(29, result.incompletePrefixes().size());
    }

    @Test
    void atm_notTruncated() {
        var result = enumerate(ATM_TYPE, "ATM");
        assertFalse(result.truncated());
    }

    @Test
    void atm_generatedSourceContainsClassName() {
        var source = generate(ATM_TYPE, "ATM");
        assertTrue(source.contains("class ATMProtocolTest"));
    }

    @Test
    void atm_generatedSourceIsStable() {
        var first = generate(ATM_TYPE, "ATM");
        var second = generate(ATM_TYPE, "ATM");
        assertEquals(first, second, "Generated source must be deterministic");
    }

    @Test
    void atm_totalTestCount() {
        var result = enumerate(ATM_TYPE, "ATM");
        int total = result.validPaths().size()
                + result.violations().size()
                + result.incompletePrefixes().size();
        assertEquals(68, total, "ATM should have exactly 68 generated tests");
    }

    @Test
    void atm_clientProgramCount() {
        var cp = clientPrograms(ATM_TYPE);
        assertEquals(22, cp.programs().size(), "ATM tree programs match flat count with zip");
    }

    @Test
    void atm_generatedSourceContainsSwitch() {
        var source = generate(ATM_TYPE, "ATM");
        assertTrue(source.contains("var enterPINResult = obj.enterPIN()"));
        assertTrue(source.contains("switch (enterPINResult)"));
        assertTrue(source.contains("case AUTH ->"));
        assertTrue(source.contains("case REJECTED ->"));
        assertTrue(source.contains("var withdrawResult = obj.withdraw()"));
        assertTrue(source.contains("case OK ->"));
        assertTrue(source.contains("case INSUFFICIENT ->"));
    }

    // =========================================================================
    // Cross-cutting
    // =========================================================================

    @Test
    void allSamples_totalTestCount() {
        var fh = enumerate(FILE_HANDLE_TYPE, "FileHandle");
        var si = enumerate(SIMPLE_ITERATOR_TYPE, "SimpleIterator");
        var atm = enumerate(ATM_TYPE, "ATM");

        int fhTotal = fh.validPaths().size() + fh.violations().size() + fh.incompletePrefixes().size();
        int siTotal = si.validPaths().size() + si.violations().size() + si.incompletePrefixes().size();
        int atmTotal = atm.validPaths().size() + atm.violations().size() + atm.incompletePrefixes().size();

        assertEquals(96, fhTotal + siTotal + atmTotal,
                "Total generated tests across all samples should be 96");
    }
}
