package me.codexadrian.tempad.common.network.messages.c2s;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.items.TempadItem;
import me.codexadrian.tempad.common.items.TempadSettings;
import me.codexadrian.tempad.common.utils.TeleportUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record UseTempadMacroPacket(int color) implements Packet<UseTempadMacroPacket> {
    public static final ResourceLocation ID = new ResourceLocation(Tempad.MODID, "use_tempad_macro");
    public static final PacketHandler<UseTempadMacroPacket> HANDLER = new Handler();
    @Override public ResourceLocation getID() { return ID; }
    @Override public PacketHandler<UseTempadMacroPacket> getHandler() { return HANDLER; }

    private static final class Handler implements PacketHandler<UseTempadMacroPacket> {
        @Override public void encode(UseTempadMacroPacket packet, FriendlyByteBuf buffer) { buffer.writeVarInt(packet.color); }
        @Override public UseTempadMacroPacket decode(FriendlyByteBuf buffer) { return new UseTempadMacroPacket(buffer.readVarInt()); }
        @Override public PacketContext handle(UseTempadMacroPacket packet) {
            return (player, level) -> {
                var stack = TeleportUtils.findTempad(player);
                if (stack.getItem() instanceof TempadItem && "favorite".equals(TempadSettings.macro(stack)))
                    OpenFavoritedLocationPacket.openFavorite(player, level, packet.color);
            };
        }
    }
}
