package com.bica.reborn.channel;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.duality.DualityChecker;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.morphism.MorphismChecker;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ChannelChecker} — channel duality (Step 157a port).
 */
class ChannelCheckerTest {

    private static SessionType parse(String input) {
        return Parser.parse(input);
    }

    // =========================================================================
    // Build channel product
    // =========================================================================

    @Nested
    class BuildChannelProductTests {

        @Test
        void endChannelProduct() {
            SessionType type = parse("end");
            StateSpace product = ChannelChecker.buildChannelProduct(type);
            assertNotNull(product);
            // end has 1 state; product = 1 × 1 = 1
            assertEquals(1, product.states().size());
        }

        @Test
        void simpleBranchChannelProduct() {
            SessionType type = parse("&{a: end}");
            StateSpace product = ChannelChecker.buildChannelProduct(type);
            assertNotNull(product);
            // &{a: end} has 2 states; dual = +{a: end} has 2 states; product = 4
            assertEquals(4, product.states().size());
        }

        @Test
        void channelProductIsLattice() {
            SessionType type = parse("&{a: end, b: end}");
            StateSpace product = ChannelChecker.buildChannelProduct(type);
            assertTrue(LatticeChecker.checkLattice(product).isLattice());
        }
    }

    // =========================================================================
    // Channel duality check
    // =========================================================================

    @Nested
    class CheckChannelDualityTests {

        @Test
        void endChannelDuality() {
            ChannelResult result = ChannelChecker.checkChannelDuality(parse("end"));
            assertTrue(result.channelIsLattice());
            assertTrue(result.dualityIsomorphism());
            // end has no transitions, so complementarity is vacuously true
            assertTrue(result.branchComplementarity());
        }

        @Test
        void simpleBranchDuality() {
            ChannelResult result = ChannelChecker.checkChannelDuality(parse("&{a: end}"));
            assertTrue(result.channelIsLattice());
            assertTrue(result.dualityIsomorphism());
            assertTrue(result.branchComplementarity());
        }

        @Test
        void twoBranchDuality() {
            ChannelResult result = ChannelChecker.checkChannelDuality(
                    parse("&{a: end, b: end}"));
            assertTrue(result.channelIsLattice());
            assertTrue(result.dualityIsomorphism());
            assertTrue(result.branchComplementarity());
        }

        @Test
        void nestedBranchDuality() {
            ChannelResult result = ChannelChecker.checkChannelDuality(
                    parse("&{open: &{read: end, write: end}}"));
            assertTrue(result.channelIsLattice());
            assertTrue(result.dualityIsomorphism());
            assertTrue(result.branchComplementarity());
        }

        @Test
        void selectionDuality() {
            ChannelResult result = ChannelChecker.checkChannelDuality(
                    parse("+{ok: end, err: end}"));
            assertTrue(result.channelIsLattice());
            assertTrue(result.dualityIsomorphism());
            assertTrue(result.branchComplementarity());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "end",
                "&{a: end}",
                "+{ok: end}",
                "&{a: end, b: end}",
                "&{a: &{b: end}}"
        })
        void channelProductAlwaysLattice(String typeStr) {
            ChannelResult result = ChannelChecker.checkChannelDuality(parse(typeStr));
            assertTrue(result.channelIsLattice(),
                    "channel product should be a lattice for: " + typeStr);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "end",
                "&{a: end}",
                "+{ok: end}",
                "&{a: end, b: end}",
                "&{a: &{b: end}}"
        })
        void dualityIsomorphismHolds(String typeStr) {
            ChannelResult result = ChannelChecker.checkChannelDuality(parse(typeStr));
            assertTrue(result.dualityIsomorphism(),
                    "L(S) should be isomorphic to L(dual(S)) for: " + typeStr);
        }
    }

    // =========================================================================
    // Role embedding
    // =========================================================================

    @Nested
    class RoleEmbeddingTests {

        @Test
        void roleAEmbeddingEnd() {
            SessionType type = parse("end");
            Map<Integer, Integer> emb = ChannelChecker.roleEmbedding(type, "A");
            assertNotNull(emb);
            assertEquals(1, emb.size()); // end has 1 state
        }

        @Test
        void roleAEmbeddingBranch() {
            SessionType type = parse("&{a: end}");
            Map<Integer, Integer> emb = ChannelChecker.roleEmbedding(type, "A");
            assertNotNull(emb);
            assertEquals(2, emb.size()); // 2 states in L(S)
        }

        @Test
        void roleBEmbeddingBranch() {
            SessionType type = parse("&{a: end}");
            Map<Integer, Integer> emb = ChannelChecker.roleEmbedding(type, "B");
            assertNotNull(emb);
            assertEquals(2, emb.size()); // 2 states in L(dual(S))
        }

        @Test
        void roleAEmbeddingIsOrderPreserving() {
            SessionType type = parse("&{a: end, b: end}");
            Map<Integer, Integer> emb = ChannelChecker.roleEmbedding(type, "A");
            StateSpace ssA = StateSpaceBuilder.build(type);
            StateSpace product = ChannelChecker.buildChannelProduct(type);
            assertTrue(MorphismChecker.isOrderPreserving(ssA, product, emb));
        }

        @Test
        void roleBEmbeddingIsOrderPreserving() {
            SessionType type = parse("&{a: end, b: end}");
            SessionType d = DualityChecker.dual(type);
            Map<Integer, Integer> emb = ChannelChecker.roleEmbedding(type, "B");
            StateSpace ssB = StateSpaceBuilder.build(d);
            StateSpace product = ChannelChecker.buildChannelProduct(type);
            assertTrue(MorphismChecker.isOrderPreserving(ssB, product, emb));
        }
    }

    // =========================================================================
    // State space sizes
    // =========================================================================

    @Nested
    class StateSizeTests {

        @Test
        void channelResultHasCorrectSizes() {
            ChannelResult result = ChannelChecker.checkChannelDuality(
                    parse("&{a: end, b: end}"));
            // &{a:end, b:end} has 3 states (top, a-target=end, b-target=end merged into end)
            // Actually: top=branch state, then a→end, b→end. States: {branch, end} = 2?
            // No: the branch state + end = 2 states. dual = +{a:end, b:end} = 2 states.
            // Product = 2 × 2 = 4 states.
            assertEquals(result.roleA().states().size() * result.roleB().states().size(),
                    result.channelProduct().states().size());
        }
    }
}
