package com.bica.reborn.ast;

import com.bica.reborn.ast.Branch.Choice;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for AST node equality, immutability, and null rejection. */
class SessionTypeTest {

    @Nested
    class Equality {

        @Test
        void endEqualsEnd() {
            assertEquals(new End(), new End());
        }

        @Test
        void endHashConsistent() {
            assertEquals(new End().hashCode(), new End().hashCode());
        }

        @Test
        void varEquals() {
            assertEquals(new Var("X"), new Var("X"));
        }

        @Test
        void varNotEqual() {
            assertNotEquals(new Var("X"), new Var("Y"));
        }

        @Test
        void varHashConsistent() {
            assertEquals(new Var("X").hashCode(), new Var("X").hashCode());
        }

        @Test
        void branchEquals() {
            var b1 = new Branch(List.of(new Choice("m", new End())));
            var b2 = new Branch(List.of(new Choice("m", new End())));
            assertEquals(b1, b2);
        }

        @Test
        void branchHashConsistent() {
            var b1 = new Branch(List.of(new Choice("m", new End())));
            var b2 = new Branch(List.of(new Choice("m", new End())));
            assertEquals(b1.hashCode(), b2.hashCode());
        }

        @Test
        void complexEquality() {
            var a = new Branch(List.of(
                    new Choice("m", new End()),
                    new Choice("n", new Var("X"))));
            var b = new Branch(List.of(
                    new Choice("m", new End()),
                    new Choice("n", new Var("X"))));
            assertEquals(a, b);
        }

        @Test
        void branchNotEqualToSelect() {
            var a = new Branch(List.of(new Choice("m", new End())));
            var b = new Select(List.of(new Choice("m", new End())));
            assertNotEquals(a, b);
        }

        @Test
        void nodesInSet() {
            Set<SessionType> s = new HashSet<>();
            s.add(new End());
            s.add(new End());
            s.add(new Var("X"));
            s.add(new Var("X"));
            assertEquals(2, s.size());
        }
    }

    @Nested
    class Immutability {

        @Test
        void branchChoicesImmutable() {
            var branch = new Branch(List.of(new Choice("m", new End())));
            assertThrows(UnsupportedOperationException.class,
                    () -> branch.choices().add(new Choice("n", new End())));
        }

        @Test
        void selectChoicesImmutable() {
            var select = new Select(List.of(new Choice("m", new End())));
            assertThrows(UnsupportedOperationException.class,
                    () -> select.choices().add(new Choice("n", new End())));
        }
    }

    @Nested
    class NullRejection {

        @Test
        void varRejectsNullName() {
            assertThrows(NullPointerException.class, () -> new Var(null));
        }

        @Test
        void branchRejectsNullChoices() {
            assertThrows(NullPointerException.class, () -> new Branch(null));
        }

        @Test
        void selectRejectsNullChoices() {
            assertThrows(NullPointerException.class, () -> new Select(null));
        }

        @Test
        void parallelRejectsNullLeft() {
            assertThrows(NullPointerException.class, () -> new Parallel(null, new End()));
        }

        @Test
        void parallelRejectsNullRight() {
            assertThrows(NullPointerException.class, () -> new Parallel(new End(), null));
        }

        @Test
        void recRejectsNullVar() {
            assertThrows(NullPointerException.class, () -> new Rec(null, new End()));
        }

        @Test
        void recRejectsNullBody() {
            assertThrows(NullPointerException.class, () -> new Rec("X", null));
        }

        @Test
        void sequenceRejectsNullLeft() {
            assertThrows(NullPointerException.class, () -> new Sequence(null, new End()));
        }

        @Test
        void sequenceRejectsNullRight() {
            assertThrows(NullPointerException.class, () -> new Sequence(new End(), null));
        }

        @Test
        void choiceRejectsNullLabel() {
            assertThrows(NullPointerException.class, () -> new Choice(null, new End()));
        }

        @Test
        void choiceRejectsNullBody() {
            assertThrows(NullPointerException.class, () -> new Choice("m", null));
        }
    }
}
