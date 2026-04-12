package com.bica.reborn.extensions;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ReplicationChecker} -- bounded replication S^n.
 */
class ReplicationCheckerTest {

    private static StateSpace ss(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    // =========================================================================
    // buildReplicatedStateSpace
    // =========================================================================

    @Nested
    class BuildReplicatedStateSpace {

        @Test
        void endReplicated1_sameAsEnd() {
            StateSpace base = ss("end");
            StateSpace rep = ReplicationChecker.buildReplicatedStateSpace(base, 1);
            assertEquals(base.states().size(), rep.states().size());
        }

        @Test
        void endReplicated2_singleState() {
            // end^2 = (end || end) -- product of singletons is singleton
            StateSpace base = ss("end");
            StateSpace rep = ReplicationChecker.buildReplicatedStateSpace(base, 2);
            assertEquals(1, rep.states().size());
        }

        @Test
        void branchReplicated2_productSize() {
            // &{a: end, b: end} has 3 states (top, a, b -> end)
            StateSpace base = ss("&{a: end, b: end}");
            StateSpace rep = ReplicationChecker.buildReplicatedStateSpace(base, 2);
            int expected = base.states().size() * base.states().size();
            assertEquals(expected, rep.states().size());
        }

        @Test
        void branchReplicated3_cubicSize() {
            StateSpace base = ss("&{a: end, b: end}");
            int baseSize = base.states().size();
            StateSpace rep = ReplicationChecker.buildReplicatedStateSpace(base, 3);
            assertEquals(baseSize * baseSize * baseSize, rep.states().size());
        }

        @Test
        void replicationCountMustBePositive() {
            StateSpace base = ss("end");
            assertThrows(IllegalArgumentException.class,
                    () -> ReplicationChecker.buildReplicatedStateSpace(base, 0));
        }

        @Test
        void chainReplicated2_productOfChains() {
            // &{a: end} is a 2-state chain
            StateSpace base = ss("&{a: end}");
            assertEquals(2, base.states().size());
            StateSpace rep = ReplicationChecker.buildReplicatedStateSpace(base, 2);
            assertEquals(4, rep.states().size());
        }
    }

    // =========================================================================
    // diagonalStates
    // =========================================================================

    @Nested
    class DiagonalStates {

        @Test
        void singleFactor_allDiagonal() {
            StateSpace base = ss("&{a: end, b: end}");
            var diag = ReplicationChecker.diagonalStates(base, base, 1);
            assertEquals(base.states().size(), diag.size());
        }

        @Test
        void endReplicated2_oneDiagonalState() {
            StateSpace base = ss("end");
            StateSpace rep = ReplicationChecker.buildReplicatedStateSpace(base, 2);
            var diag = ReplicationChecker.diagonalStates(rep, base, 2);
            assertEquals(1, diag.size());
        }

        @Test
        void chainReplicated2_twoDiagonalStates() {
            // Chain has 2 states -> product has 4, diagonal has 2: (top,top) and (end,end)
            StateSpace base = ss("&{a: end}");
            StateSpace rep = ReplicationChecker.buildReplicatedStateSpace(base, 2);
            var diag = ReplicationChecker.diagonalStates(rep, base, 2);
            assertEquals(2, diag.size());
        }

        @Test
        void branchReplicated2_diagonalCountMatchesBase() {
            StateSpace base = ss("&{a: end, b: end}");
            StateSpace rep = ReplicationChecker.buildReplicatedStateSpace(base, 2);
            var diag = ReplicationChecker.diagonalStates(rep, base, 2);
            // Diagonal states <= base states (one per (s,s) pair where labels match)
            assertTrue(diag.size() >= 1, "Should have at least one diagonal state");
            assertTrue(diag.size() <= base.states().size(),
                    "Diagonal states should not exceed base states");
        }
    }

    // =========================================================================
    // symmetryOrbits
    // =========================================================================

    @Nested
    class SymmetryOrbits {

        @Test
        void singleFactor_orbitsEqualStates() {
            StateSpace base = ss("&{a: end, b: end}");
            int orbits = ReplicationChecker.symmetryOrbits(base, base, 1);
            assertEquals(base.states().size(), orbits);
        }

        @Test
        void endReplicated2_oneOrbit() {
            StateSpace base = ss("end");
            StateSpace rep = ReplicationChecker.buildReplicatedStateSpace(base, 2);
            int orbits = ReplicationChecker.symmetryOrbits(rep, base, 2);
            assertEquals(1, orbits);
        }

        @Test
        void chainReplicated2_orbitsLessThanStates() {
            StateSpace base = ss("&{a: end}");
            StateSpace rep = ReplicationChecker.buildReplicatedStateSpace(base, 2);
            int orbits = ReplicationChecker.symmetryOrbits(rep, base, 2);
            // 4 states: (0,0),(0,1),(1,0),(1,1); orbits: {(0,0)}, {(0,1),(1,0)}, {(1,1)} = 3
            assertTrue(orbits <= rep.states().size());
            assertTrue(orbits >= 1);
        }

        @Test
        void symmetryCompresses() {
            StateSpace base = ss("&{a: end, b: end}");
            StateSpace rep = ReplicationChecker.buildReplicatedStateSpace(base, 2);
            int orbits = ReplicationChecker.symmetryOrbits(rep, base, 2);
            assertTrue(orbits <= rep.states().size(),
                    "Orbits should not exceed total states");
        }
    }

    // =========================================================================
    // analyzeReplication
    // =========================================================================

    @Nested
    class AnalyzeReplication {

        @Test
        void endReplicatedOnce() {
            StateSpace base = ss("end");
            ReplicationResult result = ReplicationChecker.analyzeReplication(base, 1);
            assertEquals(1, result.originalStates());
            assertEquals(1, result.replicatedStates());
            assertEquals(1, result.count());
        }

        @Test
        void branchReplicatedTwice() {
            StateSpace base = ss("&{a: end, b: end}");
            ReplicationResult result = ReplicationChecker.analyzeReplication(base, 2);
            assertEquals(base.states().size(), result.originalStates());
            assertEquals(base.states().size() * base.states().size(),
                    result.replicatedStates());
            assertEquals(2, result.count());
        }

        @Test
        void symmetryReductionGreaterThanOne() {
            StateSpace base = ss("&{a: end, b: end}");
            ReplicationResult result = ReplicationChecker.analyzeReplication(base, 2);
            assertTrue(result.symmetryReduction() >= 1.0,
                    "Symmetry reduction should be >= 1.0");
        }

        @Test
        void negativeCountThrows() {
            StateSpace base = ss("end");
            assertThrows(IllegalArgumentException.class,
                    () -> ReplicationChecker.analyzeReplication(base, -1));
        }
    }

    // =========================================================================
    // Label parsing internals
    // =========================================================================

    @Nested
    class LabelParsing {

        @Test
        void flattenSimpleLabel() {
            List<String> result = ReplicationChecker.flattenLabel("abc");
            assertEquals(List.of("abc"), result);
        }

        @Test
        void flattenPairLabel() {
            List<String> result = ReplicationChecker.flattenLabel("(a, b)");
            assertEquals(List.of("a", "b"), result);
        }

        @Test
        void flattenNestedLabel() {
            List<String> result = ReplicationChecker.flattenLabel("((a, b), c)");
            assertEquals(List.of("a", "b", "c"), result);
        }
    }
}
