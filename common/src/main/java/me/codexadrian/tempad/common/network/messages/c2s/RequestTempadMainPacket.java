package me.codexadrian.tempad.common.network.messages.c2s;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.items.TempadSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public record RequestTempadMainPacket(UUID deviceId) implements Packet<RequestTempadMainPacket> {
    public static final ResourceLocation ID = new ResourceLocation(Tempad.MODID, "request_tempad_main");
    public static final PacketHandler<RequestTempadMainPacket> HANDLER = new Handler();
    @Override public ResourceLocation getID() { return ID; }
    @Override public PacketHandler<RequestTempadMainPacket> getHandler() { return HANDLER; }

    private static final class Handler implements PacketHandler<RequestTempadMainPacket> {
        @Override public void encode(RequestTempadMainPacket packet, FriendlyByteBuf buffer) { buffer.writeUUID(packet.deviceId); }
        @Override public RequestTempadMainPacket decode(FriendlyByteBuf buffer) { return new RequestTempadMainPacket(buffer.readUUID()); }
        @Override public PacketContext handle(RequestTempadMainPacket packet) {
            return (player, level) -> {
                if (!(player instanceof ServerPlayer serverPlayer)) return;
                var stack = TempadSettings.find(player, packet.deviceId);
                if (!stack.isEmpty()) TempadSettings.openMain(serverPlayer, stack);
            };
        }
    }
}
