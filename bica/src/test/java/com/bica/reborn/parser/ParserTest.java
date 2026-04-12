package com.bica.reborn.parser;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the recursive-descent session type parser. */
class ParserTest {

    // === Basic constructs ===

    @Nested
    class BasicConstructs {

        @Test
        void parseEnd() {
            assertEquals(new End(), Parser.parse("end"));
        }

        @Test
        void parseVariable() {
            assertEquals(new Var("X"), Parser.parse("X"));
        }

        @Test
        void parseBranch() {
            var result = Parser.parse("&{m: end, n: end}");
            assertEquals(new Branch(List.of(
                    new Choice("m", new End()),
                    new Choice("n", new End()))), result);
        }

        @Test
        void parseSelect() {
            var result = Parser.parse("+{OK: end, ERROR: end}");
            assertEquals(new Select(List.of(
                    new Choice("OK", new End()),
                    new Choice("ERROR", new End()))), result);
        }

        @Test
        void parseParallel() {
            var result = Parser.parse("(end || end)");
            assertEquals(new Parallel(new End(), new End()), result);
        }

        @Test
        void parseRec() {
            var result = Parser.parse("rec X . end");
            assertEquals(new Rec("X", new End()), result);
        }

        @Test
        void parseParenthesizedGrouping() {
            assertEquals(new End(), Parser.parse("(end)"));
        }
    }

    // === Sequencing (T2b: bare ident `.` becomes Chain, complex left becomes Sequence) ===

    @Nested
    class Sequencing {

        @Test
        void simpleMethodCall() {
            // T2b: m . end → Chain("m", End)
            var result = Parser.parse("m . end");
            assertEquals(new Chain("m", new End()), result);
        }

        @Test
        void chainedMethods() {
            // T2b: a . b . end → Chain("a", Chain("b", End)) (right-associative)
            var result = Parser.parse("a . b . end");
            assertEquals(new Chain("a", new Chain("b", new End())), result);
        }

        @Test
        void methodThenBranch() {
            var result = Parser.parse("init . &{m: end, n: end}");
            var expected = new Chain("init", new Branch(List.of(
                    new Choice("m", new End()),
                    new Choice("n", new End()))));
            assertEquals(expected, result);
        }

        @Test
        void complexLeftBecomesSequence() {
            // (a || b) . end — complex left still becomes Sequence (not Chain)
            var result = Parser.parse("(a || b) . end");
            var expected = new Sequence(new Parallel(new Var("a"), new Var("b")), new End());
            assertEquals(expected, result);
        }

        @Test
        void branchThenMethod() {
            // &{m: end} . close . end — Branch on left becomes Sequence,
            // right-hand `close . end` is Chain
            var result = Parser.parse("&{m: end} . close . end");
            var expected = new Sequence(
                    new Branch(List.of(new Choice("m", new End()))),
                    new Chain("close", new End()));
            assertEquals(expected, result);
        }

        @Test
        void recBodyWithSequencing() {
            // T2b: rec X . m . X → Rec("X", Chain("m", Var("X")))
            var result = Parser.parse("rec X . m . X");
            assertEquals(new Rec("X", new Chain("m", new Var("X"))), result);
        }
    }

    // === Complex / spec examples ===

    @Nested
    class SpecExamples {

        @Test
        void sharedFile() {
            // T2b: init, use, close each become Chain
            String src = "init . &{open: +{OK: use . close . end, ERROR: end}}";
            var result = Parser.parse(src);
            var expected = new Chain("init", new Branch(List.of(
                    new Choice("open", new Select(List.of(
                            new Choice("OK", new Chain("use", new Chain("close", new End()))),
                            new Choice("ERROR", new End())))))));
            assertEquals(expected, result);
        }

        @Test
        void concurrentFileAccess() {
            // T2b: read . end and write . end are Chain
            var result = Parser.parse("(read . end || write . end)");
            var expected = new Parallel(
                    new Chain("read", new End()),
                    new Chain("write", new End()));
            assertEquals(expected, result);
        }

