package com.aiusage.model;

import java.time.LocalDate;

public class CostData {
    private String apiKeyId;
    private String model;
    private LocalDate date;
    private double amount;
    private String currency;

    public CostData() { this.currency = "USD"; }

    public String getApiKeyId() { return apiKeyId; }
    public void setApiKeyId(String apiKeyId) { this.apiKeyId = apiKeyId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
