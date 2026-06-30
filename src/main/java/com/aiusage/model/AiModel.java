package com.aiusage.model;

import java.time.LocalDate;
import java.util.List;

public class AiModel {
    private String name;
    private String displayName;
    private String provider;
    private String baseUrl;
    private boolean enabled;
    private List<ApiKeyEntry> apiKeys;

    public AiModel() {}

    public AiModel(String name, String displayName, String provider, String baseUrl) {
        this.name = name;
        this.displayName = displayName;
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.enabled = true;
    }

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
    public List<ApiKeyEntry> getApiKeys() { return apiKeys; }
    public void setApiKeys(List<ApiKeyEntry> apiKeys) { this.apiKeys = apiKeys; }
}
