package com.bica.reborn.globaltype;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.ParseError;
import com.bica.reborn.statespace.StateSpace;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for multiparty global types: AST, parser, pretty-printer,
 * role extraction, state-space construction, and lattice properties.
 */
class GlobalTypeTest {

    // -----------------------------------------------------------------------
    // AST construction
    // -----------------------------------------------------------------------

    @Nested
    class AstTests {

        @Test
        void gEndIsRecord() {
            GEnd end = new GEnd();
            assertEquals(end, new GEnd());
        }

        @Test
        void gVarHoldsName() {
            GVar v = new GVar("X");
            assertEquals("X", v.name());
        }

        @Test
        void gVarRejectsNull() {
            assertThrows(NullPointerException.class, () -> new GVar(null));
        }

        @Test
        void gMessageWithSingleChoice() {
            var msg = new GMessage("A", "B",
                    List.of(new GMessage.Choice("m", new GEnd())));
            assertEquals("A", msg.sender());
            assertEquals("B", msg.receiver());
            assertEquals(1, msg.choices().size());
            assertEquals("m", msg.choices().getFirst().label());
        }

        @Test
        void gMessageWithMultipleChoices() {
            var msg = new GMessage("A", "B", List.of(
                    new GMessage.Choice("m1", new GEnd()),
                    new GMessage.Choice("m2", new GEnd())));
            assertEquals(2, msg.choices().size());
        }

        @Test
        void gMessageRejectsEmptyChoices() {
            assertThrows(IllegalArgumentException.class,
                    () -> new GMessage("A", "B", List.of()));
        }

        @Test
        void gMessageRejectsNullSender() {
            assertThrows(NullPointerException.class,
                    () -> new GMessage(null, "B",
                            List.of(new GMessage.Choice("m", new GEnd()))));
        }

        @Test
        void gMessageChoicesAreImmutable() {
            var choices = new java.util.ArrayList<>(
                    List.of(new GMessage.Choice("m", new GEnd())));
            var msg = new GMessage("A", "B", choices);
            assertThrows(UnsupportedOperationException.class,
                    () -> msg.choices().add(new GMessage.Choice("x", new GEnd())));
        }

        @Test
        void gParallelHoldsChildren() {
            var par = new GParallel(new GEnd(), new GVar("X"));
            assertInstanceOf(GEnd.class, par.left());
            assertInstanceOf(GVar.class, par.right());
        }

        @Test
        void gRecHoldsVarAndBody() {
            var rec = new GRec("X", new GVar("X"));
            assertEquals("X", rec.var());
            assertInstanceOf(GVar.class, rec.body());
        }

        @Test
        void gRecRejectsNullVar() {
            assertThrows(NullPointerException.class,
                    () -> new GRec(null, new GEnd()));
        }

        @Test
        void recordEquality() {
            assertEquals(
                    new GMessage("A", "B",
                            List.of(new GMessage.Choice("m", new GEnd()))),
                    new GMessage("A", "B",
                            List.of(new GMessage.Choice("m", new GEnd()))));
        }

        @Test
        void sealedInterfaceExhaustive() {
            GlobalType g = new GEnd();
            String result = switch (g) {
                case GEnd e -> "end";
                case GVar v -> "var";
                case GMessage m -> "msg";
                case GParallel p -> "par";
                case GRec r -> "rec";
            };
            assertEquals("end", result);
        }
    }

    // -----------------------------------------------------------------------
    // Role extraction
    // -----------------------------------------------------------------------

    @Nested
    class RoleTests {

        @Test
        void endHasNoRoles() {
            assertTrue(GlobalTypeChecker.roles(new GEnd()).isEmpty());
        }

        @Test
        void varHasNoRoles() {
            assertTrue(GlobalTypeChecker.roles(new GVar("X")).isEmpty());
        }

        @Test
        void messageHasSenderAndReceiver() {
            var g = new GMessage("A", "B",
                    List.of(new GMessage.Choice("m", new GEnd())));
            assertEquals(Set.of("A", "B"), GlobalTypeChecker.roles(g));
        }

        @Test
        void nestedMessagesCollectAllRoles() {
            var g = new GMessage("A", "B", List.of(
                    new GMessage.Choice("m",
                            new GMessage("C", "D", List.of(
                                    new GMessage.Choice("n", new GEnd()))))));
            assertEquals(Set.of("A", "B", "C", "D"), GlobalTypeChecker.roles(g));
        }

