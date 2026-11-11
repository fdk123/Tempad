package me.codexadrian.tempad.common.utils.forge;

import me.codexadrian.tempad.common.items.WalletContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.server.level.ServerPlayer;

public final class WalletOpenerImpl {
    private WalletOpenerImpl() {
    }

    public static void open(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                (id, inventory, ignored) -> new ChestMenu(MenuType.GENERIC_9x2, id, inventory, new WalletContainer(stack), 2),
                Component.translatable("item.tempad.card_wallet")));
    }
}
