package com.aiusage.config;

import java.util.List;

public class ModelConfig {
    private String name;
    private String displayName;
    private String provider;
    private String baseUrl;
    private boolean enabled;
    private double monthlyBudget;
    private List<ApiKeyConfig> apiKeys;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public double getMonthlyBudget() { return monthlyBudget; }
    public void setMonthlyBudget(double monthlyBudget) { this.monthlyBudget = monthlyBudget; }
    public List<ApiKeyConfig> getApiKeys() { return apiKeys; }
    public void setApiKeys(List<ApiKeyConfig> apiKeys) { this.apiKeys = apiKeys; }

    public static class ApiKeyConfig {
        private String id;
        private String label;
        private String key;
        private boolean enabled;
        private boolean admin;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isAdmin() { return admin; }
        public void setAdmin(boolean admin) { this.admin = admin; }
    }
}
