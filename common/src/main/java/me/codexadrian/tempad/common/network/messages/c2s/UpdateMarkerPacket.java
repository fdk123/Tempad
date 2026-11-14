package me.codexadrian.tempad.common.network.messages.c2s;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.block.TimedoorMarkerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record UpdateMarkerPacket(BlockPos pos, String name, int color, boolean publicAccess, String accessMode,
                                 boolean locked, float yOffset, int angle) implements Packet<UpdateMarkerPacket> {
    public static final ResourceLocation ID = new ResourceLocation(Tempad.MODID, "update_marker");
    public static final PacketHandler<UpdateMarkerPacket> HANDLER = new Handler();
    @Override public ResourceLocation getID() { return ID; }
    @Override public PacketHandler<UpdateMarkerPacket> getHandler() { return HANDLER; }

    private static final class Handler implements PacketHandler<UpdateMarkerPacket> {
        @Override public void encode(UpdateMarkerPacket message, FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.pos);
            buffer.writeUtf(message.name, 48);
            buffer.writeInt(message.color);
            buffer.writeBoolean(message.publicAccess);
            buffer.writeUtf(message.accessMode, 24);
            buffer.writeBoolean(message.locked);
            buffer.writeFloat(message.yOffset);
            buffer.writeInt(message.angle);
        }

        @Override public UpdateMarkerPacket decode(FriendlyByteBuf buffer) {
            return new UpdateMarkerPacket(buffer.readBlockPos(), buffer.readUtf(48), buffer.readInt(), buffer.readBoolean(), buffer.readUtf(24),
                buffer.readBoolean(), buffer.readFloat(), buffer.readInt());
        }

        @Override public PacketContext handle(UpdateMarkerPacket message) {
            return (player, level) -> {
                if (player.distanceToSqr(message.pos.getX() + 0.5, message.pos.getY() + 0.5, message.pos.getZ() + 0.5) > 64
                        || !player.mayInteract(level, message.pos) || !Float.isFinite(message.yOffset)) return;
                if (level.getBlockEntity(message.pos) instanceof TimedoorMarkerBlockEntity marker && marker.canEdit(player))
                    marker.update(player, message.name, message.color, message.publicAccess, message.accessMode, message.locked, message.yOffset, message.angle);
            };
        }
    }
}
