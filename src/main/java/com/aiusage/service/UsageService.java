package com.aiusage.service;

import com.aiusage.model.*;
import com.aiusage.service.provider.AiProvider;
import com.aiusage.service.provider.ProviderFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class UsageService {
    private final CacheService cacheService;

    public UsageService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public List<DashboardData> refreshDashboard(List<AiModel> models) {
        List<DashboardData> result = new ArrayList<>();
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);

        for (AiModel model : models) {
            DashboardData dd = new DashboardData(
                model.getName(), model.getProvider(), model.getDisplayName()
            );
            dd.setMonthlyBudget(model.getMonthlyBudget());
            List<DashboardData.KeySummary> summaries = new ArrayList<>();
            double totalCost = 0;
            long totalTokens = 0;
            int totalRequests = 0;
            StringBuilder errors = new StringBuilder();

            ApiKeyEntry adminKey = findAdminKey(model);

            if (adminKey != null && "openai".equals(model.getProvider())) {
                try {
                    AggregatedData resultData = fetchOpenAIGrouped(model, adminKey, monthStart, now);
                    for (AggregatedKey ak : resultData.keys) {
                        summaries.add(new DashboardData.KeySummary(
                            ak.id, ak.label, ak.cost, ak.tokens, ak.requests
                        ));
                        totalCost += ak.cost;
                        totalTokens += ak.tokens;
                        totalRequests += ak.requests;
                    }
                } catch (Exception e) {
                    String msg = "Admin key fetch failed: " + e.getMessage();
                    System.err.println(msg);
                    errors.append(msg).append(" | ");
                }
            } else if (adminKey != null && "anthropic".equals(model.getProvider())) {
                try {
                    AggregatedData resultData = fetchAnthropicGrouped(model, adminKey, monthStart, now);
                    for (AggregatedKey ak : resultData.keys) {
                        summaries.add(new DashboardData.KeySummary(
                            ak.id, ak.label, ak.cost, ak.tokens, ak.requests
                        ));
                        totalCost += ak.cost;
                        totalTokens += ak.tokens;
                        totalRequests += ak.requests;
                    }
                } catch (Exception e) {
                    String msg = "Admin key fetch failed: " + e.getMessage();
                    System.err.println(msg);
                    errors.append(msg).append(" | ");
                }
            }

            if (summaries.isEmpty() && model.getApiKeys() != null) {
                for (ApiKeyEntry key : model.getApiKeys()) {
                    if (key.isAdmin()) continue;
                    try {
                        fetchPerKey(model, key, monthStart, now, summaries, errors);
                    } catch (Exception e) {
                        String msg = key.getLabel() + ": " + e.getMessage();
                        System.err.println("Error fetching data for key " + msg);
                        summaries.add(new DashboardData.KeySummary(
                            key.getId(), key.getLabel(), 0, 0, 0, msg
                        ));
                        errors.append(msg).append(" | ");
                    }
                }
                for (DashboardData.KeySummary s : summaries) {
                    totalCost += s.getCost();
                    totalTokens += s.getTokens();
                    totalRequests += s.getRequests();
                }
            }

            if (errors.length() > 0) {
                dd.setLastError(errors.substring(0, errors.length() - 3));
            }
            dd.setTotalCost(totalCost);
            dd.setTotalTokens(totalTokens);
            dd.setTotalRequests(totalRequests);
            dd.setKeySummaries(summaries);
            result.add(dd);
        }
        return result;
    }

    public double getTotalCost(List<DashboardData> dashboard) {
        return dashboard.stream().mapToDouble(DashboardData::getTotalCost).sum();
    }

    public long getTotalTokens(List<DashboardData> dashboard) {
        return dashboard.stream().mapToLong(DashboardData::getTotalTokens).sum();
    }

    // -- private helpers --

    private static ApiKeyEntry findAdminKey(AiModel model) {
        if (model.getApiKeys() == null) return null;
        return model.getApiKeys().stream()
            .filter(ApiKeyEntry::isAdmin)
            .findFirst()
            .orElse(null);
    }

    private void fetchPerKey(AiModel model, ApiKeyEntry key,
                              LocalDate start, LocalDate end,
                              List<DashboardData.KeySummary> summaries,
                              StringBuilder errors) throws Exception {
        AiProvider provider = ProviderFactory.getProvider(model.getProvider());
        List<CostData> costs = cacheService.getOrFetchCosts(
            provider, key.getKey(), model.getBaseUrl(), start, end, key.getId()
        );
        List<UsageData> usage = cacheService.getOrFetchUsage(
            provider, key.getKey(), model.getBaseUrl(), start, end, key.getId()
        );
        double keyCost = costs.stream().mapToDouble(CostData::getAmount).sum();
        long keyTokens = usage.stream().mapToLong(UsageData::getTotalTokens).sum();
        int keyRequests = usage.stream().mapToInt(UsageData::getNumRequests).sum();
        summaries.add(new DashboardData.KeySummary(
            key.getId(), key.getLabel(), keyCost, keyTokens, keyRequests
        ));
    }

    /**
     * Uses the admin key to fetch all org usage and cost data from OpenAI.
     * Both endpoints support group_by=api_key_id so we can directly attribute
     * costs to each API key.
     */
    private AggregatedData fetchOpenAIGrouped(AiModel model, ApiKeyEntry adminKey,
                                               LocalDate start, LocalDate end) throws Exception {
        AiProvider provider = ProviderFactory.getProvider("openai");
        String cacheTag = model.getName() + "_admin";

        List<CostData> allCosts = cacheService.getOrFetchCosts(
            provider, adminKey.getKey(), model.getBaseUrl(), start, end, cacheTag
        );
        List<UsageData> allUsage = cacheService.getOrFetchUsage(
            provider, adminKey.getKey(), model.getBaseUrl(), start, end, cacheTag
        );

        Map<String, List<CostData>> costsByKey = allCosts.stream()
            .filter(c -> c.getApiKeyId() != null && !c.getApiKeyId().isEmpty())
            .collect(Collectors.groupingBy(CostData::getApiKeyId));
        Map<String, List<UsageData>> usageByKey = allUsage.stream()
            .filter(u -> u.getApiKeyId() != null && !u.getApiKeyId().isEmpty())
            .collect(Collectors.groupingBy(UsageData::getApiKeyId));

        Map<String, String> idToLabel = resolveKeyNames(provider, adminKey, model.getBaseUrl());

        Set<String> allKeyIds = new HashSet<>();
        allKeyIds.addAll(costsByKey.keySet());
        allKeyIds.addAll(usageByKey.keySet());

        List<AggregatedKey> keys = new ArrayList<>();
        for (String apiKeyId : allKeyIds) {
            double cost = costsByKey.getOrDefault(apiKeyId, List.of()).stream()
                .mapToDouble(CostData::getAmount).sum();
            long tokens = usageByKey.getOrDefault(apiKeyId, List.of()).stream()
                .mapToLong(UsageData::getTotalTokens).sum();
            int requests = usageByKey.getOrDefault(apiKeyId, List.of()).stream()
                .mapToInt(UsageData::getNumRequests).sum();
            keys.add(new AggregatedKey(apiKeyId, idToLabel.getOrDefault(apiKeyId, apiKeyId),
                cost, tokens, requests));
        }
        return new AggregatedData(keys);
    }

    /**
     * Uses the admin key to fetch all org usage and cost data from Anthropic.
     * The usage endpoint supports group_by[]=api_key_id, but the cost endpoint
     * does not. Costs are therefore distributed across keys proportionally to
     * their token consumption.
     */
    private AggregatedData fetchAnthropicGrouped(AiModel model, ApiKeyEntry adminKey,
                                                  LocalDate start, LocalDate end) throws Exception {
        AiProvider provider = ProviderFactory.getProvider("anthropic");
        String cacheTag = model.getName() + "_admin";

        List<UsageData> allUsage = cacheService.getOrFetchUsage(
            provider, adminKey.getKey(), model.getBaseUrl(), start, end, cacheTag
        );
        List<CostData> allCosts = cacheService.getOrFetchCosts(
            provider, adminKey.getKey(), model.getBaseUrl(), start, end, cacheTag
        );

        Map<String, List<UsageData>> usageByKey = allUsage.stream()
            .filter(u -> u.getApiKeyId() != null && !u.getApiKeyId().isEmpty())
            .collect(Collectors.groupingBy(UsageData::getApiKeyId));

        double totalOrgCost = allCosts.stream()
            .mapToDouble(CostData::getAmount).sum();
        long totalOrgTokens = allUsage.stream()
            .mapToLong(UsageData::getTotalTokens).sum();

        Map<String, String> idToLabel = resolveKeyNames(provider, adminKey, model.getBaseUrl());

        List<AggregatedKey> keys = new ArrayList<>();
        for (Map.Entry<String, List<UsageData>> entry : usageByKey.entrySet()) {
            String apiKeyId = entry.getKey();
            long tokens = entry.getValue().stream()
                .mapToLong(UsageData::getTotalTokens).sum();
            int requests = entry.getValue().stream()
                .mapToInt(UsageData::getNumRequests).sum();
            double cost = totalOrgTokens > 0
                ? totalOrgCost * ((double) tokens / totalOrgTokens)
                : 0;
            keys.add(new AggregatedKey(apiKeyId, idToLabel.getOrDefault(apiKeyId, apiKeyId),
                cost, tokens, requests));
        }
        return new AggregatedData(keys);
    }

    private static Map<String, String> resolveKeyNames(AiProvider provider,
                                                         ApiKeyEntry adminKey,
                                                         String baseUrl) {
        try {
            return provider.fetchApiKeyNames(adminKey.getKey(), baseUrl);
        } catch (Exception ex) {
            System.err.println("Could not fetch API key names: " + ex.getMessage());
            return Map.of();
        }
    }

    // -- Data transfer objects for aggregated processing --

    private record AggregatedKey(String id, String label, double cost,
                                  long tokens, int requests) {}

    private record AggregatedData(List<AggregatedKey> keys) {}
}
