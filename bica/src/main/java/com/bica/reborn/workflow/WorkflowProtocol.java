package com.bica.reborn.workflow;

/**
 * Constants defining the session type protocols for each agent role
 * in the research workflow.
 *
 * <p>The orchestrator protocol uses parallel constructors to model
 * independent phases:
 * <pre>
 * rec S . &amp;{scan: +{proposals:
 *   &amp;{research: +{report:
 *     (&amp;{implement: +{moduleReady: end}} || &amp;{writePaper: +{paperReady: end}}) .
 *     (&amp;{writeTests: +{testsPass: end}} || &amp;{writeProofs: +{proofsReady: end}}) .
 *     &amp;{evaluate: +{accepted: +{nextStep: S, allDone: end},
 *                      needsFixes: &amp;{review: +{fixes: S}}}}}}}}
 * </pre>
 */
public final class WorkflowProtocol {

    private WorkflowProtocol() {} // constants only

    // -- Individual agent protocols ------------------------------------------

    /** Supervisor: scans programme, produces proposals. */
    public static final String SUPERVISOR =
            "&{scan: +{proposals: end}}";

    /** Researcher: investigates a step, produces a report. */
    public static final String RESEARCHER =
            "&{investigate: +{report: end}}";

    /** Evaluator: grades deliverables, accepts or requests fixes. */
    public static final String EVALUATOR =
            "&{evaluate: +{accepted: end, needsFixes: &{listFixes: end}}}";

    /** Implementer: implements a module. */
    public static final String IMPLEMENTER =
            "&{implement: +{moduleReady: end, error: &{abort: end}}}";

    /** Tester: writes and runs tests. */
    public static final String TESTER =
            "&{writeTests: +{testsPass: end, testsFail: &{abort: end}}}";

    /** Writer: writes an educational paper. */
    public static final String WRITER =
            "&{writePaper: +{paperReady: end}}";

    /** Prover: writes companion proofs. */
    public static final String PROVER =
            "&{writeProofs: +{proofsReady: end}}";

    /** Reviewer: reviews deliverables and suggests fixes. */
    public static final String REVIEWER =
            "&{review: +{fixes: end}}";

    // -- Orchestrator protocol -----------------------------------------------

    /**
     * The full orchestrator session type with parallel phases.
     *
     * <p>Phase 2: implement || write (parallel, independent).
     * <p>Phase 3: test || prove (parallel, independent).
     */
    public static final String ORCHESTRATOR =
            "rec S . &{scan: +{proposals: " +
            "&{research: +{report: " +
            "(&{implement: +{moduleReady: end}} || &{writePaper: +{paperReady: end}}) . " +
            "(&{writeTests: +{testsPass: end}} || &{writeProofs: +{proofsReady: end}}) . " +
            "&{evaluate: +{accepted: +{nextStep: S, allDone: end}, " +
            "needsFixes: &{review: +{fixes: S}}}}}}}}";

    // -- All protocol names (for iteration) ----------------------------------

    /** All agent type names. */
    public static final String[] AGENT_TYPES = {
        "Supervisor", "Researcher", "Evaluator", "Implementer",
        "Tester", "Writer", "Prover", "Reviewer"
    };

    /** Return the session type string for the given agent type name. */
    public static String protocolFor(String agentType) {
        return switch (agentType) {
            case "Supervisor" -> SUPERVISOR;
            case "Researcher" -> RESEARCHER;
            case "Evaluator" -> EVALUATOR;
            case "Implementer" -> IMPLEMENTER;
            case "Tester" -> TESTER;
            case "Writer" -> WRITER;
            case "Prover" -> PROVER;
            case "Reviewer" -> REVIEWER;
            default -> throw new IllegalArgumentException("Unknown agent type: " + agentType);
        };
    }
}
