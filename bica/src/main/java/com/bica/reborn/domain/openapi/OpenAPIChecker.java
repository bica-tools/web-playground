package com.bica.reborn.domain.openapi;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stateful API contracts via OpenAPI extensions.
 *
 * <p>Models REST API lifecycles as session types and validates that sequences
 * of API calls conform to the protocol defined by the session type.
 *
 * <p>Ported from {@code reticulate/reticulate/openapi.py} (Step 71).
 */
public final class OpenAPIChecker {

    private OpenAPIChecker() {}

    // -----------------------------------------------------------------------
    // Data types
    // -----------------------------------------------------------------------

    /**
     * A single REST API endpoint with state constraints.
     *
     * @param method             HTTP method (GET, POST, PUT, PATCH, DELETE).
     * @param path               URL path pattern (e.g. /orders/{id}).
     * @param preconditionState  State required before this call.
     * @param postconditionState State after successful call.
     * @param description        Human-readable description.
     * @param responseCodes      Expected HTTP status codes.
     */
    public record OpenAPIEndpoint(
            String method,
            String path,
            String preconditionState,
            String postconditionState,
            String description,
            List<Integer> responseCodes) {

        public OpenAPIEndpoint(String method, String path,
                               String preconditionState, String postconditionState,
                               String description) {
            this(method, path, preconditionState, postconditionState, description, List.of(200));
        }

        public OpenAPIEndpoint(String method, String path,
                               String preconditionState, String postconditionState) {
            this(method, path, preconditionState, postconditionState, "", List.of(200));
        }
    }

    /**
     * Result of validating an API call trace against a session type.
     */
    public record TraceValidationResult(
            boolean valid,
            int stepsCompleted,
            int violationIndex,
            String violationMessage,
            String finalState) {

        public static TraceValidationResult success(int steps, String finalState) {
            return new TraceValidationResult(true, steps, -1, "", finalState);
        }

        public static TraceValidationResult failure(int steps, int violationIdx,
                                                     String message, String finalState) {
            return new TraceValidationResult(false, steps, violationIdx, message, finalState);
        }
    }

    /**
     * Complete stateful API contract.
     */
    public record APIContract(
            List<OpenAPIEndpoint> endpoints,
            SessionType sessionType,
            String sessionTypeString,
            Map<String, List<Map.Entry<String, String>>> stateMachine,
            List<String> states,
            String initialState,
            List<String> terminalStates,
            StateSpace stateSpace,
            LatticeResult latticeResult) {}

    // -----------------------------------------------------------------------
    // State machine construction
    // -----------------------------------------------------------------------

    public static String sanitizeLabel(String label) {
        String out = label.replace(" ", "_").replace("/", "_")
                .replace("{", "").replace("}", "");
        while (out.contains("__")) {
            out = out.replace("__", "_");
        }
        if (out.startsWith("_")) out = out.substring(1);
        if (out.endsWith("_")) out = out.substring(0, out.length() - 1);
        return out.toLowerCase();
    }

