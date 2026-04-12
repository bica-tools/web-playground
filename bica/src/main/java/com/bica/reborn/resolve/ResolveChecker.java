package com.bica.reborn.resolve;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;

import java.util.*;

/**
 * Resolve equation-based definitions into tree ASTs (Step 155 port).
 *
 * <p>Transforms a list of named definitions into a single SessionType AST
 * by resolving name references. Self-recursive and mutually recursive
 * definitions are wrapped with Rec nodes.
 */
public final class ResolveChecker {

    private ResolveChecker() {}

    /**
     * A named definition: name = body.
     */
    public record Definition(String name, SessionType body) {
        public Definition {
            Objects.requireNonNull(name);
            Objects.requireNonNull(body);
        }
    }

    /**
     * A program is a list of definitions.
     */
    public record Program(List<Definition> definitions) {
        public Program {
            Objects.requireNonNull(definitions);
            definitions = List.copyOf(definitions);
        }
    }

    // =========================================================================
    // Resolve
    // =========================================================================

    /**
     * Resolve a program into a single SessionType AST.
     *
     * @param program The program to resolve.
     * @return The resolved AST for the first definition.
     * @throws IllegalArgumentException on errors.
     */
    public static SessionType resolve(Program program) {
        if (program.definitions().isEmpty()) {
            throw new IllegalArgumentException("empty program: no definitions to resolve");
        }

        // Check for duplicates
        Map<String, Integer> seen = new HashMap<>();
        for (int i = 0; i < program.definitions().size(); i++) {
            String name = program.definitions().get(i).name();
            if (seen.containsKey(name)) {
                throw new IllegalArgumentException(
                        "duplicate definition: " + name +
                                " (first at index " + seen.get(name) + ", again at " + i + ")");
            }
            seen.put(name, i);
        }

        Map<String, SessionType> env = new LinkedHashMap<>();
        for (var d : program.definitions()) env.put(d.name(), d.body());
        Set<String> defNames = env.keySet();

        String entryName = program.definitions().getFirst().name();
        return resolveName(entryName, env, defNames, new HashSet<>());
    }

    /**
     * Resolve a program represented as a list of definitions.
     */
    public static SessionType resolveProgram(List<Definition> definitions) {
        return resolve(new Program(definitions));
    }

    // =========================================================================
    // Internal resolution
    // =========================================================================

    private static SessionType resolveName(
            String name, Map<String, SessionType> env,
            Set<String> defNames, Set<String> expanding) {

        if (!env.containsKey(name)) {
            throw new IllegalArgumentException("undefined name: " + name);
        }

        SessionType body = env.get(name);
        Set<String> free = freeDefNames(body, defNames, new HashSet<>());
        boolean isSelfRecursive = free.contains(name);
        boolean isMutuallyRecursive = reaches(name, env, defNames);

        if (isSelfRecursive || isMutuallyRecursive) {
            SessionType resolved = resolveBody(body, name, env, defNames, union(expanding, name));
            return new Rec(name, resolved);
        } else {
            return resolveBody(body, name, env, defNames, expanding);
        }
    }

    private static boolean reaches(String name, Map<String, SessionType> env, Set<String> defNames) {
        Set<String> visited = new HashSet<>();
        Set<String> free = freeDefNames(env.get(name), defNames, new HashSet<>());
        Deque<String> stack = new ArrayDeque<>(free);
        stack.remove(name);

        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (current.equals(name)) return true;
            if (!visited.add(current) || !env.containsKey(current)) continue;
            Set<String> refs = freeDefNames(env.get(current), defNames, new HashSet<>());
            for (String ref : refs) {
                if (!visited.contains(ref)) stack.push(ref);
            }
        }
        return false;
    }

    private static SessionType resolveBody(
            SessionType node, String currentName,
            Map<String, SessionType> env, Set<String> defNames,
            Set<String> expanding) {

        if (node instanceof End || node instanceof Var v && !defNames.contains(v.name())) {
            return node;
        }
        if (node instanceof Var v) {
            if (v.name().equals(currentName)) return node; // Self-reference for Rec
            if (expanding.contains(v.name())) return new Var(v.name()); // Mutual recursion
            return resolveName(v.name(), env, defNames, expanding);
        }
        if (node instanceof Branch b) {
            List<Choice> newChoices = new ArrayList<>();
            for (Choice c : b.choices()) {
                newChoices.add(new Choice(c.label(),
                        resolveBody(c.body(), currentName, env, defNames, expanding)));
            }
            return new Branch(newChoices);
        }
        if (node instanceof Select s) {
            List<Choice> newChoices = new ArrayList<>();
            for (var c : s.choices()) {
                newChoices.add(new Choice(c.label(),
                        resolveBody(c.body(), currentName, env, defNames, expanding)));
            }
            return new Select(newChoices);
        }
        if (node instanceof Parallel p) {
            return new Parallel(
                    resolveBody(p.left(), currentName, env, defNames, expanding),
                    resolveBody(p.right(), currentName, env, defNames, expanding));
        }
        if (node instanceof Rec r) {
            Set<String> innerDefs = new HashSet<>(defNames);
            innerDefs.remove(r.var());
            return new Rec(r.var(),
                    resolveBody(r.body(), currentName, env, innerDefs, expanding));
        }
        if (node instanceof Sequence seq) {
            return new Sequence(
                    resolveBody(seq.left(), currentName, env, defNames, expanding),
                    resolveBody(seq.right(), currentName, env, defNames, expanding));
        }
        return node;
    }

    private static Set<String> freeDefNames(SessionType node, Set<String> defNames, Set<String> recBound) {
        if (node instanceof End) return Set.of();
        if (node instanceof Var v) {
            if (defNames.contains(v.name()) && !recBound.contains(v.name())) {
                return Set.of(v.name());
            }
            return Set.of();
        }
        if (node instanceof Branch b) {
            Set<String> result = new HashSet<>();
            for (var c : b.choices()) result.addAll(freeDefNames(c.body(), defNames, recBound));
            return result;
        }
        if (node instanceof Select s) {
            Set<String> result = new HashSet<>();
            for (var c : s.choices()) result.addAll(freeDefNames(c.body(), defNames, recBound));
            return result;
        }
        if (node instanceof Parallel p) {
            Set<String> result = new HashSet<>(freeDefNames(p.left(), defNames, recBound));
            result.addAll(freeDefNames(p.right(), defNames, recBound));
            return result;
        }
        if (node instanceof Rec r) {
            Set<String> newBound = new HashSet<>(recBound);
            newBound.add(r.var());
            return freeDefNames(r.body(), defNames, newBound);
        }
        if (node instanceof Sequence seq) {
            Set<String> result = new HashSet<>(freeDefNames(seq.left(), defNames, recBound));
            result.addAll(freeDefNames(seq.right(), defNames, recBound));
            return result;
        }
        return Set.of();
    }

    private static Set<String> union(Set<String> set, String element) {
        Set<String> result = new HashSet<>(set);
        result.add(element);
        return result;
    }
}
