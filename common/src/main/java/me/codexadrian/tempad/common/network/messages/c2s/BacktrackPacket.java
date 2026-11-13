package me.codexadrian.tempad.common.network.messages.c2s;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.data.TravelHistoryData;
import me.codexadrian.tempad.common.items.TimeTwisterData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record BacktrackPacket(long time, boolean embedded) implements Packet<BacktrackPacket> {
    public static final ResourceLocation ID = new ResourceLocation(Tempad.MODID, "backtrack");
    public static final PacketHandler<BacktrackPacket> HANDLER = new Handler();
    @Override public ResourceLocation getID() { return ID; }
    @Override public PacketHandler<BacktrackPacket> getHandler() { return HANDLER; }

    private static final class Handler implements PacketHandler<BacktrackPacket> {
        @Override public void encode(BacktrackPacket message, FriendlyByteBuf buffer) { buffer.writeLong(message.time); buffer.writeBoolean(message.embedded); }
        @Override public BacktrackPacket decode(FriendlyByteBuf buffer) { return new BacktrackPacket(buffer.readLong(), buffer.readBoolean()); }
        @Override public PacketContext handle(BacktrackPacket message) {
            return (player, level) -> {
                if (!(player instanceof ServerPlayer serverPlayer)) return;
                int cost = Math.max(0, TempadConfig.timeTwisterBacktrackCost);
                var source = TimeTwisterData.findSource(player, message.embedded, player.getAbilities().instabuild ? 0 : cost);
                if (source.isEmpty() || player.getCooldowns().isOnCooldown(source.getItem())
                        || (!player.getAbilities().instabuild && TimeTwisterData.available(source, message.embedded) < cost)) return;
                TravelHistoryData history = TravelHistoryData.get(serverPlayer);
                if (history.find(player.getUUID(), message.time) == null || !history.backtrack(serverPlayer, message.time)) return;
                if (!player.getAbilities().instabuild) TimeTwisterData.consume(source, message.embedded, cost);
                player.getCooldowns().addCooldown(source.getItem(), 40);
            };
        }
    }
}
