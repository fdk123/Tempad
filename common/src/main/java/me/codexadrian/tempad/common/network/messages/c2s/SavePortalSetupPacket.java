package me.codexadrian.tempad.common.network.messages.c2s;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.data.PortalSetupData;
import me.codexadrian.tempad.common.data.TempadLocationHandler;
import me.codexadrian.tempad.common.items.TempadSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

/** Saves portal settings only to the device that opened the screen. */
public record SavePortalSetupPacket(UUID deviceId, PortalSetupData settings, UUID location) implements Packet<SavePortalSetupPacket> {
    public static final ResourceLocation ID = new ResourceLocation(Tempad.MODID, "save_portal_setup");
    public static final PacketHandler<SavePortalSetupPacket> HANDLER = new Handler();
    @Override public ResourceLocation getID() { return ID; }
    @Override public PacketHandler<SavePortalSetupPacket> getHandler() { return HANDLER; }

    private static final class Handler implements PacketHandler<SavePortalSetupPacket> {
        @Override public void encode(SavePortalSetupPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.deviceId);
            buffer.writeFloat(packet.settings.leftRight());
            buffer.writeFloat(packet.settings.upDown());
            buffer.writeFloat(packet.settings.forwardBack());
            buffer.writeVarInt(packet.settings.angle());
            buffer.writeBoolean(packet.settings.upright());
            buffer.writeOptional(Optional.ofNullable(packet.location), FriendlyByteBuf::writeUUID);
        }

        @Override public SavePortalSetupPacket decode(FriendlyByteBuf buffer) {
            UUID deviceId = buffer.readUUID();
            PortalSetupData settings = new PortalSetupData(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt(), buffer.readBoolean());
            return new SavePortalSetupPacket(deviceId, settings, buffer.readOptional(FriendlyByteBuf::readUUID).orElse(null));
        }

        @Override public PacketContext handle(SavePortalSetupPacket packet) {
            return (player, level) -> {
                var stack = TempadSettings.find(player, packet.deviceId);
                if (stack.isEmpty()) return;
                if (packet.location != null) {
                    LocationData target = TempadLocationHandler.getLocation(level, player.getUUID(), packet.location);
                    if (target == null || target.getLevelKey() == null || !target.isTeleportable()) return;
                }
                PortalSetupData.save(stack, packet.settings, packet.location);
            };
        }
    }
}
