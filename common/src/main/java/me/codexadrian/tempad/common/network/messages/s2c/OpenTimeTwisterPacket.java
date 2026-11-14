package me.codexadrian.tempad.common.network.messages.s2c;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.data.TravelHistoryData;
import me.codexadrian.tempad.common.utils.ClientUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record OpenTimeTwisterPacket(List<TravelHistoryData.Entry> history, boolean embedded, int charge, int cost, UUID deviceId) implements Packet<OpenTimeTwisterPacket> {
    public static final ResourceLocation ID = new ResourceLocation(Tempad.MODID, "open_time_twister");
    public static final PacketHandler<OpenTimeTwisterPacket> HANDLER = new Handler();
    @Override public ResourceLocation getID() { return ID; }
    @Override public PacketHandler<OpenTimeTwisterPacket> getHandler() { return HANDLER; }

    private static final class Handler implements PacketHandler<OpenTimeTwisterPacket> {
        @Override public void encode(OpenTimeTwisterPacket message, FriendlyByteBuf buffer) {
            buffer.writeCollection(message.history, (buf, entry) -> {
                buf.writeLong(entry.time());
                buf.writeOptional(Optional.ofNullable(entry.marker()), FriendlyByteBuf::writeResourceLocation);
                buf.writeResourceKey(entry.dimension());
                buf.writeDouble(entry.position().x);
                buf.writeDouble(entry.position().y);
                buf.writeDouble(entry.position().z);
            });
            buffer.writeBoolean(message.embedded);
            buffer.writeVarInt(message.charge);
            buffer.writeVarInt(message.cost);
            buffer.writeOptional(Optional.ofNullable(message.deviceId), FriendlyByteBuf::writeUUID);
        }

        @Override public OpenTimeTwisterPacket decode(FriendlyByteBuf buffer) {
            List<TravelHistoryData.Entry> history = buffer.readList(buf -> new TravelHistoryData.Entry(
                buf.readLong(), buf.readOptional(FriendlyByteBuf::readResourceLocation).orElse(null),
                buf.readResourceKey(Registries.DIMENSION), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())));
            return new OpenTimeTwisterPacket(history, buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readOptional(FriendlyByteBuf::readUUID).orElse(null));
        }

        @Override public PacketContext handle(OpenTimeTwisterPacket message) { return (player, level) -> ClientUtils.openTimeTwisterScreen(message); }
    }
}
