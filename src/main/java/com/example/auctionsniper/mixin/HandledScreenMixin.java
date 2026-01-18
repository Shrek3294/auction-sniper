package com.example.auctionsniper.mixin;

import com.example.auctionsniper.AuctionSniperMod;
import com.example.auctionsniper.AutoRelistHandler;
import com.example.auctionsniper.DebugLog;
import com.example.auctionsniper.NtfyService;
import com.example.auctionsniper.SniperConfig;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends net.minecraft.client.gui.screen.Screen {

    @Unique
    private static final Pattern PRICE_PATTERN = Pattern.compile("Price:\\s*\\$([\\d,.]+)\\s*([KMB]?)",
            Pattern.CASE_INSENSITIVE);
    @Unique
    private static final Pattern SELLER_PATTERN = Pattern.compile("Seller:\\s*(.+)", Pattern.CASE_INSENSITIVE);
    @Unique
    private static final long DEDUPE_WINDOW_MS = 60_000;
    @Unique
    private static final Map<String, Long> SEEN_LISTINGS = new HashMap<>();

    @Unique
    private long lastScanTime = 0;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (System.currentTimeMillis() - lastScanTime < 500)
            return;
        lastScanTime = System.currentTimeMillis();

        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        ScreenHandler handler = screen.getScreenHandler();

        if (isConfirmPurchaseScreen(screen)
                && (SniperConfig.autoBuyEnabled || AutoRelistHandler.shouldConfirm())) {
            attemptConfirmPurchase(handler);
            return;
        }

        for (Slot slot : handler.slots) {
            if (!slot.hasStack())
                continue;

            ItemStack stack = slot.getStack();
            String itemName = stack.getName().getString();
            String targetName = SniperConfig.targetItemName;

            if (targetName != null && !targetName.isBlank()
                    && itemName.toLowerCase().contains(targetName.toLowerCase())) {
                checkItemPrice(stack, handler, slot.id);
            }
        }
    }

    @Unique
    private void checkItemPrice(ItemStack stack, ScreenHandler handler, int slotId) {
        List<Text> tooltip = stack.getTooltip(Item.TooltipContext.create(MinecraftClient.getInstance().world),
                MinecraftClient.getInstance().player,
                TooltipType.BASIC);
        String seller = "";
        for (Text line : tooltip) {
            Matcher sellerMatcher = SELLER_PATTERN.matcher(line.getString());
            if (sellerMatcher.find()) {
                seller = sellerMatcher.group(1).trim();
                break;
            }
        }

        for (Text line : tooltip) {
            String text = line.getString();
            Matcher matcher = PRICE_PATTERN.matcher(text);

            if (matcher.find()) {
                String priceString = matcher.group(1).replace(",", "");
                String suffix = matcher.group(2);
                try {
                    double price = parsePrice(priceString, suffix);
                    if (price <= SniperConfig.maxPrice) {
                        String itemName = stack.getName().getString();
                        String listingKey = buildListingKey(itemName, price, seller);
                        if (shouldNotify(listingKey)) {
                            notifySnipe(itemName, price, seller, listingKey);
                            if (SniperConfig.autoBuyEnabled) {
                                attemptPurchase(handler, slotId, itemName);
                            }
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    @Unique
    private double parsePrice(String amount, String suffix) {
        double value = Double.parseDouble(amount);
        if (suffix == null || suffix.isEmpty())
            return value;
        return switch (suffix.toUpperCase()) {
            case "K" -> value * 1_000d;
            case "M" -> value * 1_000_000d;
            case "B" -> value * 1_000_000_000d;
            default -> value;
        };
    }

    @Unique
    private void notifySnipe(String itemName, double price, String seller, String listingKey) {
        String sellerInfo = seller == null || seller.isBlank() ? "Unknown" : seller;
        String msg = "SNIPE FOUND: " + itemName + " for $" + price + " (Seller: " + sellerInfo + ")";
        AuctionSniperMod.LOGGER.info(msg);
        DebugLog.log("match item='" + itemName + "' price=" + String.format("%.2f", price)
                + " seller='" + sellerInfo + "' key='" + listingKey + "'");
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
        NtfyService.sendNotification(msg);
    }

    @Unique
    private String buildListingKey(String itemName, double price, String seller) {
        String sellerValue = seller == null ? "" : seller.trim().toLowerCase();
        return (itemName + "|" + String.format("%.2f", price) + "|" + sellerValue).toLowerCase();
    }

    @Unique
    private boolean shouldNotify(String key) {
        long now = System.currentTimeMillis();
        SEEN_LISTINGS.entrySet().removeIf(entry -> now - entry.getValue() > DEDUPE_WINDOW_MS);
        Long lastSeen = SEEN_LISTINGS.get(key);
        if (lastSeen != null && now - lastSeen <= DEDUPE_WINDOW_MS) {
            return false;
        }
        SEEN_LISTINGS.put(key, now);
        return true;
    }

    @Unique
    private boolean isConfirmPurchaseScreen(HandledScreen<?> screen) {
        String normalized = normalizeTitle(screen.getTitle().getString());
        return normalized.contains("confirm");
    }

    @Unique
    private String normalizeTitle(String title) {
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

    @Unique
    private void attemptConfirmPurchase(ScreenHandler handler) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.interactionManager == null || client.player == null) {
            return;
        }
        for (Slot slot : handler.slots) {
            if (!slot.hasStack()) {
                continue;
            }
            ItemStack stack = slot.getStack();
            String name = stack.getName().getString().toLowerCase();
            if (name.contains("confirm") || stack.isOf(Items.LIME_STAINED_GLASS_PANE)) {
                client.interactionManager.clickSlot(
                        handler.syncId,
                        slot.id,
                        0,
                        SlotActionType.PICKUP,
                        client.player);
                DebugLog.log("auto-confirm click slot=" + slot.id);
                AutoRelistHandler.onConfirmClicked();
                return;
            }
        }
    }

    @Unique
    private void attemptPurchase(ScreenHandler handler, int slotId, String itemName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.interactionManager == null || client.player == null) {
            return;
        }
        client.interactionManager.clickSlot(
                handler.syncId,
                slotId,
                0,
                SlotActionType.PICKUP,
                client.player);
        DebugLog.log("auto-buy listing-click slot=" + slotId);
        AutoRelistHandler.onListingClicked(itemName);
    }
}
