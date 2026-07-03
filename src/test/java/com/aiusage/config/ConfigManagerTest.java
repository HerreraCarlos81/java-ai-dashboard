package com.aiusage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that configuration files are correctly serialised, deserialised,
 * and that default values are applied when no config file exists yet.
 */
class ConfigManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultsToEmptyModelList() {
        ConfigManager manager = new ConfigManager();
        assertNotNull(manager.getModels());
        assertTrue(manager.getModels().isEmpty());
    }

    @Test
    void loadCreatesDefaultConfigWhenFileMissing() throws Exception {
        ConfigManager manager = new ConfigManager();
        AppConfig config = manager.loadConfig();
        assertNotNull(config);
        assertEquals("1.0", config.getVersion());
    }

    @Test
    void configRoundTripPreservesVersion() throws Exception {
        File testFile = tempDir.resolve("test-config.json").toFile();
        ObjectMapper mapper = new ObjectMapper();

        AppConfig original = new AppConfig();
        original.setVersion("2.0");
        mapper.writeValue(testFile, original);

        AppConfig loaded = mapper.readValue(testFile, AppConfig.class);
        assertEquals("2.0", loaded.getVersion());
    }

    @Test
    void configRoundTripPreservesMonthlyBudget() throws Exception {
        File testFile = tempDir.resolve("budget-config.json").toFile();
        ObjectMapper mapper = new ObjectMapper();

        ModelConfig model = new ModelConfig();
        model.setName("test-model");
        model.setDisplayName("Test");
        model.setProvider("openai");
        model.setBaseUrl("https://api.openai.com/v1");
        model.setEnabled(true);
        model.setMonthlyBudget(50.00);

        AppConfig config = new AppConfig();
        config.setVersion("1.0");
        config.setModels(List.of(model));

        mapper.writeValue(testFile, config);

        AppConfig loaded = mapper.readValue(testFile, AppConfig.class);
        assertNotNull(loaded.getModels());
        assertEquals(1, loaded.getModels().size());
        assertEquals(50.00, loaded.getModels().get(0).getMonthlyBudget(), 0.001);
    }

    @Test
    void configRoundTripPreservesApiKeyConfig() throws Exception {
        File testFile = tempDir.resolve("key-config.json").toFile();
        ObjectMapper mapper = new ObjectMapper();

        ModelConfig.ApiKeyConfig apiKey = new ModelConfig.ApiKeyConfig();
        apiKey.setId("key-1");
        apiKey.setLabel("Production");
        apiKey.setKey("sk-test-12345");
        apiKey.setEnabled(true);
        apiKey.setAdmin(true);

        ModelConfig model = new ModelConfig();
        model.setName("test-model");
        model.setApiKeys(List.of(apiKey));

        AppConfig config = new AppConfig();
        config.setVersion("1.0");
        config.setModels(List.of(model));

        mapper.writeValue(testFile, config);

        AppConfig loaded = mapper.readValue(testFile, AppConfig.class);
        ModelConfig.ApiKeyConfig loadedKey = loaded.getModels().get(0).getApiKeys().get(0);
        assertEquals("key-1", loadedKey.getId());
        assertEquals("Production", loadedKey.getLabel());
        assertEquals("sk-test-12345", loadedKey.getKey());
        assertTrue(loadedKey.isAdmin());
    }
}
