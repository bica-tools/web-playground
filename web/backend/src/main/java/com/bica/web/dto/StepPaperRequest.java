package com.bica.web.dto;

/** Admin CRUD request for step papers. */
public record StepPaperRequest(
        String stepNumber,
        String slug,
        String title,
        String abstractText,
        String phase,
        String domain,
        String status,
        String proofBacking,
        String grade,
        Integer wordCount,
        String tags,
        String pdfPath,
        boolean hasProofsTex,
        String leanFiles,
        String reticulateModule,
        String bicaPackage,
        String dependsOn,
        String relatedSteps,
        String supersededBy,
        int version,
        String revisionNotes,
        String blogSlugs,
        String venuePaperSlug) {
}
