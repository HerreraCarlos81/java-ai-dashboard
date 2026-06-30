package com.aiusage.config;

import java.util.List;

public class AppConfig {
    private String version;
    private List<ModelConfig> models;

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public List<ModelConfig> getModels() { return models; }
    public void setModels(List<ModelConfig> models) { this.models = models; }
}
