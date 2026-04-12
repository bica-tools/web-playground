package com.bica.reborn.globaltype;

import com.bica.reborn.ast.*;
import com.bica.reborn.parser.PrettyPrinter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MPST projection: global type → local session type.
 */
class ProjectionTest {

    // -----------------------------------------------------------------------
    // Basic projection
    // -----------------------------------------------------------------------

    @Nested
    class BasicProjection {

        @Test
        void endProjectsToEnd() {
            assertEquals(new End(), Projection.project(new GEnd(), "A"));
        }

        @Test
        void varProjectsToVar() {
            assertEquals(new Var("X"), Projection.project(new GVar("X"), "A"));
        }

        @Test
        void senderGetsSelect() {
            GlobalType g = GlobalTypeParser.parse("A -> B : {m: end}");
            SessionType local = Projection.project(g, "A");
            assertInstanceOf(Select.class, local);
            Select sel = (Select) local;
            assertEquals(1, sel.choices().size());
            assertEquals("m", sel.choices().getFirst().label());
        }

        @Test
        void receiverGetsBranch() {
            GlobalType g = GlobalTypeParser.parse("A -> B : {m: end}");
            SessionType local = Projection.project(g, "B");
            assertInstanceOf(Branch.class, local);
            Branch br = (Branch) local;
            assertEquals(1, br.choices().size());
            assertEquals("m", br.choices().getFirst().label());
        }

        @Test
        void uninvolvedGetsMergedEnd() {
            GlobalType g = GlobalTypeParser.parse("A -> B : {m: end}");
            SessionType local = Projection.project(g, "C");
            assertEquals(new End(), local);
        }

        @Test
        void senderMultipleChoices() {
            GlobalType g = GlobalTypeParser.parse(
                    "A -> B : {m1: end, m2: end}");
            SessionType local = Projection.project(g, "A");
            Select sel = (Select) local;
            assertEquals(2, sel.choices().size());
            assertEquals("m1", sel.choices().get(0).label());
            assertEquals("m2", sel.choices().get(1).label());
        }

        @Test
        void receiverMultipleChoices() {
            GlobalType g = GlobalTypeParser.parse(
                    "A -> B : {m1: end, m2: end}");
            SessionType local = Projection.project(g, "B");
            Branch br = (Branch) local;
            assertEquals(2, br.choices().size());
        }
    }

    // -----------------------------------------------------------------------
    // Sequential / nested messages
    // -----------------------------------------------------------------------

    @Nested
    class SequentialProjection {

        @Test
        void requestResponseClient() {
            GlobalType g = GlobalTypeParser.parse(
                    "Client -> Server : {request: "
                    + "Server -> Client : {response: end}}");
            SessionType local = Projection.project(g, "Client");
            // Client: +{request: &{response: end}}
            assertInstanceOf(Select.class, local);
            Select sel = (Select) local;
            assertInstanceOf(Branch.class, sel.choices().getFirst().body());
        }

        @Test
        void requestResponseServer() {
            GlobalType g = GlobalTypeParser.parse(
                    "Client -> Server : {request: "
                    + "Server -> Client : {response: end}}");
            SessionType local = Projection.project(g, "Server");
            // Server: &{request: +{response: end}}
            assertInstanceOf(Branch.class, local);
            Branch br = (Branch) local;
            assertInstanceOf(Select.class, br.choices().getFirst().body());
        }

        @Test
        void threePartyChainMiddleRole() {
            // A -> B : {m: B -> C : {n: end}}
            GlobalType g = GlobalTypeParser.parse(
                    "A -> B : {m: B -> C : {n: end}}");
            SessionType localB = Projection.project(g, "B");
            // B: &{m: +{n: end}}
            assertInstanceOf(Branch.class, localB);
            Branch br = (Branch) localB;
            assertInstanceOf(Select.class, br.choices().getFirst().body());
        }

        @Test
        void threePartyChainEndRole() {
            GlobalType g = GlobalTypeParser.parse(
                    "A -> B : {m: B -> C : {n: end}}");
            SessionType localC = Projection.project(g, "C");
            // C: &{n: end}
            assertInstanceOf(Branch.class, localC);
        }
    }

