package me.codexadrian.tempad.common.network.messages.c2s;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.data.TempadLocationHandler;
import me.codexadrian.tempad.common.data.PlayerLocationHandler;
import me.codexadrian.tempad.common.items.TempadItem;
import me.codexadrian.tempad.common.items.TempadSettings;
import me.codexadrian.tempad.common.items.TempadUpgrades;
import me.codexadrian.tempad.common.utils.TeleportUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public record SummonTimedoorPacket(UUID location, int color, UUID deviceId) implements Packet<SummonTimedoorPacket> {
    public static Handler HANDLER = new Handler();
    public static final ResourceLocation ID = new ResourceLocation(Tempad.MODID, "timedoor");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public PacketHandler<SummonTimedoorPacket> getHandler() {
        return HANDLER;
    }

    private static class Handler implements PacketHandler<SummonTimedoorPacket> {

        @Override
        public void encode(SummonTimedoorPacket message, FriendlyByteBuf buffer) {
            buffer.writeUUID(message.location);
            buffer.writeVarInt(message.color);
            buffer.writeUUID(message.deviceId);
        }

        @Override
        public SummonTimedoorPacket decode(FriendlyByteBuf buffer) {
            return new SummonTimedoorPacket(buffer.readUUID(), buffer.readVarInt(), buffer.readUUID());
        }

        @Override
        public PacketContext handle(SummonTimedoorPacket message) {
            return (player, level) -> {
                ItemStack itemInHand = TempadSettings.find(player, message.deviceId);
                LocationData locationData = TempadLocationHandler.getLocation(level, player.getUUID(), message.location);
                if (PlayerLocationHandler.isPlayerLocation(level, message.location)
                        && !TempadUpgrades.has(itemInHand, TempadUpgrades.PLAYER_TELEPORT)) return;
                if (locationData != null && itemInHand.getItem() instanceof TempadItem tempadItem && tempadItem.getOption().canTimedoorOpen(player, itemInHand) && TeleportUtils.mayTeleport(locationData.getLevelKey(), player)) {
                    if (TempadItem.summonTimeDoor(locationData, player, message.color) != null && !player.getAbilities().instabuild)
                        tempadItem.getOption().onTimedoorOpen(player);
                }
            };
        }
    }
}
