package com.enterprise.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Number of users assigned to a location")
public record LocationUserCountResponse(

    @Schema(description = "Count of user assignments", example = "5")
    long count
) {}