        @Test
        void fullSharedFile() {
            String src = "init . &{open: +{OK: (read . end || write . end) . close . end, ERROR: end}}";
            var result = Parser.parse(src);
            var expected = new Chain("init", new Branch(List.of(
                    new Choice("open", new Select(List.of(
                            new Choice("OK", new Sequence(
                                    new Parallel(
                                            new Chain("read", new End()),
                                            new Chain("write", new End())),
                                    new Chain("close", new End()))),
                            new Choice("ERROR", new End())))))));
            assertEquals(expected, result);
        }

        @Test
        void recursiveProtocol() {
            var result = Parser.parse("rec X . &{next: X, close: end}");
            var expected = new Rec("X", new Branch(List.of(
                    new Choice("next", new Var("X")),
                    new Choice("close", new End()))));
            assertEquals(expected, result);
        }

        @Test
        void nestedRecursion() {
            var result = Parser.parse("rec X . rec Y . &{a: X, b: Y}");
            var expected = new Rec("X", new Rec("Y", new Branch(List.of(
                    new Choice("a", new Var("X")),
                    new Choice("b", new Var("Y"))))));
            assertEquals(expected, result);
        }

        @Test
        void parallelInsideBranch() {
            var result = Parser.parse("&{go: (a . end || b . end), stop: end}");
            var expected = new Branch(List.of(
                    new Choice("go", new Parallel(
                            new Chain("a", new End()),
                            new Chain("b", new End()))),
                    new Choice("stop", new End())));
            assertEquals(expected, result);
        }
    }

    // === Error handling ===

    @Nested
    class ErrorHandling {

        @Test
        void emptyInput() {
            ParseError err = assertThrows(ParseError.class, () -> Parser.parse(""));
            assertTrue(err.getMessage().contains("unexpected token EOF"));
        }

        @Test
        void trailingGarbage() {
            assertThrows(ParseError.class, () -> Parser.parse("end end"));
        }

        @Test
        void missingBraceBranch() {
            assertThrows(ParseError.class, () -> Parser.parse("&{m: end"));
        }

        @Test
        void missingBraceSelect() {
            assertThrows(ParseError.class, () -> Parser.parse("+{m: end"));
        }

        @Test
        void missingParen() {
            assertThrows(ParseError.class, () -> Parser.parse("(end"));
        }

        @Test
        void emptyBranch() {
            ParseError err = assertThrows(ParseError.class, () -> Parser.parse("&{}"));
            assertTrue(err.getMessage().contains("at least one choice"));
        }

        @Test
        void emptySelect() {
            ParseError err = assertThrows(ParseError.class, () -> Parser.parse("+{}"));
            assertTrue(err.getMessage().contains("at least one choice"));
        }

        @Test
        void missingColonInBranch() {
            ParseError err = assertThrows(ParseError.class, () -> Parser.parse("&{m end}"));
            assertTrue(err.getMessage().contains("colon"));
        }

        @Test
        void recKeywordAsVariable() {
            ParseError err = assertThrows(ParseError.class, () -> Parser.parse("rec rec . end"));
            assertTrue(err.getMessage().contains("keyword"));
        }

        @Test
        void endKeywordAsRecVariable() {
            ParseError err = assertThrows(ParseError.class, () -> Parser.parse("rec end . end"));
            assertTrue(err.getMessage().contains("keyword"));
        }

        @Test
        void dotWithoutRightSide() {
            assertThrows(ParseError.class, () -> Parser.parse("m ."));
        }

        @Test
        void unexpectedToken() {
            assertThrows(ParseError.class, () -> Parser.parse("}"));
        }

        @Test
        void errorHasPosition() {
            try {
                Parser.parse("m . }");
                fail("expected ParseError");
            } catch (ParseError e) {
                assertNotNull(e.getPos());
                assertEquals(4, e.getPos());
            }
        }
    }

    // === Edge cases ===

