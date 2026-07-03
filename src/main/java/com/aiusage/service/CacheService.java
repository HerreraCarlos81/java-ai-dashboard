package com.aiusage.service;

import com.aiusage.model.CostData;
import com.aiusage.model.UsageData;
import com.aiusage.service.provider.AiProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for provider usage and cost data.
 *
 * Each entry is identified by a composite key (keyId + type + date range)
 * and expires after a configurable TTL. The cache is invalidated explicitly
 * on each manual refresh so data is always fetched fresh from the API.
 */
public class CacheService {
    private static final Path CACHE_DIR = Paths.get(
        System.getProperty("user.home"), ".ai-usage-dashboard", "cache"
    );
    private final ObjectMapper mapper;
    private final Map<String, CacheEntry<List<UsageData>>> usageCache;
    private final Map<String, CacheEntry<List<CostData>>> costCache;
    private final long cacheTtlMinutes;

    public CacheService() {
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.usageCache = new ConcurrentHashMap<>();
        this.costCache = new ConcurrentHashMap<>();
        this.cacheTtlMinutes = 15;
        CACHE_DIR.toFile().mkdirs();
    }

    /**
     * Returns cached usage data for the given key and date range, or fetches
     * it from the provider if the cache has expired or is absent.
     *
     * When data comes from the provider, each record is tagged with keyId so
     * the service layer can correlate usage to the correct config key.
     */
    public List<UsageData> getOrFetchUsage(AiProvider provider, String apiKey,
                                            String baseUrl, LocalDate start, LocalDate end,
                                            String keyId) throws Exception {
        String cacheKey = buildCacheKey(keyId, "usage", start, end);
        CacheEntry<List<UsageData>> entry = usageCache.get(cacheKey);
        if (entry != null && !entry.isExpired(cacheTtlMinutes)) {
            return entry.getData();
        }
        List<UsageData> data = provider.fetchUsage(apiKey, baseUrl, start, end);
        for (UsageData ud : data) {
            if (ud.getApiKeyId() == null) ud.setApiKeyId(keyId);
        }
        usageCache.put(cacheKey, new CacheEntry<>(data));
        return data;
    }

    /**
     * Same contract as {@link #getOrFetchUsage} but for cost data.
     */
    public List<CostData> getOrFetchCosts(AiProvider provider, String apiKey,
                                           String baseUrl, LocalDate start, LocalDate end,
                                           String keyId) throws Exception {
        String cacheKey = buildCacheKey(keyId, "cost", start, end);
        CacheEntry<List<CostData>> entry = costCache.get(cacheKey);
        if (entry != null && !entry.isExpired(cacheTtlMinutes)) {
            return entry.getData();
        }
        List<CostData> data = provider.fetchCosts(apiKey, baseUrl, start, end);
        for (CostData cd : data) {
            if (cd.getApiKeyId() == null) cd.setApiKeyId(keyId);
        }
        costCache.put(cacheKey, new CacheEntry<>(data));
        return data;
    }

    /**
     * Clears all cached entries. Called before each manual refresh so
     * subsequent requests always hit the provider API.
     */
    public void invalidateCache() {
        usageCache.clear();
        costCache.clear();
    }

    private static String buildCacheKey(String keyId, String type,
                                         LocalDate start, LocalDate end) {
        return keyId + "_" + type + "_" + start + "_" + end;
    }

    /**
     * A timestamped wrapper around cached data.
     * Expiration is computed relative to the creation time.
     */
    private static class CacheEntry<T> {
        private final T data;
        private final long timestamp;

        CacheEntry(T data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        T getData() { return data; }

        boolean isExpired(long ttlMinutes) {
            return System.currentTimeMillis() - timestamp > ttlMinutes * 60_000;
        }
    }
}
