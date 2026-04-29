package tn.esprit.freelanciajob.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.freelanciajob.Controller.JobStatsController;
import tn.esprit.freelanciajob.Dto.response.JobStatsDTO;
import tn.esprit.freelanciajob.Service.JobStatsService;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobStatsController – Unit Tests")
class JobStatsControllerTest {

    @Mock private JobStatsService statsService;

    @InjectMocks
    private JobStatsController controller;

    @Test
    @DisplayName("GET /api/admin/job-stats should return 200 with stats")
    void getJobStats_returns200WithStats() {
        JobStatsDTO stats = JobStatsDTO.builder()
                .totalJobs(10L)
                .avgApplicationsPerJob(3.0)
                .uniqueFreelancers(5L)
                .jobsByStatus(Map.of("OPEN", 5L))
                .top5Jobs(Collections.emptyList())
                .jobsPerMonth(Collections.emptyList())
                .build();
        when(statsService.getStats()).thenReturn(stats);

        ResponseEntity<JobStatsDTO> result = controller.getJobStats();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getTotalJobs()).isEqualTo(10L);
    }
}
