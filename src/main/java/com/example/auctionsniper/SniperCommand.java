package com.example.auctionsniper;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class SniperCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(SniperCommand::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("auctionsniper")
                .then(literal("toggle")
                        .executes(context -> {
                            boolean newState = !AutoRefreshHandler.isEnabled();
                            AutoRefreshHandler.setEnabled(newState);
                            SniperConfig.autoRefreshEnabled = newState;
                            SniperConfig.save();
                            context.getSource().sendFeedback(Text.literal("\u00A7aAuto-refresh: " +
                                    (newState ? "\u00A72ENABLED" : "\u00A7cDISABLED")));
                            return 1;
                        }))
                .then(literal("status")
                        .executes(context -> {
                            boolean enabled = AutoRefreshHandler.isEnabled();
                            context.getSource().sendFeedback(Text.literal("\u00A76=== Auction Sniper Status ==="));
                            context.getSource().sendFeedback(Text.literal("\u00A7eAuto-refresh: " +
                                    (enabled ? "\u00A7aENABLED" : "\u00A7cDISABLED")));
                            context.getSource().sendFeedback(Text.literal("\u00A7eAuto-buy: " +
                                    (SniperConfig.autoBuyEnabled ? "\u00A7aENABLED" : "\u00A7cDISABLED")));
                            context.getSource().sendFeedback(Text.literal("\u00A7eAuto-relist: " +
                                    (SniperConfig.autoRelistEnabled ? "\u00A7aENABLED" : "\u00A7cDISABLED")));
                            context.getSource().sendFeedback(Text.literal("\u00A7eRelist Price: \u00A7f$" +
                                    String.format("%,.0f", SniperConfig.relistPrice)));
                            context.getSource()
                                    .sendFeedback(Text.literal("\u00A7eTarget: \u00A7f" + SniperConfig.targetItemName));
                            context.getSource().sendFeedback(Text.literal("\u00A7eMax Price: \u00A7f$" +
                                    String.format("%,.0f", SniperConfig.maxPrice)));
                            context.getSource().sendFeedback(Text.literal("\u00A7eRefresh Interval: \u00A7f" +
                                    SniperConfig.refreshIntervalMin + "-" + SniperConfig.refreshIntervalMax + "ms"));
                            return 1;
                        }))
                .then(literal("target")
                        .then(argument("itemName", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String itemName = StringArgumentType.getString(context, "itemName");
                                    SniperConfig.targetItemName = itemName;
                                    SniperConfig.save();
                                    context.getSource()
                                            .sendFeedback(Text.literal("\u00A7aTarget item set to: \u00A7f" + itemName));
                                    return 1;
                                })))
                .then(literal("price")
                        .then(argument("maxPrice", DoubleArgumentType.doubleArg(0))
                                .executes(context -> {
                                    double price = DoubleArgumentType.getDouble(context, "maxPrice");
                                    SniperConfig.maxPrice = price;
                                    SniperConfig.save();
                                    context.getSource().sendFeedback(Text.literal("\u00A7aMax price set to: \u00A7f$" +
                                            String.format("%,.0f", price)));
                                    return 1;
                                })))
                .then(literal("interval")
                        .then(argument("min", IntegerArgumentType.integer(100))
                                .then(argument("max", IntegerArgumentType.integer(100))
                                        .executes(context -> {
                                            int min = IntegerArgumentType.getInteger(context, "min");
                                            int max = IntegerArgumentType.getInteger(context, "max");
                                            if (min > max) {
                                                context.getSource()
                                                        .sendError(Text.literal("\u00A7cMin must be less than max!"));
                                                return 0;
                                            }
                                            SniperConfig.refreshIntervalMin = min;
                                            SniperConfig.refreshIntervalMax = max;
                                            SniperConfig.save();
                                            context.getSource()
                                                    .sendFeedback(Text.literal("\u00A7aRefresh interval set to: \u00A7f" +
                                                            min + "-" + max + "ms"));
                                            return 1;
                                        }))))
                .then(literal("buy")
                        .then(argument("state", StringArgumentType.word())
                                .executes(context -> {
                                    String state = StringArgumentType.getString(context, "state").toLowerCase();
                                    boolean enabled;
                                    if (state.equals("on") || state.equals("true") || state.equals("enable")) {
                                        enabled = true;
                                    } else if (state.equals("off") || state.equals("false") || state.equals("disable")) {
                                        enabled = false;
                                    } else {
                                        context.getSource().sendError(Text.literal("\u00A7cUse: /auctionsniper buy <on|off>"));
                                        return 0;
                                    }
                                    SniperConfig.autoBuyEnabled = enabled;
                                    SniperConfig.save();
                                    context.getSource().sendFeedback(Text.literal("\u00A7aAuto-buy: " +
                                            (enabled ? "\u00A72ENABLED" : "\u00A7cDISABLED")));
                                    return 1;
                                })))
                .then(literal("relist")
                        .then(argument("state", StringArgumentType.word())
                                .executes(context -> {
                                    String state = StringArgumentType.getString(context, "state").toLowerCase();
                                    boolean enabled;
                                    if (state.equals("on") || state.equals("true") || state.equals("enable")) {
                                        enabled = true;
                                    } else if (state.equals("off") || state.equals("false") || state.equals("disable")) {
                                        enabled = false;
                                    } else {
                                        context.getSource().sendError(Text.literal("\u00A7cUse: /auctionsniper relist <on|off>"));
                                        return 0;
                                    }
                                    SniperConfig.autoRelistEnabled = enabled;
                                    SniperConfig.save();
                                    context.getSource().sendFeedback(Text.literal("\u00A7aAuto-relist: " +
                                            (enabled ? "\u00A72ENABLED" : "\u00A7cDISABLED")));
                                    return 1;
                                })))
                .then(literal("relistprice")
                        .then(argument("price", DoubleArgumentType.doubleArg(0))
                                .executes(context -> {
                                    double price = DoubleArgumentType.getDouble(context, "price");
                                    SniperConfig.relistPrice = price;
                                    SniperConfig.save();
                                    context.getSource().sendFeedback(Text.literal("\u00A7aRelist price set to: \u00A7f$" +
                                            String.format("%,.0f", price)));
                                    return 1;
                                })))
                .then(literal("ahcmd")
                        .then(argument("format", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String format = StringArgumentType.getString(context, "format");
                                    SniperConfig.ahCommandFormat = format;
                                    SniperConfig.save();
                                    context.getSource()
                                            .sendFeedback(Text.literal("\u00A7aAH command set to: \u00A7f" + format));
                                    return 1;
                                })))
                .then(literal("sellcmd")
                        .then(argument("format", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String format = StringArgumentType.getString(context, "format");
                                    SniperConfig.ahSellCommandFormat = format;
                                    SniperConfig.save();
                                    context.getSource().sendFeedback(
                                            Text.literal("\u00A7aAH sell command set to: \u00A7f" + format));
                                    return 1;
                                })))
                .then(literal("ntfy")
                        .then(argument("topic", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String topic = StringArgumentType.getString(context, "topic");
                                    SniperConfig.ntfyTopic = topic;
                                    SniperConfig.save();
                                    context.getSource()
                                            .sendFeedback(Text.literal("\u00A7aNtfy topic set to: \u00A7f" + topic));
                                    return 1;
                                })))
                .then(literal("config")
                        .executes(context -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            Screen parent = client.currentScreen;
                            client.setScreen(new AuctionSniperConfigScreen(parent));
                            return 1;
                        }))
                .then(literal("help")
                        .executes(context -> {
                            context.getSource().sendFeedback(Text.literal("\u00A76=== Auction Sniper Commands ==="));
                            context.getSource()
                                    .sendFeedback(Text.literal("\u00A7e/auctionsniper toggle \u00A77- Toggle auto-refresh"));
                            context.getSource()
                                    .sendFeedback(Text.literal("\u00A7e/auctionsniper status \u00A77- Show current settings"));
                            context.getSource()
                                    .sendFeedback(Text.literal("\u00A7e/auctionsniper target <item> \u00A77- Set target item"));
                            context.getSource()
                                    .sendFeedback(Text.literal("\u00A7e/auctionsniper price <amount> \u00A77- Set max price"));
                            context.getSource().sendFeedback(Text
                                    .literal("\u00A7e/auctionsniper interval <min> <max> \u00A77- Set refresh interval (ms)"));
                            context.getSource()
                                    .sendFeedback(Text.literal("\u00A7e/auctionsniper buy <on|off> \u00A77- Toggle auto-buy"));
                            context.getSource()
                                    .sendFeedback(Text.literal("\u00A7e/auctionsniper relist <on|off> \u00A77- Toggle auto-relist"));
                            context.getSource().sendFeedback(
                                    Text.literal("\u00A7e/auctionsniper relistprice <amount> \u00A77- Set relist price"));
                            context.getSource()
                                    .sendFeedback(Text.literal("\u00A7e/auctionsniper ahcmd <format> \u00A77- Set AH command"));
                            context.getSource()
                                    .sendFeedback(Text.literal("\u00A7e/auctionsniper sellcmd <format> \u00A77- Set AH sell command"));
                            context.getSource()
                                    .sendFeedback(Text.literal("\u00A7e/auctionsniper ntfy <topic> \u00A77- Set ntfy topic"));
                            context.getSource()
                                    .sendFeedback(Text.literal("\u00A7e/auctionsniper config \u00A77- Open config screen"));
                            return 1;
                        })));
    }
}
