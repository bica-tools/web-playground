package com.bica.reborn.domain.fhir;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.List;
import java.util.Objects;

/**
 * FHIR clinical workflow verification via session types.
 *
 * <p>Models HL7 FHIR clinical workflows as session types and verifies
 * their correctness through lattice analysis.
 *
 * <p>Ported from {@code reticulate/reticulate/fhir.py} (Step 72).
 */
public final class FHIRChecker {

    private FHIRChecker() {}

    // -----------------------------------------------------------------------
    // Data types
    // -----------------------------------------------------------------------

    /**
     * A named FHIR clinical workflow modelled as a session type.
     */
    public record FHIRWorkflow(
            String name,
            List<String> resources,
            List<String> transitions,
            String sessionTypeString,
            String description) {

        public FHIRWorkflow {
            Objects.requireNonNull(name);
            Objects.requireNonNull(sessionTypeString);
            resources = List.copyOf(resources);
            transitions = List.copyOf(transitions);
        }
    }

    /**
     * Complete analysis result for a FHIR workflow.
     */
    public record FHIRAnalysisResult(
            FHIRWorkflow workflow,
            SessionType ast,
            StateSpace stateSpace,
            LatticeResult latticeResult,
            int numStates,
            int numTransitions,
            boolean isWellFormed) {}

    // -----------------------------------------------------------------------
    // Workflow definitions
    // -----------------------------------------------------------------------

    /** Patient registration, triage, examination, and diagnosis. */
    public static FHIRWorkflow patientIntakeWorkflow() {
        return new FHIRWorkflow(
                "PatientIntake",
                List.of("Patient", "Encounter", "Condition", "Observation"),
                List.of("register", "triage", "examine", "diagnose", "admit", "discharge"),
                "&{register: &{triage: +{URGENT: &{examine: &{diagnose: "
                        + "+{ADMIT: end, DISCHARGE: end}}}, "
                        + "ROUTINE: &{examine: &{diagnose: "
                        + "+{ADMIT: end, DISCHARGE: end}}}}}}",
                "Patient intake workflow: registration, triage, examination, diagnosis, disposition."
        );
    }

    /** Medication prescribing, review, dispensing, and administration. */
    public static FHIRWorkflow medicationOrderWorkflow() {
        return new FHIRWorkflow(
                "MedicationOrder",
                List.of("MedicationRequest", "MedicationDispense", "MedicationAdministration"),
                List.of("prescribe", "reviewRx", "APPROVED", "REJECTED", "dispense", "administer"),
                "&{prescribe: &{reviewRx: +{APPROVED: &{dispense: "
                        + "&{administer: end}}, REJECTED: end}}}",
                "Medication order workflow: prescribe, pharmacy review, dispense, administer."
        );
    }

    /** Lab order, specimen collection, processing, reporting, review. */
    public static FHIRWorkflow labOrderWorkflow() {
        return new FHIRWorkflow(
                "LabOrder",
                List.of("ServiceRequest", "Specimen", "DiagnosticReport", "Observation"),
                List.of("order", "collect", "process", "report", "reviewResult"),
                "&{order: &{collect: &{process: &{report: "
                        + "&{reviewResult: +{NORMAL: end, ABNORMAL: end}}}}}}",
                "Laboratory order workflow: order, specimen collection, processing, reporting, review."
        );
    }

    /** Referral request, review, accept/reject, and scheduling. */
    public static FHIRWorkflow referralWorkflow() {
        return new FHIRWorkflow(
                "Referral",
                List.of("ServiceRequest", "Appointment", "Task"),
                List.of("requestReferral", "reviewReferral", "ACCEPT", "REJECT", "schedule"),
                "&{requestReferral: &{reviewReferral: "
                        + "+{ACCEPT: &{schedule: end}, REJECT: end}}}",
                "Clinical referral workflow: request, specialist review, accept/reject, scheduling."
        );
    }

    /** Emergency triage, assessment, treatment, admit/discharge. */
    public static FHIRWorkflow emergencyWorkflow() {
        return new FHIRWorkflow(
                "Emergency",
                List.of("Encounter", "Condition", "Procedure", "Observation"),
                List.of("triageED", "assess", "treat", "ADMIT", "DISCHARGE_ED"),
                "&{triageED: &{assess: &{treat: "
                        + "+{ADMIT: end, DISCHARGE_ED: end}}}}",
                "Emergency department workflow: triage, assessment, treatment, disposition."
        );
    }

    /** Immunization screening, administration, and observation. */
    public static FHIRWorkflow immunizationWorkflow() {
        return new FHIRWorkflow(
                "Immunization",
                List.of("Immunization", "ImmunizationRecommendation", "Consent", "Observation"),
                List.of("screen", "ELIGIBLE", "INELIGIBLE", "consent", "vaccinate", "observe"),
                "&{screen: +{ELIGIBLE: &{consent: &{vaccinate: "
                        + "&{observe: end}}}, INELIGIBLE: end}}",
                "Immunization workflow: eligibility screening, consent, administration, observation."
        );
    }

    /** Surgical workflow: consent, prep, operate, recovery. */
    public static FHIRWorkflow surgicalWorkflow() {
        return new FHIRWorkflow(
                "Surgical",
                List.of("Consent", "Procedure", "Encounter", "Observation"),
                List.of("surgicalConsent", "prep", "operate", "recovery", "STABLE", "COMPLICATIONS"),
                "&{surgicalConsent: &{prep: &{operate: "
                        + "&{recovery: +{STABLE: end, COMPLICATIONS: end}}}}}",
                "Surgical workflow: consent, preparation, procedure, recovery."
        );
    }

    /** Discharge planning: summary, instructions, follow-up. */
    public static FHIRWorkflow dischargeWorkflow() {
        return new FHIRWorkflow(
                "Discharge",
                List.of("Encounter", "CarePlan", "MedicationRequest", "Appointment"),
                List.of("createSummary", "reconcileMeds", "instructions", "scheduleFU"),
                "&{createSummary: &{reconcileMeds: "
                        + "&{instructions: &{scheduleFU: end}}}}",
                "Discharge workflow: summary, medication reconciliation, instructions, follow-up."
        );
    }

    /** All pre-defined FHIR workflows. */
    public static List<FHIRWorkflow> allWorkflows() {
        return List.of(
                patientIntakeWorkflow(), medicationOrderWorkflow(),
                labOrderWorkflow(), referralWorkflow(),
                emergencyWorkflow(), immunizationWorkflow(),
                surgicalWorkflow(), dischargeWorkflow());
    }

    // -----------------------------------------------------------------------
    // Verification
    // -----------------------------------------------------------------------

    /** Run the full verification pipeline on a FHIR workflow. */
    public static FHIRAnalysisResult verifyWorkflow(FHIRWorkflow workflow) {
        SessionType ast = Parser.parse(workflow.sessionTypeString());
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);

        return new FHIRAnalysisResult(
                workflow, ast, ss, lr,
                ss.states().size(), ss.transitions().size(),
                lr.isLattice());
    }

    /** Verify all pre-defined FHIR workflows. */
    public static List<FHIRAnalysisResult> verifyAllWorkflows() {
        return allWorkflows().stream()
                .map(FHIRChecker::verifyWorkflow)
                .toList();
    }
}
