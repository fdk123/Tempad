package me.codexadrian.tempad.client.gui;

import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.data.PortalSetupData;
import me.codexadrian.tempad.common.network.NetworkHandler;
import me.codexadrian.tempad.common.network.messages.c2s.SavePortalSetupPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Configures the portal opened by a Workstation carrying this Tempad. */
public final class PortalSetupScreen extends Screen {
    private final Screen parent;
    private final UUID deviceId;
    private final PortalSetupData initial;
    private final List<LocationData> locations;
    private UUID selected;
    private EditBox leftRight;
    private EditBox upDown;
    private EditBox forwardBack;
    private EditBox angle;
    private Button orientation;
    private Button destination;
    private boolean upright;

    public PortalSetupScreen(Screen parent, UUID deviceId, PortalSetupData initial, UUID selected, List<LocationData> locations) {
        super(Component.translatable("screen.tempad.portal_setup.title"));
        this.parent = parent;
        this.deviceId = deviceId;
        this.initial = initial;
        this.locations = new ArrayList<>(locations.stream().filter(LocationData::isTeleportable).toList());
        this.selected = selected != null && this.locations.stream().anyMatch(location -> location.getId().equals(selected)) ? selected : null;
        this.upright = initial.upright();
    }

    @Override protected void init() {
        int x = width / 2 - 130;
        int y = height / 2 - 110;
        leftRight = input(x + 165, y + 28, initial.leftRight());
        upDown = input(x + 165, y + 52, initial.upDown());
        forwardBack = input(x + 165, y + 76, initial.forwardBack());
        angle = input(x + 165, y + 100, initial.angle());
        orientation = addRenderableWidget(Button.builder(Component.empty(), button -> { upright = !upright; refresh(); })
            .bounds(x + 18, y + 128, 224, 20).build());
        destination = addRenderableWidget(Button.builder(Component.empty(), button -> { cycleTarget(); refresh(); })
            .bounds(x + 18, y + 154, 224, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
            .bounds(x + 43, y + 187, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
            .bounds(x + 137, y + 187, 80, 20).build());
        refresh();
    }

    private EditBox input(int x, int y, Number value) {
        EditBox box = new EditBox(font, x, y, 74, 18, Component.empty());
        box.setValue(value.toString());
        addRenderableWidget(box);
        return box;
    }

    private void cycleTarget() {
        List<UUID> options = new ArrayList<>();
        options.add(null);
        locations.forEach(location -> options.add(location.getId()));
        int current = options.indexOf(selected);
        selected = options.get((current + 1) % options.size());
    }

    private void refresh() {
        orientation.setMessage(Component.translatable("screen.tempad.portal_setup.orientation",
            Component.translatable(upright ? "screen.tempad.portal_setup.vertical" : "screen.tempad.portal_setup.floor")));
        String name = locations.stream().filter(location -> location.getId().equals(selected)).findFirst().map(LocationData::getName).orElse(null);
        destination.setMessage(Component.translatable("screen.tempad.portal_setup.destination",
            name == null ? Component.translatable("screen.tempad.portal_setup.favorite") : Component.literal(name)));
    }

    private void save() {
        try {
            float x = Float.parseFloat(leftRight.getValue());
            float y = Float.parseFloat(upDown.getValue());
            float z = Float.parseFloat(forwardBack.getValue());
            int rotation = Integer.parseInt(angle.getValue());
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) throw new NumberFormatException();
            NetworkHandler.CHANNEL.sendToServer(new SavePortalSetupPacket(deviceId, new PortalSetupData(x, y, z, rotation, upright), selected));
            if (minecraft != null) minecraft.setScreen(null);
        } catch (NumberFormatException invalid) {
            if (minecraft != null && minecraft.player != null)
                minecraft.player.displayClientMessage(Component.translatable("screen.tempad.portal_setup.invalid"), true);
        }
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int x = width / 2 - 130;
        int y = height / 2 - 110;
        graphics.fill(x, y, x + 260, y + 216, 0xDD191C24);
        graphics.drawCenteredString(font, title, width / 2, y + 8, 0xFFEDBD69);
        graphics.drawString(font, Component.translatable("screen.tempad.portal_setup.left_right"), x + 18, y + 33, 0xFFE0E0E0);
        graphics.drawString(font, Component.translatable("screen.tempad.portal_setup.up_down"), x + 18, y + 57, 0xFFE0E0E0);
        graphics.drawString(font, Component.translatable("screen.tempad.portal_setup.forward_back"), x + 18, y + 81, 0xFFE0E0E0);
        graphics.drawString(font, Component.translatable("screen.tempad.portal_setup.angle"), x + 18, y + 105, 0xFFE0E0E0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
