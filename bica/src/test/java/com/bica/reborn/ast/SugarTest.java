package com.bica.reborn.ast;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.parser.PrettyPrinter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sugar tests under the T2b grammar (2026-04-11).
 *
 * <p>{@code m . S} is no longer sugar for {@code &{m: S}} — it is a
 * first-class {@link Chain} primitive. Tests here pin the T2b semantics:
 *
 * <ul>
 *   <li>{@code desugar(Sequence(Var(m), S))} → {@code Chain(m, desugar(S))}</li>
 *   <li>{@code desugar(Chain(m, S))} = {@code Chain(m, desugar(S))}
 *       (preserved, not converted)</li>
 *   <li>{@code ensugar} is identity on {@link Chain} and on single-arm
 *       {@link Branch} — it must not collapse the two forms</li>
 * </ul>
 */
class SugarTest {

    // --- desugar tests ---

    @Test
    void desugar_convertsSequenceVarToChain() {
        // m . end  (legacy Sequence form)  →  Chain(m, end)
        SessionType input = new Sequence(new Var("m"), new End());
        SessionType result = Sugar.desugar(input);
        assertEquals(new Chain("m", new End()), result);
    }

    @Test
    void desugar_leavesNonVarSequenceUnchanged() {
        Parallel par = new Parallel(new End(), new End());
        SessionType input = new Sequence(par, new End());
        SessionType result = Sugar.desugar(input);
        assertEquals(input, result);
    }

    @Test
    void desugar_isRecursive() {
        // m . n . end (legacy)  →  Chain(m, Chain(n, end))
        SessionType input = new Sequence(new Var("m"),
                new Sequence(new Var("n"), new End()));
        SessionType result = Sugar.desugar(input);
        SessionType expected = new Chain("m", new Chain("n", new End()));
        assertEquals(expected, result);
    }

    @Test
    void desugar_identityOnEnd() {
        assertEquals(new End(), Sugar.desugar(new End()));
    }

    @Test
    void desugar_identityOnVar() {
        Var v = new Var("X");
        assertEquals(v, Sugar.desugar(v));
    }

    @Test
    void desugar_identityOnMultiBranch() {
        Branch b = new Branch(List.of(
            new Branch.Choice("a", new End()),
            new Branch.Choice("b", new End())
        ));
        assertEquals(b, Sugar.desugar(b));
    }

    @Test
    void desugar_preservesSingleArmBranch() {
        // Under T2b, &{m: end} is NOT the same as m . end — desugar preserves it.
        Branch b = new Branch(List.of(new Branch.Choice("m", new End())));
        assertEquals(b, Sugar.desugar(b));
    }

    @Test
    void desugar_identityOnSelect() {
        Select s = new Select(List.of(
            new Branch.Choice("x", new End()),
            new Branch.Choice("y", new End())
        ));
        assertEquals(s, Sugar.desugar(s));
    }

    @Test
    void desugar_identityOnParallel() {
        Parallel p = new Parallel(new End(), new End());
        assertEquals(p, Sugar.desugar(p));
    }

    @Test
    void desugar_preservesChain() {
        // Chain is the core-grammar primitive — desugar recurses but does not rewrite.
        Chain input = new Chain("m", new Chain("n", new End()));
        assertEquals(input, Sugar.desugar(input));
    }

    @Test
    void desugar_recursIntoRec() {
        // rec X . m . X (legacy)  →  rec X . Chain(m, X)
        SessionType input = new Rec("X", new Sequence(new Var("m"), new Var("X")));
        SessionType result = Sugar.desugar(input);
        SessionType expected = new Rec("X", new Chain("m", new Var("X")));
        assertEquals(expected, result);
    }

    @Test
    void desugar_recursIntoBranch() {
        // &{a: m . end}  →  &{a: Chain(m, end)}
        Branch input = new Branch(List.of(
            new Branch.Choice("a", new Sequence(new Var("m"), new End()))
        ));
        SessionType result = Sugar.desugar(input);
        Branch expected = new Branch(List.of(
            new Branch.Choice("a", new Chain("m", new End()))
        ));
        assertEquals(expected, result);
    }

