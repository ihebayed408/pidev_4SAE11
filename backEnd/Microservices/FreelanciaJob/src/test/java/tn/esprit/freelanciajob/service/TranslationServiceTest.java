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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import tn.esprit.freelanciajob.Service.TranslationService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranslationService – Unit Tests")
class TranslationServiceTest {

    @Spy  private ObjectMapper objectMapper = new ObjectMapper();
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private TranslationService translationService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(translationService, "restTemplate", restTemplate);
    }

    @Nested
    @DisplayName("translate()")
    class TranslateTests {

        @Test
        @DisplayName("should return null for null input")
        void nullText_returnsNull() {
            String result = translationService.translate(null, "fr", "en");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return blank for blank input")
        void blankText_returnsBlank() {
            String result = translationService.translate("   ", "fr", "en");
            assertThat(result).isEqualTo("   ");
        }

        @Test
        @DisplayName("should translate short text successfully")
        void shortText_translatedSuccessfully() {
            String response = "{\"responseData\":{\"translatedText\":\"Bonjour le monde\"}}";
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(response);

            String result = translationService.translate("Hello world", "fr", "en");

            assertThat(result).isEqualTo("Bonjour le monde");
        }

        @Test
        @DisplayName("should default source language to en when null")
        void nullSourceLang_defaultsToEn() {
            String response = "{\"responseData\":{\"translatedText\":\"Hola\"}}";
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(response);

            String result = translationService.translate("Hello", "es", null);

            assertThat(result).isEqualTo("Hola");
        }

        @Test
        @DisplayName("should default source language to en when 'auto'")
        void autoSourceLang_defaultsToEn() {
            String response = "{\"responseData\":{\"translatedText\":\"Hola\"}}";
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(response);

            String result = translationService.translate("Hello", "es", "auto");

            assertThat(result).isEqualTo("Hola");
        }

        @Test
        @DisplayName("should return original text when API fails")
        void apiFails_returnsOriginalText() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenThrow(new RuntimeException("Network error"));

            String result = translationService.translate("Hello", "fr", "en");

            assertThat(result).isEqualTo("Hello");
        }

        @Test
        @DisplayName("should return original text when API returns null translated text")
        void nullTranslatedText_returnsOriginal() {
            String response = "{\"responseData\":{\"translatedText\":null}}";
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(response);

            String result = translationService.translate("Hello", "fr", "en");

            assertThat(result).isEqualTo("Hello");
        }

        @Test
        @DisplayName("should split long text into chunks")
        void longText_splitIntoChunks() {
            String longText = "Hello world. ".repeat(50); // > 480 chars
            String response = "{\"responseData\":{\"translatedText\":\"Bonjour\"}}";
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(response);

            String result = translationService.translate(longText, "fr", "en");

            assertThat(result).isNotBlank();
            // Should have made multiple API calls
            verify(restTemplate, atLeast(2)).getForObject(anyString(), eq(String.class));
        }

        @Test
        @DisplayName("should split at sentence boundary when possible")
        void longTextWithSentences_splitsAtBoundary() {
            // Build text > 480 chars with sentences
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                sb.append("This is sentence number ").append(i).append(" and it has some content. ");
            }
            String response = "{\"responseData\":{\"translatedText\":\"Translated\"}}";
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(response);

            String result = translationService.translate(sb.toString(), "fr", "en");

            assertThat(result).contains("Translated");
        }
    }
}
