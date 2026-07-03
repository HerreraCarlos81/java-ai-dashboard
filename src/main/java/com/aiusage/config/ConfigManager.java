package com.aiusage.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.aiusage.model.AiModel;
import com.aiusage.model.ApiKeyEntry;
import com.aiusage.util.EncryptionUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {
    private static final Path CONFIG_DIR = Paths.get(
        System.getProperty("user.home"), ".ai-usage-dashboard"
    );
    private static final String CONFIG_FILE = "config.json";

    private final ObjectMapper mapper;
    private final File configFile;
    private final EncryptionUtil encryptionUtil;
    private AppConfig currentConfig;

    public ConfigManager() {
        this.mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.configFile = CONFIG_DIR.resolve(CONFIG_FILE).toFile();
        this.encryptionUtil = new EncryptionUtil();
    }

    public AppConfig loadConfig() throws IOException {
        if (configFile.exists()) {
            currentConfig = mapper.readValue(configFile, AppConfig.class);
            decryptKeys(currentConfig);
        } else {
            CONFIG_DIR.toFile().mkdirs();
            currentConfig = createDefaultConfig();
            saveConfig();
        }
        return currentConfig;
    }

    public void saveConfig() throws IOException {
        CONFIG_DIR.toFile().mkdirs();
        encryptKeys(currentConfig);
        mapper.writeValue(configFile, currentConfig);
        decryptKeys(currentConfig);
    }

    // Keys are encrypted before serialisation and decrypted after loading.
    // This keeps plaintext keys in memory only during the application session.
    private void encryptKeys(AppConfig config) {
        if (config.getModels() == null) return;
        for (ModelConfig model : config.getModels()) {
            if (model.getApiKeys() == null) continue;
            for (ModelConfig.ApiKeyConfig key : model.getApiKeys()) {
                String raw = key.getKey();
                if (raw != null && !raw.isEmpty() && !EncryptionUtil.isEncrypted(raw)) {
                    key.setKey(encryptionUtil.encrypt(raw));
                }
            }
        }
    }

    private void decryptKeys(AppConfig config) {
        if (config.getModels() == null) return;
        for (ModelConfig model : config.getModels()) {
            if (model.getApiKeys() == null) continue;
            for (ModelConfig.ApiKeyConfig key : model.getApiKeys()) {
                String val = key.getKey();
                if (val != null && !val.isEmpty() && EncryptionUtil.isEncrypted(val)) {
                    key.setKey(encryptionUtil.decrypt(val));
                }
            }
        }
    }

    public List<AiModel> getModels() {
        if (currentConfig == null || currentConfig.getModels() == null) {
            return Collections.emptyList();
        }
        return currentConfig.getModels().stream()
            .filter(ModelConfig::isEnabled)
            .map(this::toModel)
            .collect(Collectors.toList());
    }

    private AiModel toModel(ModelConfig mc) {
        AiModel model = new AiModel(mc.getName(), mc.getDisplayName(),
            mc.getProvider(), mc.getBaseUrl());
        model.setMonthlyBudget(mc.getMonthlyBudget());
        if (mc.getApiKeys() != null) {
            model.setApiKeys(mc.getApiKeys().stream()
                .filter(ModelConfig.ApiKeyConfig::isEnabled)
                .map(k -> {
                    ApiKeyEntry e = new ApiKeyEntry(k.getId(), k.getLabel(), k.getKey());
                    e.setAdmin(k.isAdmin());
                    return e;
                })
                .collect(Collectors.toList()));
        } else {
            model.setApiKeys(new ArrayList<>());
        }
        return model;
    }

    private AppConfig createDefaultConfig() {
        AppConfig config = new AppConfig();
        config.setVersion("1.0");
        return config;
    }

    public File getConfigFile() { return configFile; }
    public AppConfig getCurrentConfig() { return currentConfig; }
}