    // -----------------------------------------------------------------------
    // Recursive types
    // -----------------------------------------------------------------------

    @Nested
    class RecursiveProjection {

        @Test
        void streamingProducer() {
            GlobalType g = GlobalTypeParser.parse(
                    "rec X . Producer -> Consumer : {data: X, done: end}");
            SessionType local = Projection.project(g, "Producer");
            // rec X . +{data: X, done: end}
            assertInstanceOf(Rec.class, local);
            Rec rec = (Rec) local;
            assertInstanceOf(Select.class, rec.body());
        }

        @Test
        void streamingConsumer() {
            GlobalType g = GlobalTypeParser.parse(
                    "rec X . Producer -> Consumer : {data: X, done: end}");
            SessionType local = Projection.project(g, "Consumer");
            // rec X . &{data: X, done: end}
            assertInstanceOf(Rec.class, local);
            Rec rec = (Rec) local;
            assertInstanceOf(Branch.class, rec.body());
        }

        @Test
        void uninvolvedInRecursionGetsEnd() {
            GlobalType g = GlobalTypeParser.parse(
                    "rec X . A -> B : {m: X, done: end}");
            SessionType local = Projection.project(g, "C");
            assertEquals(new End(), local);
        }

        @Test
        void negotiationBuyer() {
            GlobalType g = GlobalTypeParser.parse(
                    "rec X . Buyer -> Seller : {offer: "
                    + "Seller -> Buyer : {accept: end, counter: X}}");
            SessionType local = Projection.project(g, "Buyer");
            assertInstanceOf(Rec.class, local);
            Rec rec = (Rec) local;
            assertInstanceOf(Select.class, rec.body());
            Select sel = (Select) rec.body();
            assertEquals(1, sel.choices().size());
            // offer: &{accept: end, counter: X}
            assertInstanceOf(Branch.class, sel.choices().getFirst().body());
        }

        @Test
        void negotiationSeller() {
            GlobalType g = GlobalTypeParser.parse(
                    "rec X . Buyer -> Seller : {offer: "
                    + "Seller -> Buyer : {accept: end, counter: X}}");
            SessionType local = Projection.project(g, "Seller");
            assertInstanceOf(Rec.class, local);
            Rec rec = (Rec) local;
            assertInstanceOf(Branch.class, rec.body());
            Branch br = (Branch) rec.body();
            // offer: +{accept: end, counter: X}
            assertInstanceOf(Select.class, br.choices().getFirst().body());
        }
    }

    // -----------------------------------------------------------------------
    // Parallel
    // -----------------------------------------------------------------------

    @Nested
    class ParallelProjection {

        @Test
        void roleInBothBranchesGetsParallel() {
            // (A -> B : {m: end} || B -> C : {n: end})
            // B is in both branches
            GlobalType g = GlobalTypeParser.parse(
                    "(A -> B : {m: end} || B -> C : {n: end})");
            SessionType localB = Projection.project(g, "B");
            assertInstanceOf(Parallel.class, localB);
        }

        @Test
        void roleInOneBranchOnly() {
            GlobalType g = GlobalTypeParser.parse(
                    "(A -> B : {m: end} || C -> D : {n: end})");
            SessionType localA = Projection.project(g, "A");
            // A only in left branch: +{m: end}
            assertInstanceOf(Select.class, localA);
        }

        @Test
        void roleInNeitherBranchGetsEnd() {
            GlobalType g = GlobalTypeParser.parse(
                    "(A -> B : {m: end} || C -> D : {n: end})");
            SessionType local = Projection.project(g, "E");
            assertEquals(new End(), local);
        }

        @Test
        void roleInRightBranchOnly() {
            GlobalType g = GlobalTypeParser.parse(
                    "(A -> B : {m: end} || C -> D : {n: end})");
            SessionType localD = Projection.project(g, "D");
            assertInstanceOf(Branch.class, localD);
        }
    }

    // -----------------------------------------------------------------------
    // Merge failures
    // -----------------------------------------------------------------------

    @Nested
    class MergeFailures {

