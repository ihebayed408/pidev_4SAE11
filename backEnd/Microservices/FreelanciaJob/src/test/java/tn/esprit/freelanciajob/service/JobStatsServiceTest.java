package tn.esprit.freelanciajob.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelanciajob.Dto.JobStats;
import tn.esprit.freelanciajob.Dto.projection.MonthlyJobProjection;
import tn.esprit.freelanciajob.Dto.projection.StatusCountProjection;
import tn.esprit.freelanciajob.Dto.response.JobStatsDTO;
import tn.esprit.freelanciajob.Repository.JobApplicationRepository;
import tn.esprit.freelanciajob.Repository.JobRepository;
import tn.esprit.freelanciajob.Service.JobStatsService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobStatsService – Unit Tests")
class JobStatsServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private JobApplicationRepository applicationRepository;

    @InjectMocks
    private JobStatsService jobStatsService;

    @Test
    @DisplayName("should aggregate all KPIs correctly")
    void getStats_aggregatesAllKPIs() {
        when(jobRepository.count()).thenReturn(10L);
        when(applicationRepository.count()).thenReturn(30L);
        when(applicationRepository.countUniqueFreelancers()).thenReturn(15L);

        StatusCountProjection openCount = mock(StatusCountProjection.class);
        when(openCount.getStatus()).thenReturn("OPEN");
        when(openCount.getCount()).thenReturn(5L);
        StatusCountProjection filledCount = mock(StatusCountProjection.class);
        when(filledCount.getStatus()).thenReturn("FILLED");
        when(filledCount.getCount()).thenReturn(3L);
        when(jobRepository.countByStatus()).thenReturn(List.of(openCount, filledCount));

        JobStats stats1 = new JobStats(1L, "Job A", 10L);
        JobStats stats2 = new JobStats(2L, "Job B", 8L);
        when(jobRepository.getJobsStatistics()).thenReturn(List.of(stats1, stats2));

        MonthlyJobProjection monthly = mock(MonthlyJobProjection.class);
        when(monthly.getMonth()).thenReturn("2025-01");
        when(monthly.getCount()).thenReturn(4L);
        when(jobRepository.getJobsPerMonth()).thenReturn(List.of(monthly));

        JobStatsDTO result = jobStatsService.getStats();

        assertThat(result.getTotalJobs()).isEqualTo(10L);
        assertThat(result.getAvgApplicationsPerJob()).isEqualTo(3.0);
        assertThat(result.getUniqueFreelancers()).isEqualTo(15L);
        assertThat(result.getJobsByStatus()).containsEntry("OPEN", 5L);
        assertThat(result.getJobsByStatus()).containsEntry("FILLED", 3L);
        assertThat(result.getTop5Jobs()).hasSize(2);
        assertThat(result.getJobsPerMonth()).hasSize(1);
    }

    @Test
    @DisplayName("should return zero avg when no jobs exist")
    void noJobs_zeroAvg() {
        when(jobRepository.count()).thenReturn(0L);
        when(applicationRepository.count()).thenReturn(0L);
        when(applicationRepository.countUniqueFreelancers()).thenReturn(0L);
        when(jobRepository.countByStatus()).thenReturn(List.of());
        when(jobRepository.getJobsStatistics()).thenReturn(List.of());
        when(jobRepository.getJobsPerMonth()).thenReturn(List.of());

        JobStatsDTO result = jobStatsService.getStats();

        assertThat(result.getTotalJobs()).isZero();
        assertThat(result.getAvgApplicationsPerJob()).isZero();
        assertThat(result.getJobsByStatus()).isEmpty();
        assertThat(result.getTop5Jobs()).isEmpty();
    }

    @Test
    @DisplayName("should limit top jobs to 5")
    void manyJobs_limitsToTop5() {
        when(jobRepository.count()).thenReturn(10L);
        when(applicationRepository.count()).thenReturn(50L);
        when(applicationRepository.countUniqueFreelancers()).thenReturn(20L);
        when(jobRepository.countByStatus()).thenReturn(List.of());

        List<JobStats> sevenJobs = List.of(
                new JobStats(1L, "J1", 10L), new JobStats(2L, "J2", 9L),
                new JobStats(3L, "J3", 8L), new JobStats(4L, "J4", 7L),
                new JobStats(5L, "J5", 6L), new JobStats(6L, "J6", 5L),
                new JobStats(7L, "J7", 4L)
        );
        when(jobRepository.getJobsStatistics()).thenReturn(sevenJobs);
        when(jobRepository.getJobsPerMonth()).thenReturn(List.of());

        JobStatsDTO result = jobStatsService.getStats();

        assertThat(result.getTop5Jobs()).hasSize(5);
    }
}
