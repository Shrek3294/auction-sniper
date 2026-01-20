package com.example.auctionsniper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class AuctionSniperConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 21;
    private static final int FIELD_HEIGHT = 20;
    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final int ERROR_COLOR = 0xFFFF5555;

    private final Screen parent;
    private final List<Label> labels = new ArrayList<>();

    private TextFieldWidget targetItemNameField;
    private TextFieldWidget maxPriceField;
    private TextFieldWidget refreshMinField;
    private TextFieldWidget refreshMaxField;
    private TextFieldWidget ntfyTopicField;
    private TextFieldWidget ahCommandFormatField;
    private TextFieldWidget relistPriceField;
    private TextFieldWidget sellCommandFormatField;

    private boolean autoRefreshEnabled;
    private boolean autoBuyEnabled;
    private boolean autoRelistEnabled;

    private ButtonWidget autoRefreshButton;
    private ButtonWidget autoBuyButton;
    private ButtonWidget autoRelistButton;

    private Text errorText = null;

    public AuctionSniperConfigScreen(Screen parent) {
        super(Text.literal("Auction Sniper"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        labels.clear();
        errorText = null;

        int centerX = width / 2;
        int labelX = centerX - 155;
        int fieldX = centerX - 10;
        int fieldWidth = 185;

        int y = 32;

        targetItemNameField = addTextField(fieldX, y, fieldWidth, SniperConfig.targetItemName);
        labels.add(new Label(Text.literal("Target Item Name"), labelX, y + 6));
        y += ROW_HEIGHT;

        maxPriceField = addTextField(fieldX, y, fieldWidth, Double.toString(SniperConfig.maxPrice));
        labels.add(new Label(Text.literal("Max Price"), labelX, y + 6));
        y += ROW_HEIGHT;

        refreshMinField = addTextField(fieldX, y, (fieldWidth - 6) / 2, Integer.toString(SniperConfig.refreshIntervalMin));
        refreshMaxField = addTextField(fieldX + (fieldWidth + 6) / 2, y, (fieldWidth - 6) / 2,
                Integer.toString(SniperConfig.refreshIntervalMax));
        labels.add(new Label(Text.literal("Refresh Interval (ms)"), labelX, y + 6));
        y += ROW_HEIGHT;

        ntfyTopicField = addTextField(fieldX, y, fieldWidth, SniperConfig.ntfyTopic);
        labels.add(new Label(Text.literal("ntfy Topic"), labelX, y + 6));
        y += ROW_HEIGHT;

        autoRefreshEnabled = SniperConfig.autoRefreshEnabled;
        autoRefreshButton = addDrawableChild(
                ButtonWidget.builder(toggleLabel("Auto-refresh", autoRefreshEnabled), button -> {
                    autoRefreshEnabled = !autoRefreshEnabled;
                    button.setMessage(toggleLabel("Auto-refresh", autoRefreshEnabled));
                }).dimensions(fieldX, y, fieldWidth, FIELD_HEIGHT).build());
        labels.add(new Label(Text.literal("Auto-refresh"), labelX, y + 6));
        y += ROW_HEIGHT;

        autoBuyEnabled = SniperConfig.autoBuyEnabled;
        autoBuyButton = addDrawableChild(
                ButtonWidget.builder(toggleLabel("Auto-buy", autoBuyEnabled), button -> {
                    autoBuyEnabled = !autoBuyEnabled;
                    button.setMessage(toggleLabel("Auto-buy", autoBuyEnabled));
                }).dimensions(fieldX, y, fieldWidth, FIELD_HEIGHT).build());
        labels.add(new Label(Text.literal("Auto-buy"), labelX, y + 6));
        y += ROW_HEIGHT;

        ahCommandFormatField = addTextField(fieldX, y, fieldWidth, SniperConfig.ahCommandFormat);
        labels.add(new Label(Text.literal("AH Command Format"), labelX, y + 6));
        y += ROW_HEIGHT;

        autoRelistEnabled = SniperConfig.autoRelistEnabled;
        autoRelistButton = addDrawableChild(
                ButtonWidget.builder(toggleLabel("Auto-relist", autoRelistEnabled), button -> {
                    autoRelistEnabled = !autoRelistEnabled;
                    button.setMessage(toggleLabel("Auto-relist", autoRelistEnabled));
                }).dimensions(fieldX, y, fieldWidth, FIELD_HEIGHT).build());
        labels.add(new Label(Text.literal("Auto-relist"), labelX, y + 6));
        y += ROW_HEIGHT;

        relistPriceField = addTextField(fieldX, y, fieldWidth, Double.toString(SniperConfig.relistPrice));
        labels.add(new Label(Text.literal("Relist Price"), labelX, y + 6));
        y += ROW_HEIGHT;

        sellCommandFormatField = addTextField(fieldX, y, fieldWidth, SniperConfig.ahSellCommandFormat);
        labels.add(new Label(Text.literal("Sell Command Format"), labelX, y + 6));

        int buttonY = height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(centerX - 155, buttonY, 100, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> saveAndClose())
                .dimensions(centerX - 50, buttonY, 100, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), button -> resetToDefaults())
                .dimensions(centerX + 55, buttonY, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, LABEL_COLOR);

        for (Label label : labels) {
            context.drawTextWithShadow(textRenderer, label.text, label.x, label.y, LABEL_COLOR);
        }

        if (errorText != null) {
            context.drawCenteredTextWithShadow(textRenderer, errorText, width / 2, 28, ERROR_COLOR);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0101010);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private TextFieldWidget addTextField(int x, int y, int width, String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, FIELD_HEIGHT, Text.empty());
        field.setText(value == null ? "" : value);
        return addDrawableChild(field);
    }

    private static Text toggleLabel(String name, boolean enabled) {
        return Text.literal(name + ": ")
                .append(Text.literal(enabled ? "ON" : "OFF").formatted(enabled ? Formatting.GREEN : Formatting.RED));
    }

    private void resetToDefaults() {
        errorText = null;

        SniperConfig.Defaults defaults = SniperConfig.defaults();
        targetItemNameField.setText(defaults.targetItemName());
        maxPriceField.setText(Double.toString(defaults.maxPrice()));
        refreshMinField.setText(Integer.toString(defaults.refreshIntervalMin()));
        refreshMaxField.setText(Integer.toString(defaults.refreshIntervalMax()));
        ntfyTopicField.setText(defaults.ntfyTopic());
        ahCommandFormatField.setText(defaults.ahCommandFormat());
        relistPriceField.setText(Double.toString(defaults.relistPrice()));
        sellCommandFormatField.setText(defaults.ahSellCommandFormat());

        autoRefreshEnabled = defaults.autoRefreshEnabled();
        autoBuyEnabled = defaults.autoBuyEnabled();
        autoRelistEnabled = defaults.autoRelistEnabled();

        autoRefreshButton.setMessage(toggleLabel("Auto-refresh", autoRefreshEnabled));
        autoBuyButton.setMessage(toggleLabel("Auto-buy", autoBuyEnabled));
        autoRelistButton.setMessage(toggleLabel("Auto-relist", autoRelistEnabled));
    }

    private void saveAndClose() {
        errorText = null;

        String targetItemName = targetItemNameField.getText().trim();
        String ntfyTopic = ntfyTopicField.getText().trim();
        String ahCommandFormat = ahCommandFormatField.getText().trim();
        String sellCommandFormat = sellCommandFormatField.getText().trim();

        Double maxPrice = parseNonNegativeDouble("Max Price", maxPriceField.getText());
        if (maxPrice == null) return;
        Double relistPrice = parseNonNegativeDouble("Relist Price", relistPriceField.getText());
        if (relistPrice == null) return;
        Integer refreshMin = parseMinInt("Refresh Min", refreshMinField.getText(), 100);
        if (refreshMin == null) return;
        Integer refreshMax = parseMinInt("Refresh Max", refreshMaxField.getText(), 100);
        if (refreshMax == null) return;
        if (refreshMax < refreshMin) {
            errorText = Text.literal("Refresh max must be >= min.");
            return;
        }
        if (!ntfyTopic.isEmpty() && ntfyTopic.matches(".*\\s+.*")) {
            errorText = Text.literal("ntfy topic cannot contain spaces.");
            return;
        }
        if (!ahCommandFormat.isEmpty() && !ahCommandFormat.contains("%s")) {
            errorText = Text.literal("AH command format must include %s.");
            return;
        }
        if (!sellCommandFormat.isEmpty() && !sellCommandFormat.contains("%s")) {
            errorText = Text.literal("Sell command format must include %s.");
            return;
        }

        SniperConfig.targetItemName = targetItemName;
        SniperConfig.maxPrice = maxPrice;
        SniperConfig.ntfyTopic = ntfyTopic;
        SniperConfig.refreshIntervalMin = refreshMin;
        SniperConfig.refreshIntervalMax = refreshMax;
        SniperConfig.autoRefreshEnabled = autoRefreshEnabled;
        SniperConfig.autoBuyEnabled = autoBuyEnabled;
        SniperConfig.ahCommandFormat = ahCommandFormat;
        SniperConfig.autoRelistEnabled = autoRelistEnabled;
        SniperConfig.relistPrice = relistPrice;
        SniperConfig.ahSellCommandFormat = sellCommandFormat;
        SniperConfig.save();

        AutoRefreshHandler.setEnabled(autoRefreshEnabled);

        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(parent);
    }

    private Double parseNonNegativeDouble(String label, String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            errorText = Text.literal(label + " is required.");
            return null;
        }
        try {
            double value = Double.parseDouble(trimmed.replace(",", ""));
            if (value < 0) {
                errorText = Text.literal(label + " must be >= 0.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            errorText = Text.literal(label + " must be a number.");
            return null;
        }
    }

    private Integer parseMinInt(String label, String input, int minValue) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            errorText = Text.literal(label + " is required.");
            return null;
        }
        try {
            int value = Integer.parseInt(trimmed);
            if (value < minValue) {
                errorText = Text.literal(label + " must be >= " + minValue + ".");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            errorText = Text.literal(label + " must be an integer.");
            return null;
        }
    }

    private record Label(Text text, int x, int y) {}
}
