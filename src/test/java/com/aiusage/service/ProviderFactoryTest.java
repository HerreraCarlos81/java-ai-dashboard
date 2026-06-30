package com.aiusage.service.provider;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProviderFactoryTest {
    @Test
    void testGetOpenAIProvider() {
        AiProvider provider = ProviderFactory.getProvider("openai");
        assertNotNull(provider);
        assertTrue(provider instanceof OpenAIProvider);
    }

    @Test
    void testGetAnthropicProvider() {
        AiProvider provider = ProviderFactory.getProvider("anthropic");
        assertNotNull(provider);
    }

    @Test
    void testHasProvider() {
        assertTrue(ProviderFactory.hasProvider("openai"));
        assertTrue(ProviderFactory.hasProvider("anthropic"));
    }

    @Test
    void testUnknownProvider() {
        assertThrows(IllegalArgumentException.class, () -> {
            ProviderFactory.getProvider("unknown");
        });
    }
}
