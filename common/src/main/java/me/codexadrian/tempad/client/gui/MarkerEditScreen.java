package me.codexadrian.tempad.client.gui;

import me.codexadrian.tempad.common.network.NetworkHandler;
import me.codexadrian.tempad.common.network.messages.c2s.UpdateMarkerPacket;
import me.codexadrian.tempad.common.network.messages.s2c.OpenMarkerScreenPacket;
import me.codexadrian.tempad.common.utils.PlatformUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/** Native 1.20.1 settings screen for both marker blocks. */
public final class MarkerEditScreen extends Screen {
    private final OpenMarkerScreenPacket marker;
    private EditBox name;
    private EditBox color;
    private EditBox offset;
    private EditBox angle;
    private boolean publicAccess;
    private String accessMode;
    private boolean locked;
    private Button accessButton;
    private Button lockButton;

    public MarkerEditScreen(OpenMarkerScreenPacket marker) {
        super(Component.translatable(marker.chronomark() ? "block.tempad.chronomark" : "block.tempad.timedoor_marker"));
        this.marker = marker;
        this.accessMode = marker.accessMode();
        this.publicAccess = marker.chronomark() ? "public".equals(accessMode) : marker.publicAccess();
        this.locked = marker.locked();
    }

    @Override protected void init() {
        int x = width / 2 - 130;
        int y = height / 2 - 105;
        name = field(x + 110, y + 28, 130, marker.name(), 48);
        color = field(x + 110, y + 53, 130, String.format("#%06X", marker.color() & 0xFFFFFF), 7);
        accessButton = addRenderableWidget(Button.builder(accessLabel(), button -> {
            if (marker.chronomark()) {
                accessMode = switch (accessMode) {
                    case "private" -> "public";
                    case "public" -> PlatformUtils.isModLoaded("ftbteams") ? "ftbteams" : "private";
                    default -> "private";
                };
                publicAccess = "public".equals(accessMode);
            } else {
                publicAccess = !publicAccess;
                accessMode = publicAccess ? "public" : "private";
            }
            accessButton.setMessage(accessLabel());
        }).bounds(x + 110, y + 78, 130, 20).build());
        lockButton = addRenderableWidget(Button.builder(lockLabel(), button -> {
            locked = !locked;
            lockButton.setMessage(lockLabel());
        }).bounds(x + 110, y + 103, 130, 20).build());
        if (marker.chronomark()) {
            offset = field(x + 110, y + 128, 130, String.format(Locale.ROOT, "%.2f", marker.yOffset()), 8);
            angle = field(x + 110, y + 153, 130, Integer.toString(marker.angle()), 5);
        }
        int buttonsY = y + (marker.chronomark() ? 181 : 138);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save()).bounds(x + 38, buttonsY, 84, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose()).bounds(x + 138, buttonsY, 84, 20).build());
    }

    private EditBox field(int x, int y, int width, String value, int maxLength) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.empty());
        box.setMaxLength(maxLength);
        box.setValue(value);
        return addRenderableWidget(box);
    }

    private Component accessLabel() { return Component.translatable("screen.tempad.marker." + (marker.chronomark() ? accessMode : publicAccess ? "public" : "private")); }
    private Component lockLabel() { return Component.translatable(locked ? "screen.tempad.marker.locked" : "screen.tempad.marker.unlocked"); }

    private void save() {
        int rgb = marker.color();
        try { rgb = Integer.parseInt(color.getValue().replace("#", ""), 16) & 0xFFFFFF; } catch (NumberFormatException ignored) {}
        float yOffset = marker.yOffset();
        int yaw = marker.angle();
        if (marker.chronomark()) {
            try { yOffset = Float.parseFloat(offset.getValue()); } catch (NumberFormatException ignored) {}
            try { yaw = Integer.parseInt(angle.getValue()); } catch (NumberFormatException ignored) {}
        }
        NetworkHandler.CHANNEL.sendToServer(new UpdateMarkerPacket(marker.pos(), name.getValue(), rgb, publicAccess, accessMode, locked, yOffset, yaw));
        onClose();
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int x = width / 2 - 130;
        int y = height / 2 - 105;
        graphics.fill(x, y, x + 260, y + (marker.chronomark() ? 210 : 167), 0xDD191C24);
        graphics.drawCenteredString(font, title, width / 2, y + 9, 0xFFEDBD69);
        graphics.drawString(font, Component.translatable("screen.tempad.marker.name"), x + 12, y + 34, 0xFFFFFFFF);
        graphics.drawString(font, Component.translatable("screen.tempad.marker.color"), x + 12, y + 59, 0xFFFFFFFF);
        graphics.drawString(font, Component.translatable("screen.tempad.marker.access"), x + 12, y + 84, 0xFFFFFFFF);
        graphics.drawString(font, Component.translatable("screen.tempad.marker.lock"), x + 12, y + 109, 0xFFFFFFFF);
        if (marker.chronomark()) {
            graphics.drawString(font, Component.translatable("screen.tempad.marker.offset"), x + 12, y + 134, 0xFFFFFFFF);
            graphics.drawString(font, Component.translatable("screen.tempad.marker.angle"), x + 12, y + 159, 0xFFFFFFFF);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
