package com.bica.web.dto;

import java.util.List;

public record CompositionRequest(
        List<ParticipantEntry> participants,
        String globalType
) {
    public record ParticipantEntry(String name, String typeString) {}
}
