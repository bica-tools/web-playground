package com.bica.reborn.parser;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests for the session type pretty-printer. */
class PrettyPrinterTest {

    @Test
    void prettyEnd() {
        assertEquals("end", PrettyPrinter.pretty(new End()));
    }

    @Test
    void prettyVar() {
        assertEquals("X", PrettyPrinter.pretty(new Var("X")));
    }

    @Test
    void singleBranchAsExplicit() {
        // T2b (2026-04-11): single-arm Branch prints as &{m: end} explicitly.
        // The `m . end` surface form is reserved for Chain.
        assertEquals("&{m: end}",
                PrettyPrinter.pretty(new Branch(List.of(new Choice("m", new End())))));
    }

    @Test
    void chainAsSugar() {
        // T2b: Chain is the canonical producer of the `m . S` surface form.
        assertEquals("m . end",
                PrettyPrinter.pretty(new Chain("m", new End())));
    }

    @Test
    void multiBranch() {
        var node = new Branch(List.of(
                new Choice("m", new End()),
                new Choice("n", new Var("X"))));
        assertEquals("&{m: end, n: X}", PrettyPrinter.pretty(node));
    }

    @Test
    void prettySelect() {
        var node = new Select(List.of(
                new Choice("OK", new End()),
                new Choice("ERR", new End())));
        assertEquals("+{OK: end, ERR: end}", PrettyPrinter.pretty(node));
    }

    @Test
    void prettyParallel() {
        var node = new Parallel(new End(), new Var("X"));
        assertEquals("(end || X)", PrettyPrinter.pretty(node));
    }

    @Test
    void prettyRec() {
        var node = new Rec("X", new Var("X"));
        assertEquals("rec X . X", PrettyPrinter.pretty(node));
    }

    @Test
    void prettySequence() {
        var node = new Sequence(new Parallel(new End(), new End()), new Var("X"));
        assertEquals("(end || end) . X", PrettyPrinter.pretty(node));
    }

    @Test
    void roundtripSimple() {
        String src = "rec X . &{next: X, close: end}";
        assertEquals(Parser.parse(src),
                Parser.parse(PrettyPrinter.pretty(Parser.parse(src))));
    }

    @Test
    void roundtripComplex() {
        String src = "init . &{open: +{OK: (read . end || write . end) . close . end, ERROR: end}}";
        assertEquals(Parser.parse(src),
                Parser.parse(PrettyPrinter.pretty(Parser.parse(src))));
    }

    @Test
    void roundtripParallel() {
        String src = "(a . end || b . end)";
        assertEquals(Parser.parse(src),
                Parser.parse(PrettyPrinter.pretty(Parser.parse(src))));
    }
}
