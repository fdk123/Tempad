package me.codexadrian.tempad.client.forge;

import me.codexadrian.tempad.common.forge.MetronomeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/** Vanilla-rendered 3.0 Metronome controls; no upstream artwork is used. */
public final class MetronomeScreen extends AbstractContainerScreen<MetronomeMenu> {
    private Button lockButton;

    public MetronomeScreen(MetronomeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 211;
        imageHeight = 178;
    }

    @Override protected void init() {
        super.init();
        lockButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (minecraft != null && minecraft.gameMode != null && menu.canEditLock())
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 150, topPos + 8, 52, 20).build());
        updateLockButton();
    }

    @Override protected void containerTick() {
        super.containerTick();
        updateLockButton();
    }

    private void updateLockButton() {
        lockButton.active = menu.canEditLock();
        lockButton.setMessage(Component.translatable(menu.locked()
            ? "screen.tempad.metronome.locked" : "screen.tempad.metronome.unlocked"));
    }

    @Override protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xEE191C24);
        graphics.fill(x, y, x + imageWidth, y + 2, 0xFFAD7A3C);
        graphics.drawString(font, title, x + 25, y + 14, 0xFFEDBD69);
        graphics.drawString(font, Component.translatable("screen.tempad.metronome.input"), x + 25, y + 29, 0xFFC8C8C8);
        graphics.drawString(font, Component.translatable("screen.tempad.metronome.output"), x + 115, y + 29, 0xFFC8C8C8);
        for (int slot = 0; slot < 8; slot++) {
            int slotX = x + 25 + (slot < 4 ? slot : slot + 1) * 18;
            graphics.fill(slotX - 1, y + 39, slotX + 17, y + 57, 0xFF8D785B);
            graphics.fill(slotX, y + 40, slotX + 16, y + 56, 0xFF343943);
        }
        graphics.fill(x + 101, y + 39, x + 109, y + 57, 0xFF40392E);
        int stored = Math.max(0, menu.stored());
        int capacity = Math.max(1, menu.capacity());
        int width = Math.min(161, (int) (161L * Math.min(stored, capacity) / capacity));
        graphics.fill(x + 25, y + 62, x + 186, y + 68, 0xFF373C46);
        graphics.fill(x + 25, y + 62, x + 25 + width, y + 68, 0xFFED9F40);
        Component status = menu.bootTime() <= 0
            ? Component.translatable("screen.tempad.metronome.charge", stored, capacity)
            : menu.bootTime() >= 100 && stored < capacity
                ? Component.translatable("screen.tempad.metronome.startup", stored, capacity)
                : Component.translatable("screen.tempad.metronome.booting", 100 - menu.bootTime());
        graphics.drawString(font, status, x + 25, y + 71, 0xFFDEDEDE);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) drawSlot(graphics, x + 25 + column * 18, y + 85 + row * 18);
        }
        for (int column = 0; column < 9; column++) drawSlot(graphics, x + 25 + column * 18, y + 143);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF5D5D63);
        graphics.fill(x, y, x + 16, y + 16, 0xFF343943);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) { }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
