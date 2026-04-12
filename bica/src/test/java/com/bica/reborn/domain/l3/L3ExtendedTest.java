package com.bica.reborn.domain.l3;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for L3Extended protocol catalog — Java port of test_l3_extended.py.
 */
class L3ExtendedTest {

    @Test
    void catalogHas60Protocols() {
        assertEquals(60, L3Extended.CATALOG.size());
    }

    static Stream<String> protocolNames() {
        return L3Extended.CATALOG.keySet().stream();
    }

    @ParameterizedTest
    @MethodSource("protocolNames")
    void parsesWithoutError(String name) {
        String st = L3Extended.CATALOG.get(name);
        assertDoesNotThrow(() -> Parser.parse(st), name + " should parse");
    }

    @ParameterizedTest
    @MethodSource("protocolNames")
    void buildsStateSpace(String name) {
        SessionType ast = Parser.parse(L3Extended.CATALOG.get(name));
        StateSpace ss = StateSpaceBuilder.build(ast);
        assertTrue(ss.states().size() > 0, name + " should have states");
        assertTrue(ss.transitions().size() > 0, name + " should have transitions");
    }

    @ParameterizedTest
    @MethodSource("protocolNames")
    void isLattice(String name) {
        SessionType ast = Parser.parse(L3Extended.CATALOG.get(name));
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        assertTrue(lr.isLattice(), name + " should be a lattice");
    }

    @ParameterizedTest
    @MethodSource("protocolNames")
    void fullAnalysis(String name) {
        L3Result r = L3Analyzer.analyze(name, L3Extended.CATALOG.get(name), "L3");
        assertTrue(r.isLattice(), name + " must be a lattice");
        assertNotNull(r.classification());
    }

    // --- Domain group counts ---

    @Test
    void javaStdlibCount() {
        long count = L3Extended.CATALOG.keySet().stream()
                .filter(k -> k.startsWith("java_")).count();
        assertEquals(15, count, "Should have 15 Java stdlib protocols");
    }

    @Test
    void pythonStdlibCount() {
        long count = L3Extended.CATALOG.keySet().stream()
                .filter(k -> k.startsWith("python_")).count();
        assertEquals(10, count, "Should have 10 Python stdlib protocols");
    }

    // --- Specific protocols ---

    @Test
    void kafkaProducerExists() {
        assertTrue(L3Extended.CATALOG.containsKey("kafka_producer"));
    }

    @Test
    void dockerLifecycleExists() {
        assertTrue(L3Extended.CATALOG.containsKey("docker_lifecycle"));
    }

    @Test
    void websocketClientExists() {
        assertTrue(L3Extended.CATALOG.containsKey("websocket_client"));
    }

    @Test
    void undoRedoExists() {
        assertTrue(L3Extended.CATALOG.containsKey("undo_redo"));
    }
}
