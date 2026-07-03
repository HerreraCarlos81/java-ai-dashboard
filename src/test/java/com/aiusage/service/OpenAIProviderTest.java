package com.aiusage.service.provider;

import com.aiusage.model.CostData;
import com.aiusage.model.UsageData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the OpenAI provider's response parsers and connection behaviour.
 * The parse methods are tested with sample JSON that mirrors the real API
 * response shape returned when {@code group_by=api_key_id} is used.
 */
class OpenAIProviderTest {

    @Test
    void providerNameIsOpenai() {
        assertEquals("openai", new OpenAIProvider().name());
    }

    @Test
    void parseUsageResponseExtractsTokensAndKeyId() {
        String json = "{\"data\":[{\"results\":[{\"input_tokens\":100,\"output_tokens\":50,"
            + "\"num_model_requests\":5,\"api_key_id\":\"key_abc\"}]}]}";
        List<UsageData> result = new OpenAIProvider().parseUsageResponse(json);
        assertEquals(1, result.size());
        assertEquals(100, result.get(0).getInputTokens());
        assertEquals(50, result.get(0).getOutputTokens());
        assertEquals(5, result.get(0).getNumRequests());
        assertEquals("key_abc", result.get(0).getApiKeyId());
    }

    @Test
    void parseUsageResponseHandlesEmptyData() {
        String json = "{\"data\":[]}";
        List<UsageData> result = new OpenAIProvider().parseUsageResponse(json);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseCostResponseWithObjectAmount() {
        String json = "{\"data\":[{\"results\":[{\"amount\":{\"value\":12.50,\"currency\":\"usd\"},"
            + "\"api_key_id\":\"key_xyz\",\"line_item\":\"gpt-4\"}]}]}";
        List<CostData> result = new OpenAIProvider().parseCostResponse(json);
        assertEquals(1, result.size());
        assertEquals(12.50, result.get(0).getAmount(), 0.001);
        assertEquals("usd", result.get(0).getCurrency());
        assertEquals("key_xyz", result.get(0).getApiKeyId());
        assertEquals("gpt-4", result.get(0).getModel());
    }

    @Test
    void parseCostResponseWithPlainNumberAmount() {
        String json = "{\"data\":[{\"results\":[{\"amount\":12.50}]}]}";
        List<CostData> result = new OpenAIProvider().parseCostResponse(json);
        assertEquals(1, result.size());
        assertEquals(12.50, result.get(0).getAmount(), 0.001);
        assertEquals("usd", result.get(0).getCurrency());
    }

    @Test
    void testConnectionFailsWithInvalidKey() {
        boolean result = new OpenAIProvider().testConnection(
            "invalid-key", "https://api.openai.com/v1");
        assertFalse(result);
    }
}
