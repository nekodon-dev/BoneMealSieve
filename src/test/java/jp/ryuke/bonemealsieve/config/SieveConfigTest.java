package jp.ryuke.bonemealsieve.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SieveConfigTest {
    @TempDir Path directory;

    @Test
    void createsDefaultConfig() throws Exception {
        Path path = directory.resolve("config/bonemeal_sieve.json");
        SieveConfig.load(path);
        assertEquals(0.30, SieveConfig.qualityBoneMealChance());
        assertTrue(Files.readString(path).contains("\"qualityBoneMealChance\": 0.3"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.3, 0.65, 1.0})
    void loadsProbabilityIncludingEndpoints(double chance) throws Exception {
        Path path = directory.resolve("bonemeal_sieve.json");
        Files.writeString(path, "{\"qualityBoneMealChance\":" + chance + "}");
        SieveConfig.load(path);
        assertEquals(chance, SieveConfig.qualityBoneMealChance());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.1", "1.1", "1e999", "null", "true", "\"0.5\"", "[]", "{}"})
    void rejectsInvalidProbabilityAndPreservesFile(String value) throws Exception {
        Path path = directory.resolve("bonemeal_sieve.json");
        String json = "{\"qualityBoneMealChance\":" + value + "}";
        Files.writeString(path, json);
        SieveConfig.load(path);
        assertEquals(0.30, SieveConfig.qualityBoneMealChance());
        assertEquals(json, Files.readString(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{broken", "null", "[]", "{}"})
    void handlesMalformedOrMissingSettings(String json) throws Exception {
        Path path = directory.resolve("bonemeal_sieve.json");
        Files.writeString(path, json);
        SieveConfig.load(path);
        assertEquals(0.30, SieveConfig.qualityBoneMealChance());
        assertEquals(json, Files.readString(path));
    }
}
