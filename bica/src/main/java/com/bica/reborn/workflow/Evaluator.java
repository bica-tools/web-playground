package com.bica.reborn.workflow;

import java.util.*;

/**
 * Evaluates step paper quality with criteria across structure, content, and writing.
 *
 * <p>Grades:
 * <ul>
 *   <li>A+ (90+): accepted, no gaps or weaknesses</li>
 *   <li>A  (80+): near-complete, minor issues</li>
 *   <li>B  (65+): significant gaps</li>
 *   <li>C  (50+): major rework needed</li>
 *   <li>F  (&lt;50): not started or fundamentally broken</li>
 * </ul>
 */
public class Evaluator {

    /** Result of evaluating one criterion. */
    public record Criterion(String name, int maxPoints, int points, boolean passed, String message) {}

    /** Complete evaluation result. */
    public record EvaluationResult(
            String stepNumber,
            String grade,
            int score,
            boolean accepted,
            List<Criterion> criteria,
            List<String> fixes,
            List<String> strengths,
            List<String> weaknesses) {

        public EvaluationResult {
            Objects.requireNonNull(stepNumber);
            Objects.requireNonNull(grade);
            Objects.requireNonNull(criteria);
            Objects.requireNonNull(fixes);
            Objects.requireNonNull(strengths);
            Objects.requireNonNull(weaknesses);
            criteria = List.copyOf(criteria);
            fixes = List.copyOf(fixes);
            strengths = List.copyOf(strengths);
            weaknesses = List.copyOf(weaknesses);
        }
    }

    /** Input describing a step's deliverables. */
    public record StepDeliverables(
            String stepNumber,
            boolean hasPaper,
            int wordCount,
            boolean hasProofs,
            boolean hasModule,
            boolean testsPass,
            int testCount,
            int formalEnvironments,
            int workedExamples,
            int references) {}

    /**
     * Evaluate a step's deliverables.
     *
     * @param deliverables the step's deliverable status
     * @return the evaluation result
     */
    public EvaluationResult evaluate(StepDeliverables deliverables) {
        var criteria = new ArrayList<Criterion>();
        var fixes = new ArrayList<String>();
        var strengths = new ArrayList<String>();
        var weaknesses = new ArrayList<String>();

        // Paper exists (10 points)
        if (deliverables.hasPaper()) {
            criteria.add(new Criterion("Paper exists", 10, 10, true, "main.tex present"));
            strengths.add("Paper exists");
        } else {
            criteria.add(new Criterion("Paper exists", 10, 0, false, "main.tex not found"));
            fixes.add("Create main.tex with 5000+ words");
        }

        // Word count (10 points)
        int wordPoints = deliverables.wordCount() >= 5000 ? 10
                : deliverables.wordCount() >= 4000 ? 7
                : deliverables.wordCount() >= 3000 ? 4 : 0;
        boolean wordPass = deliverables.wordCount() >= 5000;
        criteria.add(new Criterion("Word count >= 5000", 10, wordPoints, wordPass,
                deliverables.wordCount() + " words"));
        if (!wordPass) {
            fixes.add("Expand paper to 5000+ words (currently " + deliverables.wordCount() + ")");
            weaknesses.add("Below word count target");
        }

        // Proofs (10 points)
        if (deliverables.hasProofs()) {
            criteria.add(new Criterion("Companion proofs.tex", 10, 10, true, "proofs.tex present"));
        } else {
            criteria.add(new Criterion("Companion proofs.tex", 10, 0, false, "proofs.tex missing"));
            fixes.add("Add companion proofs.tex");
        }

        // Module (10 points)
        if (deliverables.hasModule()) {
            criteria.add(new Criterion("Module exists", 10, 10, true, "Module implemented"));
            strengths.add("Module implemented");
        } else {
            criteria.add(new Criterion("Module exists", 10, 0, false, "No implementation"));
            fixes.add("Implement module");
        }

        // Tests (10 points)
        int testPoints = deliverables.testsPass() ? 10 : deliverables.testCount() > 0 ? 5 : 0;
        criteria.add(new Criterion("Tests pass", 10, testPoints,
                deliverables.testsPass(), deliverables.testCount() + " tests"));
        if (!deliverables.testsPass()) {
            fixes.add("Fix failing tests");
        }

        // Formal environments (10 points)
        int formalPoints = deliverables.formalEnvironments() >= 4 ? 10
                : deliverables.formalEnvironments() >= 2 ? 5 : 0;
        criteria.add(new Criterion("Formal density", 10, formalPoints,
                deliverables.formalEnvironments() >= 4,
                deliverables.formalEnvironments() + " formal environments"));

        int score = computeScore(criteria);
        String grade = computeGrade(score);
        boolean accepted = "A+".equals(grade);

        return new EvaluationResult(deliverables.stepNumber(), grade, score,
                accepted, criteria, fixes, strengths, weaknesses);
    }

    /**
     * Compute the total score from criteria, normalized to 0-100.
     */
    public int computeScore(List<Criterion> criteria) {
        int total = criteria.stream().mapToInt(Criterion::points).sum();
        int max = criteria.stream().mapToInt(Criterion::maxPoints).sum();
        return max > 0 ? (int) Math.round(100.0 * total / max) : 0;
    }

    private String computeGrade(int score) {
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 65) return "B";
        if (score >= 50) return "C";
        return "F";
    }
}
