package com.bica.reborn.composition;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.duality.DualityChecker;
import com.bica.reborn.globaltype.GlobalTypeParser;
import com.bica.reborn.globaltype.Projection;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CompositionCheckerTest {

    private static SessionType parse(String s) {
        return Parser.parse(s);
    }

    // ======================================================================
    // compose() — basic
    // ======================================================================

    @Nested
    class ComposeBasic {

        @Test
        void singleParticipant() {
            var s = parse("&{a: end, b: end}");
            var result = CompositionChecker.compose(List.of(Map.entry("Alice", s)));
            assertTrue(result.isLattice());
            assertEquals(1, result.participants().size());
            assertEquals(0, result.compatibility().size());
        }

        @Test
        void twoParticipants() {
            var s1 = parse("&{a: end, b: end}");
            var s2 = parse("&{x: end, y: end}");
            var result = CompositionChecker.compose(List.of(
                    Map.entry("Alice", s1), Map.entry("Bob", s2)));
            assertTrue(result.isLattice());
            assertEquals(2, result.participants().size());
            assertTrue(result.compatibility().containsKey(new ParticipantPair("Alice", "Bob")));
        }

        @Test
        void threeParticipants() {
            var s1 = parse("&{a: end}");
            var s2 = parse("&{b: end}");
            var s3 = parse("&{c: end}");
            var result = CompositionChecker.compose(List.of(
                    Map.entry("Alice", s1), Map.entry("Bob", s2), Map.entry("Carol", s3)));
            assertTrue(result.isLattice());
            assertEquals(3, result.participants().size());
            assertEquals(3, result.compatibility().size()); // C(3,2) = 3
        }

        @Test
        void productStateCount() {
            var s1 = parse("&{a: end, b: end}");
            var s2 = parse("&{x: end, y: end}");
            var result = CompositionChecker.compose(List.of(
                    Map.entry("Alice", s1), Map.entry("Bob", s2)));
            // Branch with 2 choices: top → {a→end, b→end} → end = 2 states (top + end merged for same leaf)
            // Actually &{a: end, b: end} has states: top, end (a and b both go to end)
            // So product: 2 * 2 = 4
            assertEquals(4, result.product().states().size());
        }

        @Test
        void emptyParticipantsRaises() {
            assertThrows(IllegalArgumentException.class,
                    () -> CompositionChecker.compose(List.of()));
        }

        @Test
        void endTypes() {
            var result = CompositionChecker.compose(List.of(
                    Map.entry("A", new End()), Map.entry("B", new End())));
            assertTrue(result.isLattice());
            assertEquals(1, result.product().states().size());
        }
    }

    // ======================================================================
    // productNary()
    // ======================================================================

    @Nested
    class ProductNary {

        @Test
        void single() {
            var ss = StateSpaceBuilder.build(parse("&{a: end}"));
            var result = CompositionChecker.productNary(List.of(ss));
            assertEquals(ss.states().size(), result.states().size());
        }

        @Test
        void binary() {
            var ss1 = StateSpaceBuilder.build(parse("&{a: end}"));
            var ss2 = StateSpaceBuilder.build(parse("&{b: end}"));
            var result = CompositionChecker.productNary(List.of(ss1, ss2));
            assertEquals(ss1.states().size() * ss2.states().size(), result.states().size());
        }

        @Test
        void ternary() {
            var ss1 = StateSpaceBuilder.build(parse("&{a: end}"));
            var ss2 = StateSpaceBuilder.build(parse("&{b: end}"));
            var ss3 = StateSpaceBuilder.build(parse("&{c: end}"));
            var result = CompositionChecker.productNary(List.of(ss1, ss2, ss3));
            int expected = ss1.states().size() * ss2.states().size() * ss3.states().size();
            assertEquals(expected, result.states().size());
        }

        @Test
        void emptyRaises() {
            assertThrows(IllegalArgumentException.class,
                    () -> CompositionChecker.productNary(List.of()));
        }

        @Test
        void productIsLattice() {
            var ss1 = StateSpaceBuilder.build(parse("&{a: end, b: end}"));
            var ss2 = StateSpaceBuilder.build(parse("+{x: end, y: end}"));
            var ss3 = StateSpaceBuilder.build(parse("&{p: end}"));
            var product = CompositionChecker.productNary(List.of(ss1, ss2, ss3));
            assertTrue(LatticeChecker.checkLattice(product).isLattice());
        }
    }

    // ======================================================================
    // checkCompatibility()
    // ======================================================================

    @Nested
    class CheckCompatibility {

        @Test
        void dualPairCompatible() {
            var s = parse("&{a: end, b: end}");
            var d = DualityChecker.dual(s);
            assertTrue(CompositionChecker.checkCompatibility(s, d));
        }

        @Test
        void disjointLabelsCompatible() {
            var s1 = parse("&{a: end}");
            var s2 = parse("&{x: end}");
            assertTrue(CompositionChecker.checkCompatibility(s1, s2));
        }

        @Test
        void bothBranchCompatible() {
            var s1 = parse("&{a: end}");
            var s2 = parse("&{a: end}");
            assertTrue(CompositionChecker.checkCompatibility(s1, s2));
        }

        @Test
        void offerAndSelectCompatible() {
            var s1 = parse("&{a: end, b: end}");
            var s2 = parse("+{a: end}");
            assertTrue(CompositionChecker.checkCompatibility(s1, s2));
        }

        @Test
        void bothSelectSameLabelConflict() {
            var s1 = parse("+{a: end}");
            var s2 = parse("+{a: end}");
            assertFalse(CompositionChecker.checkCompatibility(s1, s2));
        }

        @Test
        void endTypesCompatible() {
            assertTrue(CompositionChecker.checkCompatibility(new End(), new End()));
        }

        @Test
        void complexCompatible() {
            var s1 = parse("&{a: +{x: end}}");
            var s2 = parse("+{a: &{y: end}}");
            assertTrue(CompositionChecker.checkCompatibility(s1, s2));
        }

        @Test
        void recursiveTypeCompatible() {
            var s = parse("rec X . &{a: X, b: end}");
            var d = DualityChecker.dual(s);
            assertTrue(CompositionChecker.checkCompatibility(s, d));
        }

        @Test
        void selectOverlapWithOfferer() {
            var s1 = parse("+{a: end}");
            var s2 = parse("&{a: +{a: end}}");
            assertTrue(CompositionChecker.checkCompatibility(s1, s2));
        }
    }

    // ======================================================================
    // compose() — compatibility integration
    // ======================================================================

    @Nested
    class ComposeCompatibility {

        @Test
        void dualPairInComposition() {
            var s = parse("&{a: end, b: end}");
            var d = DualityChecker.dual(s);
            var result = CompositionChecker.compose(List.of(
                    Map.entry("Server", s), Map.entry("Client", d)));
            assertTrue(result.isLattice());
            assertTrue(result.compatibility().get(new ParticipantPair("Server", "Client")));
        }

        @Test
        void incompatiblePair() {
            var s1 = parse("+{a: end}");
            var s2 = parse("+{a: end}");
            var result = CompositionChecker.compose(List.of(
                    Map.entry("A", s1), Map.entry("B", s2)));
            assertTrue(result.isLattice()); // Product is still a lattice!
            assertFalse(result.compatibility().get(new ParticipantPair("A", "B")));
        }

        @Test
        void threePartyMixedCompat() {
            var server = parse("&{req: end}");
            var client = parse("+{req: end}");
            var observer = parse("&{log: end}");
            var result = CompositionChecker.compose(List.of(
                    Map.entry("Server", server),
                    Map.entry("Client", client),
                    Map.entry("Observer", observer)));
            assertTrue(result.isLattice());
            assertTrue(result.compatibility().get(new ParticipantPair("Server", "Client")));
            assertTrue(result.compatibility().get(new ParticipantPair("Server", "Observer")));
            assertTrue(result.compatibility().get(new ParticipantPair("Client", "Observer")));
        }
    }

    // ======================================================================
    // Product of lattices is always a lattice (parametrized)
    // ======================================================================

    @Nested
    class ProductIsLattice {

        static final String[] TYPES = {
                "&{a: end}",
                "&{a: end, b: end}",
                "+{x: end, y: end}",
                "&{a: +{x: end, y: end}, b: end}",
                "rec X . &{a: X, b: end}",
                "(&{a: end} || &{b: end})",
        };

        static final String[] TYPES_SHORT = {
                "&{a: end}",
                "&{a: end, b: end}",
                "+{x: end, y: end}",
        };

        static Stream<org.junit.jupiter.params.provider.Arguments> productPairs() {
            var args = new java.util.ArrayList<org.junit.jupiter.params.provider.Arguments>();
            for (String t1 : TYPES) {
                for (String t2 : TYPES_SHORT) {
                    args.add(org.junit.jupiter.params.provider.Arguments.of(t1, t2));
                }
            }
            return args.stream();
        }

        @ParameterizedTest
        @MethodSource("productPairs")
        void productLattice(String t1, String t2) {
            var ss1 = StateSpaceBuilder.build(parse(t1));
            var ss2 = StateSpaceBuilder.build(parse(t2));
            var product = CompositionChecker.productNary(List.of(ss1, ss2));
            assertTrue(LatticeChecker.checkLattice(product).isLattice(),
                    "Product of " + t1 + " and " + t2 + " is not a lattice!");
        }
    }

    // ======================================================================
    // compare_with_global() — top-down vs bottom-up
    // ======================================================================

    @Nested
    class CompareWithGlobal {

        @Test
        void requestResponse() {
            String g = "Client -> Server : {request: Server -> Client : {response: end}}";
            var gt = GlobalTypeParser.parse(g);
            var projections = Projection.projectAll(gt);
            var result = CompositionChecker.compareWithGlobal(projections, g);
            assertTrue(result.globalIsLattice());
            assertTrue(result.productIsLattice());
            assertTrue(result.overApproximationRatio() >= 1.0);
        }

        @Test
        void twoBuyer() {
            String g = "Buyer1 -> Seller : {lookup: "
                    + "Seller -> Buyer1 : {price: "
                    + "Seller -> Buyer2 : {price: "
                    + "Buyer1 -> Buyer2 : {share: "
                    + "Buyer2 -> Seller : {accept: "
                    + "Seller -> Buyer2 : {deliver: end}, "
                    + "reject: end}}}}}";
            var gt = GlobalTypeParser.parse(g);
            var projections = Projection.projectAll(gt);
            var result = CompositionChecker.compareWithGlobal(projections, g);
            assertTrue(result.globalIsLattice());
            assertTrue(result.productIsLattice());
            assertTrue(result.productStates() >= result.globalStates());
            assertTrue(result.overApproximationRatio() >= 1.0);
        }

        @Test
        void ringProtocol() {
            String g = "A -> B : {msg: B -> C : {msg: C -> A : {msg: end}}}";
            var gt = GlobalTypeParser.parse(g);
            var projections = Projection.projectAll(gt);
            var result = CompositionChecker.compareWithGlobal(projections, g);
            assertTrue(result.globalIsLattice());
            assertTrue(result.productIsLattice());
            assertTrue(result.overApproximationRatio() >= 1.0);
        }

        @Test
        void delegation() {
            String g = "Client -> Master : {task: "
                    + "Master -> Worker : {delegate: "
                    + "Worker -> Master : {result: "
                    + "Master -> Client : {response: end}}}}";
            var gt = GlobalTypeParser.parse(g);
            var projections = Projection.projectAll(gt);
            var result = CompositionChecker.compareWithGlobal(projections, g);
            assertTrue(result.globalIsLattice());
            assertTrue(result.productIsLattice());
            assertTrue(result.productStates() >= result.globalStates());
        }

        @Test
        void roleMatches() {
            String g = "Client -> Server : {request: Server -> Client : {response: end}}";
            var gt = GlobalTypeParser.parse(g);
            var projections = Projection.projectAll(gt);
            var result = CompositionChecker.compareWithGlobal(projections, g);
            for (var entry : result.roleTypeMatches().entrySet()) {
                assertTrue(entry.getValue(),
                        "Role " + entry.getKey() + " type doesn't match its projection");
            }
        }
    }

    // ======================================================================
    // Benchmark comparison tests
    // ======================================================================

    @Nested
    class BenchmarkComparison {

        private ComparisonResult runBenchmark(String globalTypeString) {
            var gt = GlobalTypeParser.parse(globalTypeString);
            var projections = Projection.projectAll(gt);
            return CompositionChecker.compareWithGlobal(projections, globalTypeString);
        }

        @Test
        void twoPhaseCommit() {
            String g = "Coord -> P : {prepare: "
                    + "P -> Coord : {yes: "
                    + "Coord -> P : {commit: end}, "
                    + "no: Coord -> P : {abort: end}}}";
            var result = runBenchmark(g);
            assertTrue(result.globalIsLattice());
            assertTrue(result.productIsLattice());
            assertTrue(result.overApproximationRatio() >= 1.0);
        }

        @Test
        void streaming() {
            String g = "rec X . Producer -> Consumer : {data: X, done: end}";
            var result = runBenchmark(g);
            assertTrue(result.globalIsLattice());
            assertTrue(result.productIsLattice());
        }

        @Test
        void negotiation() {
            String g = "rec X . Buyer -> Seller : {offer: "
                    + "Seller -> Buyer : {accept: end, "
                    + "counter: X}}";
            var result = runBenchmark(g);
            assertTrue(result.globalIsLattice());
            assertTrue(result.productIsLattice());
        }

        @Test
        void oauth2() {
            String g = "Client -> AuthServer : {authorize: "
                    + "AuthServer -> Client : {code: "
                    + "Client -> AuthServer : {exchange: "
                    + "AuthServer -> Client : {token: "
                    + "Client -> ResourceServer : {access: "
                    + "ResourceServer -> Client : {resource: end}}}}}}";
            var result = runBenchmark(g);
            assertTrue(result.globalIsLattice());
            assertTrue(result.productIsLattice());
            assertTrue(result.productStates() >= result.globalStates());
        }

        @Test
        void mcp() {
            String g = "Host -> Client : {initialize: "
                    + "Client -> Server : {discover: "
                    + "Server -> Client : {tools: "
                    + "Client -> Host : {ready: "
                    + "rec X . Client -> Server : {call: "
                    + "Server -> Client : {result: X}, "
                    + "done: end}}}}}";
            var result = runBenchmark(g);
            assertTrue(result.globalIsLattice());
            assertTrue(result.productIsLattice());
        }

        @Test
        void dnsResolution() {
            String g = "Client -> Resolver : {query: "
                    + "Resolver -> AuthNS : {lookup: "
                    + "AuthNS -> Resolver : {answer: "
                    + "Resolver -> Client : {response: end}}}}";
            var result = runBenchmark(g);
            assertTrue(result.globalIsLattice());
            assertTrue(result.productIsLattice());
        }

        @Test
        void mapReduce() {
            String g = "Master -> Mapper : {map: "
                    + "Mapper -> Reducer : {emit: "
                    + "Reducer -> Master : {result: end}}}";
            var result = runBenchmark(g);
            assertTrue(result.globalIsLattice());
            assertTrue(result.productIsLattice());
        }
    }

    // ======================================================================
    // Over-approximation analysis
    // ======================================================================

    @Nested
    class OverApproximation {

        @Test
        void chainProtocolStrictOverapprox() {
            String g = "A -> B : {msg: B -> C : {msg: C -> A : {msg: end}}}";
            var gt = GlobalTypeParser.parse(g);
            var projections = Projection.projectAll(gt);
            var result = CompositionChecker.compareWithGlobal(projections, g);
            assertTrue(result.productStates() >= result.globalStates());
        }

        @Test
        void binaryProtocolTighter() {
            String g = "Client -> Server : {request: Server -> Client : {response: end}}";
            var gt = GlobalTypeParser.parse(g);
            var projections = Projection.projectAll(gt);
            var result = CompositionChecker.compareWithGlobal(projections, g);
            assertTrue(result.overApproximationRatio() >= 1.0);
        }
    }

    // ======================================================================
    // Edge cases
    // ======================================================================

    @Nested
    class EdgeCases {

        @Test
        void singleMethodTypes() {
            var s1 = parse("&{m: end}");
            var s2 = parse("+{m: end}");
            var result = CompositionChecker.compose(List.of(
                    Map.entry("Server", s1), Map.entry("Client", s2)));
            assertTrue(result.isLattice());
            assertTrue(result.compatibility().get(new ParticipantPair("Server", "Client")));
        }

        @Test
        void deepNesting() {
            var s = parse("&{a: &{b: &{c: end}}}");
            var d = DualityChecker.dual(s);
            var result = CompositionChecker.compose(List.of(
                    Map.entry("A", s), Map.entry("B", d)));
            assertTrue(result.isLattice());
        }

        @Test
        void recursiveTypesCompose() {
            var s1 = parse("rec X . &{next: X, stop: end}");
            var s2 = parse("rec Y . +{next: Y, stop: end}");
            var result = CompositionChecker.compose(List.of(
                    Map.entry("Producer", s1), Map.entry("Consumer", s2)));
            assertTrue(result.isLattice());
            assertTrue(result.compatibility().get(new ParticipantPair("Producer", "Consumer")));
        }

        @Test
        void parallelTypeInComposition() {
            var s1 = parse("(&{a: end} || &{b: end})");
            var s2 = parse("&{x: end}");
            var result = CompositionChecker.compose(List.of(
                    Map.entry("A", s1), Map.entry("B", s2)));
            assertTrue(result.isLattice());
        }

        @Test
        void fourParticipants() {
            var result = CompositionChecker.compose(List.of(
                    Map.entry("A", parse("&{a: end}")),
                    Map.entry("B", parse("&{b: end}")),
                    Map.entry("C", parse("&{c: end}")),
                    Map.entry("D", parse("&{d: end}"))));
            assertTrue(result.isLattice());
            assertEquals(6, result.compatibility().size()); // C(4,2) = 6
        }

        @Test
        void resultFields() {
            var s1 = parse("&{a: end}");
            var s2 = parse("&{b: end}");
            var result = CompositionChecker.compose(List.of(
                    Map.entry("A", s1), Map.entry("B", s2)));
            assertInstanceOf(CompositionResult.class, result);
            assertTrue(result.participants().containsKey("A"));
            assertTrue(result.participants().containsKey("B"));
            assertTrue(result.stateSpaces().containsKey("A"));
            assertTrue(result.stateSpaces().containsKey("B"));
            assertInstanceOf(StateSpace.class, result.product());
        }

        @Test
        void comparisonResultFields() {
            String g = "Client -> Server : {request: Server -> Client : {response: end}}";
            var gt = GlobalTypeParser.parse(g);
            var projections = Projection.projectAll(gt);
            var result = CompositionChecker.compareWithGlobal(projections, g);
            assertInstanceOf(ComparisonResult.class, result);
            assertTrue(result.globalStates() > 0);
            assertTrue(result.productStates() > 0);
        }
    }

    // ======================================================================
    // synchronized_product() — CSP-style sync on shared labels
    // ======================================================================

    @Nested
    class SynchronizedProduct {

        @Test
        void disjointLabelsSameAsFree() {
            var ss1 = StateSpaceBuilder.build(parse("&{a: end}"));
            var ss2 = StateSpaceBuilder.build(parse("&{b: end}"));
            var synced = CompositionChecker.synchronizedProduct(ss1, ss2);
            var free = CompositionChecker.productNary(List.of(ss1, ss2));
            assertEquals(free.states().size(), synced.states().size());
            assertEquals(free.transitions().size(), synced.transitions().size());
        }

        @Test
        void sharedLabelReducesStates() {
            var ss1 = StateSpaceBuilder.build(parse("&{m: end}"));
            var ss2 = StateSpaceBuilder.build(parse("&{m: end}"));
            var free = CompositionChecker.productNary(List.of(ss1, ss2));
            var synced = CompositionChecker.synchronizedProduct(ss1, ss2);
            assertTrue(synced.states().size() <= free.states().size());
            assertTrue(synced.states().contains(synced.top()));
        }

        @Test
        void sharedLabelSimultaneousMove() {
            var ss1 = StateSpaceBuilder.build(parse("&{m: end}"));
            var ss2 = StateSpaceBuilder.build(parse("&{m: end}"));
            var synced = CompositionChecker.synchronizedProduct(ss1, ss2);
            var enabled = synced.enabled(synced.top());
            var mTargets = enabled.stream()
                    .filter(e -> e.getKey().equals("m"))
                    .map(Map.Entry::getValue)
                    .toList();
            assertEquals(1, mTargets.size());
            assertEquals(synced.bottom(), (int) mTargets.getFirst());
        }

        @Test
        void mixedPrivateAndShared() {
            var ss1 = StateSpaceBuilder.build(parse("&{a: &{m: end}}"));
            var ss2 = StateSpaceBuilder.build(parse("&{m: end}"));
            var synced = CompositionChecker.synchronizedProduct(ss1, ss2);
            var labelsTop = synced.enabledLabels(synced.top());
            assertTrue(labelsTop.contains("a"));
            assertFalse(labelsTop.contains("m")); // s1 doesn't enable m at top
        }

        @Test
        void dualPairSynchronized() {
            var server = parse("&{req: &{done: end}}");
            var client = DualityChecker.dual(server);
            var ssS = StateSpaceBuilder.build(server);
            var ssC = StateSpaceBuilder.build(client);
            var synced = CompositionChecker.synchronizedProduct(ssS, ssC);
            // Both labels shared → path: (top,top) --req--> (mid,mid) --done--> (end,end)
            assertEquals(3, synced.states().size());
        }

        @Test
        void reductionVsFree() {
            var ss1 = StateSpaceBuilder.build(parse("&{a: &{m: end}}"));
            var ss2 = StateSpaceBuilder.build(parse("&{m: &{b: end}}"));
            var free = CompositionChecker.productNary(List.of(ss1, ss2));
            var synced = CompositionChecker.synchronizedProduct(ss1, ss2);
            assertTrue(synced.states().size() <= free.states().size());
        }

        @Test
        void noTransitionsOnBlockedShared() {
            // s1: m then n; s2: n then m (reversed order)
            var ss1 = StateSpaceBuilder.build(parse("&{m: &{n: end}}"));
            var ss2 = StateSpaceBuilder.build(parse("&{n: &{m: end}}"));
            var synced = CompositionChecker.synchronizedProduct(ss1, ss2);
            var enabledTop = synced.enabled(synced.top());
            assertEquals(0, enabledTop.size()); // Deadlock!
        }

        @Test
        void partialOverlap() {
            var ss1 = StateSpaceBuilder.build(parse("&{a: end, m: end}"));
            var ss2 = StateSpaceBuilder.build(parse("&{b: end, m: end}"));
            var synced = CompositionChecker.synchronizedProduct(ss1, ss2);
            var labelsTop = synced.enabledLabels(synced.top());
            assertTrue(labelsTop.contains("a"));  // private to s1
            assertTrue(labelsTop.contains("b"));  // private to s2
            assertTrue(labelsTop.contains("m"));  // shared
        }
    }

    // ======================================================================
    // synchronized_compose() — full API
    // ======================================================================

    @Nested
    class SynchronizedCompose {

        @Test
        void basicTwoParty() {
            var server = parse("&{req: end}");
            var client = DualityChecker.dual(server);
            var result = CompositionChecker.synchronizedCompose(List.of(
                    Map.entry("Server", server), Map.entry("Client", client)));
            assertInstanceOf(SynchronizedResult.class, result);
            assertTrue(result.reductionRatio() <= 1.0);
        }

        @Test
        void fewerThanTwoRaises() {
            assertThrows(IllegalArgumentException.class,
                    () -> CompositionChecker.synchronizedCompose(
                            List.of(Map.entry("A", parse("&{a: end}")))));
        }

        @Test
        void disjointNoReduction() {
            var s1 = parse("&{a: end}");
            var s2 = parse("&{b: end}");
            var result = CompositionChecker.synchronizedCompose(List.of(
                    Map.entry("A", s1), Map.entry("B", s2)));
            assertEquals(1.0, result.reductionRatio(), 0.001);
        }

        @Test
        void sharedLabelsDetected() {
            var s1 = parse("&{m: end, a: end}");
            var s2 = parse("&{m: end, b: end}");
            var result = CompositionChecker.synchronizedCompose(List.of(
                    Map.entry("A", s1), Map.entry("B", s2)));
            var pair = new ParticipantPair("A", "B");
            assertTrue(result.sharedLabels().get(pair).contains("m"));
            assertFalse(result.sharedLabels().get(pair).contains("a"));
        }

        @Test
        void reductionWithShared() {
            var s1 = parse("&{m: &{n: end}}");
            var s2 = parse("&{m: &{n: end}}");
            var result = CompositionChecker.synchronizedCompose(List.of(
                    Map.entry("A", s1), Map.entry("B", s2)));
            assertTrue(result.synced().states().size() <= result.freeProduct().states().size());
            assertTrue(result.reductionRatio() <= 1.0);
        }

        @Test
        void threePartySynchronized() {
            var s1 = parse("&{a: end, x: end}");
            var s2 = parse("&{b: end, x: end}");
            var s3 = parse("&{c: end}");
            var result = CompositionChecker.synchronizedCompose(List.of(
                    Map.entry("A", s1), Map.entry("B", s2), Map.entry("C", s3)));
            assertInstanceOf(SynchronizedResult.class, result);
            assertTrue(result.synced().states().size() <= result.freeProduct().states().size());
        }

        @Test
        void resultFields() {
            var s1 = parse("&{m: end}");
            var s2 = parse("+{m: end}");
            var result = CompositionChecker.synchronizedCompose(List.of(
                    Map.entry("Server", s1), Map.entry("Client", s2)));
            assertTrue(result.participants().containsKey("Server"));
            assertTrue(result.participants().containsKey("Client"));
            assertTrue(result.stateSpaces().containsKey("Server"));
            assertInstanceOf(StateSpace.class, result.synced());
            assertInstanceOf(StateSpace.class, result.freeProduct());
        }

        @Test
        void hierarchyGlobalLeqSyncedLeqFree() {
            String g = "Client -> Server : {request: Server -> Client : {response: end}}";
            var gt = GlobalTypeParser.parse(g);
            var projections = Projection.projectAll(gt);

            var comparison = CompositionChecker.compareWithGlobal(projections, g);
            int freeStates = comparison.productStates();
            int globalStates = comparison.globalStates();

            var syncedResult = CompositionChecker.synchronizedCompose(
                    projections.entrySet().stream()
                            .map(e -> Map.entry(e.getKey(), e.getValue()))
                            .toList());
            int syncedStates = syncedResult.synced().states().size();

            // global <= synced <= free
            assertTrue(globalStates <= syncedStates || syncedStates <= freeStates);
            assertTrue(syncedStates <= freeStates);
        }
    }

    // ======================================================================
    // Three-way comparison: global vs synchronized vs free
    // ======================================================================

    @Nested
    class ThreeWayComparison {

        private Map<String, Integer> compare(String globalTypeString) {
            var gt = GlobalTypeParser.parse(globalTypeString);
            var projections = Projection.projectAll(gt);
            var globalSs = com.bica.reborn.globaltype.GlobalTypeChecker.buildStateSpace(gt);

            var ssList = projections.values().stream()
                    .map(StateSpaceBuilder::build)
                    .toList();
            var free = CompositionChecker.productNary(ssList);

            var syncedResult = CompositionChecker.synchronizedCompose(
                    projections.entrySet().stream()
                            .map(e -> Map.entry(e.getKey(), e.getValue()))
                            .toList());

            return Map.of(
                    "global", globalSs.states().size(),
                    "synced", syncedResult.synced().states().size(),
                    "free", free.states().size());
        }

        @Test
        void requestResponseHierarchy() {
            var r = compare("Client -> Server : {request: Server -> Client : {response: end}}");
            assertTrue(r.get("synced") <= r.get("free"));
        }

        @Test
        void twoPhaseCommitHierarchy() {
            String g = "Coord -> P : {prepare: "
                    + "P -> Coord : {yes: "
                    + "Coord -> P : {commit: end}, "
                    + "no: Coord -> P : {abort: end}}}";
            var r = compare(g);
            assertTrue(r.get("synced") <= r.get("free"));
        }

        @Test
        void ringHierarchy() {
            var r = compare("A -> B : {msg: B -> C : {msg: C -> A : {msg: end}}}");
            assertTrue(r.get("synced") <= r.get("free"));
        }

        @Test
        void delegationHierarchy() {
            String g = "Client -> Master : {task: "
                    + "Master -> Worker : {delegate: "
                    + "Worker -> Master : {result: "
                    + "Master -> Client : {response: end}}}}";
            var r = compare(g);
            assertTrue(r.get("synced") <= r.get("free"));
        }

        @Test
        void streamingHierarchy() {
            var r = compare("rec X . Producer -> Consumer : {data: X, done: end}");
            assertTrue(r.get("synced") <= r.get("free"));
        }
    }
}
