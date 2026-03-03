package com.esprit.planning.service;

import com.esprit.planning.client.ProjectClient;
import com.esprit.planning.dto.*;
import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.exception.ProgressCannotDecreaseException;
import com.esprit.planning.repository.ProgressCommentRepository;
import com.esprit.planning.repository.ProgressUpdateRepository;
import com.esprit.planning.repository.ProjectDeadlineSyncRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProgressUpdateService. Verifies CRUD, finders, validation, stats, rankings,
 * and ensureProjectDeadlineInCalendarForUser with mocked repositories and clients.
 */
@ExtendWith(MockitoExtension.class)
class ProgressUpdateServiceTest {

    @Mock
    private ProgressUpdateRepository progressUpdateRepository;

    @Mock
    private ProgressCommentRepository progressCommentRepository;

    @Mock
    private PlanningNotificationService planningNotificationService;

    @Mock
    private ProjectClient projectClient;

    @Mock
    private GoogleCalendarService googleCalendarService;

    @Mock
    private ProjectDeadlineSyncRepository projectDeadlineSyncRepository;

    @InjectMocks
    private ProgressUpdateService progressUpdateService;

    @Test
    void findAll_returnsAllFromRepository() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 50);
        when(progressUpdateRepository.findAll()).thenReturn(List.of(u));

        List<ProgressUpdate> result = progressUpdateService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("T");
        verify(progressUpdateRepository).findAll();
    }

    @Test
    void findById_whenFound_returnsUpdate() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "Found", 75);
        when(progressUpdateRepository.findById(1L)).thenReturn(Optional.of(u));

        ProgressUpdate result = progressUpdateService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Found");
    }

    @Test
    void findById_whenNotFound_throws() {
        when(progressUpdateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressUpdateService.findById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ProgressUpdate not found");
    }

    @Test
    void findByProjectId_returnsListFromRepository() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(progressUpdate(1L, 1L, 10L, "T", 50)));

        List<ProgressUpdate> result = progressUpdateService.findByProjectId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo(1L);
    }

    @Test
    void findLatestByProjectId_whenFound_returnsOptional() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "Latest", 80);
        when(progressUpdateRepository.findFirstByProjectIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(u));

        Optional<ProgressUpdate> result = progressUpdateService.findLatestByProjectId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Latest");
    }

    @Test
    void findLatestByProjectId_whenNotFound_returnsEmpty() {
        when(progressUpdateRepository.findFirstByProjectIdOrderByCreatedAtDesc(999L)).thenReturn(Optional.empty());

        Optional<ProgressUpdate> result = progressUpdateService.findLatestByProjectId(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void getNextAllowedPercentageForProject_returnsMaxFromRepository() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 60);
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(u));

        Integer result = progressUpdateService.getNextAllowedPercentageForProject(1L);

        assertThat(result).isEqualTo(60);
    }

    @Test
    void validate_validRequest_returnsValidResponse() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of());
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setProjectId(1L);
        request.setFreelancerId(10L);
        request.setProgressPercentage(50);

        ProgressUpdateValidationResponse result = progressUpdateService.validate(request);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getMinAllowed()).isEqualTo(0);
        assertThat(result.getProvided()).isEqualTo(50);
    }

    @Test
    void validate_missingProjectId_returnsInvalid() {
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setFreelancerId(10L);
        request.setProgressPercentage(50);

        ProgressUpdateValidationResponse result = progressUpdateService.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("projectId"));
    }

    @Test
    void validate_progressDecreases_returnsInvalid() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(progressUpdate(1L, 1L, 10L, "T", 70)));
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setProjectId(1L);
        request.setFreelancerId(10L);
        request.setProgressPercentage(50);

        ProgressUpdateValidationResponse result = progressUpdateService.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMinAllowed()).isEqualTo(70);
        assertThat(result.getErrors()).anyMatch(e -> e.contains("cannot be less"));
    }

    @Test
    void create_whenProgressDecreases_throwsProgressCannotDecreaseException() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(progressUpdate(1L, 1L, 10L, "T", 80)));
        ProgressUpdate toCreate = progressUpdate(null, 1L, 10L, "New", 30);

        assertThatThrownBy(() -> progressUpdateService.create(toCreate))
                .isInstanceOf(ProgressCannotDecreaseException.class)
                .satisfies(ex -> {
                    ProgressCannotDecreaseException e = (ProgressCannotDecreaseException) ex;
                    assertThat(e.getMinAllowed()).isEqualTo(80);
                    assertThat(e.getProvided()).isEqualTo(30);
                });
    }

    @Test
    void create_valid_savesAndReturns() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of());
        ProgressUpdate toCreate = progressUpdate(null, 1L, 10L, "New", 25);
        ProgressUpdate saved = progressUpdate(1L, 1L, 10L, "New", 25);
        when(progressUpdateRepository.save(any(ProgressUpdate.class))).thenReturn(saved);

        ProgressUpdate result = progressUpdateService.create(toCreate);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(progressUpdateRepository).save(any(ProgressUpdate.class));
    }

    @Test
    void deleteById_delegatesToRepository() {
        ProgressUpdate existing = progressUpdate(1L, 1L, 10L, "T", 50);
        when(progressUpdateRepository.findById(1L)).thenReturn(Optional.of(existing));

        progressUpdateService.deleteById(1L);

        verify(progressUpdateRepository).deleteById(1L);
    }

    @Test
    void getProgressTrendByProject_returnsTrendFromRepository() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 40);
        u.setCreatedAt(LocalDateTime.now());
        when(progressUpdateRepository.findByProjectIdAndCreatedAtBetween(eq(1L), any(), any())).thenReturn(List.of(u));

        List<ProgressTrendPointDto> result = progressUpdateService.getProgressTrendByProject(
                1L, LocalDate.now().minusDays(7), LocalDate.now());

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getProgressPercentage()).isEqualTo(40);
    }

    @Test
    void getDashboardStatistics_returnsAggregatedStats() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 50);
        when(progressUpdateRepository.findAll()).thenReturn(List.of(u));
        when(progressCommentRepository.countByProgressUpdate_IdIn(anyList())).thenReturn(0L);

        DashboardStatsDto result = progressUpdateService.getDashboardStatistics();

        assertThat(result.getTotalUpdates()).isEqualTo(1);
        assertThat(result.getDistinctProjectCount()).isEqualTo(1);
        assertThat(result.getDistinctFreelancerCount()).isEqualTo(1);
    }

    @Test
    void getProjectIdsWithStalledProgress_returnsStalledFromRepository() {
        LocalDateTime old = LocalDateTime.now().minusDays(10);
        Object[] row = new Object[]{1L, old};
        when(progressUpdateRepository.findProjectIdAndMaxUpdatedAt())
                .thenReturn(List.<Object[]>of(row));
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 30);
        u.setUpdatedAt(old);
        when(progressUpdateRepository.findByProjectIdAndUpdatedAt(1L, old)).thenReturn(Optional.of(u));

        List<StalledProjectDto> result = progressUpdateService.getProjectIdsWithStalledProgress(7);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo(1L);
        assertThat(result.get(0).getLastProgressPercentage()).isEqualTo(30);
    }

    @Test
    void getFreelancersByActivity_returnsFromRepository() {
        when(progressUpdateRepository.findFreelancerIdAndUpdateCountOrderByCountDesc(any(PageRequest.class)))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 5L}));
        when(progressCommentRepository.countByProgressUpdate_FreelancerId(10L)).thenReturn(2L);

        List<FreelancerActivityDto> result = progressUpdateService.getFreelancersByActivity(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFreelancerId()).isEqualTo(10L);
        assertThat(result.get(0).getUpdateCount()).isEqualTo(5);
        assertThat(result.get(0).getCommentCount()).isEqualTo(2);
    }

    @Test
    void getMostActiveProjects_returnsFromRepository() {
        when(progressUpdateRepository.findProjectIdAndUpdateCountOrderByCountDescBetween(any(), any(), any(PageRequest.class)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 8L}));

        List<ProjectActivityDto> result = progressUpdateService.getMostActiveProjects(10, Optional.empty(), Optional.empty());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo(1L);
        assertThat(result.get(0).getUpdateCount()).isEqualTo(8);
    }

    @Test
    void getProgressStatisticsByFreelancer_returnsDto() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 50);
        u.setUpdatedAt(LocalDateTime.now());
        when(progressUpdateRepository.findByFreelancerId(10L)).thenReturn(List.of(u));
        when(progressCommentRepository.countByProgressUpdate_IdIn(List.of(1L))).thenReturn(1L);

        FreelancerProgressStatsDto result = progressUpdateService.getProgressStatisticsByFreelancer(10L);

        assertThat(result.getFreelancerId()).isEqualTo(10L);
        assertThat(result.getTotalUpdates()).isEqualTo(1);
        assertThat(result.getTotalComments()).isEqualTo(1);
    }

    @Test
    void getProgressStatisticsByProject_returnsDto() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 60);
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(u));
        when(progressCommentRepository.countByProgressUpdate_IdIn(List.of(1L))).thenReturn(0L);

        ProjectProgressStatsDto result = progressUpdateService.getProgressStatisticsByProject(1L);

        assertThat(result.getProjectId()).isEqualTo(1L);
        assertThat(result.getUpdateCount()).isEqualTo(1);
        assertThat(result.getCurrentProgressPercentage()).isEqualTo(60);
    }

    @Test
    void findAllFilteredForExport_delegatesToRepositoryWithSpec() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 50);
        when(progressUpdateRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(u));

        List<ProgressUpdate> result = progressUpdateService.findAllFilteredForExport(
                Optional.of(1L), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        assertThat(result).hasSize(1);
        verify(progressUpdateRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void findByContractId_returnsListFromRepository() {
        when(progressUpdateRepository.findByContractId(5L)).thenReturn(List.of(progressUpdate(1L, 1L, 10L, "T", 50)));

        List<ProgressUpdate> result = progressUpdateService.findByContractId(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo(1L);
    }

    @Test
    void findByFreelancerId_returnsListFromRepository() {
        when(progressUpdateRepository.findByFreelancerId(10L)).thenReturn(List.of(progressUpdate(1L, 1L, 10L, "T", 50)));

        List<ProgressUpdate> result = progressUpdateService.findByFreelancerId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFreelancerId()).isEqualTo(10L);
    }

    @Test
    void findLatestByFreelancerId_whenFound_returnsOptional() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "Latest", 80);
        when(progressUpdateRepository.findFirstByFreelancerIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(u));

        Optional<ProgressUpdate> result = progressUpdateService.findLatestByFreelancerId(10L);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Latest");
    }

    @Test
    void findLatestByContractId_whenFound_returnsOptional() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "Contract Latest", 90);
        when(progressUpdateRepository.findFirstByContractIdOrderByCreatedAtDesc(5L)).thenReturn(Optional.of(u));

        Optional<ProgressUpdate> result = progressUpdateService.findLatestByContractId(5L);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Contract Latest");
    }

    @Test
    void update_whenProgressDecreases_throwsProgressCannotDecreaseException() {
        ProgressUpdate existing = progressUpdate(1L, 1L, 10L, "Existing", 80);
        ProgressUpdate otherUpdate = progressUpdate(2L, 1L, 10L, "Other", 85);
        ProgressUpdate updated = progressUpdate(1L, 1L, 10L, "Updated", 30);
        when(progressUpdateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(existing, otherUpdate));

        assertThatThrownBy(() -> progressUpdateService.update(1L, updated))
                .isInstanceOf(ProgressCannotDecreaseException.class);
    }

    @Test
    void update_valid_savesAndReturns() {
        ProgressUpdate existing = progressUpdate(1L, 1L, 10L, "Old", 40);
        ProgressUpdate updated = progressUpdate(1L, 1L, 10L, "Updated", 60);
        when(progressUpdateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(existing));
        when(progressUpdateRepository.save(any(ProgressUpdate.class))).thenReturn(updated);

        ProgressUpdate result = progressUpdateService.update(1L, updated);

        assertThat(result).isNotNull();
        verify(progressUpdateRepository).save(any(ProgressUpdate.class));
    }

    @Test
    void getProgressReportForProject_returnsReportDto() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 50);
        u.setCreatedAt(LocalDateTime.now().minusDays(5));
        u.setUpdatedAt(LocalDateTime.now());
        when(progressUpdateRepository.findByProjectIdAndCreatedAtBetween(eq(1L), any(), any())).thenReturn(List.of(u));
        when(progressCommentRepository.countByProgressUpdate_IdIn(List.of(1L))).thenReturn(2L);

        var result = progressUpdateService.getProgressReportForProject(1L, LocalDate.now().minusDays(30), LocalDate.now());

        assertThat(result.getProjectId()).isEqualTo(1L);
        assertThat(result.getUpdateCount()).isEqualTo(1);
        assertThat(result.getCommentCount()).isEqualTo(2);
        assertThat(result.getAverageProgressPercentage()).isEqualTo(50.0);
    }

    @Test
    void getProgressReportForProject_withNullDates_usesDefaults() {
        when(progressUpdateRepository.findByProjectIdAndCreatedAtBetween(eq(1L), any(), any())).thenReturn(List.of());

        var result = progressUpdateService.getProgressReportForProject(1L, null, null);

        assertThat(result.getProjectId()).isEqualTo(1L);
        assertThat(result.getUpdateCount()).isEqualTo(0);
    }

    @Test
    void getProgressStatisticsByContract_returnsDto() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 70);
        when(progressUpdateRepository.findByContractId(5L)).thenReturn(List.of(u));
        when(progressCommentRepository.countByProgressUpdate_IdIn(List.of(1L))).thenReturn(0L);

        var result = progressUpdateService.getProgressStatisticsByContract(5L);

        assertThat(result.getContractId()).isEqualTo(5L);
        assertThat(result.getUpdateCount()).isEqualTo(1);
        assertThat(result.getCurrentProgressPercentage()).isEqualTo(70);
    }

    @Test
    void validate_missingFreelancerId_returnsInvalid() {
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setProjectId(1L);
        request.setProgressPercentage(50);

        ProgressUpdateValidationResponse result = progressUpdateService.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("freelancerId"));
    }

    @Test
    void validate_invalidProgressRange_returnsInvalid() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of());
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setProjectId(1L);
        request.setFreelancerId(10L);
        request.setProgressPercentage(150);

        ProgressUpdateValidationResponse result = progressUpdateService.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("between 0 and 100"));
    }

    @Test
    void validate_nullProgress_returnsInvalid() {
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setProjectId(1L);
        request.setFreelancerId(10L);

        ProgressUpdateValidationResponse result = progressUpdateService.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("progressPercentage"));
    }

    @Test
    void getMaxProgressPercentageForProject_excludesUpdateId() {
        ProgressUpdate u1 = progressUpdate(1L, 1L, 10L, "U1", 60);
        ProgressUpdate u2 = progressUpdate(2L, 1L, 10L, "U2", 80);
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(u1, u2));

        int result = progressUpdateService.getMaxProgressPercentageForProject(1L, 2L);

        assertThat(result).isEqualTo(60);
    }

    @Test
    void ensureProjectDeadlineInCalendarForUser_whenAlreadySynced_returnsEarly() {
        when(projectDeadlineSyncRepository.findByProjectId(1L)).thenReturn(
                Optional.of(com.esprit.planning.entity.ProjectDeadlineSync.builder()
                        .projectId(1L).calendarEventId("evt1").syncedAt(LocalDateTime.now()).build()));

        progressUpdateService.ensureProjectDeadlineInCalendarForUser(1L, 10L);

        verify(projectDeadlineSyncRepository).findByProjectId(1L);
        verify(projectClient, never()).getProjectById(any());
    }

    private static ProgressUpdate progressUpdate(Long id, Long projectId, Long freelancerId, String title, int pct) {
        ProgressUpdate u = new ProgressUpdate();
        u.setId(id);
        u.setProjectId(projectId);
        u.setContractId(null);
        u.setFreelancerId(freelancerId);
        u.setTitle(title);
        u.setDescription(null);
        u.setProgressPercentage(pct);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        return u;
    }
}
