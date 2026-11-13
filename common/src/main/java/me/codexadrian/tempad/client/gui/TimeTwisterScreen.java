package me.codexadrian.tempad.client.gui;

import me.codexadrian.tempad.common.data.TravelHistoryData;
import me.codexadrian.tempad.common.network.NetworkHandler;
import me.codexadrian.tempad.common.network.messages.c2s.BacktrackPacket;
import me.codexadrian.tempad.common.network.messages.s2c.OpenTimeTwisterPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Text-only timeline for the 1.20.1 backport. The server validates every selection. */
public final class TimeTwisterScreen extends Screen {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private final OpenTimeTwisterPacket data;

    public TimeTwisterScreen(OpenTimeTwisterPacket data) {
        super(Component.translatable("screen.tempad.time_twister.title"));
        this.data = data;
    }

    @Override protected void init() {
        int x = width / 2 - 150;
        int y = (height - 230) / 2;
        boolean enough = data.charge() >= data.cost() || (minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild);
        int count = Math.min(8, data.history().size());
        for (int i = 0; i < count; i++) {
            TravelHistoryData.Entry entry = data.history().get(i);
            Component marker = entry.marker() == null ? Component.empty() : Component.translatable("history.tempad." + entry.marker().getPath());
            Component label = Component.literal(DATE.format(Instant.ofEpochMilli(entry.time())) + "  ").append(marker);
            Button button = addRenderableWidget(Button.builder(label, ignored -> {
                NetworkHandler.CHANNEL.sendToServer(new BacktrackPacket(entry.time(), data.embedded()));
                onClose();
            }).bounds(x, y + 39 + i * 21, 300, 20).build());
            button.active = enough;
            var pos = entry.position();
            button.setTooltip(Tooltip.create(Component.literal(entry.dimension().location() + "  "
                + String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z))));
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> onClose())
            .bounds(width / 2 - 40, y + 209, 80, 20).build());
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int x = width / 2 - 157;
        int y = (height - 230) / 2;
        graphics.fill(x, y, x + 314, y + 232, 0xDD191C24);
        graphics.drawCenteredString(font, title, width / 2, y + 7, 0xFFEDBD69);
        graphics.drawCenteredString(font, Component.translatable("screen.tempad.time_twister.charge", data.charge(), data.cost()), width / 2, y + 22, 0xFFE0E0E0);
        if (data.history().isEmpty()) graphics.drawCenteredString(font, Component.translatable("screen.tempad.time_twister.empty"), width / 2, y + 93, 0xFFAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
