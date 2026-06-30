package com.aiusage.model;

import java.time.LocalDate;

public class UsageData {
    private String apiKeyId;
    private String model;
    private LocalDate date;
    private long inputTokens;
    private long outputTokens;
    private long cachedInputTokens;
    private long totalTokens;
    private int numRequests;

    public UsageData() {}

    public String getApiKeyId() { return apiKeyId; }
    public void setApiKeyId(String apiKeyId) { this.apiKeyId = apiKeyId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public long getInputTokens() { return inputTokens; }
    public void setInputTokens(long inputTokens) { this.inputTokens = inputTokens; }
    public long getOutputTokens() { return outputTokens; }
    public void setOutputTokens(long outputTokens) { this.outputTokens = outputTokens; }
    public long getCachedInputTokens() { return cachedInputTokens; }
    public void setCachedInputTokens(long cachedInputTokens) { this.cachedInputTokens = cachedInputTokens; }
    public long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(long totalTokens) { this.totalTokens = totalTokens; }
    public int getNumRequests() { return numRequests; }
    public void setNumRequests(int numRequests) { this.numRequests = numRequests; }

    public long getComputedTotal() {
        return inputTokens + outputTokens + cachedInputTokens;
    }
}