    @Test
    void desugar_recursIntoSelect() {
        Select input = new Select(List.of(
            new Branch.Choice("a", new Sequence(new Var("m"), new End()))
        ));
        SessionType result = Sugar.desugar(input);
        Select expected = new Select(List.of(
            new Branch.Choice("a", new Chain("m", new End()))
        ));
        assertEquals(expected, result);
    }

    @Test
    void desugar_recursIntoParallel() {
        Parallel input = new Parallel(
            new Sequence(new Var("a"), new End()),
            new Sequence(new Var("b"), new End())
        );
        SessionType result = Sugar.desugar(input);
        Parallel expected = new Parallel(
            new Chain("a", new End()),
            new Chain("b", new End())
        );
        assertEquals(expected, result);
    }

    // --- ensugar tests ---

    @Test
    void ensugar_preservesSingleArmBranch() {
        // T2b: single-arm Branch is NOT collapsed to Sequence or Chain —
        // the distinction is the whole point of T2b.
        Branch input = new Branch(List.of(new Branch.Choice("m", new End())));
        SessionType result = Sugar.ensugar(input);
        assertEquals(input, result);
    }

    @Test
    void ensugar_leavesMultiBranchUnchanged() {
        Branch input = new Branch(List.of(
            new Branch.Choice("a", new End()),
            new Branch.Choice("b", new End())
        ));
        assertEquals(input, Sugar.ensugar(input));
    }

    @Test
    void ensugar_isRecursive() {
        // &{m: &{n: end}}  →  preserved (no single-arm collapse)
        Branch input = new Branch(List.of(
            new Branch.Choice("m", new Branch(List.of(
                new Branch.Choice("n", new End())
            )))
        ));
        SessionType result = Sugar.ensugar(input);
        assertEquals(input, result);
    }

    @Test
    void ensugar_preservesChain() {
        Chain input = new Chain("m", new Chain("n", new End()));
        assertEquals(input, Sugar.ensugar(input));
    }

    @Test
    void ensugar_rewritesLegacySequenceToChain() {
        // Legacy Sequence(Var(m), ...) is bridged to Chain for consistency
        // with desugar. This is the only non-identity rewrite ensugar performs
        // under T2b.
        SessionType input = new Sequence(new Var("m"), new End());
        SessionType result = Sugar.ensugar(input);
        assertEquals(new Chain("m", new End()), result);
    }

    @Test
    void ensugar_recursIntoRec() {
        Rec input = new Rec("X", new Branch(List.of(
            new Branch.Choice("m", new Var("X"))
        )));
        SessionType result = Sugar.ensugar(input);
        assertEquals(input, result);
    }

    @Test
    void ensugar_recursIntoSelect() {
        Select input = new Select(List.of(
            new Branch.Choice("a", new Branch(List.of(
                new Branch.Choice("x", new End())
            )))
        ));
        SessionType result = Sugar.ensugar(input);
        assertEquals(input, result);
    }

    @Test
    void ensugar_recursIntoParallel() {
        Parallel input = new Parallel(
            new Branch(List.of(new Branch.Choice("a", new End()))),
            new Branch(List.of(new Branch.Choice("b", new End())))
        );
        SessionType result = Sugar.ensugar(input);
        assertEquals(input, result);
    }

    // --- round-trip tests ---

    @Test
    void roundTrip_desugarEnsugarIsIdentityOnChain() {
        SessionType core = new Chain("m", new End());
        assertEquals(core, Sugar.desugar(Sugar.ensugar(core)));
    }

    @Test
    void roundTrip_ensugarDesugarIsIdentityOnSingleArmBranch() {
        // Under T2b single-arm Branch is preserved by both passes.
        SessionType core = new Branch(List.of(new Branch.Choice("m", new End())));
        SessionType roundTripped = Sugar.ensugar(Sugar.desugar(core));
        assertEquals(core, roundTripped);
    }

