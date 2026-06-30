package com.aiusage.service;

import com.aiusage.model.CostData;
import com.aiusage.model.UsageData;
import com.aiusage.service.provider.AiProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CacheService {
    private static final Path CACHE_DIR = Paths.get(
        System.getProperty("user.home"), ".ai-usage-dashboard", "cache"
    );
    private final ObjectMapper mapper;
    private final Map<String, List<UsageData>> usageCache;
    private final Map<String, List<CostData>> costCache;
    private final long cacheTtlMinutes;

    public CacheService() {
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.usageCache = new ConcurrentHashMap<>();
        this.costCache = new ConcurrentHashMap<>();
        this.cacheTtlMinutes = 15;
        CACHE_DIR.toFile().mkdirs();
    }

    public List<UsageData> getOrFetchUsage(AiProvider provider, String apiKey,
                                            String baseUrl, LocalDate start, LocalDate end,
                                            String keyId) throws Exception {
        String cacheKey = keyId + "_usage_" + start + "_" + end;
        if (usageCache.containsKey(cacheKey)) {
            return usageCache.get(cacheKey);
        }
        List<UsageData> data = provider.fetchUsage(apiKey, baseUrl, start, end);
        for (UsageData ud : data) {
            if (ud.getApiKeyId() == null) ud.setApiKeyId(keyId);
        }
        usageCache.put(cacheKey, data);
        return data;
    }

    public List<CostData> getOrFetchCosts(AiProvider provider, String apiKey,
                                           String baseUrl, LocalDate start, LocalDate end,
                                           String keyId) throws Exception {
        String cacheKey = keyId + "_cost_" + start + "_" + end;
        if (costCache.containsKey(cacheKey)) {
            return costCache.get(cacheKey);
        }
        List<CostData> data = provider.fetchCosts(apiKey, baseUrl, start, end);
        for (CostData cd : data) {
            if (cd.getApiKeyId() == null) cd.setApiKeyId(keyId);
        }
        costCache.put(cacheKey, data);
        return data;
    }

    public void invalidateCache() {
        usageCache.clear();
        costCache.clear();
    }
}
