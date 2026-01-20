package com.example.auctionsniper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SniperConfig {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("auctionsniper.json")
            .toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String targetItemName = "Ancient Debris";
    public static double maxPrice = 1600000.0;
    public static String ntfyTopic = "minecraft_sniper_test";
    public static int refreshIntervalMin = 1000;
    public static int refreshIntervalMax = 1500;
    public static boolean autoRefreshEnabled = true;
    public static boolean autoBuyEnabled = false;
    public static String ahCommandFormat = "ah %s";
    public static boolean autoRelistEnabled = false;
    public static double relistPrice = 0.0;
    public static String ahSellCommandFormat = "ah sell %s";

    private static class ConfigData {
        String targetItemName = "Ancient Debris";
        double maxPrice = 1600000.0;
        String ntfyTopic = "minecraft_sniper_test";
        int refreshIntervalMin = 1000;
        int refreshIntervalMax = 1500;
        boolean autoRefreshEnabled = true;
        boolean autoBuyEnabled = false;
        String ahCommandFormat = "ah %s";
        boolean autoRelistEnabled = false;
        double relistPrice = 0.0;
        String ahSellCommandFormat = "ah sell %s";
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
                if (loaded != null) {
                    targetItemName = loaded.targetItemName;
                    maxPrice = loaded.maxPrice;
                    ntfyTopic = loaded.ntfyTopic;
                    refreshIntervalMin = loaded.refreshIntervalMin;
                    refreshIntervalMax = loaded.refreshIntervalMax;
                    autoRefreshEnabled = loaded.autoRefreshEnabled;
                    autoBuyEnabled = loaded.autoBuyEnabled;
                    ahCommandFormat = loaded.ahCommandFormat;
                    autoRelistEnabled = loaded.autoRelistEnabled;
                    relistPrice = loaded.relistPrice;
                    ahSellCommandFormat = loaded.ahSellCommandFormat;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            ConfigData data = new ConfigData();
            data.targetItemName = targetItemName;
            data.maxPrice = maxPrice;
            data.ntfyTopic = ntfyTopic;
            data.refreshIntervalMin = refreshIntervalMin;
            data.refreshIntervalMax = refreshIntervalMax;
            data.autoRefreshEnabled = autoRefreshEnabled;
            data.autoBuyEnabled = autoBuyEnabled;
            data.ahCommandFormat = ahCommandFormat;
            data.autoRelistEnabled = autoRelistEnabled;
            data.relistPrice = relistPrice;
            data.ahSellCommandFormat = ahSellCommandFormat;
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void resetToDefaults() {
        Defaults defaults = defaults();
        targetItemName = defaults.targetItemName();
        maxPrice = defaults.maxPrice();
        ntfyTopic = defaults.ntfyTopic();
        refreshIntervalMin = defaults.refreshIntervalMin();
        refreshIntervalMax = defaults.refreshIntervalMax();
        autoRefreshEnabled = defaults.autoRefreshEnabled();
        autoBuyEnabled = defaults.autoBuyEnabled();
        ahCommandFormat = defaults.ahCommandFormat();
        autoRelistEnabled = defaults.autoRelistEnabled();
        relistPrice = defaults.relistPrice();
        ahSellCommandFormat = defaults.ahSellCommandFormat();
        save();
    }

    public static Defaults defaults() {
        ConfigData data = new ConfigData();
        return new Defaults(
                data.targetItemName,
                data.maxPrice,
                data.ntfyTopic,
                data.refreshIntervalMin,
                data.refreshIntervalMax,
                data.autoRefreshEnabled,
                data.autoBuyEnabled,
                data.ahCommandFormat,
                data.autoRelistEnabled,
                data.relistPrice,
                data.ahSellCommandFormat
        );
    }

    public record Defaults(
            String targetItemName,
            double maxPrice,
            String ntfyTopic,
            int refreshIntervalMin,
            int refreshIntervalMax,
            boolean autoRefreshEnabled,
            boolean autoBuyEnabled,
            String ahCommandFormat,
            boolean autoRelistEnabled,
            double relistPrice,
            String ahSellCommandFormat
    ) {
    }
}
