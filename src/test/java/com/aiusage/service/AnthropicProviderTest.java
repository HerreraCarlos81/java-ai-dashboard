package com.aiusage.service.provider;

import com.aiusage.model.CostData;
import com.aiusage.model.UsageData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Anthropic provider's response parsers.
 * Response shape mirrors the real Admin API with group_by[]=api_key_id.
 */
class AnthropicProviderTest {

    @Test
    void providerNameIsAnthropic() {
        assertEquals("anthropic", new AnthropicProvider().name());
    }

    @Test
    void parseUsageResponseExtractsTokensAndKeyId() {
        String json = "{\"data\":[{\"results\":[{\"uncached_input_tokens\":200,"
            + "\"output_tokens\":100,\"cache_read_input_tokens\":50,"
            + "\"num_requests\":10,\"api_key_id\":\"key_abc\","
            + "\"model\":\"claude-sonnet-4-5\"}]}]}";
        List<UsageData> result = new AnthropicProvider().parseUsageResponse(json);
        assertEquals(1, result.size());
        assertEquals(200, result.get(0).getInputTokens());
        assertEquals(100, result.get(0).getOutputTokens());
        assertEquals(50, result.get(0).getCachedInputTokens());
        assertEquals(350, result.get(0).getTotalTokens());
        assertEquals(10, result.get(0).getNumRequests());
        assertEquals("key_abc", result.get(0).getApiKeyId());
        assertEquals("claude-sonnet-4-5", result.get(0).getModel());
    }

    @Test
    void parseUsageResponseHandlesNoGroupBy() {
        String json = "{\"data\":[{\"results\":[{\"uncached_input_tokens\":100,"
            + "\"output_tokens\":50}]}]}";
        List<UsageData> result = new AnthropicProvider().parseUsageResponse(json);
        assertEquals(1, result.size());
        assertNull(result.get(0).getApiKeyId());
    }

    @Test
    void parseCostResponseConvertsCentsToDollars() {
        String json = "{\"data\":[{\"results\":[{\"amount\":\"543\","
            + "\"model\":\"claude-sonnet-4-5\"}]}]}";
        List<CostData> result = new AnthropicProvider().parseCostResponse(json);
        assertEquals(1, result.size());
        assertEquals(5.43, result.get(0).getAmount(), 0.001);
        assertEquals("USD", result.get(0).getCurrency());
        assertEquals("claude-sonnet-4-5", result.get(0).getModel());
    }

    @Test
    void parseCostResponseHandlesZeroAmount() {
        String json = "{\"data\":[{\"results\":[{\"amount\":\"0\"}]}]}";
        List<CostData> result = new AnthropicProvider().parseCostResponse(json);
        assertEquals(1, result.size());
        assertEquals(0.0, result.get(0).getAmount(), 0.001);
    }
}
