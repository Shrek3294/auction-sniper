package com.example.auctionsniper;

import net.minecraft.client.MinecraftClient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DebugLog {
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static BufferedWriter writer;
    private static Path logPath;

    private DebugLog() {
    }

    public static synchronized void log(String message) {
        ensureWriter();
        if (writer == null) {
            return;
        }
        try {
            String ts = LocalDateTime.now().format(TS_FORMAT);
            writer.write(ts + " " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            AuctionSniperMod.LOGGER.warn("AuctionSniper debug log write failed", e);
        }
    }

    private static void ensureWriter() {
        if (writer != null) {
            return;
        }
        Path baseDir;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.runDirectory != null) {
            baseDir = client.runDirectory.toPath();
        } else {
            baseDir = Path.of(".");
        }
        Path logsDir = baseDir.resolve("logs");
        logPath = logsDir.resolve("auctionsniper-debug.log");
        try {
            Files.createDirectories(logsDir);
            writer = Files.newBufferedWriter(
                    logPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            AuctionSniperMod.LOGGER.warn("AuctionSniper debug log init failed: {}", logPath, e);
            writer = null;
        }
    }
}