        @Test
        void mergeFailureThrowsProjectionError() {
            // A -> B : {m1: C -> D : {x: end}, m2: C -> D : {y: end}}
            // Projecting onto C: merge of +{x: end} and +{y: end} should fail
            GlobalType g = new GMessage("A", "B", List.of(
                    new GMessage.Choice("m1",
                            new GMessage("C", "D", List.of(
                                    new GMessage.Choice("x", new GEnd())))),
                    new GMessage.Choice("m2",
                            new GMessage("C", "D", List.of(
                                    new GMessage.Choice("y", new GEnd()))))));
            assertThrows(ProjectionError.class,
                    () -> Projection.project(g, "C"));
        }

        @Test
        void mergeSucceedsWhenEqual() {
            // A -> B : {m1: C -> D : {x: end}, m2: C -> D : {x: end}}
            // Projecting onto C: both branches project to +{x: end}
            GlobalType g = new GMessage("A", "B", List.of(
                    new GMessage.Choice("m1",
                            new GMessage("C", "D", List.of(
                                    new GMessage.Choice("x", new GEnd())))),
                    new GMessage.Choice("m2",
                            new GMessage("C", "D", List.of(
                                    new GMessage.Choice("x", new GEnd()))))));
            SessionType local = Projection.project(g, "C");
            assertInstanceOf(Select.class, local);
        }

        @Test
        void checkProjectionReturnsFalseOnFailure() {
            GlobalType g = new GMessage("A", "B", List.of(
                    new GMessage.Choice("m1",
                            new GMessage("C", "D", List.of(
                                    new GMessage.Choice("x", new GEnd())))),
                    new GMessage.Choice("m2",
                            new GMessage("C", "D", List.of(
                                    new GMessage.Choice("y", new GEnd()))))));
            ProjectionResult result = Projection.checkProjection(g, "C");
            assertFalse(result.isWellDefined());
        }

        @Test
        void checkProjectionReturnsTrueOnSuccess() {
            GlobalType g = GlobalTypeParser.parse("A -> B : {m: end}");
            ProjectionResult result = Projection.checkProjection(g, "A");
            assertTrue(result.isWellDefined());
            assertEquals("+{m: end}", result.localTypeStr());
        }
    }

    // -----------------------------------------------------------------------
    // projectAll
    // -----------------------------------------------------------------------

    @Nested
    class ProjectAll {

        @Test
        void projectAllReturnsAllRoles() {
            GlobalType g = GlobalTypeParser.parse(
                    "A -> B : {m: B -> C : {n: end}}");
            Map<String, SessionType> all = Projection.projectAll(g);
            assertEquals(Set.of("A", "B", "C"), all.keySet());
        }

        @Test
        void projectAllRequestResponse() {
            GlobalType g = GlobalTypeParser.parse(
                    "Client -> Server : {request: "
                    + "Server -> Client : {response: end}}");
            Map<String, SessionType> all = Projection.projectAll(g);
            assertEquals(2, all.size());
            assertInstanceOf(Select.class, all.get("Client"));
            assertInstanceOf(Branch.class, all.get("Server"));
        }
    }

    // -----------------------------------------------------------------------
    // Benchmark protocols
    // -----------------------------------------------------------------------

    @Nested
    class BenchmarkProjections {

        @Test
        void twoBuyerAllRolesProject() {
            GlobalType g = GlobalTypeParser.parse(
                    "Buyer1 -> Seller : {lookup: "
                    + "Seller -> Buyer1 : {price: "
                    + "Seller -> Buyer2 : {price: "
                    + "Buyer1 -> Buyer2 : {share: "
                    + "Buyer2 -> Seller : {accept: "
                    + "Seller -> Buyer2 : {deliver: end}, "
                    + "reject: end}}}}}");
            Map<String, SessionType> all = Projection.projectAll(g);
            assertEquals(3, all.size());
            assertNotNull(all.get("Buyer1"));
            assertNotNull(all.get("Buyer2"));
            assertNotNull(all.get("Seller"));
        }