        @Test
        void parallelCollectsFromBothBranches() {
            var g = new GParallel(
                    new GMessage("A", "B",
                            List.of(new GMessage.Choice("m", new GEnd()))),
                    new GMessage("C", "D",
                            List.of(new GMessage.Choice("n", new GEnd()))));
            assertEquals(Set.of("A", "B", "C", "D"), GlobalTypeChecker.roles(g));
        }

        @Test
        void recCollectsFromBody() {
            var g = new GRec("X",
                    new GMessage("A", "B", List.of(
                            new GMessage.Choice("m", new GVar("X")))));
            assertEquals(Set.of("A", "B"), GlobalTypeChecker.roles(g));
        }

        @Test
        void twoBuyerRoles() {
            var g = GlobalTypeParser.parse(
                    "Buyer1 -> Seller : {lookup: "
                    + "Seller -> Buyer1 : {price: "
                    + "Seller -> Buyer2 : {price: "
                    + "Buyer1 -> Buyer2 : {share: "
                    + "Buyer2 -> Seller : {accept: "
                    + "Seller -> Buyer2 : {deliver: end}, "
                    + "reject: end}}}}}");
            assertEquals(Set.of("Buyer1", "Buyer2", "Seller"),
                    GlobalTypeChecker.roles(g));
        }
    }

    // -----------------------------------------------------------------------
    // Pretty-printer
    // -----------------------------------------------------------------------

    @Nested
    class PrettyPrinterTests {

        @Test
        void printEnd() {
            assertEquals("end", GlobalTypePrettyPrinter.pretty(new GEnd()));
        }

        @Test
        void printVar() {
            assertEquals("X", GlobalTypePrettyPrinter.pretty(new GVar("X")));
        }

        @Test
        void printMessage() {
            var g = new GMessage("A", "B",
                    List.of(new GMessage.Choice("m", new GEnd())));
            assertEquals("A -> B : {m: end}", GlobalTypePrettyPrinter.pretty(g));
        }

        @Test
        void printMultipleChoices() {
            var g = new GMessage("A", "B", List.of(
                    new GMessage.Choice("m1", new GEnd()),
                    new GMessage.Choice("m2", new GEnd())));
            assertEquals("A -> B : {m1: end, m2: end}",
                    GlobalTypePrettyPrinter.pretty(g));
        }

        @Test
        void printParallel() {
            var g = new GParallel(new GEnd(), new GEnd());
            assertEquals("(end || end)", GlobalTypePrettyPrinter.pretty(g));
        }

        @Test
        void printRec() {
            var g = new GRec("X", new GVar("X"));
            assertEquals("rec X . X", GlobalTypePrettyPrinter.pretty(g));
        }

        @Test
        void printNested() {
            var g = new GMessage("A", "B", List.of(
                    new GMessage.Choice("m",
                            new GMessage("B", "A", List.of(
                                    new GMessage.Choice("n", new GEnd()))))));
            assertEquals("A -> B : {m: B -> A : {n: end}}",
                    GlobalTypePrettyPrinter.pretty(g));
        }

        @Test
        void roundTripSimple() {
            String input = "A -> B : {m: end}";
            GlobalType parsed = GlobalTypeParser.parse(input);
            assertEquals(input, GlobalTypePrettyPrinter.pretty(parsed));
        }
    }

    // -----------------------------------------------------------------------
    // Parser (tokenizer + parse)
    // -----------------------------------------------------------------------

    @Nested
    class ParserTests {

        @Test
        void parseEnd() {
            assertEquals(new GEnd(), GlobalTypeParser.parse("end"));
        }

        @Test
        void parseVar() {
            assertEquals(new GVar("X"), GlobalTypeParser.parse("X"));
        }

        @Test
        void parseSingleMessage() {
            GlobalType g = GlobalTypeParser.parse("A -> B : {m: end}");
            assertInstanceOf(GMessage.class, g);
            GMessage msg = (GMessage) g;
            assertEquals("A", msg.sender());
            assertEquals("B", msg.receiver());
            assertEquals(1, msg.choices().size());
            assertEquals("m", msg.choices().getFirst().label());
        }

        @Test
        void parseMultipleChoices() {
            GlobalType g = GlobalTypeParser.parse(
                    "A -> B : {m1: end, m2: end}");
            GMessage msg = (GMessage) g;
            assertEquals(2, msg.choices().size());
        }

        @Test
        void parseRecursive() {
            GlobalType g = GlobalTypeParser.parse(
                    "rec X . A -> B : {data: X, done: end}");
            assertInstanceOf(GRec.class, g);
            GRec rec = (GRec) g;
            assertEquals("X", rec.var());
            assertInstanceOf(GMessage.class, rec.body());
        }

