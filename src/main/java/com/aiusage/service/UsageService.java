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

            if (model.getApiKeys() != null) {
                for (ApiKeyEntry key : model.getApiKeys()) {
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
                        System.err.println("Error fetching data for key " + key.getLabel()
                            + ": " + e.getMessage());
                        summaries.add(new DashboardData.KeySummary(
                            key.getId(), key.getLabel(), 0, 0, 0
                        ));
                    }
                }
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
