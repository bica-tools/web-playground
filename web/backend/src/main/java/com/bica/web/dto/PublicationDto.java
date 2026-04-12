package com.bica.web.dto;

import java.util.List;

public record PublicationDto(
        String id,
        String title,
        String layer,
        List<String> venues,
        String priority,
        String deadline,
        String status
) {}