        @Test
        void parseParallel() {
            GlobalType g = GlobalTypeParser.parse(
                    "(A -> B : {m: end} || C -> D : {n: end})");
            assertInstanceOf(GParallel.class, g);
        }

        @Test
        void parseNestedMessages() {
            GlobalType g = GlobalTypeParser.parse(
                    "A -> B : {m: B -> C : {n: end}}");
            GMessage outer = (GMessage) g;
            GMessage inner = (GMessage) outer.choices().getFirst().body();
            assertEquals("B", inner.sender());
            assertEquals("C", inner.receiver());
        }

        @Test
        void parseTwoBuyer() {
            GlobalType g = GlobalTypeParser.parse(
                    "Buyer1 -> Seller : {lookup: "
                    + "Seller -> Buyer1 : {price: "
                    + "Seller -> Buyer2 : {price: "
                    + "Buyer1 -> Buyer2 : {share: "
                    + "Buyer2 -> Seller : {accept: "
                    + "Seller -> Buyer2 : {deliver: end}, "
                    + "reject: end}}}}}");
            assertInstanceOf(GMessage.class, g);
        }

        @Test
        void parseUnicodeParallel() {
            GlobalType g = GlobalTypeParser.parse(
                    "(A -> B : {m: end} \u2225 C -> D : {n: end})");
            assertInstanceOf(GParallel.class, g);
        }

        @Test
        void parseUnicodeRec() {
            GlobalType g = GlobalTypeParser.parse(
                    "\u03BCX . A -> B : {m: X, done: end}");
            assertInstanceOf(GRec.class, g);
        }

        @Test
        void parseErrorOnInvalidInput() {
            assertThrows(ParseError.class,
                    () -> GlobalTypeParser.parse("A -> : {m: end}"));
        }

        @Test
        void parseErrorOnMissingBrace() {
            assertThrows(ParseError.class,
                    () -> GlobalTypeParser.parse("A -> B : {m: end"));
        }

        @Test
        void parseErrorOnUnexpectedChar() {
            assertThrows(ParseError.class,
                    () -> GlobalTypeParser.parse("A -> B # {m: end}"));
        }
    }

    // -----------------------------------------------------------------------
    // State-space construction
    // -----------------------------------------------------------------------

    @Nested
    class StateSpaceTests {

        @Test
        void endStateSpace() {
            StateSpace ss = GlobalTypeChecker.buildStateSpace(new GEnd());
            assertEquals(1, ss.states().size());
            assertEquals(ss.top(), ss.bottom());
            assertTrue(ss.transitions().isEmpty());
        }

