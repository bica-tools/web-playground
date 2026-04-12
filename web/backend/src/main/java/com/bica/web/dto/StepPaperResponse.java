package com.bica.web.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Full detail DTO for a single step paper view. */
public record StepPaperResponse(
        Long id,
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
        String venuePaperSlug,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ReactionCountResponse> reactionCounts,
        long commentCount) {
}
