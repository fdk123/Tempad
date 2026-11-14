package me.codexadrian.tempad.common.network.messages.s2c;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.utils.ClientUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record OpenTempadScreenPacket(List<LocationData> locationData,
                                     UUID favorite, UUID deviceId, boolean canSaveLocations, boolean hasKnowledgeRepository, boolean hasTimeTwister,
                                     String defaultApp, String defaultMacro, String locationOrder, boolean startGuide) implements Packet<OpenTempadScreenPacket> {
    public static Handler HANDLER = new Handler();
    public static final ResourceLocation ID = new ResourceLocation(Tempad.MODID, "open_screen");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public PacketHandler<OpenTempadScreenPacket> getHandler() {
        return HANDLER;
    }

    private static class Handler implements PacketHandler<OpenTempadScreenPacket> {

        @Override
        public void encode(OpenTempadScreenPacket message, FriendlyByteBuf buffer) {
            buffer.writeCollection(message.locationData, (buf, locationData) -> {
                buf.writeUUID(locationData.getId());
                buf.writeUtf(locationData.getName());
                buf.writeBlockPos(locationData.getBlockPos());
                buf.writeResourceKey(locationData.getLevelKey());
                buf.writeBoolean(locationData.isTeleportable());
                buf.writeBoolean(locationData.isDeletable());
                buf.writeBoolean(locationData.isDownloadable());
                buf.writeBoolean(locationData.hasPreciseArrival());
                if (locationData.hasPreciseArrival()) {
                    var arrival = locationData.getArrivalPosition();
                    buf.writeDouble(arrival.x);
                    buf.writeDouble(arrival.y);
                    buf.writeDouble(arrival.z);
                    buf.writeFloat(locationData.getArrivalYaw());
                }
                buf.writeInt(locationData.getPortalColor());
            });
            buffer.writeOptional(Optional.ofNullable(message.favorite), FriendlyByteBuf::writeUUID);
            buffer.writeUUID(message.deviceId);
            buffer.writeBoolean(message.canSaveLocations);
            buffer.writeBoolean(message.hasKnowledgeRepository);
            buffer.writeBoolean(message.hasTimeTwister);
            buffer.writeUtf(message.defaultApp, 32);
            buffer.writeUtf(message.defaultMacro, 32);
            buffer.writeUtf(message.locationOrder, 32);
            buffer.writeBoolean(message.startGuide);
        }

        @Override
        public OpenTempadScreenPacket decode(FriendlyByteBuf buffer) {
            return new OpenTempadScreenPacket(buffer.readList((buf) -> {
                UUID id = buf.readUUID();
                String name = buf.readUtf();
                BlockPos pos = buf.readBlockPos();
                ResourceKey<Level> levelResourceKey = buf.readResourceKey(Registries.DIMENSION);
                boolean teleportable = buf.readBoolean();
                boolean deletable = buf.readBoolean();
                boolean downloadable = buf.readBoolean();
                net.minecraft.world.phys.Vec3 arrival = buf.readBoolean() ? new net.minecraft.world.phys.Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
                float yaw = arrival == null ? 0.0F : buf.readFloat();
                int color = buf.readInt();
                return new LocationData(name, levelResourceKey, pos, id, teleportable, deletable, downloadable, arrival, yaw, color);
            }), buffer.readOptional(FriendlyByteBuf::readUUID).orElse(null), buffer.readUUID(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readUtf(32), buffer.readUtf(32), buffer.readUtf(32), buffer.readBoolean());
        }

        @Override
        public PacketContext handle(OpenTempadScreenPacket message) {
            return (player, level) -> ClientUtils.openScreen(message);
        }
    }
}