        @Test
        void singleMessageStateSpace() {
            GlobalType g = GlobalTypeParser.parse("A -> B : {m: end}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            assertEquals(2, ss.states().size());
            assertEquals(1, ss.transitions().size());
            assertEquals("A->B:m", ss.transitions().getFirst().label());
        }

        @Test
        void multipleChoicesStateSpace() {
            GlobalType g = GlobalTypeParser.parse(
                    "A -> B : {m1: end, m2: end}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            assertEquals(2, ss.states().size()); // top + end (both choices -> end)
            assertEquals(2, ss.transitions().size());
        }

        @Test
        void sequentialMessagesStateSpace() {
            GlobalType g = GlobalTypeParser.parse(
                    "A -> B : {m: B -> A : {n: end}}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            assertEquals(3, ss.states().size());
            assertEquals(2, ss.transitions().size());
        }

        @Test
        void roleAnnotatedLabels() {
            GlobalType g = GlobalTypeParser.parse(
                    "Client -> Server : {request: "
                    + "Server -> Client : {response: end}}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            var labels = ss.transitions().stream()
                    .map(StateSpace.Transition::label)
                    .toList();
            assertTrue(labels.contains("Client->Server:request"));
            assertTrue(labels.contains("Server->Client:response"));
        }

        @Test
        void recursiveStateSpace() {
            GlobalType g = GlobalTypeParser.parse(
                    "rec X . A -> B : {data: X, done: end}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            // States: entry + end
            assertEquals(2, ss.states().size());
            // data loops back, done goes to end
            assertEquals(2, ss.transitions().size());
        }

        @Test
        void unboundVariableThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> GlobalTypeChecker.buildStateSpace(new GVar("X")));
        }

        @Test
        void parallelStateSpace() {
            GlobalType g = GlobalTypeParser.parse(
                    "(A -> B : {m: end} || C -> D : {n: end})");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            // Product: (top,top), (bot,top), (top,bot), (bot,bot)=end
            assertEquals(4, ss.states().size());
        }

        @Test
        void twoBuyerStateSpace() {
            GlobalType g = GlobalTypeParser.parse(
                    "Buyer1 -> Seller : {lookup: "
                    + "Seller -> Buyer1 : {price: "
                    + "Seller -> Buyer2 : {price: "
                    + "Buyer1 -> Buyer2 : {share: "
                    + "Buyer2 -> Seller : {accept: "
                    + "Seller -> Buyer2 : {deliver: end}, "
                    + "reject: end}}}}}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            assertNotEquals(ss.top(), ss.bottom());
            assertTrue(ss.states().size() >= 7);
        }

        @Test
        void twoPhaseCommitStateSpace() {
            GlobalType g = GlobalTypeParser.parse(
                    "Coord -> P : {prepare: "
                    + "P -> Coord : {yes: "
                    + "Coord -> P : {commit: end}, "
                    + "no: Coord -> P : {abort: end}}}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            assertTrue(ss.states().size() >= 4);
        }
    }

    // -----------------------------------------------------------------------
    // Lattice properties of global state spaces
    // -----------------------------------------------------------------------

    @Nested
    class LatticeTests {

        @Test
        void endIsLattice() {
            StateSpace ss = GlobalTypeChecker.buildStateSpace(new GEnd());
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice());
        }

        @Test
        void singleMessageIsLattice() {
            GlobalType g = GlobalTypeParser.parse("A -> B : {m: end}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice());
        }

        @Test
        void branchingIsLattice() {
            GlobalType g = GlobalTypeParser.parse(
                    "A -> B : {m1: end, m2: end}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice());
        }

        @Test
        void sequentialIsLattice() {
            GlobalType g = GlobalTypeParser.parse(
                    "A -> B : {m: B -> A : {n: end}}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice());
        }

        @Test
        void recursiveIsLattice() {
            GlobalType g = GlobalTypeParser.parse(
                    "rec X . A -> B : {data: X, done: end}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice());
        }

        @Test
        void parallelIsLattice() {
            GlobalType g = GlobalTypeParser.parse(
                    "(A -> B : {m: end} || C -> D : {n: end})");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice());
        }

        @Test
        void twoBuyerIsLattice() {
            GlobalType g = GlobalTypeParser.parse(
                    "Buyer1 -> Seller : {lookup: "
                    + "Seller -> Buyer1 : {price: "
                    + "Seller -> Buyer2 : {price: "
                    + "Buyer1 -> Buyer2 : {share: "
                    + "Buyer2 -> Seller : {accept: "
                    + "Seller -> Buyer2 : {deliver: end}, "
                    + "reject: end}}}}}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice());
        }

        @Test
        void requestResponseIsLattice() {
            GlobalType g = GlobalTypeParser.parse(
                    "Client -> Server : {request: "
                    + "Server -> Client : {response: end}}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice());
        }

        @Test
        void twoPhaseCommitIsLattice() {
            GlobalType g = GlobalTypeParser.parse(
                    "Coord -> P : {prepare: "
                    + "P -> Coord : {yes: "
                    + "Coord -> P : {commit: end}, "
                    + "no: Coord -> P : {abort: end}}}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice());
        }

        @Test
        void streamingIsLattice() {
            GlobalType g = GlobalTypeParser.parse(
                    "rec X . Producer -> Consumer : {data: X, done: end}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice());
        }

        @Test
        void ringIsLattice() {
            GlobalType g = GlobalTypeParser.parse(
                    "A -> B : {msg: B -> C : {msg: C -> A : {msg: end}}}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice());
        }

        @Test
        void delegationIsLattice() {
            GlobalType g = GlobalTypeParser.parse(
                    "Client -> Master : {task: "
                    + "Master -> Worker : {delegate: "
                    + "Worker -> Master : {result: "
                    + "Master -> Client : {response: end}}}}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice());
        }

        @Test
        void negotiationIsLattice() {
            GlobalType g = GlobalTypeParser.parse(
                    "rec X . Buyer -> Seller : {offer: "
                    + "Seller -> Buyer : {accept: end, "
                    + "counter: X}}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice());
        }

        @Test
        void authServiceIsLattice() {
            GlobalType g = GlobalTypeParser.parse(
                    "Client -> Server : {login: "
                    + "Server -> Client : {granted: "
                    + "rec X . Client -> Server : {request: "
                    + "Server -> Client : {response: X}, "
                    + "logout: end}, "
                    + "denied: end}}");
            StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice());
        }
    }
}
