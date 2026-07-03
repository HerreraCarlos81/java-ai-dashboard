package com.aiusage.service.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the provider registry can resolve supported providers
 * and rejects unknown provider names with a clear error.
 */
class ProviderFactoryTest {

    @Test
    void getOpenAIProviderReturnsOpenAIProvider() {
        AiProvider provider = ProviderFactory.getProvider("openai");
        assertNotNull(provider);
        assertTrue(provider instanceof OpenAIProvider);
    }

    @Test
    void getAnthropicProviderReturnsAnthropicProvider() {
        AiProvider provider = ProviderFactory.getProvider("anthropic");
        assertNotNull(provider);
        assertTrue(provider instanceof AnthropicProvider);
    }

    @Test
    void hasProviderReturnsTrueForRegisteredProviders() {
        assertTrue(ProviderFactory.hasProvider("openai"));
        assertTrue(ProviderFactory.hasProvider("anthropic"));
    }

    @Test
    void hasProviderReturnsFalseForUnknownProviders() {
        assertFalse(ProviderFactory.hasProvider("unknown"));
    }

    @Test
    void getUnknownProviderThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> ProviderFactory.getProvider("unknown"));
    }
}
