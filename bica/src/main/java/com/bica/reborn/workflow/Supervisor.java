package com.bica.reborn.workflow;

import java.util.*;

/**
 * Scans programme status, identifies incomplete work, and prioritizes next steps.
 *
 * <p>The supervisor examines the known steps, their grades, and their
 * completion status to generate proposals for what to work on next.
 */
public class Supervisor {

    /** Status of a single step. */
    public record StepStatus(
            String number,
            String title,
            String grade,
            String status,
            boolean hasPaper,
            boolean hasModule,
            int testCount) {}

    /** A proposed action. */
    public record Proposal(
            String stepNumber,
            String title,
            String reason,
            String priority) {}

    /** Snapshot of the programme state. */
    public record ProgrammeSnapshot(
            int totalStepsPlanned,
            int completeSteps,
            int totalTests,
            List<StepStatus> steps,
            List<Proposal> proposals) {

        public ProgrammeSnapshot {
            Objects.requireNonNull(steps, "steps must not be null");
            Objects.requireNonNull(proposals, "proposals must not be null");
            steps = List.copyOf(steps);
            proposals = List.copyOf(proposals);
        }
    }

    private final List<StepStatus> knownSteps = new ArrayList<>();

    /**
     * Register a known step with its current status.
     */
    public void addStep(StepStatus step) {
        knownSteps.add(Objects.requireNonNull(step));
    }

    /**
     * Scan the programme and build a snapshot.
     *
     * @return the programme snapshot with status and proposals
     */
    public ProgrammeSnapshot scanProgramme() {
        int complete = 0;
        int totalTests = 0;

        for (StepStatus s : knownSteps) {
            if ("A+".equals(s.grade()) || "A".equals(s.grade())) {
                complete++;
            }
            totalTests += s.testCount();
        }

        List<Proposal> proposals = identifyGaps();
        proposals = prioritize(proposals);

        return new ProgrammeSnapshot(
                knownSteps.size(), complete, totalTests,
                List.copyOf(knownSteps), proposals);
    }

    /**
     * Identify gaps: steps that need work.
     *
     * @return list of proposals for incomplete steps
     */
    public List<Proposal> identifyGaps() {
        var proposals = new ArrayList<Proposal>();

        for (StepStatus s : knownSteps) {
            if ("A+".equals(s.grade())) {
                continue; // already complete
            }

            if (s.hasModule() && !s.hasPaper()) {
                proposals.add(new Proposal(s.number(), s.title(),
                        "Module exists but no paper — quick win", "high"));
            } else if (s.hasPaper() && !"A+".equals(s.grade())) {
                proposals.add(new Proposal(s.number(), s.title(),
                        "Paper exists but grade is " + s.grade() + " — needs expansion", "medium"));
            } else if (!s.hasModule() && !s.hasPaper()) {
                proposals.add(new Proposal(s.number(), s.title(),
                        "Not started — needs implementation and paper", "low"));
            }
        }

        return proposals;
    }

    /**
     * Prioritize proposals: high before medium before low.
     *
     * @param proposals unsorted proposals
     * @return sorted proposals
     */
    public List<Proposal> prioritize(List<Proposal> proposals) {
        var sorted = new ArrayList<>(proposals);
        sorted.sort(Comparator.comparingInt(p -> switch (p.priority()) {
            case "high" -> 0;
            case "medium" -> 1;
            case "low" -> 2;
            default -> 3;
        }));
        return sorted;
    }
}