    @Test
    void roundTrip_complexNested() {
        SessionType core = new Branch(List.of(
            new Branch.Choice("m", new Branch(List.of(
                new Branch.Choice("n", new End())
            )))
        ));
        assertEquals(core, Sugar.desugar(Sugar.ensugar(core)));
    }

    // --- prettyCore tests ---

    @Test
    void prettyCore_singleBranch_usesFullNotation() {
        Branch b = new Branch(List.of(new Branch.Choice("m", new End())));
        assertEquals("&{m: end}", PrettyPrinter.prettyCore(b));
    }

    @Test
    void prettyCore_multiBranch() {
        Branch b = new Branch(List.of(
            new Branch.Choice("a", new End()),
            new Branch.Choice("b", new End())
        ));
        assertEquals("&{a: end, b: end}", PrettyPrinter.prettyCore(b));
    }

    @Test
    void prettyCore_select() {
        Select s = new Select(List.of(
            new Branch.Choice("x", new End()),
            new Branch.Choice("y", new End())
        ));
        assertEquals("+{x: end, y: end}", PrettyPrinter.prettyCore(s));
    }

    @Test
    void prettyCore_parallel() {
        Parallel p = new Parallel(new End(), new End());
        assertEquals("(end || end)", PrettyPrinter.prettyCore(p));
    }

    @Test
    void prettyCore_rec() {
        Rec r = new Rec("X", new Var("X"));
        assertEquals("rec X . X", PrettyPrinter.prettyCore(r));
    }

    @Test
    void prettyCore_chain() {
        // T2b: Chain prints as m . S in both pretty and prettyCore
        Chain ch = new Chain("m", new End());
        assertEquals("m . end", PrettyPrinter.prettyCore(ch));
    }

    @Test
    void prettyCore_sequence() {
        Sequence s = new Sequence(
            new Parallel(new End(), new End()),
            new End()
        );
        assertEquals("(end || end) . end", PrettyPrinter.prettyCore(s));
    }

    // --- pretty vs prettyCore comparison ---

    @Test
    void pretty_vsPrettyCore_singleArmBranchIsExplicit() {
        // T2b: both printers render single-arm Branch as explicit &{m: end}.
        // The "m . end" surface form now means Chain, not single-arm Branch.
        Branch b = new Branch(List.of(new Branch.Choice("m", new End())));
        assertEquals("&{m: end}", PrettyPrinter.pretty(b));
        assertEquals("&{m: end}", PrettyPrinter.prettyCore(b));
    }

    @Test
    void pretty_vsPrettyCore_chainUsesSugarForm() {
        // Chain is the canonical producer of "m . S" surface syntax.
        Chain ch = new Chain("m", new End());
        assertEquals("m . end", PrettyPrinter.pretty(ch));
        assertEquals("m . end", PrettyPrinter.prettyCore(ch));
    }

    @Test
    void pretty_vsPrettyCore_multiBranch_identical() {
        Branch b = new Branch(List.of(
            new Branch.Choice("a", new End()),
            new Branch.Choice("b", new End())
        ));
        String expected = "&{a: end, b: end}";
        assertEquals(expected, PrettyPrinter.pretty(b));
        assertEquals(expected, PrettyPrinter.prettyCore(b));
    }

    @Test
    void pretty_vsPrettyCore_nestedChain() {
        // T2b: chained methods produce nested Chain nodes and render as m . n . end
        Chain nested = new Chain("m", new Chain("n", new End()));
        assertEquals("m . n . end", PrettyPrinter.pretty(nested));
        assertEquals("m . n . end", PrettyPrinter.prettyCore(nested));
    }

    // --- benchmark parse test ---

    @Test
    void prettyCore_parsedBenchmark() {
        // Under T2b, `open . rec X . ...` parses with Chain at the top.
        // The core form preserves Chain and single-arm Branch distinctly.
        SessionType parsed = Parser.parse("open . rec X . &{read: +{data: X, eof: close . end}}");
        String core = PrettyPrinter.prettyCore(Sugar.desugar(parsed));
        assertEquals("open . rec X . &{read: +{data: X, eof: close . end}}", core);
    }
}
