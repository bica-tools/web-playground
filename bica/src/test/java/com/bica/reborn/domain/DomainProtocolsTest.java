package com.bica.reborn.domain;

import com.bica.reborn.domain.database.DBProtocolChecker;
import com.bica.reborn.domain.database.DBProtocolChecker.*;
import com.bica.reborn.domain.fhir.FHIRChecker;
import com.bica.reborn.domain.fhir.FHIRChecker.*;
import com.bica.reborn.domain.iot.IoTProtocolChecker;
import com.bica.reborn.domain.iot.IoTProtocolChecker.*;
import com.bica.reborn.domain.llmagent.LLMAgentChecker;
import com.bica.reborn.domain.llmagent.LLMAgentChecker.*;
import com.bica.reborn.domain.mining.ProtocolMiner;
import com.bica.reborn.domain.mining.ProtocolMiner.*;
import com.bica.reborn.domain.openapi.OpenAPIChecker;
import com.bica.reborn.domain.openapi.OpenAPIChecker.*;
import com.bica.reborn.domain.security.SecurityProtocolChecker;
import com.bica.reborn.domain.security.SecurityProtocolChecker.*;
import com.bica.reborn.domain.smartcontract.SmartContractChecker;
import com.bica.reborn.domain.smartcontract.SmartContractChecker.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Combined test suite for all domain protocol modules.
 * 5+ tests per module, 40+ total.
 */
class DomainProtocolsTest {

    // ======================================================================
    // 1. OpenAPI
    // ======================================================================
    @Nested
    class OpenAPITests {

        @Test
        void crudLifecycleProducesEndpoints() {
            var endpoints = OpenAPIChecker.crudLifecycle("orders");
            assertEquals(6, endpoints.size());
            assertEquals("POST", endpoints.get(0).method());
            assertEquals("/orders", endpoints.get(0).path());
        }

        @Test
        void apiToSessionTypeStringProducesNonEmptyType() {
            var endpoints = OpenAPIChecker.crudLifecycle("orders");
            String st = OpenAPIChecker.apiToSessionTypeString(endpoints);
            assertNotNull(st);
            assertFalse(st.isEmpty());
            assertTrue(st.contains("&{"));
        }

        @Test
        void emptyEndpointsReturnEnd() {
            String st = OpenAPIChecker.apiToSessionTypeString(List.of());
            assertEquals("end", st);
        }

        @Test
        void apiContractBuildsSuccessfully() {
            var endpoints = OpenAPIChecker.paymentFlow();
            APIContract contract = OpenAPIChecker.apiToContract(endpoints);
            assertNotNull(contract.sessionType());
            assertNotNull(contract.stateSpace());
            assertNotNull(contract.latticeResult());
            assertTrue(contract.stateSpace().states().size() > 0);
        }

        @Test
        void traceValidationAcceptsValidTrace() {
            var endpoints = OpenAPIChecker.orderFulfillmentFlow();
            APIContract contract = OpenAPIChecker.apiToContract(endpoints);
            var result = OpenAPIChecker.validateApiTrace(contract,
                    List.of("POST /orders", "POST /orders/{id}/pay",
                            "POST /orders/{id}/ship", "POST /orders/{id}/deliver"));
            assertTrue(result.valid());
            assertEquals(4, result.stepsCompleted());
        }

        @Test
        void traceValidationRejectsInvalidTrace() {
            var endpoints = OpenAPIChecker.orderFulfillmentFlow();
            APIContract contract = OpenAPIChecker.apiToContract(endpoints);
            var result = OpenAPIChecker.validateApiTrace(contract,
                    List.of("POST /orders/{id}/ship")); // skip pay
            assertFalse(result.valid());
            assertEquals(0, result.violationIndex());
        }

        @Test
        void authFlowProducesEndpoints() {
            var endpoints = OpenAPIChecker.authFlow();
            assertEquals(4, endpoints.size());
        }

        @Test
        void sanitizeLabelWorks() {
            assertEquals("post_orders_id", OpenAPIChecker.sanitizeLabel("POST /orders/{id}"));
        }
    }

    // ======================================================================
    // 2. FHIR
    // ======================================================================
    @Nested
    class FHIRTests {

        @Test
        void patientIntakeWorkflowIsDefined() {
            FHIRWorkflow wf = FHIRChecker.patientIntakeWorkflow();
            assertEquals("PatientIntake", wf.name());
            assertFalse(wf.sessionTypeString().isEmpty());
        }

        @Test
        void patientIntakeFormsLattice() {
            FHIRAnalysisResult r = FHIRChecker.verifyWorkflow(FHIRChecker.patientIntakeWorkflow());
            assertTrue(r.isWellFormed(), "PatientIntake should form a lattice");
            assertTrue(r.numStates() > 0);
        }

        @Test
        void medicationOrderFormsLattice() {
            FHIRAnalysisResult r = FHIRChecker.verifyWorkflow(FHIRChecker.medicationOrderWorkflow());
            assertTrue(r.isWellFormed());
        }

        @Test
        void labOrderFormsLattice() {
            FHIRAnalysisResult r = FHIRChecker.verifyWorkflow(FHIRChecker.labOrderWorkflow());
            assertTrue(r.isWellFormed());
        }

