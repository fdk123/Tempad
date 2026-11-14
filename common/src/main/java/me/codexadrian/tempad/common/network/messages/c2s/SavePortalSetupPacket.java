package me.codexadrian.tempad.common.network.messages.c2s;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.data.PortalSetupData;
import me.codexadrian.tempad.common.data.TempadLocationHandler;
import me.codexadrian.tempad.common.data.PlayerLocationHandler;
import me.codexadrian.tempad.common.block.WorkstationBlockEntity;
import me.codexadrian.tempad.common.items.TempadSettings;
import me.codexadrian.tempad.common.items.TempadUpgrades;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

/** Saves portal settings only to the device that opened the screen. */
public record SavePortalSetupPacket(UUID deviceId, PortalSetupData settings, UUID location,
                                    BlockPos workstationPos) implements Packet<SavePortalSetupPacket> {
    public SavePortalSetupPacket(UUID deviceId, PortalSetupData settings, UUID location) {
        this(deviceId, settings, location, null);
    }
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
            buffer.writeOptional(Optional.ofNullable(packet.workstationPos), FriendlyByteBuf::writeBlockPos);
        }

        @Override public SavePortalSetupPacket decode(FriendlyByteBuf buffer) {
            UUID deviceId = buffer.readUUID();
            PortalSetupData settings = new PortalSetupData(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt(), buffer.readBoolean());
            return new SavePortalSetupPacket(deviceId, settings, buffer.readOptional(FriendlyByteBuf::readUUID).orElse(null),
                buffer.readOptional(FriendlyByteBuf::readBlockPos).orElse(null));
        }

        @Override public PacketContext handle(SavePortalSetupPacket packet) {
            return (player, level) -> {
                WorkstationBlockEntity workstation = null;
                ItemStack stack;
                if (packet.workstationPos != null) {
                    if (player.distanceToSqr(packet.workstationPos.getX() + 0.5,
                            packet.workstationPos.getY() + 0.5, packet.workstationPos.getZ() + 0.5) > 64
                            || !(level.getBlockEntity(packet.workstationPos) instanceof WorkstationBlockEntity found)
                            || found.isRemoved() || !found.canAccess(player)) return;
                    workstation = found;
                    stack = found.getTempad();
                    if (stack.isEmpty() || !TempadSettings.id(stack).equals(packet.deviceId)) return;
                } else stack = TempadSettings.find(player, packet.deviceId);
                if (stack.isEmpty()) return;
                if (packet.location != null) {
                    boolean playerTarget = PlayerLocationHandler.isPlayerLocation(level, packet.location);
                    if (playerTarget && !TempadUpgrades.has(stack, TempadUpgrades.PLAYER_TELEPORT)) return;
                    LocationData target = playerTarget
                        ? PlayerLocationHandler.resolveForDevice(level, player.getUUID(), packet.location)
                        : TempadLocationHandler.getLocation(level, player.getUUID(), packet.location);
                    if (target == null || target.getLevelKey() == null || !target.isTeleportable()) return;
                }
                PortalSetupData.save(stack, packet.settings, packet.location);
                if (workstation != null) workstation.notifyTempadChanged();
            };
        }
    }
}
