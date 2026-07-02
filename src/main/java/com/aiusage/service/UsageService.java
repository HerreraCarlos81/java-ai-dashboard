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
            List<DashboardData.KeySummary> summaries = new ArrayList<>();
            double totalCost = 0;
            long totalTokens = 0;
            int totalRequests = 0;

            StringBuilder errors = new StringBuilder();

            ApiKeyEntry adminKey = model.getApiKeys() == null ? null
                : model.getApiKeys().stream().filter(ApiKeyEntry::isAdmin).findFirst().orElse(null);

            if ("openai".equals(model.getProvider()) && adminKey != null) {
                try {
                    AiProvider provider = ProviderFactory.getProvider("openai");
                    List<CostData> allCosts = cacheService.getOrFetchCosts(
                        provider, adminKey.getKey(), model.getBaseUrl(),
                        monthStart, now, model.getName() + "_admin"
                    );
                    List<UsageData> allUsage = cacheService.getOrFetchUsage(
                        provider, adminKey.getKey(), model.getBaseUrl(),
                        monthStart, now, model.getName() + "_admin"
                    );

                    Map<String, List<CostData>> costsByKey = allCosts.stream()
                        .filter(c -> c.getApiKeyId() != null && !c.getApiKeyId().isEmpty())
                        .collect(Collectors.groupingBy(CostData::getApiKeyId));
                    Map<String, List<UsageData>> usageByKey = allUsage.stream()
                        .filter(u -> u.getApiKeyId() != null && !u.getApiKeyId().isEmpty())
                        .collect(Collectors.groupingBy(UsageData::getApiKeyId));

                    Map<String, String> idToLabel = new HashMap<>();
                    try {
                        idToLabel = provider.fetchApiKeyNames(adminKey.getKey(), model.getBaseUrl());
                    } catch (Exception ex) {
                        System.err.println("Could not fetch API key names: " + ex.getMessage());
                    }

                    Set<String> allKeyIds = new HashSet<>();
                    allKeyIds.addAll(costsByKey.keySet());
                    allKeyIds.addAll(usageByKey.keySet());

                    for (String apiKeyId : allKeyIds) {
                        double keyCost = costsByKey.getOrDefault(apiKeyId, List.of()).stream()
                            .mapToDouble(CostData::getAmount).sum();
                        long keyTokens = usageByKey.getOrDefault(apiKeyId, List.of()).stream()
                            .mapToLong(UsageData::getTotalTokens).sum();
                        int keyRequests = usageByKey.getOrDefault(apiKeyId, List.of()).stream()
                            .mapToInt(UsageData::getNumRequests).sum();

                        String label = idToLabel.getOrDefault(apiKeyId, apiKeyId);

                        summaries.add(new DashboardData.KeySummary(
                            apiKeyId, label, keyCost, keyTokens, keyRequests
                        ));
                        totalCost += keyCost;
                        totalTokens += keyTokens;
                        totalRequests += keyRequests;
                    }
                } catch (Exception e) {
                    String msg = "Admin key fetch failed: " + e.getMessage();
                    System.err.println(msg);
                    errors.append(msg).append(" | ");
                }
            } else if ("anthropic".equals(model.getProvider()) && adminKey != null) {
                try {
                    AiProvider provider = ProviderFactory.getProvider("anthropic");
                    List<UsageData> allUsage = cacheService.getOrFetchUsage(
                        provider, adminKey.getKey(), model.getBaseUrl(),
                        monthStart, now, model.getName() + "_admin"
                    );

                    List<CostData> allCosts = cacheService.getOrFetchCosts(
                        provider, adminKey.getKey(), model.getBaseUrl(),
                        monthStart, now, model.getName() + "_admin"
                    );

                    Map<String, List<UsageData>> usageByKey = allUsage.stream()
                        .filter(u -> u.getApiKeyId() != null && !u.getApiKeyId().isEmpty())
                        .collect(Collectors.groupingBy(UsageData::getApiKeyId));

                    double totalOrgCost = allCosts.stream()
                        .mapToDouble(CostData::getAmount).sum();
                    long totalOrgTokens = allUsage.stream()
                        .mapToLong(UsageData::getTotalTokens).sum();

                    Map<String, String> idToLabel = new HashMap<>();
                    try {
                        idToLabel = provider.fetchApiKeyNames(adminKey.getKey(), model.getBaseUrl());
                    } catch (Exception ex) {
                        System.err.println("Could not fetch API key names: " + ex.getMessage());
                    }

                    for (Map.Entry<String, List<UsageData>> entry : usageByKey.entrySet()) {
                        String apiKeyId = entry.getKey();
                        long keyTokens = entry.getValue().stream()
                            .mapToLong(UsageData::getTotalTokens).sum();
                        int keyRequests = entry.getValue().stream()
                            .mapToInt(UsageData::getNumRequests).sum();
                        double keyCost = totalOrgTokens > 0
                            ? totalOrgCost * ((double) keyTokens / totalOrgTokens)
                            : 0;

                        String label = idToLabel.getOrDefault(apiKeyId, apiKeyId);

                        summaries.add(new DashboardData.KeySummary(
                            apiKeyId, label, keyCost, keyTokens, keyRequests
                        ));
                        totalCost += keyCost;
                        totalTokens += keyTokens;
                        totalRequests += keyRequests;
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
                        AiProvider provider = ProviderFactory.getProvider(model.getProvider());
                        List<CostData> costs = cacheService.getOrFetchCosts(
                            provider, key.getKey(), model.getBaseUrl(),
                            monthStart, now, key.getId()
                        );
                        List<UsageData> usage = cacheService.getOrFetchUsage(
                            provider, key.getKey(), model.getBaseUrl(),
                            monthStart, now, key.getId()
                        );

                        double keyCost = costs.stream().mapToDouble(CostData::getAmount).sum();
                        long keyTokens = usage.stream().mapToLong(UsageData::getTotalTokens).sum();
                        int keyRequests = usage.stream().mapToInt(UsageData::getNumRequests).sum();

                        summaries.add(new DashboardData.KeySummary(
                            key.getId(), key.getLabel(), keyCost, keyTokens, keyRequests
                        ));
                        totalCost += keyCost;
                        totalTokens += keyTokens;
                        totalRequests += keyRequests;
                    } catch (Exception e) {
                        String msg = key.getLabel() + ": " + e.getMessage();
                        System.err.println("Error fetching data for key " + msg);
                        summaries.add(new DashboardData.KeySummary(
                            key.getId(), key.getLabel(), 0, 0, 0, msg
                        ));
                        errors.append(msg).append(" | ");
                    }
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
}