        @Test
        void allFhirWorkflowsDefined() {
            List<FHIRWorkflow> all = FHIRChecker.allWorkflows();
            assertEquals(8, all.size());
        }

        @Test
        void verifyAllWorkflowsReturnsResults() {
            List<FHIRAnalysisResult> results = FHIRChecker.verifyAllWorkflows();
            assertEquals(8, results.size());
            assertTrue(results.stream().allMatch(r -> r.numStates() > 0));
        }
    }

    // ======================================================================
    // 3. Security
    // ======================================================================
    @Nested
    class SecurityTests {

        @Test
        void oauth2AuthCodeFormsLattice() {
            SecurityAnalysisResult r = SecurityProtocolChecker.verifyProtocol(
                    SecurityProtocolChecker.oauth2AuthCode());
            assertTrue(r.isWellFormed());
        }

        @Test
        void tls13HandshakeFormsLattice() {
            SecurityAnalysisResult r = SecurityProtocolChecker.verifyProtocol(
                    SecurityProtocolChecker.tls13Handshake());
            assertTrue(r.isWellFormed());
        }

        @Test
        void kerberosAuthFormsLattice() {
            SecurityAnalysisResult r = SecurityProtocolChecker.verifyProtocol(
                    SecurityProtocolChecker.kerberosAuth());
            assertTrue(r.isWellFormed());
        }

        @Test
        void allSecurityProtocolsDefined() {
            assertEquals(8, SecurityProtocolChecker.allProtocols().size());
        }

        @Test
        void verifyAllProtocolsReturnsResults() {
            var results = SecurityProtocolChecker.verifyAllProtocols();
            assertEquals(8, results.size());
            assertTrue(results.stream().allMatch(r -> r.numStates() > 0));
        }

        @Test
        void protocolEvolutionAuthCodeVsPkce() {
            ProtocolEvolutionResult r = SecurityProtocolChecker.checkEvolution(
                    SecurityProtocolChecker.oauth2AuthCode(),
                    SecurityProtocolChecker.oauth2Pkce());
            // PKCE extends auth code, so auth code is NOT subtype of PKCE (different structure)
            assertNotNull(r);
        }
    }

    // ======================================================================
    // 4. IoT
    // ======================================================================
    @Nested
    class IoTTests {

        @Test
        void mqttQos0FormsLattice() {
            IoTAnalysisResult r = IoTProtocolChecker.verifyProtocol(
                    IoTProtocolChecker.mqttPublishSubscribe(0));
            assertTrue(r.isWellFormed());
        }

        @Test
        void mqttQos2FormsLattice() {
            IoTAnalysisResult r = IoTProtocolChecker.verifyProtocol(
                    IoTProtocolChecker.mqttPublishSubscribe(2));
            assertTrue(r.isWellFormed());
        }