    @Nested
    class EdgeCases {

        @Test
        void deeplyNested() {
            // T2b: a . b . c . d . e . end → 5 nested Chain nodes
            var node = Parser.parse("a . b . c . d . e . end");
            for (String name : new String[]{"a", "b", "c", "d", "e"}) {
                assertInstanceOf(Chain.class, node);
                Chain ch = (Chain) node;
                assertEquals(name, ch.method());
                node = ch.cont();
            }
            assertEquals(new End(), node);
        }

        @Test
        void whitespaceInsensitive() {
            var compact = Parser.parse("&{m:end,n:end}");
            var spaced = Parser.parse("&{  m  :  end  ,  n  :  end  }");
            assertEquals(compact, spaced);
        }

        @Test
        void newlinesOk() {
            var result = Parser.parse("&{\n    m: end,\n    n: end\n}");
            assertEquals(new Branch(List.of(
                    new Choice("m", new End()),
                    new Choice("n", new End()))), result);
        }

        @Test
        void parallelWithComplexBranches() {
            var result = Parser.parse("(&{a: end, b: end} || +{c: end, d: end})");
            var expected = new Parallel(
                    new Branch(List.of(new Choice("a", new End()), new Choice("b", new End()))),
                    new Select(List.of(new Choice("c", new End()), new Choice("d", new End()))));
            assertEquals(expected, result);
        }

        @Test
        void recWithParallelBody() {
            var result = Parser.parse("rec X . (X || end)");
            assertEquals(new Rec("X", new Parallel(new Var("X"), new End())), result);
        }

        @Test
        void selectWithSequencingInBody() {
            // T2b: m . n . end inside a branch body becomes Chain-Chain-end
            var result = Parser.parse("+{ok: m . n . end, err: end}");
            var expected = new Select(List.of(
                    new Choice("ok", new Chain("m", new Chain("n", new End()))),
                    new Choice("err", new End())));
            assertEquals(expected, result);
        }
    }

    // === Benchmark protocols ===

