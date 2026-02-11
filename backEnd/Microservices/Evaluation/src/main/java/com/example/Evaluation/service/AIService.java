package com.example.Evaluation.service;

import com.example.Evaluation.entity.Question;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.model:openai/gpt-5.2}")
    private String apiModel;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.equals("your-api-key") || apiKey.startsWith("${")) {
            System.out.println("API Key not injected correctly. Attempting to load from .env...");
            io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                    .ignoreIfMissing()
                    .directory("../../") // Try looking up two levels (to backEnd/)
                    .filename(".env")
                    .load();
            
            String envKey = dotenv.get("API_KEY");
            if (envKey != null && !envKey.isBlank()) {
                this.apiKey = envKey;
                System.out.println("Loaded API Key from .env manually.");
            } else {
                 System.err.println("Could not load API_KEY from .env");
            }
        }
    }

    public List<Question> generateQuestions(String skillName) {
        String prompt = String.format("Generate 5 multiple choice questions for the skill '%s' in JSON format. " +
                "The output must be a raw JSON array of objects (no markdown, no code blocks). " +
                "Each object must have these fields: " +
                "'questionText' (string), " +
                "'options' (comma separated string of options, e.g. 'A. option1, B. option2'), " +
                "'correctOption' (string, e.g. 'Option A'), " +
                "'points' (integer, default 1).", skillName);

        Map<String, Object> requestBody = Map.of(
                "model", apiModel,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant that generates technical interview questions."),
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 4096,
                "stream", false,
                "temperature", 0.7
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Log key (masked)
        String maskedKey = (apiKey != null && apiKey.length() > 10) 
            ? apiKey.substring(0, 8) + "..." 
            : (apiKey != null ? apiKey : "null");
        System.out.println("Using API Key: " + maskedKey);
        
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            return parseResponse(response.getBody());
        } catch (Exception e) {
            System.err.println("Error calling AI API: " + e.getMessage());
            // Rethrow or return empty
            throw new RuntimeException("Failed to generate questions", e);
        }
    }

    private List<Question> parseResponse(String jsonResponse) {
        try {
            if (jsonResponse == null) return new ArrayList<>();
            
            var rootNode = objectMapper.readTree(jsonResponse);
            // Different providers might have different response structures.
            // OpenAI standard: choices[0].message.content
            String content = "";
            if (rootNode.has("choices") && rootNode.get("choices").isArray() && rootNode.get("choices").size() > 0) {
                content = rootNode.get("choices").get(0).path("message").path("content").asText();
            } else {
                // Fallback or log structure if unexpected
                System.err.println("Unexpected JSON response structure: " + jsonResponse);
                return new ArrayList<>();
            }
            
            content = content.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(content, new TypeReference<List<Question>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse AI response", e);
        }
    }
}
