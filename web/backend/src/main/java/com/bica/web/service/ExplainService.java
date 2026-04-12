package com.bica.web.service;

import com.bica.reborn.ast.*;
import com.bica.reborn.parser.Parser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExplainService {

    public String explain(String typeString) {
        SessionType ast = Parser.parse(typeString);
        var sb = new StringBuilder();
        explain(ast, sb, 0, true);
        return sb.toString().trim();
    }

    /**
     * Generate a narrative "story" from a session type, with characters and domain flavour.
     * @param typeString the session type
     * @param glossary optional method-to-description mapping (e.g. {"open": "opens the file"})
     */
    public String story(String typeString, Map<String, String> glossary) {
        SessionType ast = Parser.parse(typeString);
        var sb = new StringBuilder();
        var ctx = new StoryContext(glossary != null ? glossary : Map.of());
        story(ast, sb, ctx, true);
        return sb.toString().trim();
    }

    private record StoryContext(Map<String, String> glossary) {
        String describe(String method) {
            if (glossary.containsKey(method)) return glossary.get(method);
            // Auto-humanize: replace underscores and camelCase
            String s = method.replaceAll("([a-z])([A-Z])", "$1 $2").replace("_", " ").toLowerCase();
            return s + "s";
        }
    }

    private void story(SessionType node, StringBuilder sb, StoryContext ctx, boolean isRoot) {
        switch (node) {
            case End e -> sb.append("The interaction comes to an end.");

            case Var v -> sb.append("The cycle repeats.");

            case Branch b -> {
                if (b.choices().size() == 1) {
                    var c = b.choices().getFirst();
                    String desc = ctx.describe(c.label());
                    if (isRoot) sb.append("Our story begins. The client **").append(desc).append("**. ");
                    else sb.append("The client **").append(desc).append("**. ");
                    story(c.body(), sb, ctx, false);
                } else {
                    if (isRoot) sb.append("The client faces a decision. ");
                    else sb.append("A choice presents itself. ");
                    sb.append("They can: ");
                    var labels = b.choices().stream().map(c -> "**" + ctx.describe(c.label()) + "**").collect(Collectors.joining(", or "));
                    sb.append(labels).append(".\n\n");
                    for (var c : b.choices()) {
                        sb.append("- Choosing to **").append(ctx.describe(c.label())).append("**: ");
                        story(c.body(), sb, ctx, false);
                        sb.append("\n");
                    }
                }
            }

            case Select s -> {
                sb.append("The server deliberates and announces its decision. ");
                var labels = s.choices().stream().map(c -> "**" + c.label() + "**").collect(Collectors.joining(", or "));
                sb.append("The outcome is: ").append(labels).append(".\n\n");
                for (var c : s.choices()) {
                    sb.append("- If the answer is **").append(c.label()).append("**: ");
                    story(c.body(), sb, ctx, false);
                    sb.append("\n");
                }
            }

            case Parallel p -> {
                sb.append("Now two things happen **at the same time**:\n\n");
                sb.append("  **Meanwhile, on one side**: ");
                story(p.left(), sb, ctx, false);
                sb.append("\n\n  **And on the other side**: ");
                story(p.right(), sb, ctx, false);
                sb.append("\n\nBoth must finish before the story continues.");
            }

            case Rec r -> {
                sb.append("What follows is a **recurring pattern**. ");
                story(r.body(), sb, ctx, false);
            }

            case Sequence seq -> {
                story(seq.left(), sb, ctx, isRoot);
                sb.append(" ");
                story(seq.right(), sb, ctx, false);
            }
        }
    }

    private void explain(SessionType node, StringBuilder sb, int depth, boolean isRoot) {
        switch (node) {
            case End e -> sb.append("The session terminates.");

            case Var v -> sb.append("The protocol loops back (recursion variable ").append(v.name()).append(").");

            case Branch b -> {
                if (b.choices().size() == 1) {
                    // Sequencing sugar: single-method branch
                    var c = b.choices().getFirst();
                    if (isRoot) sb.append("The protocol begins by calling **").append(c.label()).append("**. ");
                    else sb.append("Method **").append(c.label()).append("** is called. ");
                    explain(c.body(), sb, depth, false);
                } else {
                    String actor = "the environment";
                    if (isRoot) sb.append("The protocol offers a choice. ");
                    else sb.append("At this point, ").append(actor).append(" chooses. ");
                    sb.append("The available methods are: ");
                    sb.append(b.choices().stream().map(c -> "**" + c.label() + "**").collect(Collectors.joining(", ")));
                    sb.append(".\n\n");
                    for (var c : b.choices()) {
                        sb.append("- If **").append(c.label()).append("** is called: ");
                        explain(c.body(), sb, depth + 1, false);
                        sb.append("\n");
                    }
                }
            }

            case Select s -> {
                String actor = "the system";
                if (isRoot) sb.append("The system decides the outcome. ");
                else sb.append("Then ").append(actor).append(" responds. ");
                sb.append("Possible outcomes: ");
                sb.append(s.choices().stream().map(c -> "**" + c.label() + "**").collect(Collectors.joining(", ")));
                sb.append(".\n\n");
                for (var c : s.choices()) {
                    sb.append("- On **").append(c.label()).append("**: ");
                    explain(c.body(), sb, depth + 1, false);
                    sb.append("\n");
                }
            }

            case Parallel p -> {
                sb.append("Two activities run **concurrently**:\n\n");
                sb.append("  **Thread 1**: ");
                explain(p.left(), sb, depth + 1, false);
                sb.append("\n\n  **Thread 2**: ");
                explain(p.right(), sb, depth + 1, false);
                sb.append("\n\nBoth threads must complete before the protocol continues.");
            }

            case Rec r -> {
                sb.append("This is a **repeating** protocol (loop). ");
                explain(r.body(), sb, depth, false);
            }

            case Sequence seq -> {
                explain(seq.left(), sb, depth, isRoot);
                sb.append(" Then: ");
                explain(seq.right(), sb, depth, false);
            }
        }
    }
}
