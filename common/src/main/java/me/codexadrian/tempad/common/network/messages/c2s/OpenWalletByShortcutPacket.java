package me.codexadrian.tempad.common.network.messages.c2s;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.items.CardWalletItem;
import me.codexadrian.tempad.common.network.NetworkHandler;
import me.codexadrian.tempad.common.utils.WalletOpener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record OpenWalletByShortcutPacket() implements Packet<OpenWalletByShortcutPacket> {
    public static final Handler HANDLER = new Handler();
    public static final ResourceLocation ID = new ResourceLocation(Tempad.MODID, "shortcut_wallet");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public PacketHandler<OpenWalletByShortcutPacket> getHandler() {
        return HANDLER;
    }

    private static class Handler implements PacketHandler<OpenWalletByShortcutPacket> {
        @Override
        public void encode(OpenWalletByShortcutPacket message, FriendlyByteBuf buffer) {
        }

        @Override
        public OpenWalletByShortcutPacket decode(FriendlyByteBuf buffer) {
            return new OpenWalletByShortcutPacket();
        }

        @Override
        public PacketContext handle(OpenWalletByShortcutPacket message) {
            return (player, level) -> {
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    ItemStack stack = player.getInventory().getItem(slot);
                    if (stack.getItem() instanceof CardWalletItem) {
                        WalletOpener.open(player, stack);
                        return;
                    }
                }
            };
        }
    }
}
