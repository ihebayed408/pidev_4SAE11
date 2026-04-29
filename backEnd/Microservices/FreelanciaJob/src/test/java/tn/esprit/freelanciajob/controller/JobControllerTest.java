package tn.esprit.freelanciajob.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.esprit.freelanciajob.Controller.ApiExceptionHandler;
import tn.esprit.freelanciajob.Controller.JobController;
import tn.esprit.freelanciajob.Dto.request.JobRequest;
import tn.esprit.freelanciajob.Dto.request.JobSearchRequest;
import tn.esprit.freelanciajob.Dto.response.JobResponse;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.Enums.ClientType;
import tn.esprit.freelanciajob.Entity.Enums.JobStatus;
import tn.esprit.freelanciajob.Entity.Enums.LocationType;
import tn.esprit.freelanciajob.Service.AiJobGeneratorService;
import tn.esprit.freelanciajob.Service.IJobService;
import tn.esprit.freelanciajob.Service.JobStatsService;
import tn.esprit.freelanciajob.Service.ProfileFitScoreService;
import tn.esprit.freelanciajob.Service.TranslationService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc tests for {@link JobController}.
 *
 * No Spring context is loaded — the controller is instantiated directly
 * with mocked services, making the tests fast and deterministic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JobController – Web Layer Tests")
class JobControllerTest {

    @Mock private IJobService           jobService;
    @Mock private TranslationService    translationService;
    @Mock private AiJobGeneratorService aiJobGeneratorService;
    @Mock private JobStatsService       jobStatsService;
    @Mock private ProfileFitScoreService fitScoreService;

    @InjectMocks
    private JobController jobController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(jobController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Job stubJob(Long id) {
        return Job.builder()
                .id(id).clientId(1L).clientType(ClientType.INDIVIDUAL)
                .title("Java Developer").description("Build microservices.")
                .budgetMin(BigDecimal.valueOf(2000)).budgetMax(BigDecimal.valueOf(5000))
                .currency("USD").category("Engineering").locationType(LocationType.REMOTE)
                .status(JobStatus.OPEN)
                .build();
    }

    private JobResponse stubResponse(Long id) {
        JobResponse r = new JobResponse();
        r.setId(id);
        r.setClientId(1L);
        r.setTitle("Java Developer");
        r.setCategory("Engineering");
        r.setStatus(JobStatus.OPEN.name());
        r.setSkills(Collections.emptyList());
        return r;
    }

    private JobRequest validRequest() {
        JobRequest r = new JobRequest();
        r.setClientId(1L);
        r.setClientType(ClientType.INDIVIDUAL);
        r.setTitle("Java Developer");
        r.setDescription("Build microservices with Spring Boot and Docker.");
        r.setBudgetMin(BigDecimal.valueOf(2000));
        r.setBudgetMax(BigDecimal.valueOf(5000));
        r.setCurrency("USD");
        r.setCategory("Engineering");
        r.setLocationType(LocationType.REMOTE);
        return r;
    }

    // ── POST /jobs/add ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /jobs/add – should return 201 CREATED with saved job")
    void addJob_validRequest_returns201() throws Exception {
        when(jobService.addJob(any())).thenReturn(stubJob(1L));

        mockMvc.perform(post("/jobs/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Java Developer"));
    }

    // ── GET /jobs/list ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /jobs/list – should return 200 with list of job responses")
    void getAll_returns200WithList() throws Exception {
        when(jobService.getAllJobResponses()).thenReturn(List.of(stubResponse(1L), stubResponse(2L)));

        mockMvc.perform(get("/jobs/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /jobs/list – should return 200 with empty array when no jobs exist")
    void getAll_noJobs_returnsEmptyArray() throws Exception {
        when(jobService.getAllJobResponses()).thenReturn(List.of());

        mockMvc.perform(get("/jobs/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /jobs/{id} ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /jobs/{id} – should return 200 with enriched job response")
    void getById_existingId_returns200() throws Exception {
        when(jobService.getJobResponse(1L)).thenReturn(stubResponse(1L));

        mockMvc.perform(get("/jobs/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /jobs/{id} – should return 400 when service throws RuntimeException (handled by ApiExceptionHandler)")
    void getById_notFound_returns400() throws Exception {
        when(jobService.getJobResponse(99L)).thenThrow(new RuntimeException("Job not found with id: 99"));

        mockMvc.perform(get("/jobs/{id}", 99L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Job not found with id: 99"));
    }

    // ── PUT /jobs/update/{id} ─────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /jobs/update/{id} – should return 200 with updated job")
    void updateJob_validRequest_returns200() throws Exception {
        when(jobService.updateJob(eq(1L), any())).thenReturn(stubJob(1L));

        mockMvc.perform(put("/jobs/update/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    // ── DELETE /jobs/{id} ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /jobs/{id} – should return 204 NO_CONTENT")
    void deleteJob_existingId_returns204() throws Exception {
        doNothing().when(jobService).deleteJob(1L);

        mockMvc.perform(delete("/jobs/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(jobService).deleteJob(1L);
    }

    // ── GET /jobs/recommended ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /jobs/recommended – should return 200 with recommended jobs for user")
    void getRecommended_returns200() throws Exception {
        when(jobService.getRecommendedJobs(5L)).thenReturn(List.of(stubResponse(1L)));

        mockMvc.perform(get("/jobs/recommended").param("userId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ── POST /jobs/filter ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /jobs/filter – should return 200 paged job results")
    void filterJobs_returns200WithPage() throws Exception {
        var page = new PageImpl<>(List.of(stubResponse(1L)));
        when(jobService.filterJobs(any(JobSearchRequest.class))).thenReturn(page);

        JobSearchRequest req = new JobSearchRequest();
        req.setPage(0);
        req.setSize(9);
        req.setSortBy("createdAt");
        req.setSortDir("desc");

        mockMvc.perform(post("/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    // ── GET /jobs/statistics ──────────────────────────────────────────────────

    @Test
    @DisplayName("GET /jobs/statistics – should return 200 with status count map")
    void getStatistics_returns200() throws Exception {
        when(jobService.getJobStatistics()).thenReturn(Map.of("OPEN", 5L, "FILLED", 2L));

        mockMvc.perform(get("/jobs/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.OPEN").value(5));
    }

    // ── GET /jobs/client/{clientId} ───────────────────────────────────────────

    @Test
    @DisplayName("GET /jobs/client/{id} – should return 200 with client's jobs")
    void getByClientId_returns200() throws Exception {
        when(jobService.getJobsByClientId(1L)).thenReturn(List.of(stubResponse(1L)));

        mockMvc.perform(get("/jobs/client/{clientId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
