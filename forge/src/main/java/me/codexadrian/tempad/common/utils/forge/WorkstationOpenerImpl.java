package me.codexadrian.tempad.common.utils.forge;

import me.codexadrian.tempad.common.block.WorkstationBlockEntity;
import me.codexadrian.tempad.common.block.WorkstationMenuContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.network.NetworkHooks;

public final class WorkstationOpenerImpl {
    private WorkstationOpenerImpl() { }
    public static void open(Player player, WorkstationBlockEntity workstation) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
            (id, inventory, ignored) -> new ChestMenu(MenuType.GENERIC_9x1, id, inventory, new WorkstationMenuContainer(serverPlayer, workstation), 1),
            Component.translatable("block.tempad.workstation.menu", workstation.getTempad().isEmpty() ? 0 : 1, workstation.isLocked())));
    }
}