        @Test
        void twoBuyerBuyer1Projection() {
            GlobalType g = GlobalTypeParser.parse(
                    "Buyer1 -> Seller : {lookup: "
                    + "Seller -> Buyer1 : {price: "
                    + "Seller -> Buyer2 : {price: "
                    + "Buyer1 -> Buyer2 : {share: "
                    + "Buyer2 -> Seller : {accept: "
                    + "Seller -> Buyer2 : {deliver: end}, "
                    + "reject: end}}}}}");
            SessionType local = Projection.project(g, "Buyer1");
            // Buyer1: +{lookup: &{price: +{share: end}}}
            assertInstanceOf(Select.class, local);
        }

        @Test
        void twoPhaseCommitAllProject() {
            GlobalType g = GlobalTypeParser.parse(
                    "Coord -> P : {prepare: "
                    + "P -> Coord : {yes: Coord -> P : {commit: end}, "
                    + "no: Coord -> P : {abort: end}}}");
            Map<String, SessionType> all = Projection.projectAll(g);
            assertEquals(2, all.size());
        }

        @Test
        void delegationAllProject() {
            GlobalType g = GlobalTypeParser.parse(
                    "Client -> Master : {task: "
                    + "Master -> Worker : {delegate: "
                    + "Worker -> Master : {result: "
                    + "Master -> Client : {response: end}}}}");
            Map<String, SessionType> all = Projection.projectAll(g);
            assertEquals(3, all.size());
        }

        @Test
        void ringAllProject() {
            GlobalType g = GlobalTypeParser.parse(
                    "A -> B : {msg: B -> C : {msg: C -> A : {msg: end}}}");
            Map<String, SessionType> all = Projection.projectAll(g);
            assertEquals(3, all.size());
        }

        @Test
        void authServiceAllProject() {
            GlobalType g = GlobalTypeParser.parse(
                    "Client -> Server : {login: "
                    + "Server -> Client : {granted: "
                    + "rec X . Client -> Server : {request: "
                    + "Server -> Client : {response: X}, "
                    + "logout: end}, "
                    + "denied: end}}");
            Map<String, SessionType> all = Projection.projectAll(g);
            assertEquals(2, all.size());
            // Both should have Rec
            assertInstanceOf(Select.class, all.get("Client"));
            assertInstanceOf(Branch.class, all.get("Server"));
        }
    }

    // -----------------------------------------------------------------------
    // Morphism verification
    // -----------------------------------------------------------------------

    @Nested
    class MorphismVerification {

        @Test
        void requestResponseMorphism() {
            GlobalType g = GlobalTypeParser.parse(
                    "Client -> Server : {request: "
                    + "Server -> Client : {response: end}}");
            ProjectionMorphismResult r = Projection.verifyProjectionMorphism(g, "Client");
            assertTrue(r.globalIsLattice());
            assertTrue(r.localIsLattice());
            assertTrue(r.isSurjective());
            assertTrue(r.isOrderPreserving());
        }

        @Test
        void twoPhaseCommitMorphismCoord() {
            GlobalType g = GlobalTypeParser.parse(
                    "Coord -> P : {prepare: "
                    + "P -> Coord : {yes: Coord -> P : {commit: end}, "
                    + "no: Coord -> P : {abort: end}}}");
            ProjectionMorphismResult r = Projection.verifyProjectionMorphism(g, "Coord");
            assertTrue(r.globalIsLattice());
            assertTrue(r.localIsLattice());
            assertTrue(r.isOrderPreserving());
        }

        @Test
        void streamingMorphism() {
            GlobalType g = GlobalTypeParser.parse(
                    "rec X . Producer -> Consumer : {data: X, done: end}");
            ProjectionMorphismResult r =
                    Projection.verifyProjectionMorphism(g, "Producer");
            assertTrue(r.globalIsLattice());
            assertTrue(r.localIsLattice());
            assertTrue(r.isSurjective());
            assertTrue(r.isOrderPreserving());
        }

        @Test
        void morphismGlobalHasMoreStates() {
            GlobalType g = GlobalTypeParser.parse(
                    "A -> B : {m: B -> C : {n: end}}");
            ProjectionMorphismResult r = Projection.verifyProjectionMorphism(g, "A");
            // Global has 3 states, A's local has 2 (select + end)
            assertTrue(r.globalStates() >= r.localStates());
        }
    }
}