    @Nested
    class BenchmarkProtocols {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
                // 1. Java Iterator
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                // 2. File Object
                "open . rec X . &{read: +{data: X, eof: close . end}}",
                // 3. SMTP
                "connect . ehlo . rec X . &{mail: rcpt . data . +{OK: X, ERR: X}, quit: end}",
                // 4. HTTP Connection
                "connect . rec X . &{request: +{OK200: readBody . X, ERR4xx: X, ERR5xx: X}, close: end}",
                // 5. OAuth 2.0
                "requestAuth . +{GRANTED: getToken . +{TOKEN: rec X . &{useToken: X, refreshToken: +{OK: X, EXPIRED: end}, revoke: end}, ERROR: end}, DENIED: end}",
                // 6. Two-Buyer
                "lookup . getPrice . (proposeA . end || proposeB . +{ACCEPT: pay . end, REJECT: end})",
                // 7. MCP
                "initialize . (rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end} || rec Y . +{NOTIFICATION: Y, DONE: end})",
                // 8. A2A
                "sendTask . rec X . +{WORKING: &{getStatus: X, cancel: end}, COMPLETED: getArtifact . end, FAILED: end}",
                // 9. File Channel
                "open . +{OK: (rec X . &{read: X, doneRead: end} || rec Y . &{write: Y, doneWrite: end}) . close . end, ERR: end}",
                // 10. ATM
                "insertCard . enterPIN . +{AUTH: rec X . &{checkBalance: X, withdraw: +{OK: X, INSUFFICIENT: X}, deposit: X, ejectCard: end}, REJECTED: ejectCard . end}",
                // 11. Reentrant Lock
                "rec X . &{lock: &{unlock: X, newCondition: &{await: &{signal: &{unlock: X}}}}, close: end}",
                // 12. WebSocket
                "connect . +{OPEN: (rec X . &{send: X, ping: X, closeSend: end} || rec Y . +{MESSAGE: Y, PONG: Y, CLOSE: end}), REFUSED: end}",
                // 13. DB Transaction
                "begin . rec X . &{query: +{RESULT: X, ERROR: X}, update: +{ROWS: X, ERROR: X}, commit: end, rollback: end}",
                // 14. Pub/Sub
                "connect . rec X . &{subscribe: X, unsubscribe: X, poll: +{MSG: X, EMPTY: X}, disconnect: end}",
                // 15. DNS Resolver
                "rec X . &{query: +{ANSWER: X, NXDOMAIN: X, SERVFAIL: X, TIMEOUT: &{retry: X, abandon: end}}, close: end}",
                // 16. Reticulate Pipeline
                "input . parse . +{OK: buildStateSpace . +{OK: (checkLattice . checkTermination . checkWFPar . end || renderDiagram . end) . returnResult . end, ERROR: end}, ERROR: end}",
                // 17. GitHub CI Workflow
                "trigger . checkout . setup . (lint . end || test . end) . +{PASS: deploy . +{OK: end, FAIL: rollback . end}, FAIL: end}"
        })
        void parsesSuccessfully(String typeString) {
            SessionType result = Parser.parse(typeString);
            assertNotNull(result);
        }

        @ParameterizedTest(name = "roundtrip {0}")
        @ValueSource(strings = {
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                "open . rec X . &{read: +{data: X, eof: close . end}}",
                "connect . ehlo . rec X . &{mail: rcpt . data . +{OK: X, ERR: X}, quit: end}",
                "connect . rec X . &{request: +{OK200: readBody . X, ERR4xx: X, ERR5xx: X}, close: end}",
                "requestAuth . +{GRANTED: getToken . +{TOKEN: rec X . &{useToken: X, refreshToken: +{OK: X, EXPIRED: end}, revoke: end}, ERROR: end}, DENIED: end}",
                "lookup . getPrice . (proposeA . end || proposeB . +{ACCEPT: pay . end, REJECT: end})",
                "initialize . (rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end} || rec Y . +{NOTIFICATION: Y, DONE: end})",
                "sendTask . rec X . +{WORKING: &{getStatus: X, cancel: end}, COMPLETED: getArtifact . end, FAILED: end}",
                "open . +{OK: (rec X . &{read: X, doneRead: end} || rec Y . &{write: Y, doneWrite: end}) . close . end, ERR: end}",
                "insertCard . enterPIN . +{AUTH: rec X . &{checkBalance: X, withdraw: +{OK: X, INSUFFICIENT: X}, deposit: X, ejectCard: end}, REJECTED: ejectCard . end}",
                "rec X . &{lock: &{unlock: X, newCondition: &{await: &{signal: &{unlock: X}}}}, close: end}",
                "connect . +{OPEN: (rec X . &{send: X, ping: X, closeSend: end} || rec Y . +{MESSAGE: Y, PONG: Y, CLOSE: end}), REFUSED: end}",
                "begin . rec X . &{query: +{RESULT: X, ERROR: X}, update: +{ROWS: X, ERROR: X}, commit: end, rollback: end}",
                "connect . rec X . &{subscribe: X, unsubscribe: X, poll: +{MSG: X, EMPTY: X}, disconnect: end}",
                "rec X . &{query: +{ANSWER: X, NXDOMAIN: X, SERVFAIL: X, TIMEOUT: &{retry: X, abandon: end}}, close: end}",
                "input . parse . +{OK: buildStateSpace . +{OK: (checkLattice . checkTermination . checkWFPar . end || renderDiagram . end) . returnResult . end, ERROR: end}, ERROR: end}",
                "trigger . checkout . setup . (lint . end || test . end) . +{PASS: deploy . +{OK: end, FAIL: rollback . end}, FAIL: end}"
        })
        void roundTrip(String typeString) {
            SessionType first = Parser.parse(typeString);
            String pretty = PrettyPrinter.pretty(first);
            SessionType second = Parser.parse(pretty);
            assertEquals(first, second);
        }
    }
}
