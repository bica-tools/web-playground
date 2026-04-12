package com.bica.reborn.domain.iot;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.List;
import java.util.Objects;

/**
 * IoT protocol verification via lattice properties.
 *
 * <p>Models IoT protocols (MQTT, CoAP, Zigbee) as session types and verifies
 * their correctness through lattice analysis.
 *
 * <p>Ported from {@code reticulate/reticulate/iot_protocols.py} (Step 86).
 */
public final class IoTProtocolChecker {

    private IoTProtocolChecker() {}

    // -----------------------------------------------------------------------
    // Data types
    // -----------------------------------------------------------------------

    /**
     * A named IoT protocol modelled as a session type.
     */
    public record IoTProtocol(
            String name,
            String sessionTypeString,
            int qosLevel,
            String description,
            String transport,
            boolean constrained) {

        public IoTProtocol {
            Objects.requireNonNull(name);
            Objects.requireNonNull(sessionTypeString);
            Objects.requireNonNull(transport);
        }
    }

    /**
     * Complete analysis result for an IoT protocol.
     */
    public record IoTAnalysisResult(
            IoTProtocol protocol,
            SessionType ast,
            StateSpace stateSpace,
            LatticeResult latticeResult,
            int numStates,
            int numTransitions,
            boolean isWellFormed) {}

    // -----------------------------------------------------------------------
    // MQTT protocol definitions
    // -----------------------------------------------------------------------

    /**
     * MQTT publish/subscribe lifecycle with configurable QoS.
     *
     * @param qos MQTT QoS level (0, 1, or 2).
     * @throws IllegalArgumentException if qos is not 0, 1, or 2.
     */
    public static IoTProtocol mqttPublishSubscribe(int qos) {
        if (qos < 0 || qos > 2) {
            throw new IllegalArgumentException("MQTT QoS must be 0, 1, or 2, got " + qos);
        }

        String st;
        String desc;

        switch (qos) {
            case 0 -> {
                st = "&{connect: +{CONNACK_OK: "
                        + "&{subscribe: &{suback: "
                        + "&{publish: &{disconnect: end}}}}, "
                        + "CONNACK_FAIL: end}}";
                desc = "MQTT QoS 0 (at most once): fire-and-forget.";
            }
            case 1 -> {
                st = "&{connect: +{CONNACK_OK: "
                        + "&{subscribe: &{suback: "
                        + "&{publish: &{puback: &{disconnect: end}}}}}, "
                        + "CONNACK_FAIL: end}}";
                desc = "MQTT QoS 1 (at least once): PUBACK acknowledgement.";
            }
            default -> {
                st = "&{connect: +{CONNACK_OK: "
                        + "&{subscribe: &{suback: "
                        + "&{publish: &{pubrec: &{pubrel: &{pubcomp: "
                        + "&{disconnect: end}}}}}}}, "
                        + "CONNACK_FAIL: end}}";
                desc = "MQTT QoS 2 (exactly once): PUBREC/PUBREL/PUBCOMP handshake.";
            }
        }

        return new IoTProtocol("MQTT_QoS" + qos, st, qos, desc, "TCP", false);
    }

    /** MQTT retained message flow. */
    public static IoTProtocol mqttRetainedMessages() {
        return new IoTProtocol(
                "MQTT_Retained",
                "&{connect: +{CONNACK_OK: "
                        + "&{publishRetained: +{RETAIN_OK: "
                        + "&{updateRetained: end, clearRetained: end}, "
                        + "RETAIN_FAIL: end}}, CONNACK_FAIL: end}}",
                1, "MQTT retained messages.", "TCP", false);
    }

    /** MQTT Last Will and Testament (LWT) pattern. */
    public static IoTProtocol mqttLastWill() {
        return new IoTProtocol(
                "MQTT_LastWill",
                "&{connectWithWill: +{CONNACK_WILL_OK: "
                        + "&{subscribe: &{suback: "
                        + "&{publish: +{GRACEFUL: &{disconnect: end}, "
                        + "UNGRACEFUL: &{willPublished: end}}}}}, "
                        + "CONNACK_FAIL: end}}",
                0, "MQTT Last Will and Testament.", "TCP", false);
    }

    // -----------------------------------------------------------------------
    // CoAP protocol definitions
    // -----------------------------------------------------------------------

