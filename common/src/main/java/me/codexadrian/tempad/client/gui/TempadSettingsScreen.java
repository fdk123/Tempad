package me.codexadrian.tempad.client.gui;

import me.codexadrian.tempad.common.network.NetworkHandler;
import me.codexadrian.tempad.common.network.messages.c2s.SaveTempadSettingsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

/** Compact Forge 1.20.1 settings for default app, shortcut macro and location order. */
public final class TempadSettingsScreen extends Screen {
    private final Screen parent;
    private final UUID deviceId;
    private final boolean hasTwister;
    private final boolean hasGuide;
    private String app;
    private String macro;
    private String order;
    private Button appButton;
    private Button macroButton;
    private Button orderButton;

    public TempadSettingsScreen(Screen parent, UUID deviceId, String app, String macro, String order, boolean hasTwister, boolean hasGuide) {
        super(Component.translatable("screen.tempad.settings.title"));
        this.parent = parent;
        this.deviceId = deviceId;
        this.app = app;
        this.macro = macro;
        this.order = order;
        this.hasTwister = hasTwister;
        this.hasGuide = hasGuide;
    }

    @Override protected void init() {
        int x = width / 2 - 95;
        int y = height / 2 - 75;
        appButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            app = switch (app) {
                case "locations" -> hasTwister ? "timeline" : hasGuide ? "guide" : "locations";
                case "timeline" -> hasGuide ? "guide" : "locations";
                default -> "locations";
            };
            refresh();
        }).bounds(x, y + 30, 190, 20).build());
        macroButton = addRenderableWidget(Button.builder(Component.empty(), button -> { macro = "favorite".equals(macro) ? "none" : "favorite"; refresh(); })
            .bounds(x, y + 55, 190, 20).build());
        orderButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            order = switch (order) { case "alphabetical" -> "dimension"; case "dimension" -> "type"; default -> "alphabetical"; };
            refresh();
        }).bounds(x, y + 80, 190, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
            NetworkHandler.CHANNEL.sendToServer(new SaveTempadSettingsPacket(deviceId, app, macro, order));
            if (minecraft != null) minecraft.setScreen(null);
        }).bounds(width / 2 - 40, y + 112, 80, 20).build());
        refresh();
    }

    private void refresh() {
        appButton.setMessage(Component.translatable("screen.tempad.settings.app", Component.translatable("screen.tempad.settings.app." + app)));
        macroButton.setMessage(Component.translatable("screen.tempad.settings.macro", Component.translatable("screen.tempad.settings.macro." + macro)));
        orderButton.setMessage(Component.translatable("screen.tempad.settings.order", Component.translatable("screen.tempad.settings.order." + order)));
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int x = width / 2 - 105;
        int y = height / 2 - 83;
        graphics.fill(x, y, x + 210, y + 147, 0xDD191C24);
        graphics.drawCenteredString(font, title, width / 2, y + 11, 0xFFEDBD69);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
