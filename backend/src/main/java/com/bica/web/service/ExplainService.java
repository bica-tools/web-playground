package com.bica.web.service;

import com.bica.reborn.ast.*;
import com.bica.reborn.parser.Parser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExplainService {

    public String explain(String typeString) {
        SessionType ast = Parser.parse(typeString);
        var sb = new StringBuilder();
        explain(ast, sb, 0, true);
        return sb.toString().trim();
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
