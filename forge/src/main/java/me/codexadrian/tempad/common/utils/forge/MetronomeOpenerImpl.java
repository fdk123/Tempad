package me.codexadrian.tempad.common.utils.forge;

import me.codexadrian.tempad.common.block.MetronomeBlockEntity;
import me.codexadrian.tempad.common.utils.MetronomeOpener;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class MetronomeOpenerImpl {
    private MetronomeOpenerImpl() { }
    public static void open(Player player, MetronomeBlockEntity metronome) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
            (id, inventory, ignored) -> new ChestMenu(MenuType.GENERIC_9x1, id, inventory, metronome.inventory(), 1),
            Component.translatable("block.tempad.metronome.menu", metronome.getChronons(), metronome.getCapacity(), metronome.isLocked())));
    }
}
