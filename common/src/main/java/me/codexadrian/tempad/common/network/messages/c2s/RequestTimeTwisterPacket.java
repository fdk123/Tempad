package me.codexadrian.tempad.common.network.messages.c2s;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.data.TravelHistoryData;
import me.codexadrian.tempad.common.items.TimeTwisterData;
import me.codexadrian.tempad.common.network.NetworkHandler;
import me.codexadrian.tempad.common.network.messages.s2c.OpenTimeTwisterPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record RequestTimeTwisterPacket() implements Packet<RequestTimeTwisterPacket> {
    public static final ResourceLocation ID = new ResourceLocation(Tempad.MODID, "request_time_twister");
    public static final PacketHandler<RequestTimeTwisterPacket> HANDLER = new Handler();
    @Override public ResourceLocation getID() { return ID; }
    @Override public PacketHandler<RequestTimeTwisterPacket> getHandler() { return HANDLER; }

    private static final class Handler implements PacketHandler<RequestTimeTwisterPacket> {
        @Override public void encode(RequestTimeTwisterPacket message, FriendlyByteBuf buffer) {}
        @Override public RequestTimeTwisterPacket decode(FriendlyByteBuf buffer) { return new RequestTimeTwisterPacket(); }
        @Override public PacketContext handle(RequestTimeTwisterPacket message) {
            return (player, level) -> {
                if (!(player instanceof ServerPlayer serverPlayer)) return;
                int cost = Math.max(0, TempadConfig.timeTwisterBacktrackCost);
                var source = TimeTwisterData.findSource(player, true, cost);
                if (source.isEmpty()) source = TimeTwisterData.findSource(player, true);
                if (source.isEmpty()) return;
                NetworkHandler.CHANNEL.sendToPlayer(new OpenTimeTwisterPacket(
                    TravelHistoryData.get(serverPlayer).relevant(player.getUUID()), true,
                    TimeTwisterData.reserve(source), cost), player);
            };
        }
    }
}
