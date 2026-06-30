package com.aiusage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void testCreateDefaultConfig() throws Exception {
        ConfigManager manager = new ConfigManager();
        assertNotNull(manager.getModels());
    }

    @Test
    void testLoadNonExistentConfigCreatesDefault() throws Exception {
        ConfigManager manager = new ConfigManager();
        AppConfig config = manager.loadConfig();
        assertNotNull(config);
        assertEquals("1.0", config.getVersion());
    }

    @Test
    void testConfigPersistence() throws Exception {
        File testFile = tempDir.resolve("test-config.json").toFile();
        ObjectMapper mapper = new ObjectMapper();
        AppConfig config = new AppConfig();
        config.setVersion("2.0");
        mapper.writeValue(testFile, config);
        AppConfig loaded = mapper.readValue(testFile, AppConfig.class);
        assertEquals("2.0", loaded.getVersion());
    }
}
