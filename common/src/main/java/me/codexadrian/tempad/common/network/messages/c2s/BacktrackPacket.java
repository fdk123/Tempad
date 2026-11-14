package me.codexadrian.tempad.common.network.messages.c2s;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.data.TravelHistoryData;
import me.codexadrian.tempad.common.items.TimeTwisterData;
import me.codexadrian.tempad.common.items.TempadSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;
import java.util.UUID;

public record BacktrackPacket(long time, boolean embedded, UUID deviceId) implements Packet<BacktrackPacket> {
    public static final ResourceLocation ID = new ResourceLocation(Tempad.MODID, "backtrack");
    public static final PacketHandler<BacktrackPacket> HANDLER = new Handler();
    @Override public ResourceLocation getID() { return ID; }
    @Override public PacketHandler<BacktrackPacket> getHandler() { return HANDLER; }

    private static final class Handler implements PacketHandler<BacktrackPacket> {
        @Override public void encode(BacktrackPacket message, FriendlyByteBuf buffer) { buffer.writeLong(message.time); buffer.writeBoolean(message.embedded); buffer.writeOptional(Optional.ofNullable(message.deviceId), FriendlyByteBuf::writeUUID); }
        @Override public BacktrackPacket decode(FriendlyByteBuf buffer) { return new BacktrackPacket(buffer.readLong(), buffer.readBoolean(), buffer.readOptional(FriendlyByteBuf::readUUID).orElse(null)); }
        @Override public PacketContext handle(BacktrackPacket message) {
            return (player, level) -> {
                if (!(player instanceof ServerPlayer serverPlayer)) return;
                int cost = Math.max(0, TempadConfig.timeTwisterBacktrackCost);
                var source = message.embedded && message.deviceId != null ? TempadSettings.find(player, message.deviceId)
                    : TimeTwisterData.findSource(player, message.embedded, player.getAbilities().instabuild ? 0 : cost);
                if (source.isEmpty() || player.getCooldowns().isOnCooldown(source.getItem())
                        || (message.embedded && (!TimeTwisterData.equipped(source) || message.deviceId == null))
                        || (!player.getAbilities().instabuild && TimeTwisterData.available(source, message.embedded) < cost)) return;
                TravelHistoryData history = TravelHistoryData.get(serverPlayer);
                if (history.find(player.getUUID(), message.time) == null || !history.backtrack(serverPlayer, message.time)) return;
                if (!player.getAbilities().instabuild) TimeTwisterData.consume(source, message.embedded, cost);
                player.getCooldowns().addCooldown(source.getItem(), 40);
            };
        }
    }
}
