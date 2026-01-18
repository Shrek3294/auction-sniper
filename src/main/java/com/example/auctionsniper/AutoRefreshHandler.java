package com.example.auctionsniper;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Random;

public class AutoRefreshHandler {
    private static final Random RANDOM = new Random();
    private static long lastRefreshTime = 0;
    private static long nextRefreshDelay = 0;
    private static boolean enabled = SniperConfig.autoRefreshEnabled;
    private static long lastAhCommandTime = 0;
    private static final long AH_COMMAND_COOLDOWN_MS = 30_000;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!enabled || client.player == null || client.world == null) {
                return;
            }

            // Check if we're in a handled screen (chest/auction house)
            if (!(client.currentScreen instanceof HandledScreen<?> screen)) {
                attemptReturnToAuctionHouse(client, "not-handled", null);
                lastRefreshTime = 0;
                return;
            }
            if (isConfirmPurchaseScreen(screen)) {
                lastRefreshTime = 0;
                return;
            }
            if (!isAuctionHouseScreen(screen)) {
                attemptReturnToAuctionHouse(client, "not-auction", screen.getTitle().getString());
                lastRefreshTime = 0;
                return;
            }

            // Initialize timing on first open
            if (lastRefreshTime == 0) {
                lastRefreshTime = System.currentTimeMillis();
                nextRefreshDelay = getRandomDelay();
                return;
            }

            // Check if it's time to refresh
            long elapsed = System.currentTimeMillis() - lastRefreshTime;
            if (elapsed >= nextRefreshDelay) {
                performRefresh(client, screen);
                lastRefreshTime = System.currentTimeMillis();
                nextRefreshDelay = getRandomDelay();
            }
        });
    }

    private static void performRefresh(MinecraftClient client, HandledScreen<?> screen) {
        // Look for a refresh button in the GUI (typically a clock, arrow, or specific
        // item)
        var handler = screen.getScreenHandler();

        for (int i = 0; i < handler.slots.size(); i++) {
            var slot = handler.slots.get(i);
            ItemStack stack = slot.getStack();

            // Common refresh button items: CLOCK, ARROW, BARRIER, etc.
            // Check item name for "refresh", "next", "previous" etc.
            if (!stack.isEmpty()) {
                String itemName = stack.getName().getString().toLowerCase();

                if (itemName.contains("refresh") ||
                        itemName.contains("next page") ||
                        itemName.contains("previous page") ||
                        stack.isOf(Items.CLOCK) ||
                        stack.isOf(Items.ARROW) ||
                        stack.isOf(Items.ANVIL)) {

                    // Simulate clicking the slot
                    clickSlot(client, handler, i);
                    return;
                }
            }
        }
    }

    private static void clickSlot(MinecraftClient client, net.minecraft.screen.ScreenHandler handler, int slotId) {
        if (client.interactionManager != null) {
            // Simulate a left-click on the slot
            client.interactionManager.clickSlot(
                    handler.syncId,
                    slotId,
                    0, // left mouse button
                    SlotActionType.PICKUP,
                    client.player);
        }
    }

    private static long getRandomDelay() {
        int min = SniperConfig.refreshIntervalMin;
        int max = SniperConfig.refreshIntervalMax;
        return min + RANDOM.nextInt(max - min + 1);
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!enabled) {
            lastRefreshTime = 0;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    private static boolean isAuctionHouseScreen(HandledScreen<?> screen) {
        String normalized = normalizeTitle(screen.getTitle().getString());
        return normalized.contains("auction");
    }

    private static boolean isConfirmPurchaseScreen(HandledScreen<?> screen) {
        String normalized = normalizeTitle(screen.getTitle().getString());
        return normalized.contains("confirm");
    }

    private static String normalizeTitle(String title) {
        return title.toLowerCase()
                .replace("ᴀ", "a")
                .replace("ᴜ", "u")
                .replace("ᴄ", "c")
                .replace("ᴛ", "t")
                .replace("ɪ", "i")
                .replace("ᴏ", "o")
                .replace("ɴ", "n")
                .replace("ᴘ", "p")
                .replace("ʀ", "r")
                .replace("ʜ", "h")
                .replace("ᴍ", "m")
                .replace("ꜰ", "f")
                .replace("ѕ", "s");
    }

    private static void attemptReturnToAuctionHouse(MinecraftClient client, String reason, String title) {
        if (AutoRelistHandler.isBusy()) {
            return;
        }
        if (SniperConfig.targetItemName == null || SniperConfig.targetItemName.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastAhCommandTime < AH_COMMAND_COOLDOWN_MS) {
            return;
        }
        if (client.player == null || client.player.networkHandler == null) {
            return;
        }
        String format = SniperConfig.ahCommandFormat == null || SniperConfig.ahCommandFormat.isBlank()
                ? "ah %s"
                : SniperConfig.ahCommandFormat;
        String command = String.format(format, SniperConfig.targetItemName).trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        client.player.networkHandler.sendChatCommand(command);
        lastAhCommandTime = now;
    }
}
