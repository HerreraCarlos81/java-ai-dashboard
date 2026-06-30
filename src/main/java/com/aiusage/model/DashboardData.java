package com.aiusage.model;

import java.util.List;

public class DashboardData {
    private String modelName;
    private String provider;
    private String displayName;
    private double totalCost;
    private long totalTokens;
    private int totalRequests;
    private List<KeySummary> keySummaries;

    public static class KeySummary {
        private String keyId;
        private String keyLabel;
        private double cost;
        private long tokens;
        private int requests;

        public KeySummary(String keyId, String keyLabel, double cost, long tokens, int requests) {
            this.keyId = keyId;
            this.keyLabel = keyLabel;
            this.cost = cost;
            this.tokens = tokens;
            this.requests = requests;
        }

        public String getKeyId() { return keyId; }
        public String getKeyLabel() { return keyLabel; }
        public double getCost() { return cost; }
        public long getTokens() { return tokens; }
        public int getRequests() { return requests; }
    }

    public DashboardData(String modelName, String provider, String displayName) {
        this.modelName = modelName;
        this.provider = provider;
        this.displayName = displayName;
    }

    public String getModelName() { return modelName; }
    public String getProvider() { return provider; }
    public String getDisplayName() { return displayName; }
    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
    public long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(long totalTokens) { this.totalTokens = totalTokens; }
    public int getTotalRequests() { return totalRequests; }
    public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
    public List<KeySummary> getKeySummaries() { return keySummaries; }
    public void setKeySummaries(List<KeySummary> keySummaries) { this.keySummaries = keySummaries; }
}
