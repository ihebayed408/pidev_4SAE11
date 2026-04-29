package tn.esprit.freelanciajob.specification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.freelanciajob.Dto.request.JobSearchRequest;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.Enums.ClientType;
import tn.esprit.freelanciajob.Entity.Enums.JobStatus;
import tn.esprit.freelanciajob.Entity.Enums.LocationType;
import tn.esprit.freelanciajob.Repository.JobRepository;
import tn.esprit.freelanciajob.Specification.JobSpecification;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("JobSpecification – Integration Tests")
class JobSpecificationTest {

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void setup() {
        jobRepository.deleteAll();

        jobRepository.save(Job.builder()
                .clientId(1L).clientType(ClientType.INDIVIDUAL)
                .title("Java Backend Developer").description("Build REST APIs with Spring Boot")
                .budgetMin(BigDecimal.valueOf(1000)).budgetMax(BigDecimal.valueOf(3000))
                .currency("USD").category("Backend").locationType(LocationType.REMOTE)
                .status(JobStatus.OPEN).requiredSkillIds(List.of(1L, 2L))
                .build());

        jobRepository.save(Job.builder()
                .clientId(2L).clientType(ClientType.COMPANY).companyName("ACME")
                .title("React Frontend Developer").description("Build modern UIs with React")
                .budgetMin(BigDecimal.valueOf(2000)).budgetMax(BigDecimal.valueOf(5000))
                .currency("USD").category("Frontend").locationType(LocationType.HYBRID)
                .status(JobStatus.OPEN).requiredSkillIds(List.of(3L, 4L))
                .build());

        jobRepository.save(Job.builder()
                .clientId(1L).clientType(ClientType.INDIVIDUAL)
                .title("Python Data Analyst").description("Analyze data with Python and ML")
                .budgetMin(BigDecimal.valueOf(500)).budgetMax(BigDecimal.valueOf(1500))
                .currency("USD").category("Data Science").locationType(LocationType.ONSITE)
                .status(JobStatus.FILLED).requiredSkillIds(List.of(5L))
                .build());
    }

    @Test
    @DisplayName("should return all jobs when no filters applied")
    void noFilters_returnsAll() {
        JobSearchRequest req = new JobSearchRequest();
        Page<Job> result = jobRepository.findAll(JobSpecification.build(req), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("should filter by keyword in title")
    void keywordInTitle_matchesCorrectly() {
        JobSearchRequest req = new JobSearchRequest();
        req.setKeyword("java");
        Page<Job> result = jobRepository.findAll(JobSpecification.build(req), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).contains("Java");
    }

    @Test
    @DisplayName("should filter by keyword in description")
    void keywordInDescription_matchesCorrectly() {
        JobSearchRequest req = new JobSearchRequest();
        req.setKeyword("react");
        Page<Job> result = jobRepository.findAll(JobSpecification.build(req), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("should filter by clientId")
    void clientIdFilter_matchesCorrectly() {
        JobSearchRequest req = new JobSearchRequest();
        req.setClientId(1L);
        Page<Job> result = jobRepository.findAll(JobSpecification.build(req), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("should filter by status")
    void statusFilter_matchesCorrectly() {
        JobSearchRequest req = new JobSearchRequest();
        req.setStatus(JobStatus.FILLED);
        Page<Job> result = jobRepository.findAll(JobSpecification.build(req), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("should filter by clientType")
    void clientTypeFilter_matchesCorrectly() {
        JobSearchRequest req = new JobSearchRequest();
        req.setClientType(ClientType.COMPANY);
        Page<Job> result = jobRepository.findAll(JobSpecification.build(req), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("should filter by locationType")
    void locationTypeFilter_matchesCorrectly() {
        JobSearchRequest req = new JobSearchRequest();
        req.setLocationType(LocationType.REMOTE);
        Page<Job> result = jobRepository.findAll(JobSpecification.build(req), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("should filter by budget range")
    void budgetRange_matchesOverlapping() {
        JobSearchRequest req = new JobSearchRequest();
        req.setBudgetMin(BigDecimal.valueOf(1500));
        req.setBudgetMax(BigDecimal.valueOf(4000));
        Page<Job> result = jobRepository.findAll(JobSpecification.build(req), PageRequest.of(0, 10));
        // Java job: max 3000 >= 1500 and min 1000 <= 4000 ✓
        // React job: max 5000 >= 1500 and min 2000 <= 4000 ✓
        // Python job: max 1500 >= 1500 and min 500 <= 4000 ✓
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("should filter by budgetMin only")
    void budgetMinOnly_matchesCorrectly() {
        JobSearchRequest req = new JobSearchRequest();
        req.setBudgetMin(BigDecimal.valueOf(4000));
        Page<Job> result = jobRepository.findAll(JobSpecification.build(req), PageRequest.of(0, 10));
        // Only React job has budgetMax (5000) >= 4000
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("should filter by category")
    void categoryFilter_matchesCorrectly() {
        JobSearchRequest req = new JobSearchRequest();
        req.setCategory("Backend");
        Page<Job> result = jobRepository.findAll(JobSpecification.build(req), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("should filter by skill IDs")
    void skillIdsFilter_matchesCorrectly() {
        JobSearchRequest req = new JobSearchRequest();
        req.setSkillIds(List.of(1L));
        Page<Job> result = jobRepository.findAll(JobSpecification.build(req), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("should combine multiple filters")
    void multipleFilters_combinedWithAnd() {
        JobSearchRequest req = new JobSearchRequest();
        req.setStatus(JobStatus.OPEN);
        req.setLocationType(LocationType.REMOTE);
        Page<Job> result = jobRepository.findAll(JobSpecification.build(req), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).contains("Java");
    }
}
