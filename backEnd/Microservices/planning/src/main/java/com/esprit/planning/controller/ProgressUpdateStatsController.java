package com.esprit.planning.controller;

import com.esprit.planning.dto.ContractProgressStatsDto;
import com.esprit.planning.dto.DashboardStatsDto;
import com.esprit.planning.dto.FreelancerProgressStatsDto;
import com.esprit.planning.dto.ProgressReportDto;
import com.esprit.planning.dto.ProjectProgressStatsDto;
import com.esprit.planning.service.ProgressUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/progress-updates/stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Progress Update Statistics", description = "Statistics and dashboard stats for progress updates")
public class ProgressUpdateStatsController {

    private final ProgressUpdateService progressUpdateService;

    @GetMapping("/freelancer/{freelancerId}")
    @Operation(summary = "Stats by freelancer", description = "Returns progress statistics for the given freelancer.")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = FreelancerProgressStatsDto.class)))
    public ResponseEntity<FreelancerProgressStatsDto> getStatsByFreelancer(
            @Parameter(description = "Freelancer ID", example = "10", required = true) @PathVariable Long freelancerId) {
        return ResponseEntity.ok(progressUpdateService.getProgressStatisticsByFreelancer(freelancerId));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Stats by project", description = "Returns progress statistics for the given project.")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = ProjectProgressStatsDto.class)))
    public ResponseEntity<ProjectProgressStatsDto> getStatsByProject(
            @Parameter(description = "Project ID", example = "1", required = true) @PathVariable Long projectId) {
        return ResponseEntity.ok(progressUpdateService.getProgressStatisticsByProject(projectId));
    }

    @GetMapping("/contract/{contractId}")
    @Operation(summary = "Stats by contract", description = "Returns progress statistics for the given contract.")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = ContractProgressStatsDto.class)))
    public ResponseEntity<ContractProgressStatsDto> getStatsByContract(
            @Parameter(description = "Contract ID", example = "1", required = true) @PathVariable Long contractId) {
        return ResponseEntity.ok(progressUpdateService.getProgressStatisticsByContract(contractId));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard stats", description = "Returns global dashboard statistics over all progress updates.")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = DashboardStatsDto.class)))
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(progressUpdateService.getDashboardStatistics());
    }

    @GetMapping("/report")
    @Operation(
            summary = "Time-bounded project report",
            description = "Returns a time-bounded report (updates, comments, averages) for a single project between from/to dates. If from/to are omitted, defaults to the last 30 days."
    )
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = ProgressReportDto.class)))
    public ResponseEntity<ProgressReportDto> getProjectReport(
            @Parameter(description = "Project ID", example = "1", required = true) @RequestParam Long projectId,
            @Parameter(description = "Start date (yyyy-MM-dd), optional") @RequestParam(required = false) java.time.LocalDate from,
            @Parameter(description = "End date (yyyy-MM-dd), optional") @RequestParam(required = false) java.time.LocalDate to
    ) {
        return ResponseEntity.ok(progressUpdateService.getProgressReportForProject(projectId, from, to));
    }
}
