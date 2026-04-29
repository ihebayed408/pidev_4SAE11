package tn.esprit.freelanciajob.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tn.esprit.freelanciajob.Dto.request.JobRequest;
import tn.esprit.freelanciajob.Dto.response.JobApplicationResponse;
import tn.esprit.freelanciajob.Dto.response.JobResponse;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.JobApplication;
import tn.esprit.freelanciajob.Entity.Enums.ApplicationStatus;
import tn.esprit.freelanciajob.Entity.Enums.ClientType;
import tn.esprit.freelanciajob.Entity.Enums.JobStatus;
import tn.esprit.freelanciajob.Entity.Enums.LocationType;
import tn.esprit.freelanciajob.Mapper.JobMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JobMapper – Unit Tests")
class JobMapperTest {

    @Nested
    @DisplayName("toEntity()")
    class ToEntityTests {

        @Test
        @DisplayName("should map all fields from request to entity")
        void allFields_mappedCorrectly() {
            JobRequest req = new JobRequest();
            req.setClientId(1L);
            req.setClientType(ClientType.COMPANY);
            req.setCompanyName("ACME");
            req.setTitle("Developer");
            req.setDescription("Build stuff");
            req.setBudgetMin(BigDecimal.valueOf(1000));
            req.setBudgetMax(BigDecimal.valueOf(5000));
            req.setCurrency("EUR");
            req.setCategory("Engineering");
            req.setLocationType(LocationType.HYBRID);
            req.setRequiredSkillIds(List.of(1L, 2L));

            Job entity = JobMapper.toEntity(req);

            assertThat(entity.getClientId()).isEqualTo(1L);
            assertThat(entity.getClientType()).isEqualTo(ClientType.COMPANY);
            assertThat(entity.getCompanyName()).isEqualTo("ACME");
            assertThat(entity.getTitle()).isEqualTo("Developer");
            assertThat(entity.getDescription()).isEqualTo("Build stuff");
            assertThat(entity.getBudgetMin()).isEqualByComparingTo(BigDecimal.valueOf(1000));
            assertThat(entity.getBudgetMax()).isEqualByComparingTo(BigDecimal.valueOf(5000));
            assertThat(entity.getCurrency()).isEqualTo("EUR");
            assertThat(entity.getCategory()).isEqualTo("Engineering");
            assertThat(entity.getLocationType()).isEqualTo(LocationType.HYBRID);
            assertThat(entity.getRequiredSkillIds()).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("should default to empty list when requiredSkillIds is null")
        void nullSkillIds_defaultsToEmptyList() {
            JobRequest req = new JobRequest();
            req.setRequiredSkillIds(null);

            Job entity = JobMapper.toEntity(req);

            assertThat(entity.getRequiredSkillIds()).isEmpty();
        }
    }

    @Nested
    @DisplayName("toDto()")
    class ToDtoTests {

        @Test
        @DisplayName("should map all fields from entity to DTO")
        void allFields_mappedCorrectly() {
            Job job = Job.builder()
                    .id(1L).clientId(10L).clientType(ClientType.INDIVIDUAL)
                    .companyName("Solo").title("Dev").description("Code")
                    .budgetMin(BigDecimal.valueOf(500)).budgetMax(BigDecimal.valueOf(2000))
                    .currency("USD").category("Tech")
                    .locationType(LocationType.REMOTE).status(JobStatus.OPEN)
                    .requiredSkillIds(List.of(3L))
                    .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                    .updatedAt(LocalDateTime.of(2025, 1, 2, 0, 0))
                    .build();

            JobResponse dto = JobMapper.toDto(job);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getClientType()).isEqualTo("INDIVIDUAL");
            assertThat(dto.getLocationType()).isEqualTo("REMOTE");
            assertThat(dto.getStatus()).isEqualTo("OPEN");
            assertThat(dto.getRequiredSkillIds()).containsExactly(3L);
        }

        @Test
        @DisplayName("should handle null optional enums gracefully")
        void nullEnums_returnNull() {
            Job job = new Job();
            job.setId(1L);
            // clientType and locationType are null; status defaults to OPEN

            JobResponse dto = JobMapper.toDto(job);

            assertThat(dto.getClientType()).isNull();
            assertThat(dto.getLocationType()).isNull();
            assertThat(dto.getStatus()).isEqualTo("OPEN"); // default value
        }
    }

    @Nested
    @DisplayName("toApplicationDto()")
    class ToApplicationDtoTests {

        @Test
        @DisplayName("should map all fields from application entity")
        void allFields_mappedCorrectly() {
            Job job = Job.builder().id(10L).title("Backend Dev").build();
            JobApplication app = JobApplication.builder()
                    .id(1L).job(job).freelancerId(20L)
                    .proposalMessage("I'm interested")
                    .expectedRate(BigDecimal.valueOf(75))
                    .status(ApplicationStatus.PENDING)
                    .createdAt(LocalDateTime.of(2025, 3, 1, 0, 0))
                    .build();

            JobApplicationResponse dto = JobMapper.toApplicationDto(app);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getJobId()).isEqualTo(10L);
            assertThat(dto.getJobTitle()).isEqualTo("Backend Dev");
            assertThat(dto.getFreelancerId()).isEqualTo(20L);
            assertThat(dto.getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("should handle null job gracefully")
        void nullJob_fieldsNull() {
            JobApplication app = JobApplication.builder()
                    .id(1L).job(null).freelancerId(20L)
                    .status(ApplicationStatus.PENDING).build();

            JobApplicationResponse dto = JobMapper.toApplicationDto(app);

            assertThat(dto.getJobId()).isNull();
            assertThat(dto.getJobTitle()).isNull();
        }

        @Test
        @DisplayName("should handle null status")
        void nullStatus_returnsNull() {
            JobApplication app = JobApplication.builder()
                    .id(1L).job(Job.builder().id(10L).build())
                    .freelancerId(20L).status(null).build();

            JobApplicationResponse dto = JobMapper.toApplicationDto(app);

            assertThat(dto.getStatus()).isNull();
        }
    }
}