    static boolean stateReachableFrom(Map<String, List<Map.Entry<String, String>>> sm,
                                       String start, String target) {
        Set<String> visited = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        for (var entry : sm.getOrDefault(start, List.of())) {
            stack.push(entry.getValue());
        }
        while (!stack.isEmpty()) {
            String s = stack.pop();
            if (s.equals(target)) return true;
            if (visited.contains(s)) continue;
            visited.add(s);
            for (var entry : sm.getOrDefault(s, List.of())) {
                stack.push(entry.getValue());
            }
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Convert a list of API endpoints to a session type string.
     */
    public static String apiToSessionTypeString(List<OpenAPIEndpoint> endpoints) {
        if (endpoints.isEmpty()) return "end";

        // Build state machine
        Map<String, List<Map.Entry<String, String>>> sm = new LinkedHashMap<>();
        Set<String> allPre = new LinkedHashSet<>();
        Set<String> allPost = new LinkedHashSet<>();

        for (var ep : endpoints) {
            String label = ep.method() + " " + ep.path();
            sm.computeIfAbsent(ep.preconditionState(), k -> new ArrayList<>())
              .add(Map.entry(label, ep.postconditionState()));
            allPre.add(ep.preconditionState());
            allPost.add(ep.postconditionState());
        }

        Set<String> allStates = new LinkedHashSet<>(allPre);
        allStates.addAll(allPost);

        // Initial state
        Set<String> initialCandidates = new TreeSet<>(allPre);
        initialCandidates.removeAll(allPost);
        String initial = initialCandidates.isEmpty()
                ? endpoints.get(0).preconditionState()
                : initialCandidates.iterator().next();

        // Terminal states
        List<String> terminal = allStates.stream()
                .filter(s -> !sm.containsKey(s) || sm.get(s).isEmpty())
                .sorted()
                .toList();

        // Ensure all states in sm
        for (String s : allStates) {
            sm.computeIfAbsent(s, k -> new ArrayList<>());
        }

        return buildTypeString(sm, initial, new HashSet<>(terminal));
    }

    private static String buildTypeString(Map<String, List<Map.Entry<String, String>>> sm,
                                           String state, Set<String> terminal) {
        return buildTypeStringRec(sm, state, terminal, new HashSet<>());
    }

    private static String buildTypeStringRec(Map<String, List<Map.Entry<String, String>>> sm,
                                              String state, Set<String> terminal,
                                              Set<String> inProgress) {
        if (terminal.contains(state) || !sm.containsKey(state) || sm.get(state).isEmpty()) {
            return "end";
        }
        if (inProgress.contains(state)) {
            List<String> keys = new ArrayList<>(sm.keySet());
            Collections.sort(keys);
            int idx = keys.indexOf(state);
            return idx <= 0 ? "X" : "X" + idx;
        }

        Set<String> nextInProgress = new HashSet<>(inProgress);
        nextInProgress.add(state);

        List<Map.Entry<String, String>> transitions = sm.get(state);
        List<String> choices = new ArrayList<>();
        for (var entry : transitions) {
            String method = sanitizeLabel(entry.getKey());
            String cont = buildTypeStringRec(sm, entry.getValue(), terminal, nextInProgress);
            choices.add(method + ": " + cont);
        }

        String body = "&{" + String.join(", ", choices) + "}";

        if (stateReachableFrom(sm, state, state)) {
            List<String> keys = new ArrayList<>(sm.keySet());
            Collections.sort(keys);
            int idx = keys.indexOf(state);
            String var = idx <= 0 ? "X" : "X" + idx;
            return "rec " + var + " . " + body;
        }
        return body;
    }

    /**
     * Build a full API contract from endpoint definitions.
     */
    public static APIContract apiToContract(List<OpenAPIEndpoint> endpoints) {
        String typeStr = apiToSessionTypeString(endpoints);
        SessionType ast = Parser.parse(typeStr);
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);

        // Build state machine for trace validation
        Map<String, List<Map.Entry<String, String>>> sm = new LinkedHashMap<>();
        Set<String> allPre = new LinkedHashSet<>();
        Set<String> allPost = new LinkedHashSet<>();
        for (var ep : endpoints) {
            String label = ep.method() + " " + ep.path();
            sm.computeIfAbsent(ep.preconditionState(), k -> new ArrayList<>())
              .add(Map.entry(label, ep.postconditionState()));
            allPre.add(ep.preconditionState());
            allPost.add(ep.postconditionState());
        }
        Set<String> allStates = new TreeSet<>(allPre);
        allStates.addAll(allPost);
        for (String s : allStates) sm.computeIfAbsent(s, k -> new ArrayList<>());

        Set<String> initialCandidates = new TreeSet<>(allPre);
        initialCandidates.removeAll(allPost);
        String initial = initialCandidates.isEmpty()
                ? endpoints.get(0).preconditionState()
                : initialCandidates.iterator().next();

        List<String> terminal = allStates.stream()
                .filter(s -> !sm.containsKey(s) || sm.get(s).isEmpty())
                .sorted().toList();

        return new APIContract(
                List.copyOf(endpoints), ast, typeStr, sm,
                new ArrayList<>(allStates), initial, terminal, ss, lr);
    }

    /**
     * Validate a sequence of API calls against a contract.
     */
    public static TraceValidationResult validateApiTrace(APIContract contract, List<String> trace) {
        if (trace.isEmpty()) {
            return TraceValidationResult.success(0, contract.initialState());
        }

        String currentState = contract.initialState();
        var sm = contract.stateMachine();

        for (int i = 0; i < trace.size(); i++) {
            String call = trace.get(i);
            var transitions = sm.getOrDefault(currentState, List.of());
            boolean matched = false;
            for (var entry : transitions) {
                if (entry.getKey().equals(call)) {
                    currentState = entry.getValue();
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                List<String> available = transitions.stream()
                        .map(Map.Entry::getKey).toList();
                return TraceValidationResult.failure(i, i,
                        "Call '" + call + "' not allowed in state '" + currentState
                                + "'. Available: " + available,
                        currentState);
            }
        }
        return TraceValidationResult.success(trace.size(), currentState);
    }

    // -----------------------------------------------------------------------
    // Common API patterns
    // -----------------------------------------------------------------------

    /** Generate CRUD lifecycle endpoints for a resource. */
    public static List<OpenAPIEndpoint> crudLifecycle(String resource) {
        String base = "/" + resource;
        String item = "/" + resource + "/{id}";
        return List.of(
                new OpenAPIEndpoint("POST", base, "init", "created", "Create a new " + resource),
                new OpenAPIEndpoint("GET", item, "created", "created", "Read " + resource + " details"),
                new OpenAPIEndpoint("PUT", item, "created", "updated", "Update " + resource),
                new OpenAPIEndpoint("GET", item, "updated", "updated", "Read updated " + resource),
                new OpenAPIEndpoint("DELETE", item, "updated", "deleted", "Delete " + resource),
                new OpenAPIEndpoint("DELETE", item, "created", "deleted", "Delete " + resource + " without update")
        );
    }

    /** Generate OAuth2-like authentication flow endpoints. */
    public static List<OpenAPIEndpoint> authFlow() {
        return List.of(
                new OpenAPIEndpoint("POST", "/auth/token", "unauthenticated", "authenticated", "Request access token"),
                new OpenAPIEndpoint("POST", "/auth/refresh", "authenticated", "authenticated", "Refresh access token"),
                new OpenAPIEndpoint("GET", "/api/resource", "authenticated", "authenticated", "Access protected resource"),
                new OpenAPIEndpoint("POST", "/auth/revoke", "authenticated", "revoked", "Revoke token")
        );
    }

    /** Generate payment processing flow endpoints. */
    public static List<OpenAPIEndpoint> paymentFlow() {
        return List.of(
                new OpenAPIEndpoint("POST", "/payments", "init", "created", "Create payment intent"),
                new OpenAPIEndpoint("POST", "/payments/{id}/authorize", "created", "authorized", "Authorize payment"),
                new OpenAPIEndpoint("POST", "/payments/{id}/capture", "authorized", "captured", "Capture authorized payment"),
                new OpenAPIEndpoint("POST", "/payments/{id}/settle", "captured", "settled", "Settle captured payment"),
                new OpenAPIEndpoint("POST", "/payments/{id}/void", "authorized", "voided", "Void authorized payment"),
                new OpenAPIEndpoint("POST", "/payments/{id}/cancel", "created", "cancelled", "Cancel payment before authorization")
        );
    }

    /** Generate order fulfillment flow endpoints. */
    public static List<OpenAPIEndpoint> orderFulfillmentFlow() {
        return List.of(
                new OpenAPIEndpoint("POST", "/orders", "cart", "placed", "Place order"),
                new OpenAPIEndpoint("POST", "/orders/{id}/pay", "placed", "paid", "Process payment"),
                new OpenAPIEndpoint("POST", "/orders/{id}/ship", "paid", "shipped", "Ship order"),
                new OpenAPIEndpoint("POST", "/orders/{id}/deliver", "shipped", "delivered", "Confirm delivery"),
                new OpenAPIEndpoint("POST", "/orders/{id}/return", "delivered", "returned", "Initiate return"),
                new OpenAPIEndpoint("POST", "/orders/{id}/cancel", "placed", "cancelled", "Cancel unpaid order")
        );
    }
}
