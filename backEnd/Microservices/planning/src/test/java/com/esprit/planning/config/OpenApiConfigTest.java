package com.esprit.planning.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OpenApiConfig. Verifies planningOpenAPI bean produces valid OpenAPI with expected structure.
 */
class OpenApiConfigTest {

    @Test
    void planningOpenAPI_returnsOpenAPIWithExpectedInfo() {
        OpenApiConfig config = new OpenApiConfig();
        ReflectionTestUtils.setField(config, "serverPort", "8081");

        OpenAPI api = config.planningOpenAPI();

        assertThat(api).isNotNull();
        assertThat(api.getInfo()).isNotNull();
        assertThat(api.getInfo().getTitle()).isEqualTo("Planning & Tracking API");
        assertThat(api.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(api.getInfo().getDescription()).contains("Progress Updates");
        assertThat(api.getServers()).hasSize(1);
        assertThat(api.getServers().get(0).getUrl()).isEqualTo("http://localhost:8081");
    }
}
