package me.codexadrian.tempad.client.gui;

import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.data.PortalSetupData;
import me.codexadrian.tempad.common.network.NetworkHandler;
import me.codexadrian.tempad.common.network.messages.c2s.SavePortalSetupPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Configures the portal opened by a Workstation carrying this Tempad. */
public final class PortalSetupScreen extends Screen {
    private final Screen parent;
    private final UUID deviceId;
    private final PortalSetupData initial;
    private final List<LocationData> locations;
    private final BlockPos workstationPos;
    private final List<Button> targetButtons = new ArrayList<>();
    private UUID selected;
    private int firstTarget;
    private EditBox search;
    private EditBox leftRight;
    private EditBox upDown;
    private EditBox forwardBack;
    private EditBox angle;
    private Button orientation;
    private Button previous;
    private Button next;
    private boolean upright;

    public PortalSetupScreen(Screen parent, UUID deviceId, PortalSetupData initial, UUID selected, List<LocationData> locations) {
        this(parent, deviceId, initial, selected, locations, null);
    }

    public PortalSetupScreen(Screen parent, UUID deviceId, PortalSetupData initial, UUID selected,
                             List<LocationData> locations, BlockPos workstationPos) {
        super(Component.translatable(workstationPos == null ? "screen.tempad.portal_setup.title" : "screen.tempad.portal_setup.workstation"));
        this.parent = parent;
        this.deviceId = deviceId;
        this.initial = initial;
        this.workstationPos = workstationPos;
        this.locations = new ArrayList<>(locations.stream().filter(LocationData::isTeleportable).toList());
        this.locations.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName()));
        this.selected = selected != null && this.locations.stream().anyMatch(location -> location.getId().equals(selected)) ? selected : null;
        this.upright = initial.upright();
    }

    @Override protected void init() {
        targetButtons.clear();
        int x = width / 2 - 180;
        int y = height / 2 - 120;
        leftRight = input(x + 88, y + 39, initial.leftRight());
        upDown = input(x + 88, y + 65, initial.upDown());
        forwardBack = input(x + 88, y + 91, initial.forwardBack());
        angle = input(x + 88, y + 117, initial.angle());
        orientation = addRenderableWidget(Button.builder(Component.empty(), button -> { upright = !upright; refresh(); })
            .bounds(x + 12, y + 151, 155, 20).build());
        search = new EditBox(font, x + 194, y + 29, 151, 18, Component.translatable("screen.tempad.portal_setup.search"));
        search.setHint(Component.translatable("screen.tempad.portal_setup.search"));
        search.setResponder(value -> { firstTarget = 0; refreshTargets(); });
        addRenderableWidget(search);
        for (int row = 0; row < 6; row++) {
            final int index = row;
            targetButtons.add(addRenderableWidget(Button.builder(Component.empty(), button -> selectTarget(index))
                .bounds(x + 194, y + 52 + row * 22, 151, 20).build()));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("◀"), button -> { firstTarget = Math.max(0, firstTarget - 6); refreshTargets(); })
            .bounds(x + 194, y + 188, 35, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal("▶"), button -> { firstTarget += 6; refreshTargets(); })
            .bounds(x + 310, y + 188, 35, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
            .bounds(x + 68, y + 211, 104, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
            .bounds(x + 188, y + 211, 104, 20).build());
        refresh();
    }

    private EditBox input(int x, int y, Number value) {
        EditBox box = new EditBox(font, x, y, 74, 18, Component.empty());
        box.setValue(value.toString());
        addRenderableWidget(box);
        return box;
    }

    private List<LocationData> filteredTargets() {
        String query = search.getValue().toLowerCase(Locale.ROOT);
        return locations.stream().filter(location -> location.getName().toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    private void selectTarget(int row) {
        int index = firstTarget + row;
        List<LocationData> matches = filteredTargets();
        if (index == 0) selected = null;
        else if (index - 1 < matches.size()) selected = matches.get(index - 1).getId();
        refreshTargets();
    }

    private void refreshTargets() {
        if (search == null || targetButtons.isEmpty()) return;
        List<LocationData> matches = filteredTargets();
        int total = matches.size() + 1;
        if (firstTarget >= total) firstTarget = Math.max(0, ((total - 1) / 6) * 6);
        for (int row = 0; row < targetButtons.size(); row++) {
            int index = firstTarget + row;
            Button button = targetButtons.get(row);
            button.visible = index < total;
            if (!button.visible) continue;
            boolean favorite = index == 0;
            LocationData location = favorite ? null : matches.get(index - 1);
            boolean active = favorite ? selected == null : location.getId().equals(selected);
            Component name = favorite ? Component.translatable("screen.tempad.portal_setup.favorite") : Component.literal(location.getName());
            button.setMessage(Component.literal(active ? "● " : "○ ").append(name));
        }
        previous.active = firstTarget > 0;
        next.active = firstTarget + 6 < total;
    }

    private void refresh() {
        orientation.setMessage(Component.translatable("screen.tempad.portal_setup.orientation",
            Component.translatable(upright ? "screen.tempad.portal_setup.vertical" : "screen.tempad.portal_setup.floor")));
        refreshTargets();
    }

    private void save() {
        try {
            float x = Float.parseFloat(leftRight.getValue());
            float y = Float.parseFloat(upDown.getValue());
            float z = Float.parseFloat(forwardBack.getValue());
            int rotation = Integer.parseInt(angle.getValue());
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) throw new NumberFormatException();
            NetworkHandler.CHANNEL.sendToServer(new SavePortalSetupPacket(deviceId, new PortalSetupData(x, y, z, rotation, upright), selected, workstationPos));
            if (minecraft != null) minecraft.setScreen(null);
        } catch (NumberFormatException invalid) {
            if (minecraft != null && minecraft.player != null)
                minecraft.player.displayClientMessage(Component.translatable("screen.tempad.portal_setup.invalid"), true);
        }
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int x = width / 2 - 180;
        int y = height / 2 - 120;
        graphics.fill(x, y, x + 360, y + 240, 0xEE191C24);
        graphics.fill(x + 180, y + 23, x + 181, y + 210, 0xFFEDBD69);
        graphics.drawCenteredString(font, title, width / 2, y + 8, 0xFFEDBD69);
        graphics.drawString(font, Component.translatable("screen.tempad.portal_setup.left_right"), x + 12, y + 44, 0xFFEDBD69);
        graphics.drawString(font, Component.translatable("screen.tempad.portal_setup.up_down"), x + 12, y + 70, 0xFFEDBD69);
        graphics.drawString(font, Component.translatable("screen.tempad.portal_setup.forward_back"), x + 12, y + 96, 0xFFEDBD69);
        graphics.drawString(font, Component.translatable("screen.tempad.portal_setup.angle"), x + 12, y + 122, 0xFFEDBD69);
        graphics.drawString(font, Component.translatable("screen.tempad.portal_setup.destination", Component.empty()), x + 194, y + 15, 0xFFEDBD69);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
