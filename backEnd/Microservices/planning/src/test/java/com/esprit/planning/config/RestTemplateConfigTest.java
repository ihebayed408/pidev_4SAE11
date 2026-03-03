package com.esprit.planning.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies RestTemplateConfig creates a usable RestTemplate bean.
 */
class RestTemplateConfigTest {

    @Test
    void restTemplate_returnsNonNullInstance() {
        RestTemplateConfig config = new RestTemplateConfig();
        RestTemplate restTemplate = config.restTemplate();
        assertThat(restTemplate).isNotNull();
    }
}
