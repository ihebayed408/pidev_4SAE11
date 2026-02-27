package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Time-bounded progress report for a project")
public class ProgressReportDto {

    @Schema(description = "Project ID")
    private Long projectId;

    @Schema(description = "Report start date (inclusive)")
    private LocalDate from;

    @Schema(description = "Report end date (inclusive)")
    private LocalDate to;

    @Schema(description = "Number of progress updates in the period")
    private long updateCount;

    @Schema(description = "Total number of comments on progress updates in the period")
    private long commentCount;

    @Schema(description = "Average progress percentage over the period")
    private Double averageProgressPercentage;

    @Schema(description = "Timestamp of the first update in the period")
    private LocalDateTime firstUpdateAt;

    @Schema(description = "Timestamp of the last update in the period")
    private LocalDateTime lastUpdateAt;
}

