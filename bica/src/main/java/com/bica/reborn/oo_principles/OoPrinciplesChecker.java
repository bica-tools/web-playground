package com.bica.reborn.oo_principles;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.morphism.Morphism;
import com.bica.reborn.morphism.MorphismChecker;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;
import com.bica.reborn.subtyping.SubtypingChecker;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Object-Oriented Principles as Lattice Theorems (Step 51b).
 *
 * <p>Maps the nine foundational OO principles onto lattice-theoretic operations
 * on session type state spaces:
 * <ul>
 *   <li>Encapsulation  &rarr; enabled_methods() hides internal structure</li>
 *   <li>Inheritance     &rarr; Gay-Hole subtyping + lattice embedding</li>
 *   <li>Polymorphism    &rarr; join (LUB) in the subtyping lattice</li>
 *   <li>Abstraction     &rarr; quotient lattice collapses internal detail</li>
 *   <li>SRP             &rarr; join-irreducibility of the protocol lattice</li>
 *   <li>OCP             &rarr; covariant width subtyping preserves existing branches</li>
 *   <li>LSP             &rarr; decidable via isSubtype() + constructive embedding</li>
 *   <li>ISP             &rarr; Birkhoff decomposition into join-irreducible components</li>
 *   <li>DIP             &rarr; Galois connection between layers</li>
 * </ul>
 *
 * <p>Static utility class; all methods are pure functions.
 */
public final class OoPrinciplesChecker {

    private OoPrinciplesChecker() {}

    // =========================================================================
    // Result records
    // =========================================================================

    /** Result of encapsulation (information hiding) analysis. */
    public record EncapsulationResult(
            int stateCount,
            Map<Integer, Set<String>> methodSets,
            boolean allStatesHaveMethods,
            boolean isEncapsulated) {}

    /** Result of inheritance (subtyping + embedding) check. */
    public record InheritanceResult(
            String parentType,
            String childType,
            boolean isSubtype,
            boolean hasEmbedding,
            boolean isValidInheritance,
            String reason) {}

    /** Result of polymorphic interface computation. */
    public record PolymorphismResult(
            List<String> typeStrings,
            Set<String> commonMethods,
            boolean hasCommonSupertype,
            String commonSupertype) {}

    /** Result of protocol abstraction via label removal. */
    public record AbstractionResult(
            int originalStates,
            int abstractStates,
            Set<String> removedLabels,
            boolean isValidAbstraction,
            double reductionRatio) {}

    /** Result of Single Responsibility Principle analysis. */
    public record SRPResult(
            int stateCount,
            Set<Integer> joinIrreducibles,
            int numResponsibilities,
            boolean isSingleResponsibility,
            Map<Integer, Set<String>> responsibilities) {}

    /** Result of Open/Closed Principle check. */
    public record OpenClosedResult(
            String originalType,
            String extendedType,
            Set<String> originalMethods,
            Set<String> extendedMethods,
            Set<String> newMethods,
            boolean preservesExisting,
            boolean isProperExtension,
            boolean satisfiesOcp) {}

    /** Result of Liskov Substitution Principle check. */
    public record LiskovResult(
            String baseType,
            String derivedType,
            boolean isSubtype,
            boolean hasEmbedding,
            boolean satisfiesLsp,
            String counterexample) {}

    /** A minimal interface extracted from a protocol lattice. */
    public record Interface(
            int state,
            Set<String> methods,
            String label) {}

    /** Result of Dependency Inversion Principle check. */
    public record DependencyInversionResult(
            int abstractStates,
            int concreteStates,
            boolean hasGaloisConnection,
            boolean satisfiesDip,
            String reason) {}

    /** Traffic-light report for all five SOLID principles. */
    public record SOLIDReport(
            SRPResult srp,
            OpenClosedResult ocp,
            LiskovResult lsp,
            List<Interface> isp,
            DependencyInversionResult dip,
            Map<String, String> summary) {}

    /** Complete OO principles analysis. */
    public record OOAnalysis(
            EncapsulationResult encapsulation,
            InheritanceResult inheritance,
            PolymorphismResult polymorphism,
            AbstractionResult abstraction,
            SOLIDReport solid) {}

    // =========================================================================
    // 1. Encapsulation: information hiding via enabled methods
    // =========================================================================

