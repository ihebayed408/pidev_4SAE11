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
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import tn.esprit.freelanciajob.Dto.response.GeneratedJobDraft;
import tn.esprit.freelanciajob.Service.AiJobGeneratorService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiJobGeneratorService – Unit Tests")
class AiJobGeneratorServiceTest {

    @Spy  private ObjectMapper objectMapper = new ObjectMapper();
    @Mock private Environment environment;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private AiJobGeneratorService aiJobGeneratorService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(aiJobGeneratorService, "apiUrl", "https://api.example.com/chat");
        ReflectionTestUtils.setField(aiJobGeneratorService, "apiKey", "test-key");
        ReflectionTestUtils.setField(aiJobGeneratorService, "apiModel", "test-model");
        ReflectionTestUtils.setField(aiJobGeneratorService, "restTemplate", restTemplate);
    }

    @Nested
    @DisplayName("generateJobDraft()")
    class GenerateJobDraftTests {

        @Test
        @DisplayName("should parse valid AI response into GeneratedJobDraft")
        void validAiResponse_returnsParsedDraft() {
            String content = "{\\\"title\\\":\\\"E-commerce Platform\\\",\\\"description\\\":\\\"Build an e-commerce site\\\",\\\"requiredSkills\\\":[\\\"React\\\",\\\"Node.js\\\"],\\\"budgetMin\\\":1000,\\\"budgetMax\\\":5000,\\\"currency\\\":\\\"USD\\\",\\\"estimatedDurationWeeks\\\":8,\\\"category\\\":\\\"Web Development\\\",\\\"locationType\\\":\\\"REMOTE\\\"}";
            String aiJson = "{\"choices\":[{\"message\":{\"content\":\"" + content + "\"}}]}";
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(aiJson));

            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("Build me an e-commerce site");

            assertThat(result.getTitle()).isEqualTo("E-commerce Platform");
            assertThat(result.getRequiredSkills()).contains("React", "Node.js");
            assertThat(result.getCategory()).isEqualTo("Web Development");
        }

        @Test
        @DisplayName("should fall back when AI call throws exception")
        void aiCallFails_usesFallbackDraft() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("Build a mobile app with flutter");

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isNotBlank();
            assertThat(result.getCategory()).isEqualTo("Mobile Development");
        }

        @Test
        @DisplayName("should fall back when API key is missing")
        void missingApiKey_usesFallbackDraft() {
            ReflectionTestUtils.setField(aiJobGeneratorService, "apiKey", "");

            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("need spring api developer for backend");

            assertThat(result).isNotNull();
            assertThat(result.getCategory()).isEqualTo("Backend");
            assertThat(result.getBudgetMin()).isNotNull();
        }

        @Test
        @DisplayName("should handle AI response with empty choices")
        void emptyChoices_usesFallback() {
            String aiJson = "{\"choices\":[]}";
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(aiJson));

            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("Design a logo");

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isNotBlank();
        }

        @Test
        @DisplayName("should handle null response body")
        void nullResponseBody_usesFallback() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(null));

            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("Something");

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Fallback draft generation")
    class FallbackDraftTests {

        @BeforeEach
        void noApiKey() {
            ReflectionTestUtils.setField(aiJobGeneratorService, "apiKey", "");
        }

        @Test
        @DisplayName("should infer Mobile Development for mobile prompts")
        void mobilePrompt_infersMobileCategory() {
            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("android mobile app");
            assertThat(result.getCategory()).isEqualTo("Mobile Development");
        }

        @Test
        @DisplayName("should infer UI/UX Design for design prompts")
        void designPrompt_infersUiUxCategory() {
            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("figma prototype design");
            assertThat(result.getCategory()).isEqualTo("UI/UX Design");
        }

        @Test
        @DisplayName("should infer Data Science for ML prompts")
        void mlPrompt_infersDataScienceCategory() {
            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("ml model for data analytics");
            assertThat(result.getCategory()).isEqualTo("Data Science");
        }

        @Test
        @DisplayName("should infer DevOps for docker/kubernetes prompts")
        void devopsPrompt_infersDevOpsCategory() {
            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("setup docker and kubernetes");
            assertThat(result.getCategory()).isEqualTo("DevOps");
        }

        @Test
        @DisplayName("should infer Content Writing for blog prompts")
        void contentPrompt_infersContentWriting() {
            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("write blog content articles");
            assertThat(result.getCategory()).isEqualTo("Content Writing");
        }

        @Test
        @DisplayName("should infer Marketing for SEO prompts")
        void marketingPrompt_infersMarketing() {
            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("seo ads marketing");
            assertThat(result.getCategory()).isEqualTo("Marketing");
        }

        @Test
        @DisplayName("should default to Web Development for generic prompts")
        void genericPrompt_defaultsWebDevelopment() {
            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("do something cool");
            assertThat(result.getCategory()).isEqualTo("Web Development");
        }

        @Test
        @DisplayName("should infer ONSITE location type")
        void onsitePrompt_infersOnsite() {
            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("need an onsite developer");
            assertThat(result.getLocationType()).isEqualTo("ONSITE");
        }

        @Test
        @DisplayName("should infer HYBRID location type")
        void hybridPrompt_infersHybrid() {
            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("looking for hybrid work");
            assertThat(result.getLocationType()).isEqualTo("HYBRID");
        }

        @Test
        @DisplayName("should default to REMOTE location type")
        void defaultPrompt_infersRemote() {
            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("need a developer");
            assertThat(result.getLocationType()).isEqualTo("REMOTE");
        }

        @Test
        @DisplayName("should truncate title to 80 chars")
        void longPrompt_titleTruncated() {
            String longPrompt = "a".repeat(120);
            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft(longPrompt);
            assertThat(result.getTitle().length()).isLessThanOrEqualTo(80);
        }

        @Test
        @DisplayName("should handle blank prompt")
        void blankPrompt_generatesDefault() {
            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("   ");
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Project Opportunity");
        }

        @Test
        @DisplayName("should infer skills from prompt keywords")
        void skillKeywords_inferredCorrectly() {
            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("angular app with python and sql");
            assertThat(result.getRequiredSkills()).contains("Angular", "Python", "SQL");
        }

        @Test
        @DisplayName("should use default skills when no keywords match")
        void noKeywords_usesDefaultSkills() {
            GeneratedJobDraft result = aiJobGeneratorService.generateJobDraft("do something");
            assertThat(result.getRequiredSkills()).isNotEmpty();
        }
    }
}
