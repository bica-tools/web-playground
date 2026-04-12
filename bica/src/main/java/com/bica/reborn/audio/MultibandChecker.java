package com.bica.reborn.audio;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.lattice.*;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.*;

/**
 * Multiband audio processing as product lattice.
 *
 * <p>Models multiband audio processing (crossover filters splitting a signal into
 * frequency bands processed independently) as parallel session types whose state
 * spaces form product lattices.
 *
 * <p>Lattice operations acquire spectral meaning:
 * <ul>
 *   <li>Meet = shared spectral content (both configurations active)</li>
 *   <li>Join = combined spectrum (either configuration active)</li>
 *   <li>Distributivity = band independence (always holds for independent bands)</li>
 * </ul>
 */
public final class MultibandChecker {

    private MultibandChecker() {}

    // -----------------------------------------------------------------------
    // Session type construction
    // -----------------------------------------------------------------------

    /**
     * Relabel a processor's session type to make labels band-unique.
     * Prefixes each label with the band name for WF-Par disjointness.
     */
    static String relabelProcessor(String processor, String bandName) {
        SessionType ast = Parser.parse(processor);
        SessionType relabeled = relabel(ast, bandName);
        return PrettyPrinter.pretty(relabeled);
    }

    private static SessionType relabel(SessionType node, String prefix) {
        return switch (node) {
            case End e -> e;
            case Var v -> new Var(prefix + "_" + v.name());
            case Branch b -> new Branch(b.choices().stream()
                    .map(c -> new Choice(prefix + "_" + c.label(),
                            relabel(c.body(), prefix)))
                    .toList());
            case Select s -> new Select(s.choices().stream()
                    .map(c -> new Choice(prefix + "_" + c.label(),
                            relabel(c.body(), prefix)))
                    .toList());
            case Rec r -> new Rec(prefix + "_" + r.var(), relabel(r.body(), prefix));
            case Parallel p -> new Parallel(
                    relabel(p.left(), prefix), relabel(p.right(), prefix));
            case Sequence seq -> new Sequence(
                    relabel(seq.left(), prefix), relabel(seq.right(), prefix));
            case Chain ch -> new Chain(
                    prefix + "_" + ch.method(), relabel(ch.cont(), prefix));
        };
    }

    /**
     * Convert frequency bands to a parallel session type.
     *
     * <p>Each band's processor becomes one branch of the parallel composition,
     * with labels prefixed by the band name for disjointness.
     */
    public static String multibandToSessionType(List<FrequencyBand> bands) {
        if (bands.size() < 2) {
            throw new IllegalArgumentException(
                    "Multiband config requires at least 2 bands, got " + bands.size());
        }
        // Verify bands are contiguous
        for (int i = 0; i < bands.size() - 1; i++) {
            if (bands.get(i).highHz() != bands.get(i + 1).lowHz()) {
                throw new IllegalArgumentException(
                        "Band gap between '" + bands.get(i).name()
                        + "' (high=" + bands.get(i).highHz() + ") and '"
                        + bands.get(i + 1).name()
                        + "' (low=" + bands.get(i + 1).lowHz() + ")");
            }
        }

        List<String> relabeled = new ArrayList<>();
        for (FrequencyBand band : bands) {
            relabeled.add(relabelProcessor(band.processor(), band.name()));
        }

        if (relabeled.size() == 2) {
            return "(" + relabeled.get(0) + " || " + relabeled.get(1) + ")";
        }

        String par = "(" + relabeled.get(0) + " || " + relabeled.get(1) + ")";
        for (int i = 2; i < relabeled.size(); i++) {
            par = "(" + par + " || " + relabeled.get(i) + ")";
        }
        return par;
    }

    // -----------------------------------------------------------------------
    // Spectral meet and join
    // -----------------------------------------------------------------------

    /**
     * Compute the spectral meet of two configurations.
     * The meet represents the shared spectral content.
     */
    public static Integer spectralMeet(StateSpace ss, int state1, int state2) {
        return LatticeChecker.computeMeet(ss, state1, state2);
    }

    /**
     * Compute the spectral join of two configurations.
     * The join represents the combined spectrum.
     */
    public static Integer spectralJoin(StateSpace ss, int state1, int state2) {
        return LatticeChecker.computeJoin(ss, state1, state2);
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    /**
     * Analyze a multiband processor as a product lattice.
     */
    public static MultibandResult analyzeMultiband(List<FrequencyBand> bands) {
        String typeStr = multibandToSessionType(bands);
        SessionType ast = Parser.parse(typeStr);
        StateSpace ss = StateSpaceBuilder.build(ast);

        LatticeResult lr = LatticeChecker.checkLattice(ss);
        DistributivityResult dr = DistributivityChecker.checkDistributive(ss);

        // Compute individual band sizes
        List<Integer> bandSizes = new ArrayList<>();
        for (FrequencyBand band : bands) {
            String bandType = relabelProcessor(band.processor(), band.name());
            StateSpace bandSs = StateSpaceBuilder.build(Parser.parse(bandType));
            bandSizes.add(bandSs.states().size());
        }

        int totalIndividual = bandSizes.stream().mapToInt(Integer::intValue).sum();
        double compression = totalIndividual > 0
                ? (double) ss.states().size() / totalIndividual
                : 1.0;

        return new MultibandResult(
                bands, typeStr, ss,
                ss.states().size(), ss.transitions().size(),
                lr.isLattice(), dr.isDistributive(),
                dr.classification(), bands.size(),
                compression, bandSizes, lr);
    }

    // -----------------------------------------------------------------------
    // Preset configurations
    // -----------------------------------------------------------------------

    /** Standard 4-band mastering configuration. */
    public static List<FrequencyBand> standard4Band() {
        return List.of(
                new FrequencyBand("sub", 20.0, 200.0,
                        "&{analyze: &{compress: &{makeup: end}}}"),
                new FrequencyBand("lomid", 200.0, 2000.0,
                        "&{analyze: &{compress: &{makeup: end}}}"),
                new FrequencyBand("himid", 2000.0, 8000.0,
                        "&{analyze: &{compress: &{makeup: end}}}"),
                new FrequencyBand("high", 8000.0, 20000.0,
                        "&{analyze: &{compress: &{makeup: end}}}"));
    }

    /** Professional 5-band mastering configuration. */
    public static List<FrequencyBand> mastering5Band() {
        return List.of(
                new FrequencyBand("sub", 20.0, 80.0, "&{eq: &{compress: end}}"),
                new FrequencyBand("bass", 80.0, 300.0, "&{eq: &{compress: end}}"),
                new FrequencyBand("mid", 300.0, 3000.0, "&{eq: &{compress: end}}"),
                new FrequencyBand("presence", 3000.0, 8000.0, "&{eq: &{compress: end}}"),
                new FrequencyBand("air", 8000.0, 20000.0, "&{eq: &{compress: end}}"));
    }

    /** De-esser as a 2-band processor. */
    public static List<FrequencyBand> deEsser2Band() {
        return List.of(
                new FrequencyBand("body", 20.0, 5000.0, "&{bypass: end}"),
                new FrequencyBand("sibilance", 5000.0, 20000.0,
                        "&{detect: &{compress: end}}"));
    }
}