    /**
     * CoAP request/response interaction.
     *
     * @param confirmable If true, model CON request; otherwise NON.
     */
    public static IoTProtocol coapRequestResponse(boolean confirmable) {
        if (confirmable) {
            return new IoTProtocol(
                    "CoAP_CON",
                    "&{conRequest: +{ACK_PIGGYBACK: end, "
                            + "ACK_EMPTY: &{separateResponse: end}, "
                            + "TIMEOUT: &{retransmit: +{ACK_PIGGYBACK: end, "
                            + "ACK_EMPTY: &{separateResponse: end}, "
                            + "FAIL: end}}}}",
                    1, "CoAP confirmable request.", "UDP", true);
        } else {
            return new IoTProtocol(
                    "CoAP_NON",
                    "&{nonRequest: +{RESPONSE: end, NO_RESPONSE: end}}",
                    0, "CoAP non-confirmable request.", "UDP", true);
        }
    }

    /** CoAP observe pattern (RFC 7641). */
    public static IoTProtocol coapObserve() {
        return new IoTProtocol(
                "CoAP_Observe",
                "&{register: +{OBSERVE_OK: "
                        + "rec X . &{notification: +{CONTINUE: X, "
                        + "DEREGISTER: end}}, "
                        + "OBSERVE_REJECT: end}}",
                1, "CoAP observe pattern.", "UDP", true);
    }

    /** CoAP block-wise transfer (RFC 7959). */
    public static IoTProtocol coapBlockTransfer() {
        return new IoTProtocol(
                "CoAP_BlockTransfer",
                "&{requestBlock: &{blockResponse: "
                        + "rec X . +{MORE_BLOCKS: &{requestNext: &{blockResponse: X}}, "
                        + "COMPLETE: end}}}",
                1, "CoAP block-wise transfer.", "UDP", true);
    }

    // -----------------------------------------------------------------------
    // Zigbee protocol definitions
    // -----------------------------------------------------------------------

    /** Zigbee network join protocol. */
    public static IoTProtocol zigbeeJoin() {
        return new IoTProtocol(
                "Zigbee_Join",
                "&{beaconScan: &{selectNetwork: "
                        + "&{associateRequest: +{ASSOC_OK: "
                        + "&{keyExchange: +{KEY_OK: &{joinComplete: end}, "
                        + "KEY_FAIL: end}}, "
                        + "ASSOC_REJECT: end}}}}",
                -1, "Zigbee network join.", "IEEE802.15.4", true);
    }

    /** Zigbee data transfer with acknowledgement. */
    public static IoTProtocol zigbeeDataTransfer() {
        return new IoTProtocol(
                "Zigbee_DataTransfer",
                "rec X . &{sendData: +{DATA_ACK: "
                        + "+{CONTINUE: X, DONE: end}, "
                        + "DATA_NACK: +{RETRY: X, ABORT: end}}}",
                -1, "Zigbee data transfer.", "IEEE802.15.4", true);
    }

    /** All pre-defined IoT protocols. */
    public static List<IoTProtocol> allProtocols() {
        return List.of(
                mqttPublishSubscribe(0), mqttPublishSubscribe(1), mqttPublishSubscribe(2),
                mqttRetainedMessages(), mqttLastWill(),
                coapRequestResponse(true), coapRequestResponse(false),
                coapObserve(), coapBlockTransfer(),
                zigbeeJoin(), zigbeeDataTransfer());
    }

    // -----------------------------------------------------------------------
    // Verification
    // -----------------------------------------------------------------------

    /** Run the full verification pipeline on an IoT protocol. */
    public static IoTAnalysisResult verifyProtocol(IoTProtocol protocol) {
        SessionType ast = Parser.parse(protocol.sessionTypeString());
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);

        return new IoTAnalysisResult(
                protocol, ast, ss, lr,
                ss.states().size(), ss.transitions().size(),
                lr.isLattice());
    }

    /** Verify all pre-defined IoT protocols. */
    public static List<IoTAnalysisResult> verifyAllProtocols() {
        return allProtocols().stream()
                .map(IoTProtocolChecker::verifyProtocol)
                .toList();
    }

    /** Compare MQTT QoS levels 0, 1, 2. */
    public static List<IoTAnalysisResult> compareQosLevels() {
        return List.of(
                verifyProtocol(mqttPublishSubscribe(0)),
                verifyProtocol(mqttPublishSubscribe(1)),
                verifyProtocol(mqttPublishSubscribe(2)));
    }
}
