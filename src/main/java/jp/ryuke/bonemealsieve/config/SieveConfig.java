package jp.ryuke.bonemealsieve.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Server-side probability, loaded once at startup. Invalid files are preserved. */
public final class SieveConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bonemeal_sieve");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final double DEFAULT_CHANCE = 0.30;
    private static double qualityChance = DEFAULT_CHANCE;

    private SieveConfig() {}

    public static double qualityBoneMealChance() {
        return qualityChance;
    }

    public static void load() {
        load(FabricLoader.getInstance().getConfigDir().resolve("bonemeal_sieve.json"));
    }

    static void load(Path path) {
        qualityChance = DEFAULT_CHANCE;
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                JsonObject defaults = new JsonObject();
                defaults.addProperty("qualityBoneMealChance", DEFAULT_CHANCE);
                Files.writeString(path, GSON.toJson(defaults) + System.lineSeparator(), StandardCharsets.UTF_8);
                return;
            }
            JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!json.has("qualityBoneMealChance")) {
                return;
            }
            var value = json.get("qualityBoneMealChance");
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("qualityBoneMealChance must be a number");
            }
            double chance = value.getAsDouble();
            if (!Double.isFinite(chance) || chance < 0.0 || chance > 1.0) {
                throw new IllegalArgumentException("qualityBoneMealChance must be between 0.0 and 1.0");
            }
            qualityChance = chance;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not load {}. Using qualityBoneMealChance=0.30; file left unchanged.", path, exception);
        }
    }
}
