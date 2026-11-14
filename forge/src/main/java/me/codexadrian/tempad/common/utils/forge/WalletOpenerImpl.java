package me.codexadrian.tempad.common.utils.forge;

import me.codexadrian.tempad.common.items.WalletContainer;
import me.codexadrian.tempad.common.items.CardWalletItem;
import me.codexadrian.tempad.common.forge.WalletMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.server.level.ServerPlayer;

public final class WalletOpenerImpl {
    private WalletOpenerImpl() {
    }

    public static void open(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(stack.getItem() instanceof CardWalletItem)) return;
        NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                (id, inventory, ignored) -> new WalletMenu(id, inventory, new WalletContainer(stack), stack),
                Component.translatable("item.tempad.card_wallet")));
    }
}
