package me.codexadrian.tempad.common.network.messages.s2c;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.utils.ClientUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record OpenMarkerScreenPacket(BlockPos pos, boolean chronomark, String name, int color,
                                     boolean publicAccess, String accessMode, boolean locked, float yOffset, int angle) implements Packet<OpenMarkerScreenPacket> {
    public static final ResourceLocation ID = new ResourceLocation(Tempad.MODID, "open_marker");
    public static final PacketHandler<OpenMarkerScreenPacket> HANDLER = new Handler();
    @Override public ResourceLocation getID() { return ID; }
    @Override public PacketHandler<OpenMarkerScreenPacket> getHandler() { return HANDLER; }

    private static final class Handler implements PacketHandler<OpenMarkerScreenPacket> {
        @Override public void encode(OpenMarkerScreenPacket message, FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.pos);
            buffer.writeBoolean(message.chronomark);
            buffer.writeUtf(message.name, 64);
            buffer.writeInt(message.color);
            buffer.writeBoolean(message.publicAccess);
            buffer.writeUtf(message.accessMode, 24);
            buffer.writeBoolean(message.locked);
            buffer.writeFloat(message.yOffset);
            buffer.writeInt(message.angle);
        }

        @Override public OpenMarkerScreenPacket decode(FriendlyByteBuf buffer) {
            return new OpenMarkerScreenPacket(buffer.readBlockPos(), buffer.readBoolean(), buffer.readUtf(64),
                buffer.readInt(), buffer.readBoolean(), buffer.readUtf(24), buffer.readBoolean(), buffer.readFloat(), buffer.readInt());
        }

        @Override public PacketContext handle(OpenMarkerScreenPacket message) { return (player, level) -> ClientUtils.openMarkerScreen(message); }
    }
}
