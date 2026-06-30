package com.aiusage.service.provider;

import java.util.HashMap;
import java.util.Map;

public class ProviderFactory {
    private static final Map<String, AiProvider> providers = new HashMap<>();

    static {
        register(new OpenAIProvider());
        register(new AnthropicProvider());
    }

    public static void register(AiProvider provider) {
        providers.put(provider.name().toLowerCase(), provider);
    }

    public static AiProvider getProvider(String name) {
        AiProvider provider = providers.get(name.toLowerCase());
        if (provider == null) {
            throw new IllegalArgumentException("Unknown provider: " + name
                + ". Available: " + providers.keySet());
        }
        return provider;
    }

    public static boolean hasProvider(String name) {
        return providers.containsKey(name.toLowerCase());
    }
}