    /**
     * Verify that the state space properly hides internals.
     *
     * <p>Each state exposes only its enabled methods. The state space IS the
     * interface: you cannot see internal implementation, only the set of
     * methods available at each protocol state.
     */
    public static EncapsulationResult checkEncapsulation(StateSpace ss) {
        Map<Integer, Set<String>> methodSets = new HashMap<>();
        for (int state : ss.states()) {
            Set<String> labels = new HashSet<>();
            labels.addAll(ss.enabledMethods(state));
            labels.addAll(ss.enabledSelections(state));
            methodSets.put(state, Set.copyOf(labels));
        }

        boolean allHave = ss.states().stream()
                .filter(s -> s != ss.bottom())
                .allMatch(s -> !methodSets.get(s).isEmpty());

        boolean isEncapsulated = allHave || ss.states().size() == 1;

        return new EncapsulationResult(
                ss.states().size(),
                Map.copyOf(methodSets),
                allHave,
                isEncapsulated);
    }

    // =========================================================================
    // 2. Inheritance: subtype relationship
    // =========================================================================

    /**
     * Check whether {@code sChild} safely inherits from {@code sParent}.
     *
     * <p>Inheritance is valid iff:
     * <ol>
     *   <li>{@code sChild &le;_GH sParent} (Gay-Hole subtyping), AND</li>
     *   <li>{@code L(sParent)} order-embeds into {@code L(sChild)}.</li>
     * </ol>
     */
    public static InheritanceResult checkInheritance(SessionType sParent, SessionType sChild) {
        boolean sub = SubtypingChecker.isSubtype(sChild, sParent);

        boolean hasEmb;
        try {
            StateSpace ssParent = StateSpaceBuilder.build(sParent);
            StateSpace ssChild = StateSpaceBuilder.build(sChild);
            Morphism emb = MorphismChecker.findEmbedding(ssParent, ssChild);
            hasEmb = emb != null;
        } catch (Exception e) {
            hasEmb = false;
        }

        boolean valid = sub && hasEmb;
        String reason = null;
        if (!sub) {
            reason = "child is not a subtype of parent (Gay-Hole)";
        } else if (!hasEmb) {
            reason = "parent lattice does not embed into child lattice";
        }

        return new InheritanceResult(
                PrettyPrinter.pretty(sParent),
                PrettyPrinter.pretty(sChild),
                sub, hasEmb, valid, reason);
    }

    // =========================================================================
    // 3. Polymorphism: common supertype as join
    // =========================================================================

    /**
     * Compute the polymorphic interface shared by multiple types.
     */
    public static PolymorphismResult findPolymorphicInterface(List<SessionType> types) {
        if (types.isEmpty()) {
            return new PolymorphismResult(List.of(), Set.of(), false, null);
        }

        List<String> typeStrings = types.stream()
                .map(PrettyPrinter::pretty)
                .toList();

        List<Set<String>> methodSets = types.stream()
                .map(OoPrinciplesChecker::topLevelMethods)
                .toList();

        Set<String> common = new HashSet<>(methodSets.getFirst());
        for (int i = 1; i < methodSets.size(); i++) {
            common.retainAll(methodSets.get(i));
        }

        boolean hasCommon = !common.isEmpty();

        String supertype = null;
        if (hasCommon) {
            supertype = "&{" + common.stream().sorted()
                    .map(m -> m + ": end")
                    .collect(Collectors.joining(", ")) + "}";
        }

        return new PolymorphismResult(typeStrings, Set.copyOf(common), hasCommon, supertype);
    }

    /** Extract top-level method names from a session type. */
    static Set<String> topLevelMethods(SessionType t) {
        if (t instanceof Branch b) {
            return b.choices().stream().map(Choice::label).collect(Collectors.toSet());
        } else if (t instanceof Select s) {
            return s.choices().stream().map(Choice::label).collect(Collectors.toSet());
        } else if (t instanceof Rec r) {
            return topLevelMethods(r.body());
        } else if (t instanceof End) {
            return Set.of();
        }
        return Set.of();
    }

    // =========================================================================
    // 4. Abstraction: quotient lattice via label removal
    // =========================================================================

    /**
     * Create an abstract view of a protocol by removing detail labels.
     */
    public static AbstractionResult abstractProtocol(StateSpace ss, Set<String> detailLabels) {
        if (detailLabels.isEmpty()) {
            return new AbstractionResult(
                    ss.states().size(), ss.states().size(),
                    Set.of(), true, 1.0);
        }

        // Filter transitions
        var abstractTransitions = ss.transitions().stream()
                .filter(t -> !detailLabels.contains(t.label()))
                .toList();

        // Build adjacency for reachability
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new HashSet<>());
        for (var t : abstractTransitions) {
            adj.computeIfAbsent(t.source(), k -> new HashSet<>()).add(t.target());
        }

