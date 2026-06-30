package com.aiusage.service.provider;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpenAIProviderTest {
    @Test
    void testProviderName() {
        OpenAIProvider provider = new OpenAIProvider();
        assertEquals("openai", provider.name());
    }

    @Test
    void testParseUsageResponse() {
        String sampleJson = "{\"data\":[{\"results\":[{\"input_tokens\":100,\"output_tokens\":50,\"num_model_requests\":5}]}]}";
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var root = mapper.readTree(sampleJson);
            var results = root.get("data").get(0).get("results");
            assertEquals(1, results.size());
            assertEquals(100, results.get(0).path("input_tokens").asLong());
            assertEquals(50, results.get(0).path("output_tokens").asLong());
        } catch (Exception e) {
            fail("Parsing failed: " + e.getMessage());
        }
    }

    @Test
    void testParseCostResponse() {
        String sampleJson = "{\"data\":[{\"results\":[{\"amount\":12.50}]}]}";
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var root = mapper.readTree(sampleJson);
            var results = root.get("data").get(0).get("results");
            assertEquals(12.50, results.get(0).path("amount").asDouble(), 0.001);
        } catch (Exception e) {
            fail("Parsing failed: " + e.getMessage());
        }
    }

    @Test
    void testConnectionFailsWithInvalidKey() {
        OpenAIProvider provider = new OpenAIProvider();
        boolean result = provider.testConnection("invalid-key", "https://api.openai.com/v1");
        assertFalse(result);
    }
}
