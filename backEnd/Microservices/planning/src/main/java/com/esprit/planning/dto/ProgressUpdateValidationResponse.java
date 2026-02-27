package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload for validating a progress update without persisting it")
public class ProgressUpdateValidationResponse {

    @Schema(description = "Whether the provided progress update is valid according to current rules")
    private boolean valid;

    @Schema(description = "Minimum allowed progress percentage for the project, considering existing updates")
    private Integer minAllowed;

    @Schema(description = "Progress percentage provided by the client for this validation")
    private Integer provided;

    @Schema(description = "List of validation error messages (empty when valid = true)")
    private List<String> errors;
}