        @Test
        void invalidQosThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> IoTProtocolChecker.mqttPublishSubscribe(3));
        }

        @Test
        void coapConFormsLattice() {
            IoTAnalysisResult r = IoTProtocolChecker.verifyProtocol(
                    IoTProtocolChecker.coapRequestResponse(true));
            assertTrue(r.isWellFormed());
        }

        @Test
        void zigbeeJoinFormsLattice() {
            IoTAnalysisResult r = IoTProtocolChecker.verifyProtocol(
                    IoTProtocolChecker.zigbeeJoin());
            assertTrue(r.isWellFormed());
        }

        @Test
        void allIotProtocolsDefined() {
            assertEquals(11, IoTProtocolChecker.allProtocols().size());
        }

        @Test
        void compareQosLevelsReturnsThree() {
            var results = IoTProtocolChecker.compareQosLevels();
            assertEquals(3, results.size());
        }
    }

    // ======================================================================
    // 5. Database
    // ======================================================================
    @Nested
    class DatabaseTests {

        @Test
        void jdbcConnectionFormsLattice() {
            DBAnalysisResult r = DBProtocolChecker.verifyProtocol(
                    DBProtocolChecker.jdbcConnection());
            assertTrue(r.isWellFormed());
        }

        @Test
        void twoPhaseCommitFormsLattice() {
            DBAnalysisResult r = DBProtocolChecker.verifyProtocol(
                    DBProtocolChecker.twoPhaseCommit());
            assertTrue(r.isWellFormed());
        }

        @Test
        void cursorIterationFormsLattice() {
            DBAnalysisResult r = DBProtocolChecker.verifyProtocol(
                    DBProtocolChecker.cursorIteration());
            assertTrue(r.isWellFormed());
        }

        @Test
        void allDbProtocolsDefined() {
            assertEquals(8, DBProtocolChecker.allProtocols().size());
        }

        @Test
        void verifyAllDbProtocols() {
            var results = DBProtocolChecker.verifyAllProtocols();
            assertEquals(8, results.size());
            assertTrue(results.stream().allMatch(r -> r.numStates() > 0));
        }

        @Test
        void isolationComparisonWorks() {
            IsolationComparisonResult r = DBProtocolChecker.compareIsolationLevels(
                    DBProtocolChecker.jdbcConnection(),
                    DBProtocolChecker.jdbcConnection());
            assertTrue(r.isCompatible(), "Same protocol should be compatible with itself");
        }
    }

    // ======================================================================
    // 6. Smart Contracts
    // ======================================================================
    @Nested
    class SmartContractTests {

        @Test
        void erc20LifecycleIsDefined() {
            SmartContractWorkflow wf = SmartContractChecker.erc20Lifecycle();
            assertEquals("ERC20Token", wf.name());
            assertEquals("ERC-20", wf.standard());
        }

        @Test
        void erc20FormsLattice() {
            ContractAnalysisResult r = SmartContractChecker.verifyContract(
                    SmartContractChecker.erc20Lifecycle());
            assertTrue(r.isWellFormed());
        }

        @Test
        void auctionContractFormsLattice() {
            ContractAnalysisResult r = SmartContractChecker.verifyContract(
                    SmartContractChecker.auctionContract());
            assertTrue(r.isWellFormed());
        }

        @Test
        void defiLendingFormsLattice() {
            ContractAnalysisResult r = SmartContractChecker.verifyContract(
                    SmartContractChecker.defiLending());
            assertTrue(r.isWellFormed());
        }

        @Test
        void allContractWorkflowsDefined() {
            assertEquals(5, SmartContractChecker.allWorkflows().size());
        }

        @Test
        void verifyAllContracts() {
            var results = SmartContractChecker.verifyAllContracts();
            assertEquals(5, results.size());
        }
    }

    // ======================================================================
    // 7. LLM Agents
    // ======================================================================
    @Nested
    class LLMAgentTests {

        @Test
        void ragPipelineIsDefined() {
            AgentOrchestration orch = LLMAgentChecker.ragPipeline();
            assertEquals("RAG Pipeline", orch.name());
            assertEquals(5, orch.agents().size());
        }

        @Test
        void ragPipelineVerifies() {
            OrchestrationResult r = LLMAgentChecker.verifyOrchestration(
                    LLMAgentChecker.ragPipeline());
            assertTrue(r.globalStates() > 0);
            assertTrue(r.globalIsLattice());
        }

        @Test
        void toolUseLoopIsDefined() {
            AgentOrchestration orch = LLMAgentChecker.toolUseLoop();
            assertEquals("Tool Use Loop", orch.name());
            assertEquals(2, orch.agents().size());
        }

        @Test
        void toolUseLoopVerifies() {
            OrchestrationResult r = LLMAgentChecker.verifyOrchestration(
                    LLMAgentChecker.toolUseLoop());
            assertTrue(r.globalStates() > 0);
        }

        @Test
        void a2aChainIsDefined() {
            AgentOrchestration orch = LLMAgentChecker.a2aChain(3);
            assertEquals(3, orch.agents().size());
        }

        @Test
        void a2aChainTooSmallThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> LLMAgentChecker.a2aChain(1));
        }

        @Test
        void a2aBroadcastIsDefined() {
            AgentOrchestration orch = LLMAgentChecker.a2aBroadcast(2);
            assertEquals(3, orch.agents().size()); // orchestrator + 2 workers
        }
    }

    // ======================================================================
    // 8. Protocol Mining
    // ======================================================================
    @Nested
    class MiningTests {

        @Test
        void mineFromEmptyTracesReturnsEnd() {
            MiningResult r = ProtocolMiner.mineFromTraces(List.of());
            assertEquals("end", r.inferredTypeStr());
            assertEquals(0, r.numTraces());
        }

        @Test
        void mineFromSingleTrace() {
            MiningResult r = ProtocolMiner.mineFromTraces(
                    List.of(List.of("connect", "execute", "close")));
            assertEquals(1, r.numTraces());
            assertTrue(r.numStates() > 0);
            assertTrue(r.coverage() >= 0.0);
        }

        @Test
        void mineFromMultipleTraces() {
            MiningResult r = ProtocolMiner.mineFromTraces(List.of(
                    List.of("connect", "query", "close"),
                    List.of("connect", "update", "close"),
                    List.of("connect", "query", "close")));
            assertEquals(3, r.numTraces());
            assertTrue(r.numStates() > 0);
        }

        @Test
        void parseLogLinesExtractsTraces() {
            List<List<String>> traces = ProtocolMiner.parseLogLines(List.of(
                    "CALL connect", "CALL execute", "CALL close",
                    "",
                    "CALL connect", "CALL query", "CALL close"));
            assertEquals(2, traces.size());
            assertEquals(List.of("connect", "execute", "close"), traces.get(0));
        }

        @Test
        void mineFromLogsWorks() {
            MiningResult r = ProtocolMiner.mineFromLogs(List.of(
                    "CALL connect", "CALL execute", "CALL close"));
            assertEquals(1, r.numTraces());
            assertTrue(r.numStates() > 0);
        }

        @Test
        void confidenceScoreInRange() {
            MiningResult r = ProtocolMiner.mineFromTraces(
                    List.of(List.of("a", "b"), List.of("a", "c")));
            assertTrue(r.confidence() >= 0.0);
            assertTrue(r.confidence() <= 1.0);
        }
    }
}