        // Compute reachable from top
        Set<Integer> reachable = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(ss.top());
        while (!stack.isEmpty()) {
            int s = stack.pop();
            if (reachable.add(s)) {
                for (int tgt : adj.getOrDefault(s, Set.of())) {
                    stack.push(tgt);
                }
            }
        }
        reachable.add(ss.bottom());

        // Build abstract state space
        var filteredTrans = abstractTransitions.stream()
                .filter(t -> reachable.contains(t.source()) && reachable.contains(t.target()))
                .toList();

        Map<Integer, String> labels = new HashMap<>();
        for (int s : reachable) {
            labels.put(s, ss.labels().getOrDefault(s, ""));
        }

        StateSpace abstractSs = new StateSpace(
                reachable, filteredTrans, ss.top(), ss.bottom(), labels);

        boolean isLattice = LatticeChecker.checkLattice(abstractSs).isLattice();

        double ratio = ss.states().isEmpty() ? 1.0
                : (double) reachable.size() / ss.states().size();

        return new AbstractionResult(
                ss.states().size(), reachable.size(),
                Set.copyOf(detailLabels), isLattice, ratio);
    }

    // =========================================================================
    // 5. Single Responsibility: join-irreducibility
    // =========================================================================

    /**
     * Find join-irreducible elements of the state-space lattice.
     */
    static Map<Integer, Set<String>> computeJoinIrreducibles(StateSpace ss) {
        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) reach.put(s, ss.reachableFrom(s));

        // Build covers and covered-by
        Map<Integer, Set<Integer>> covers = new HashMap<>();
        Map<Integer, Set<Integer>> coveredBy = new HashMap<>();
        for (int s : ss.states()) {
            covers.put(s, new HashSet<>());
            coveredBy.put(s, new HashSet<>());
        }

        for (int s : ss.states()) {
            for (int t : reach.get(s)) {
                if (t == s) continue;
                boolean isCover = true;
                for (int u : reach.get(s)) {
                    if (u == s || u == t) continue;
                    if (reach.get(u).contains(t)) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) {
                    covers.get(s).add(t);
                    coveredBy.get(t).add(s);
                }
            }
        }

        // Join-irreducible: exactly one element covers it
        Map<Integer, Set<String>> joinIrr = new LinkedHashMap<>();
        for (int s : ss.states()) {
            if (s == ss.top()) continue;
            if (coveredBy.get(s).size() == 1) {
                Set<String> methods = new HashSet<>();
                methods.addAll(ss.enabledMethods(s));
                methods.addAll(ss.enabledSelections(s));
                joinIrr.put(s, Set.copyOf(methods));
            }
        }
        return joinIrr;
    }

    /**
     * Check the Single Responsibility Principle.
     */
    public static SRPResult checkSingleResponsibility(StateSpace ss) {
        Map<Integer, Set<String>> joinIrr = computeJoinIrreducibles(ss);

        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) reach.put(s, ss.reachableFrom(s));

        // Count top-level responsibilities (states directly covered by top)
        Set<Integer> topChildren = new HashSet<>();
        for (int s : ss.states()) {
            if (s == ss.top() || s == ss.bottom()) continue;
            if (!reach.get(ss.top()).contains(s)) continue;
            boolean isDirect = true;
            for (int u : reach.get(ss.top())) {
                if (u == ss.top() || u == s) continue;
                if (reach.get(u).contains(s)) {
                    isDirect = false;
                    break;
                }
            }
            if (isDirect) topChildren.add(s);
        }

        int numResp = ss.states().size() > 1 ? Math.max(topChildren.size(), 1) : 1;

        return new SRPResult(
                ss.states().size(),
                Set.copyOf(joinIrr.keySet()),
                numResp,
                numResp <= 1,
                Map.copyOf(joinIrr));
    }

    // =========================================================================
    // 6. Open/Closed: extend without modifying
    // =========================================================================

    /**
     * Check the Open/Closed Principle.
     */
    public static OpenClosedResult checkOpenClosed(SessionType sOriginal, SessionType sExtended) {
        Set<String> origMethods = topLevelMethods(sOriginal);
        Set<String> extMethods = topLevelMethods(sExtended);

        boolean preserves = extMethods.containsAll(origMethods);
        Set<String> newMethods = new HashSet<>(extMethods);
        newMethods.removeAll(origMethods);
        boolean isProper = !newMethods.isEmpty();
        boolean satisfies = preserves && isProper;

        return new OpenClosedResult(
                PrettyPrinter.pretty(sOriginal),
                PrettyPrinter.pretty(sExtended),
                Set.copyOf(origMethods),
                Set.copyOf(extMethods),
                Set.copyOf(newMethods),
                preserves, isProper, satisfies);
    }

    // =========================================================================
    // 7. Liskov Substitution: decidable via subtyping + embedding
    // =========================================================================

    /**
     * Check the Liskov Substitution Principle.
     */
    public static LiskovResult checkLiskov(SessionType sBase, SessionType sDerived) {
        boolean sub = SubtypingChecker.isSubtype(sDerived, sBase);

        boolean hasEmb;
        try {
            StateSpace ssBase = StateSpaceBuilder.build(sBase);
            StateSpace ssDerived = StateSpaceBuilder.build(sDerived);
            Morphism emb = MorphismChecker.findEmbedding(ssBase, ssDerived);
            hasEmb = emb != null;
        } catch (Exception e) {
            hasEmb = false;
        }

        String counterexample = null;
        if (!sub) {
            Set<String> baseMethods = topLevelMethods(sBase);
            Set<String> derivedMethods = topLevelMethods(sDerived);
            Set<String> missing = new TreeSet<>(baseMethods);
            missing.removeAll(derivedMethods);
            if (!missing.isEmpty()) {
                counterexample = "derived type is missing methods: " + new ArrayList<>(missing);
            } else {
                counterexample = "incompatible continuation types";
            }
        }

        return new LiskovResult(
                PrettyPrinter.pretty(sBase),
                PrettyPrinter.pretty(sDerived),
                sub, hasEmb, sub, counterexample);
    }

    // =========================================================================
    // 8. Interface Segregation: Birkhoff decomposition
    // =========================================================================

    /**
     * Decompose a protocol into minimal interfaces.
     */
    public static List<Interface> segregateInterfaces(StateSpace ss) {
        Map<Integer, Set<String>> joinIrr = computeJoinIrreducibles(ss);

        List<Interface> interfaces = new ArrayList<>();
        var sortedEntries = new ArrayList<>(joinIrr.entrySet());
        sortedEntries.sort(Comparator.comparingInt(Map.Entry::getKey));

        for (var entry : sortedEntries) {
            String label = ss.labels().getOrDefault(entry.getKey(), "state_" + entry.getKey());
            interfaces.add(new Interface(entry.getKey(), entry.getValue(), label));
        }

        if (interfaces.isEmpty() && ss.states().size() > 1) {
            Set<String> allMethods = new HashSet<>();
            for (int s : ss.states()) {
                allMethods.addAll(ss.enabledMethods(s));
                allMethods.addAll(ss.enabledSelections(s));
            }
            interfaces.add(new Interface(ss.top(), Set.copyOf(allMethods), "monolithic"));
        }

        return List.copyOf(interfaces);
    }

    // =========================================================================
    // 9. Dependency Inversion: Galois connection
    // =========================================================================

    /**
     * Check the Dependency Inversion Principle.
     */
    public static DependencyInversionResult checkDependencyInversion(
            StateSpace ssAbstract, StateSpace ssConcrete) {

        Morphism emb = MorphismChecker.findEmbedding(ssAbstract, ssConcrete);
        if (emb == null) {
            return new DependencyInversionResult(
                    ssAbstract.states().size(),
                    ssConcrete.states().size(),
                    false, false,
                    "abstract lattice does not embed into concrete lattice");
        }

        // gamma = embedding mapping (abstract -> concrete)
        Map<Integer, Integer> gamma = emb.mapping();

        // Build alpha: for each concrete state, find closest abstract state
        Map<Integer, Set<Integer>> concreteReach = new HashMap<>();
        for (int s : ssConcrete.states()) {
            concreteReach.put(s, ssConcrete.reachableFrom(s));
        }

        Map<Integer, Integer> alpha = new HashMap<>();
        for (int c : ssConcrete.states()) {
            Integer bestAbstract = null;
            Integer bestConcrete = null;
            for (var entry : gamma.entrySet()) {
                int a = entry.getKey();
                int gc = entry.getValue();
                if (concreteReach.get(c).contains(gc)) {
                    if (bestConcrete == null
                            || (concreteReach.get(gc).contains(bestConcrete)
                                && gc != bestConcrete)) {
                        bestAbstract = a;
                        bestConcrete = gc;
                    }
                }
            }
            alpha.put(c, bestAbstract != null ? bestAbstract : ssAbstract.top());
        }

        boolean isGc;
        if (alpha.size() == ssConcrete.states().size()
                && gamma.size() == ssAbstract.states().size()) {
            try {
                isGc = MorphismChecker.isGaloisConnection(alpha, gamma, ssConcrete, ssAbstract);
            } catch (Exception e) {
                isGc = false;
            }
        } else {
            isGc = false;
        }

        return new DependencyInversionResult(
                ssAbstract.states().size(),
                ssConcrete.states().size(),
                isGc, isGc,
                isGc ? null : "Galois connection conditions not satisfied");
    }

    // =========================================================================
    // SOLID check: all five principles
    // =========================================================================

    /**
     * Run all five SOLID principle checks.
     *
     * @param ss         the state space to check
     * @param sType      the session type (for OCP, LSP checks); may be null
     * @param sExtended  extended type (for OCP check); may be null
     * @param sBase      base type (for LSP check); may be null
     * @param ssAbstract abstract state space (for DIP check); may be null
     */
    public static SOLIDReport solidCheck(
            StateSpace ss,
            SessionType sType,
            SessionType sExtended,
            SessionType sBase,
            StateSpace ssAbstract) {

        SRPResult srp = checkSingleResponsibility(ss);

        OpenClosedResult ocp = null;
        if (sType != null && sExtended != null) {
            ocp = checkOpenClosed(sType, sExtended);
        }

        LiskovResult lsp = null;
        if (sType != null && sBase != null) {
            lsp = checkLiskov(sBase, sType);
        }

        List<Interface> isp = segregateInterfaces(ss);

        DependencyInversionResult dip = null;
        if (ssAbstract != null) {
            dip = checkDependencyInversion(ssAbstract, ss);
        }

        // Traffic light summary
        Map<String, String> summary = new LinkedHashMap<>();

        if (srp.isSingleResponsibility()) {
            summary.put("SRP", "green");
        } else if (srp.numResponsibilities() <= 3) {
            summary.put("SRP", "yellow");
        } else {
            summary.put("SRP", "red");
        }

        summary.put("OCP", ocp != null
                ? (ocp.satisfiesOcp() ? "green" : "red")
                : "yellow");

        summary.put("LSP", lsp != null
                ? (lsp.satisfiesLsp() ? "green" : "red")
                : "yellow");

        if (isp.size() > 1) {
            summary.put("ISP", "green");
        } else if (isp.size() == 1 && !"monolithic".equals(isp.getFirst().label())) {
            summary.put("ISP", "green");
        } else if (isp.size() == 1) {
            summary.put("ISP", "yellow");
        } else {
            summary.put("ISP", "red");
        }

        summary.put("DIP", dip != null
                ? (dip.satisfiesDip() ? "green" : "red")
                : "yellow");

        return new SOLIDReport(srp, ocp, lsp, isp, dip, Map.copyOf(summary));
    }

    /** Convenience overload with only the state space. */
    public static SOLIDReport solidCheck(StateSpace ss) {
        return solidCheck(ss, null, null, null, null);
    }

    // =========================================================================
    // Full OO analysis
    // =========================================================================

    /**
     * Run complete OO principles analysis on a session type state space.
     */
    public static OOAnalysis analyzeOoPrinciples(
            StateSpace ss,
            SessionType sType,
            SessionType sParent,
            SessionType sChild,
            List<SessionType> peerTypes,
            Set<String> detailLabels,
            SessionType sExtended,
            SessionType sBase,
            StateSpace ssAbstract) {

        EncapsulationResult encap = checkEncapsulation(ss);

        InheritanceResult inherit = null;
        if (sParent != null && sChild != null) {
            inherit = checkInheritance(sParent, sChild);
        }

        PolymorphismResult poly = null;
        if (peerTypes != null) {
            poly = findPolymorphicInterface(peerTypes);
        }

        AbstractionResult abstr = null;
        if (detailLabels != null) {
            abstr = abstractProtocol(ss, detailLabels);
        }

        SOLIDReport solid = solidCheck(ss, sType, sExtended, sBase, ssAbstract);

        return new OOAnalysis(encap, inherit, poly, abstr, solid);
    }

    /** Convenience overload with only the state space. */
    public static OOAnalysis analyzeOoPrinciples(StateSpace ss) {
        return analyzeOoPrinciples(ss, null, null, null, null, null, null, null, null);
    }
}
