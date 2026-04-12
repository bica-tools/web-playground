package com.bica.reborn.morphism;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the morphism hierarchy ({@link MorphismChecker}).
 *
 * <p>Ported from {@code reticulate/tests/test_morphism.py}.
 */
class MorphismCheckerTest {

    // -- helpers ---------------------------------------------------------------

    private static StateSpace ss(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    private static StateSpace chain(int n) {
        var states = new HashSet<Integer>();
        var transitions = new ArrayList<Transition>();
        var labels = new HashMap<Integer, String>();
        for (int i = 0; i < n; i++) {
            states.add(i);
            labels.put(i, "s" + i);
        }
        for (int i = 0; i < n - 1; i++) {
            transitions.add(new Transition(i, "s" + i, i + 1));
        }
        return new StateSpace(states, transitions, 0, n - 1, labels);
    }

    private static StateSpace diamond() {
        return new StateSpace(
                Set.of(0, 1, 2, 3),
                List.of(
                        new Transition(0, "a", 1),
                        new Transition(0, "b", 2),
                        new Transition(1, "c", 3),
                        new Transition(2, "d", 3)),
                0, 3,
                Map.of(0, "top", 1, "left", 2, "right", 3, "bottom"));
    }

    private static Map<Integer, Integer> identity(StateSpace s) {
        var m = new HashMap<Integer, Integer>();
        for (int st : s.states()) m.put(st, st);
        return m;
    }

    // =========================================================================
    // Order-preserving tests
    // =========================================================================

    @Nested
    class OrderPreserving {

        @Test
        void identityPreserves() {
            var s = chain(3);
            assertTrue(MorphismChecker.isOrderPreserving(s, s, identity(s)));
        }

        @Test
        void constantToBottomPreserves() {
            var s = chain(3);
            var mapping = Map.of(0, 2, 1, 2, 2, 2);
            assertTrue(MorphismChecker.isOrderPreserving(s, s, mapping));
        }

        @Test
        void constantToTopPreserves() {
            var s = chain(3);
            var mapping = Map.of(0, 0, 1, 0, 2, 0);
            assertTrue(MorphismChecker.isOrderPreserving(s, s, mapping));
        }

        @Test
        void orderReversingFails() {
            var s = chain(3);
            var mapping = Map.of(0, 2, 1, 1, 2, 0);
            assertFalse(MorphismChecker.isOrderPreserving(s, s, mapping));
        }

        @Test
        void validHomomorphism() {
            var src = chain(3);
            var tgt = chain(2);
            var mapping = Map.of(0, 0, 1, 0, 2, 1);
            assertTrue(MorphismChecker.isOrderPreserving(src, tgt, mapping));
        }
    }

    // =========================================================================
    // Order-reflecting tests
    // =========================================================================

    @Nested
    class OrderReflecting {

        @Test
        void identityReflects() {
            var s = chain(3);
            assertTrue(MorphismChecker.isOrderReflecting(s, s, identity(s)));
        }

        @Test
        void embeddingReflects() {
            var src = chain(2);
            var tgt = chain(3);
            var mapping = Map.of(0, 0, 1, 2);
            assertTrue(MorphismChecker.isOrderReflecting(src, tgt, mapping));
        }

        @Test
        void nonReflectingSurjection() {
            var src = chain(3);
            var tgt = chain(2);
            var mapping = Map.of(0, 0, 1, 0, 2, 1);
            assertFalse(MorphismChecker.isOrderReflecting(src, tgt, mapping));
        }
    }

    // =========================================================================
    // Classify morphism tests
    // =========================================================================

    @Nested
    class ClassifyMorphismTests {

        @Test
        void identityIsIsomorphism() {
            var s = chain(3);
            var m = MorphismChecker.classifyMorphism(s, s, identity(s));
            assertEquals(MorphismKind.ISOMORPHISM, m.kind());
        }

        @Test
        void injectionIsEmbedding() {
            var src = chain(2);
            var tgt = chain(3);
            var mapping = Map.of(0, 0, 1, 2);
            var m = MorphismChecker.classifyMorphism(src, tgt, mapping);
            assertEquals(MorphismKind.EMBEDDING, m.kind());
        }

        @Test
        void surjectionIsProjection() {
            var src = chain(3);
            var tgt = chain(2);
            var mapping = Map.of(0, 0, 1, 0, 2, 1);
            var m = MorphismChecker.classifyMorphism(src, tgt, mapping);
            assertEquals(MorphismKind.PROJECTION, m.kind());
        }

        @Test
        void diamondToChainIsProjection() {
            var src = diamond();
            var tgt = chain(3);
            var mapping = Map.of(0, 0, 1, 1, 2, 1, 3, 2);
            var m = MorphismChecker.classifyMorphism(src, tgt, mapping);
            assertEquals(MorphismKind.PROJECTION, m.kind());
        }

        @Test
        void invalidRaisesException() {
            var s = chain(3);
            var mapping = Map.of(0, 2, 1, 1, 2, 0);
            assertThrows(IllegalArgumentException.class,
                    () -> MorphismChecker.classifyMorphism(s, s, mapping));
        }

        @Test
        void morphismFields() {
            var s = chain(2);
            var mapping = identity(s);
            var m = MorphismChecker.classifyMorphism(s, s, mapping);
            assertSame(s, m.source());
            assertSame(s, m.target());
            assertEquals(mapping, m.mapping());
            assertNotNull(m.kind());
        }
    }

    // =========================================================================
    // Find isomorphism tests
    // =========================================================================

    @Nested
    class FindIsomorphismTests {

        @Test
        void isomorphicChains() {
            var ss1 = ss("a . b . end");
            var ss2 = ss("x . y . end");
            var m = MorphismChecker.findIsomorphism(ss1, ss2);
            assertNotNull(m);
            assertEquals(MorphismKind.ISOMORPHISM, m.kind());
            assertEquals(ss2.top(), m.mapping().get(ss1.top()));
            assertEquals(ss2.bottom(), m.mapping().get(ss1.bottom()));
        }

        @Test
        void nonIsoDifferentSizes() {
            var ss1 = ss("a . end");
            var ss2 = ss("a . b . end");
            assertNull(MorphismChecker.findIsomorphism(ss1, ss2));
        }

        @Test
        void nonIsoSameSizeDifferentStructure() {
            var c4 = chain(4);
            var d = diamond();
            assertNull(MorphismChecker.findIsomorphism(c4, d));
        }

        @Test
        void selfIsomorphism() {
            var d = diamond();
            var m = MorphismChecker.findIsomorphism(d, d);
            assertNotNull(m);
            assertEquals(MorphismKind.ISOMORPHISM, m.kind());
        }

        @Test
        void endIsoEnd() {
            var ss1 = ss("end");
            var ss2 = ss("end");
            assertNotNull(MorphismChecker.findIsomorphism(ss1, ss2));
        }

        @Test
        void productCommutativity() {
            var ss1 = ss("(a . end || b . end)");
            var ss2 = ss("(b . end || a . end)");
            var m = MorphismChecker.findIsomorphism(ss1, ss2);
            assertNotNull(m);
            assertEquals(MorphismKind.ISOMORPHISM, m.kind());
        }

        @Test
        void differentTransitionCounts() {
            var ss1 = chain(3);
            var ss2 = new StateSpace(
                    Set.of(0, 1, 2),
                    List.of(
                            new Transition(0, "a", 1),
                            new Transition(0, "b", 2),
                            new Transition(1, "c", 2)),
                    0, 2,
                    Map.of(0, "top", 1, "mid", 2, "bot"));
            assertNull(MorphismChecker.findIsomorphism(ss1, ss2));
        }

        @Test
        void singleStateIsomorphism() {
            var ss1 = ss("end");
            var m = MorphismChecker.findIsomorphism(ss1, ss1);
            assertNotNull(m);
            assertEquals(MorphismKind.ISOMORPHISM, m.kind());
        }
    }

    // =========================================================================
    // Find embedding tests
    // =========================================================================

    @Nested
    class FindEmbeddingTests {

        @Test
        void chainEmbedsInLongerChain() {
            var ss1 = chain(2);
            var ss2 = chain(3);
            var m = MorphismChecker.findEmbedding(ss1, ss2);
            assertNotNull(m);
            assertTrue(m.kind() == MorphismKind.EMBEDDING
                    || m.kind() == MorphismKind.ISOMORPHISM);
            assertEquals(0, m.mapping().get(0));
            assertEquals(2, m.mapping().get(1));
        }

        @Test
        void chainEmbedsInDiamond() {
            var ss1 = chain(2);
            var ss2 = diamond();
            var m = MorphismChecker.findEmbedding(ss1, ss2);
            assertNotNull(m);
            assertEquals(MorphismKind.EMBEDDING, m.kind());
        }

        @Test
        void largerIntoSmallerFails() {
            var ss1 = chain(4);
            var ss2 = chain(3);
            assertNull(MorphismChecker.findEmbedding(ss1, ss2));
        }

        @Test
        void embeddingIsInjective() {
            var ss1 = chain(2);
            var ss2 = chain(3);
            var m = MorphismChecker.findEmbedding(ss1, ss2);
            assertNotNull(m);
            assertEquals(m.mapping().size(),
                    new HashSet<>(m.mapping().values()).size());
        }

        @Test
        void selfEmbeddingIsIsomorphism() {
            var s = chain(3);
            var m = MorphismChecker.findEmbedding(s, s);
            assertNotNull(m);
            assertEquals(MorphismKind.ISOMORPHISM, m.kind());
        }

        @Test
        void endEmbedsInChain() {
            var ss1 = ss("end");
            var ss2 = chain(3);
            var m = MorphismChecker.findEmbedding(ss1, ss2);
            assertNotNull(m);
        }

        @Test
        void embeddingPreservesAndReflects() {
            var ss1 = chain(2);
            var ss2 = diamond();
            var m = MorphismChecker.findEmbedding(ss1, ss2);
            assertNotNull(m);
            assertTrue(MorphismChecker.isOrderPreserving(ss1, ss2, m.mapping()));
            assertTrue(MorphismChecker.isOrderReflecting(ss1, ss2, m.mapping()));
        }
    }

    // =========================================================================
    // Galois connection tests
    // =========================================================================

    @Nested
    class GaloisConnectionTests {

        @Test
        void identityPair() {
            var s = chain(3);
            var alpha = identity(s);
            var gamma = identity(s);
            assertTrue(MorphismChecker.isGaloisConnection(alpha, gamma, s, s));
        }

        @Test
        void invalidPair() {
            var s = chain(3);
            var alpha = Map.of(0, 0, 1, 0, 2, 0);
            var gamma = Map.of(0, 2, 1, 2, 2, 2);
            assertFalse(MorphismChecker.isGaloisConnection(alpha, gamma, s, s));
        }

        @Test
        void twoStateIdentity() {
            var s = chain(2);
            var alpha = Map.of(0, 0, 1, 1);
            var gamma = Map.of(0, 0, 1, 1);
            assertTrue(MorphismChecker.isGaloisConnection(alpha, gamma, s, s));
        }

        @Test
        void constantBottomBottom() {
            var s = chain(3);
            var alpha = Map.of(0, 2, 1, 2, 2, 2);
            var gamma = Map.of(0, 2, 1, 2, 2, 2);
            assertFalse(MorphismChecker.isGaloisConnection(alpha, gamma, s, s));
        }
    }

    // =========================================================================
    // Hierarchy — strict separation
    // =========================================================================

    @Nested
    class Hierarchy {

        @Test
        void isoImpliesEmbed() {
            var s = chain(3);
            var m = MorphismChecker.classifyMorphism(s, s, identity(s));
            assertEquals(MorphismKind.ISOMORPHISM, m.kind());
        }

        @Test
        void embedNotProject() {
            var src = chain(2);
            var tgt = chain(3);
            var mapping = Map.of(0, 0, 1, 2);
            var m = MorphismChecker.classifyMorphism(src, tgt, mapping);
            assertEquals(MorphismKind.EMBEDDING, m.kind());
            assertFalse(new HashSet<>(mapping.values()).contains(1));
        }

        @Test
        void projectNotEmbed() {
            var src = chain(3);
            var tgt = chain(2);
            var mapping = Map.of(0, 0, 1, 0, 2, 1);
            var m = MorphismChecker.classifyMorphism(src, tgt, mapping);
            assertEquals(MorphismKind.PROJECTION, m.kind());
        }

        @Test
        void homoNotProjectNotEmbed() {
            var src = diamond();
            var tgt = chain(4);
            var mapping = Map.of(0, 0, 1, 1, 2, 1, 3, 3);
            var m = MorphismChecker.classifyMorphism(src, tgt, mapping);
            assertEquals(MorphismKind.HOMOMORPHISM, m.kind());
        }
    }

    // =========================================================================
    // Benchmarks — self-isomorphism and identity checks
    // =========================================================================

    @Nested
    class Benchmarks {

        private static final String[] ALL_BENCHMARKS = {
            "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
            "&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}",
            "&{connect: &{ehlo: rec X . &{mail: &{rcpt: &{data: +{OK: X, ERR: X}}}, quit: end}}}",
            "&{connect: rec X . &{request: +{OK200: &{readBody: X}, ERR4xx: X, ERR5xx: X}, close: end}}",
            "&{requestAuth: +{GRANTED: &{getToken: +{TOKEN: rec X . &{useToken: X, refreshToken: +{OK: X, EXPIRED: end}, revoke: end}, ERROR: end}}, DENIED: end}}",
            "&{lookup: &{getPrice: (&{proposeA: end} || &{proposeB: +{ACCEPT: &{pay: end}, REJECT: end}})}}",
            "&{initialize: (rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end} || rec Y . +{NOTIFICATION: Y, DONE: end})}",
            "&{sendTask: rec X . +{WORKING: &{getStatus: X, cancel: end}, COMPLETED: &{getArtifact: end}, FAILED: end}}",
            "&{open: +{OK: (rec X . &{read: X, doneRead: end} || rec Y . &{write: Y, doneWrite: end}) . &{close: end}, ERR: end}}",
            "&{insertCard: &{enterPIN: +{AUTH: rec X . &{checkBalance: X, withdraw: +{OK: X, INSUFFICIENT: X}, deposit: X, ejectCard: end}, REJECTED: &{ejectCard: end}}}}",
            "rec X . &{lock: &{unlock: X, newCondition: &{await: &{signal: &{unlock: X}}}}, close: end}",
            "&{connect: +{OPEN: (rec X . &{send: X, ping: X, closeSend: end} || rec Y . +{MESSAGE: Y, PONG: Y, CLOSE: end}), REFUSED: end}}",
            "&{begin: rec X . &{query: +{RESULT: X, ERROR: X}, update: +{ROWS: X, ERROR: X}, commit: end, rollback: end}}",
            "&{connect: rec X . &{subscribe: X, unsubscribe: X, poll: +{MSG: X, EMPTY: X}, disconnect: end}}",
            "rec X . &{query: +{ANSWER: X, NXDOMAIN: X, SERVFAIL: X, TIMEOUT: &{retry: X, abandon: end}}, close: end}",
            "&{input: &{parse: +{OK: &{buildStateSpace: +{OK: (&{checkLattice: &{checkTermination: &{checkWFPar: end}}} || &{renderDiagram: end}) . &{returnResult: end}, ERROR: end}}, ERROR: end}}}",
            "&{trigger: &{checkout: &{setup: (&{lint: end} || &{test: end}) . +{PASS: &{deploy: +{OK: end, FAIL: &{rollback: end}}}, FAIL: end}}}}",
            // #17 TLS Handshake
            "&{clientHello: +{HELLO_RETRY: &{clientHello: &{serverHello: &{certificate: &{verify: &{changeCipher: end}}, psk: &{changeCipher: end}}}}, SERVER_HELLO: &{certificate: &{verify: &{changeCipher: end}}, psk: &{changeCipher: end}}}}",
            // #18 Raft Leader Election
            "rec X . +{TIMEOUT: &{requestVote: +{ELECTED: rec Y . &{appendEntries: +{ACK: Y, NACK: Y}, heartbeatTimeout: Y, stepDown: X}, REJECTED: X}}, HEARTBEAT: X, SHUTDOWN: end}",
            // #19 MQTT Client
            "&{connect: +{CONNACK: (rec X . &{publish: +{PUBACK: X, TIMEOUT: X}, disconnect: end} || rec Y . +{MESSAGE: Y, SUBACK: Y, DONE: end}) . end, REFUSED: end}}",
            // #20 Circuit Breaker
            "rec X . &{call: +{SUCCESS: X, FAILURE: +{TRIPPED: rec Y . &{probe: +{OK: X, FAIL: Y}, timeout: end}, OK: X}}, reset: end}",
            // #21 Connection Pool
            "&{init: (rec X . &{acquire: &{use: &{release: X}}, drain: end} || rec Y . &{healthCheck: +{HEALTHY: Y, UNHEALTHY: Y}, shutdown: end})}",
            // #22 gRPC BiDi Stream
            "&{open: (rec X . &{send: X, halfClose: end} || rec Y . +{RESPONSE: Y, TRAILER: end})}",
            // #23 Blockchain Tx
            "&{createTx: &{sign: &{broadcast: rec X . +{PENDING: X, CONFIRMED: &{getReceipt: end}, DROPPED: end, FAILED: end}}}}",
            // #24 Kafka Consumer
            "&{subscribe: (rec X . &{poll: +{RECORDS: &{process: &{commit: X}}, EMPTY: X}, pause: end} || rec Y . +{REBALANCE: Y, REVOKED: end})}",
            // #25 Rate Limiter
            "rec X . &{tryAcquire: +{ALLOWED: X, THROTTLED: &{wait: X, abort: end}}, close: end}",
            // #26 Saga Orchestrator
            "&{begin: (&{step1: +{OK: end, FAIL: &{compensate1: end}}} || &{step2: +{OK: end, FAIL: &{compensate2: end}}}) . +{ALL_OK: &{commit: end}, PARTIAL: &{rollback: end}}}",
            // #27 Two-Phase Commit
            "&{prepare: &{allYes: &{commit: +{ACK: end, TIMEOUT: &{abort: end}}}, anyNo: &{abort: end}}}",
            // #28 Leader Replication
            "&{electLeader: rec X . &{write: (&{replicate1: +{ACK: end, NACK: end}} || &{replicate2: +{ACK: end, NACK: end}}) . +{QUORUM: &{apply: X}, NO_QUORUM: X}, stepDown: end}}",
            // #29 Failover
            "&{connect: rec X . &{request: +{OK: X, FAIL: &{reconnect: +{UP: X, DOWN: end}}}, close: end}}",
        };

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29})
        void selfIsomorphism(int idx) {
            var s = ss(ALL_BENCHMARKS[idx]);
            var m = MorphismChecker.findIsomorphism(s, s);
            assertNotNull(m, "Benchmark #" + idx + " should be self-isomorphic");
            assertEquals(MorphismKind.ISOMORPHISM, m.kind());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29})
        void identityIsOrderPreserving(int idx) {
            var s = ss(ALL_BENCHMARKS[idx]);
            assertTrue(MorphismChecker.isOrderPreserving(s, s, identity(s)),
                    "Identity on benchmark #" + idx + " should be order-preserving");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29})
        void identityIsOrderReflecting(int idx) {
            var s = ss(ALL_BENCHMARKS[idx]);
            assertTrue(MorphismChecker.isOrderReflecting(s, s, identity(s)),
                    "Identity on benchmark #" + idx + " should be order-reflecting");
        }
    }
}
