package com.example.auctionsniper;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Locale;

public class AutoRelistHandler {
    private enum State {
        IDLE,
        WAITING_FOR_PURCHASE_CONFIRM,
        READY_TO_SELL,
        WAITING_FOR_SELL_CONFIRM
    }

    private static State state = State.IDLE;
    private static long stateSinceMs = 0;
    private static String lastItemName = null;
    private static long lastWaitLogMs = 0;
    private static final long PURCHASE_CONFIRM_TIMEOUT_MS = 15_000;
    private static final long SELL_COMMAND_TIMEOUT_MS = 15_000;
    private static final long SELL_CONFIRM_TIMEOUT_MS = 15_000;
    private static final long WAIT_LOG_COOLDOWN_MS = 5_000;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!SniperConfig.autoRelistEnabled || SniperConfig.relistPrice <= 0) {
                if (state != State.IDLE) {
                    resetState();
                }
                return;
            }
            if (client.player == null || client.world == null) {
                return;
            }
            if (state == State.IDLE) {
                return;
            }
            if (isStateTimedOut()) {
                resetState();
                return;
            }

            if (state == State.READY_TO_SELL) {
                String target = getTargetName();
                if (target == null) {
                    resetState();
                    return;
                }
                if (client.currentScreen instanceof HandledScreen<?> screen) {
                    if (isConfirmScreen(screen)) {
                        return;
                    }
                    if (!ensureItemInHand(client, screen, target)) {
                        logWait("need-item-in-hand");
                        return;
                    }
                    sendSellCommand(client);
                    return;
                }

                int hotbarSlot = findHotbarSlot(client, target);
                if (hotbarSlot >= 0) {
                    client.player.getInventory().setSelectedSlot(hotbarSlot);
                    sendSellCommand(client);
                } else {
                    logWait("need-handled-screen");
                }
            }
        });
    }

    public static void onListingClicked(String itemName) {
        if (!SniperConfig.autoRelistEnabled || SniperConfig.relistPrice <= 0) {
            return;
        }
        lastItemName = itemName;
        state = State.WAITING_FOR_PURCHASE_CONFIRM;
        stateSinceMs = System.currentTimeMillis();
        DebugLog.log("auto-relist queued item='" + itemName + "' price=" + formatPrice());
    }

    public static void onConfirmClicked() {
        if (state == State.WAITING_FOR_PURCHASE_CONFIRM) {
            state = State.READY_TO_SELL;
            stateSinceMs = System.currentTimeMillis();
            DebugLog.log("auto-relist ready price=" + formatPrice());
        } else if (state == State.WAITING_FOR_SELL_CONFIRM) {
            DebugLog.log("auto-relist confirm-clicked");
            resetState();
        }
    }

    public static boolean shouldConfirm() {
        return state == State.WAITING_FOR_PURCHASE_CONFIRM || state == State.WAITING_FOR_SELL_CONFIRM;
    }

    public static boolean isBusy() {
        return state != State.IDLE;
    }

    private static boolean isStateTimedOut() {
        long now = System.currentTimeMillis();
        return switch (state) {
            case WAITING_FOR_PURCHASE_CONFIRM -> now - stateSinceMs > PURCHASE_CONFIRM_TIMEOUT_MS;
            case READY_TO_SELL -> now - stateSinceMs > SELL_COMMAND_TIMEOUT_MS;
            case WAITING_FOR_SELL_CONFIRM -> now - stateSinceMs > SELL_CONFIRM_TIMEOUT_MS;
            default -> false;
        };
    }

    private static void resetState() {
        state = State.IDLE;
        stateSinceMs = 0;
        lastItemName = null;
        lastWaitLogMs = 0;
    }

    private static void sendSellCommand(MinecraftClient client) {
        String format = SniperConfig.ahSellCommandFormat == null || SniperConfig.ahSellCommandFormat.isBlank()
                ? "ah sell %s"
                : SniperConfig.ahSellCommandFormat;
        String command = String.format(format, formatPrice()).trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        client.player.networkHandler.sendChatCommand(command);
        state = State.WAITING_FOR_SELL_CONFIRM;
        stateSinceMs = System.currentTimeMillis();
        DebugLog.log("auto-relist command-sent command='" + command + "'");
    }

    private static String formatPrice() {
        return String.format(Locale.US, "%.0f", SniperConfig.relistPrice);
    }

    private static boolean ensureItemInHand(MinecraftClient client, HandledScreen<?> screen, String target) {
        int hotbarSlot = findHotbarSlot(client, target);
        if (hotbarSlot >= 0) {
            client.player.getInventory().setSelectedSlot(hotbarSlot);
            return true;
        }

        int mainSlot = findMainInventorySlot(client, target);
        if (mainSlot < 0) {
            return false;
        }
        int emptyHotbar = findEmptyHotbarSlot(client);
        if (emptyHotbar < 0) {
            return false;
        }

        ScreenHandler handler = screen.getScreenHandler();
        if (!handler.getCursorStack().isEmpty()) {
            return false;
        }
        int playerStart = handler.slots.size() - 36;
        int sourceSlotId = playerStart + (mainSlot - 9);
        int targetSlotId = playerStart + 27 + emptyHotbar;
        clickSlot(client, handler, sourceSlotId);
        clickSlot(client, handler, targetSlotId);
        client.player.getInventory().setSelectedSlot(emptyHotbar);
        return true;
    }

    private static int findHotbarSlot(MinecraftClient client, String target) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getName().getString().toLowerCase().contains(target)) {
                return i;
            }
        }
        return -1;
    }

    private static int findMainInventorySlot(MinecraftClient client, String target) {
        for (int i = 9; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getName().getString().toLowerCase().contains(target)) {
                return i;
            }
        }
        return -1;
    }

    private static int findEmptyHotbarSlot(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static String getTargetName() {
        if (lastItemName == null || lastItemName.isBlank()) {
            return null;
        }
        return lastItemName.toLowerCase();
    }

    private static void logWait(String reason) {
        long now = System.currentTimeMillis();
        if (now - lastWaitLogMs < WAIT_LOG_COOLDOWN_MS) {
            return;
        }
        lastWaitLogMs = now;
        DebugLog.log("auto-relist waiting reason='" + reason + "'");
    }

    private static boolean isAuctionHouseScreen(HandledScreen<?> screen) {
        String normalized = normalizeTitle(screen.getTitle().getString());
        return normalized.contains("auction");
    }

    private static boolean isConfirmScreen(HandledScreen<?> screen) {
        String normalized = normalizeTitle(screen.getTitle().getString());
        return normalized.contains("confirm");
    }

    private static String normalizeTitle(String title) {
        return title.toLowerCase()
                .replace(" '?", "a")
                .replace(" 'o", "u")
                .replace(" ',", "c")
                .replace(" '>", "t")
                .replace("¦", "i")
                .replace(" '?", "o")
                .replace("'", "n")
                .replace(" '~", "p")
                .replace("E?", "r")
                .replace("Eo", "h")
                .replace(" '?", "m")
                .replace("ˆoø", "f")
                .replace("¥", "s");
    }

    private static void clickSlot(MinecraftClient client, ScreenHandler handler, int slotId) {
        if (client.interactionManager == null || client.player == null) {
            return;
        }
        client.interactionManager.clickSlot(
                handler.syncId,
                slotId,
                0,
                SlotActionType.PICKUP,
                client.player);
    }
}
