package com.bica.reborn.samples;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.exception.ProtocolViolationException;
import com.bica.reborn.morphism.MorphismChecker;
import com.bica.reborn.morphism.Morphism;
import com.bica.reborn.morphism.MorphismKind;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

/**
 * Session-typed morphism analysis protocol.
 *
 * <p>Session type:
 * {@code setSource . setTarget . classify . +{ISOMORPHISM: end, EMBEDDING: end, PROJECTION: end, HOMOMORPHISM: end, NONE: end}}
 *
 * <p>Protocol: set source and target state spaces, classify the morphism between them.
 * The result is a 5-way selection based on the morphism kind.
 */
@Session("setSource . setTarget . classify . +{ISOMORPHISM: end, NOT_ISOMORPHISM: end}")
public class MorphismSession {

    public enum MorphismDecision { ISOMORPHISM, NOT_ISOMORPHISM }

    private enum State { INITIAL, SOURCE_SET, BOTH_SET, DONE }
    private State state = State.INITIAL;

    private StateSpace source;
    private StateSpace target;

    public void setSource(String typeStr) {
        requireState("setSource", State.INITIAL);
        source = StateSpaceBuilder.build(Parser.parse(typeStr));
        state = State.SOURCE_SET;
    }

    public void setTarget(String typeStr) {
        requireState("setTarget", State.SOURCE_SET);
        target = StateSpaceBuilder.build(Parser.parse(typeStr));
        state = State.BOTH_SET;
    }

    public MorphismDecision classify() {
        requireState("classify", State.BOTH_SET);
        state = State.DONE;
        var result = MorphismChecker.findIsomorphism(source, target);
        return result.kind() == MorphismKind.ISOMORPHISM
                ? MorphismDecision.ISOMORPHISM
                : MorphismDecision.NOT_ISOMORPHISM;
    }

    private void requireState(String method, State expected) {
        if (state != expected)
            throw new ProtocolViolationException(method, state.name());
    }
}
