package me.codexadrian.tempad.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Text-only guide for the Knowledge Repository upgrade; uses no protected 3.0 artwork. */
public final class KnowledgeRepositoryScreen extends Screen {
    private static final int PAGE_COUNT = 10;
    private final Screen parent;
    private int page;

    public KnowledgeRepositoryScreen(Screen parent) {
        super(Component.translatable("app.tempad.knowledge_repository"));
        this.parent = parent;
    }

    @Override protected void init() {
        int y = height / 2 + 70;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> { page = Math.max(0, page - 1); updateButtons(); }).bounds(width / 2 - 112, y, 24, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(width / 2 - 40, y, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> { page = Math.min(PAGE_COUNT - 1, page + 1); updateButtons(); }).bounds(width / 2 + 88, y, 24, 20).build());
        updateButtons();
    }

    private void updateButtons() {
        ((Button) children().get(0)).active = page > 0;
        ((Button) children().get(2)).active = page < PAGE_COUNT - 1;
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int x = width / 2 - 124;
        int y = height / 2 - 100;
        graphics.fill(x, y, x + 248, y + 200, 0xDD191C24);
        graphics.drawCenteredString(font, title, width / 2, y + 12, 0xFFEDBD69);
        graphics.drawCenteredString(font, Component.translatable("guide.tempad.page." + page + ".title"), width / 2, y + 32, 0xFFFFFFFF);
        List<FormattedCharSequence> lines = font.split(Component.translatable("guide.tempad.page." + page + ".body"), 212);
        for (int i = 0; i < lines.size(); i++) graphics.drawString(font, lines.get(i), x + 18, y + 53 + i * 11, 0xFFE0E0E0);
        graphics.drawCenteredString(font, (page + 1) + "/" + PAGE_COUNT, width / 2, y + 158, 0xFFAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
