package com.autotrade.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("autotrade.json");
    private static AutoTradeConfig config;

    private ConfigManager() {
    }

    public static AutoTradeConfig getConfig() {
        if (config == null) {
            config = load();
        }
        return config;
    }

    public static void save() {
        if (config == null) {
            return;
        }

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public static void reload() {
        config = load();
    }

    private static AutoTradeConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            AutoTradeConfig defaults = new AutoTradeConfig();
            config = defaults;
            save();
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            AutoTradeConfig loaded = GSON.fromJson(reader, AutoTradeConfig.class);
            if (loaded == null) {
                loaded = new AutoTradeConfig();
            }
            sanitize(loaded);
            return loaded;
        } catch (IOException | JsonSyntaxException ignored) {
            return new AutoTradeConfig();
        }
    }

    private static void sanitize(AutoTradeConfig cfg) {
        if (cfg.globalTradeIndex < 1) {
            cfg.globalTradeIndex = 1;
        }
        if (cfg.selectionScope == null) {
            cfg.selectionScope = AutoTradeConfig.SelectionScope.GLOBAL;
        }
        if (cfg.rateMode == null) {
            cfg.rateMode = AutoTradeConfig.RateMode.MODERATE;
        }
        if (cfg.tradeIndexByProfession == null) {
            cfg.tradeIndexByProfession = new java.util.HashMap<>();
        }
        if (cfg.tradeIndexByVillager == null) {
            cfg.tradeIndexByVillager = new java.util.HashMap<>();
        }
        if (cfg.autoUpdateGithubOwner == null || cfg.autoUpdateGithubOwner.isBlank()) {
            cfg.autoUpdateGithubOwner = "itsasheruwu";
        }
        if (cfg.autoUpdateGithubRepo == null || cfg.autoUpdateGithubRepo.isBlank()) {
            cfg.autoUpdateGithubRepo = "Auto-Trade-Mod";
        }
    }
}
