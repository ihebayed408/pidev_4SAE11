package tn.esprit.freelanciajob.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.freelanciajob.Client.ExperienceClient;
import tn.esprit.freelanciajob.Client.SkillClient;
import tn.esprit.freelanciajob.Dto.ExperienceDto;
import tn.esprit.freelanciajob.Dto.Skills;
import tn.esprit.freelanciajob.Dto.response.FitScoreResponse;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.Enums.JobStatus;
import tn.esprit.freelanciajob.Entity.Enums.LocationType;
import tn.esprit.freelanciajob.Repository.JobRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileFitScoreService – Unit Tests")
class ProfileFitScoreServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private SkillClient skillClient;
    @Mock private ExperienceClient experienceClient;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private tn.esprit.freelanciajob.Service.ProfileFitScoreService fitScoreService;

    @Mock private RestTemplate restTemplate;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(fitScoreService, "apiUrl", "https://api.example.com/chat");
        ReflectionTestUtils.setField(fitScoreService, "apiKey", "test-key");
        ReflectionTestUtils.setField(fitScoreService, "apiModel", "test-model");
        ReflectionTestUtils.setField(fitScoreService, "restTemplate", restTemplate);
    }

    private Job buildJob(Long id) {
        return Job.builder()
                .id(id).title("Java Spring Developer")
                .description("Build REST APIs with Spring Boot and Java")
                .category("Backend").locationType(LocationType.REMOTE)
                .budgetMin(BigDecimal.valueOf(1000)).budgetMax(BigDecimal.valueOf(3000))
                .currency("USD").status(JobStatus.OPEN)
                .requiredSkillIds(List.of(1L, 2L))
                .build();
    }

    private Skills buildSkill(Long id, String name) {
        Skills s = new Skills();
        s.setId(id);
        s.setName(name);
        return s;
    }

    private ExperienceDto buildExperience(String title) {
        ExperienceDto e = new ExperienceDto();
        e.setTitle(title);
        return e;
    }

    @Nested
    @DisplayName("computeFitScore()")
    class ComputeFitScoreTests {

        @Test
        @DisplayName("should throw NOT_FOUND when job does not exist")
        void jobNotFound_throwsNotFound() {
            when(jobRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fitScoreService.computeFitScore(99L, 1L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Job not found");
        }

        @Test
        @DisplayName("should use fallback when API key is blank")
        void blankApiKey_usesFallback() {
            ReflectionTestUtils.setField(fitScoreService, "apiKey", "");
            when(jobRepository.findById(1L)).thenReturn(Optional.of(buildJob(1L)));
            when(skillClient.getSkillsByUserId(10L)).thenReturn(List.of(buildSkill(1L, "Java")));
            when(experienceClient.getExperiencesByUserId(10L)).thenReturn(Collections.emptyList());
            when(skillClient.getSkillsByIds(List.of(1L, 2L)))
                    .thenReturn(List.of(buildSkill(1L, "Java"), buildSkill(2L, "Spring Boot")));

            FitScoreResponse result = fitScoreService.computeFitScore(1L, 10L);

            assertThat(result).isNotNull();
            assertThat(result.getScore()).isGreaterThanOrEqualTo(0);
            assertThat(result.getTier()).isNotBlank();
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("should parse valid AI response and return fit score")
        void validAiResponse_returnsParsedScore() {
            when(jobRepository.findById(1L)).thenReturn(Optional.of(buildJob(1L)));
            when(skillClient.getSkillsByUserId(10L)).thenReturn(List.of(buildSkill(1L, "Java")));
            when(experienceClient.getExperiencesByUserId(10L)).thenReturn(List.of(buildExperience("Dev")));

            String aiJson = """
                    {"choices":[{"message":{"content":"{\\"score\\":85,\\"tier\\":\\"STRONG_MATCH\\",\\"summary\\":\\"Great fit\\",\\"matchedSkills\\":[\\"Java\\"],\\"missingSkills\\":[\\"Spring\\"],\\"recommendations\\":[\\"Learn Spring\\"]}"}}]}
                    """;
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(aiJson));

            FitScoreResponse result = fitScoreService.computeFitScore(1L, 10L);

            assertThat(result.getScore()).isEqualTo(85);
            assertThat(result.getTier()).isEqualTo("STRONG_MATCH");
            assertThat(result.getMatchedSkills()).contains("Java");
            assertThat(result.getMissingSkills()).contains("Spring");
        }

        @Test
        @DisplayName("should fall back when AI call throws exception")
        void aiCallFails_usesFallback() {
            when(jobRepository.findById(1L)).thenReturn(Optional.of(buildJob(1L)));
            when(skillClient.getSkillsByUserId(10L)).thenReturn(List.of(buildSkill(1L, "Java")));
            when(experienceClient.getExperiencesByUserId(10L)).thenReturn(Collections.emptyList());
            when(skillClient.getSkillsByIds(List.of(1L, 2L)))
                    .thenReturn(List.of(buildSkill(1L, "Java"), buildSkill(2L, "Spring Boot")));
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            FitScoreResponse result = fitScoreService.computeFitScore(1L, 10L);

            assertThat(result).isNotNull();
            assertThat(result.getScore()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should handle AI response with invalid tier and derive from score")
        void invalidTier_derivedFromScore() {
            when(jobRepository.findById(1L)).thenReturn(Optional.of(buildJob(1L)));
            when(skillClient.getSkillsByUserId(10L)).thenReturn(Collections.emptyList());
            when(experienceClient.getExperiencesByUserId(10L)).thenReturn(Collections.emptyList());

            String aiJson = """
                    {"choices":[{"message":{"content":"{\\"score\\":45,\\"tier\\":\\"INVALID_TIER\\",\\"summary\\":\\"Ok match\\",\\"matchedSkills\\":[],\\"missingSkills\\":[],\\"recommendations\\":[]}"}}]}
                    """;
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(aiJson));

            FitScoreResponse result = fitScoreService.computeFitScore(1L, 10L);

            assertThat(result.getTier()).isEqualTo("PARTIAL_MATCH");
        }

        @Test
        @DisplayName("should handle AI response wrapped in markdown fences")
        void markdownFences_strippedAndParsed() {
            when(jobRepository.findById(1L)).thenReturn(Optional.of(buildJob(1L)));
            when(skillClient.getSkillsByUserId(10L)).thenReturn(Collections.emptyList());
            when(experienceClient.getExperiencesByUserId(10L)).thenReturn(Collections.emptyList());

            String content = "```json\\n{\\\"score\\\":70,\\\"tier\\\":\\\"GOOD_MATCH\\\",\\\"summary\\\":\\\"Decent\\\",\\\"matchedSkills\\\":[],\\\"missingSkills\\\":[],\\\"recommendations\\\":[]}\\n```";
            String aiJson = "{\"choices\":[{\"message\":{\"content\":\"" + content + "\"}}]}";
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(aiJson));

            FitScoreResponse result = fitScoreService.computeFitScore(1L, 10L);

            assertThat(result.getScore()).isEqualTo(70);
        }
    }

    @Nested
    @DisplayName("Fallback fit score calculation")
    class FallbackTests {

        @Test
        @DisplayName("should compute score with matching skills")
        void matchingSkills_higherScore() {
            ReflectionTestUtils.setField(fitScoreService, "apiKey", "");
            Job job = buildJob(1L);
            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
            when(skillClient.getSkillsByUserId(10L))
                    .thenReturn(List.of(buildSkill(1L, "Java"), buildSkill(2L, "Spring Boot")));
            when(experienceClient.getExperiencesByUserId(10L))
                    .thenReturn(List.of(buildExperience("Backend Dev")));
            when(skillClient.getSkillsByIds(List.of(1L, 2L)))
                    .thenReturn(List.of(buildSkill(1L, "Java"), buildSkill(2L, "Spring Boot")));

            FitScoreResponse result = fitScoreService.computeFitScore(1L, 10L);

            assertThat(result.getMatchedSkills()).isNotEmpty();
            assertThat(result.getScore()).isGreaterThan(50);
        }

        @Test
        @DisplayName("should compute low score with no matching skills")
        void noMatchingSkills_lowerScore() {
            ReflectionTestUtils.setField(fitScoreService, "apiKey", "");
            Job job = buildJob(1L);
            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
            when(skillClient.getSkillsByUserId(10L))
                    .thenReturn(List.of(buildSkill(99L, "Cooking")));
            when(experienceClient.getExperiencesByUserId(10L)).thenReturn(Collections.emptyList());
            when(skillClient.getSkillsByIds(List.of(1L, 2L)))
                    .thenReturn(List.of(buildSkill(1L, "Java"), buildSkill(2L, "Spring Boot")));

            FitScoreResponse result = fitScoreService.computeFitScore(1L, 10L);

            assertThat(result.getMissingSkills()).isNotEmpty();
            assertThat(result.getRecommendations()).isNotEmpty();
        }

        @Test
        @DisplayName("should infer skills from job description when skill IDs resolution fails")
        void skillIdResolutionFails_infersFromText() {
            ReflectionTestUtils.setField(fitScoreService, "apiKey", "");
            Job job = buildJob(1L);
            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
            when(skillClient.getSkillsByUserId(10L)).thenReturn(Collections.emptyList());
            when(experienceClient.getExperiencesByUserId(10L)).thenReturn(Collections.emptyList());
            when(skillClient.getSkillsByIds(any())).thenThrow(new RuntimeException("Service down"));

            FitScoreResponse result = fitScoreService.computeFitScore(1L, 10L);

            assertThat(result).isNotNull();
            assertThat(result.getMissingSkills()).isNotEmpty();
        }

        @Test
        @DisplayName("should handle job with no required skill IDs")
        void noRequiredSkillIds_usesInference() {
            ReflectionTestUtils.setField(fitScoreService, "apiKey", "");
            Job job = buildJob(1L);
            job.setRequiredSkillIds(null);
            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
            when(skillClient.getSkillsByUserId(10L)).thenReturn(Collections.emptyList());
            when(experienceClient.getExperiencesByUserId(10L)).thenReturn(Collections.emptyList());

            FitScoreResponse result = fitScoreService.computeFitScore(1L, 10L);

            assertThat(result).isNotNull();
            assertThat(result.getScore()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should add experience bonus to base score")
        void withExperiences_addsBonus() {
            ReflectionTestUtils.setField(fitScoreService, "apiKey", "");
            Job job = buildJob(1L);
            job.setRequiredSkillIds(Collections.emptyList());
            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
            when(skillClient.getSkillsByUserId(10L)).thenReturn(Collections.emptyList());
            when(experienceClient.getExperiencesByUserId(10L))
                    .thenReturn(List.of(buildExperience("Dev1"), buildExperience("Dev2"),
                            buildExperience("Dev3"), buildExperience("Dev4")));

            FitScoreResponse result = fitScoreService.computeFitScore(1L, 10L);

            // Experience bonus = min(20, 4*5) = 20; added to skill-based score
            assertThat(result.getScore()).isGreaterThanOrEqualTo(20);
            assertThat(result.getRecommendations()).isNotEmpty();
        }

        @Test
        @DisplayName("should handle skill client returning null")
        void skillClientReturnsNull_usesEmptyList() {
            ReflectionTestUtils.setField(fitScoreService, "apiKey", "");
            when(jobRepository.findById(1L)).thenReturn(Optional.of(buildJob(1L)));
            when(skillClient.getSkillsByUserId(10L)).thenReturn(null);
            when(experienceClient.getExperiencesByUserId(10L)).thenReturn(null);
            lenient().when(skillClient.getSkillsByIds(any()))
                    .thenReturn(List.of(buildSkill(1L, "Java"), buildSkill(2L, "Spring Boot")));

            FitScoreResponse result = fitScoreService.computeFitScore(1L, 10L);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should handle experience client throwing exception")
        void experienceClientThrows_usesEmptyList() {
            ReflectionTestUtils.setField(fitScoreService, "apiKey", "");
            when(jobRepository.findById(1L)).thenReturn(Optional.of(buildJob(1L)));
            when(skillClient.getSkillsByUserId(10L)).thenReturn(Collections.emptyList());
            when(experienceClient.getExperiencesByUserId(10L)).thenThrow(new RuntimeException("timeout"));
            lenient().when(skillClient.getSkillsByIds(any()))
                    .thenReturn(List.of(buildSkill(1L, "Java"), buildSkill(2L, "Spring Boot")));

            FitScoreResponse result = fitScoreService.computeFitScore(1L, 10L);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Tier derivation")
    class TierTests {

        @Test
        @DisplayName("score 80+ maps to STRONG_MATCH")
        void score80Plus_strongMatch() {
            ReflectionTestUtils.setField(fitScoreService, "apiKey", "");
            Job job = buildJob(1L);
            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
            // All skills match + many experiences = high score
            when(skillClient.getSkillsByUserId(10L))
                    .thenReturn(List.of(buildSkill(1L, "Java"), buildSkill(2L, "Spring Boot")));
            when(experienceClient.getExperiencesByUserId(10L))
                    .thenReturn(List.of(buildExperience("A"), buildExperience("B"),
                            buildExperience("C"), buildExperience("D")));
            when(skillClient.getSkillsByIds(List.of(1L, 2L)))
                    .thenReturn(List.of(buildSkill(1L, "Java"), buildSkill(2L, "Spring Boot")));

            FitScoreResponse result = fitScoreService.computeFitScore(1L, 10L);

            assertThat(result.getScore()).isGreaterThanOrEqualTo(80);
            assertThat(result.getTier()).isEqualTo("STRONG_MATCH");
        }
    }
}
