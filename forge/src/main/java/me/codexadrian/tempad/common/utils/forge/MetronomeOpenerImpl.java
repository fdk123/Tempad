package me.codexadrian.tempad.common.utils.forge;

import me.codexadrian.tempad.common.block.MetronomeBlockEntity;
import me.codexadrian.tempad.common.forge.MetronomeMenu;
import me.codexadrian.tempad.common.utils.MetronomeOpener;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class MetronomeOpenerImpl {
    private MetronomeOpenerImpl() { }
    public static void open(Player player, MetronomeBlockEntity metronome) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
            (id, inventory, ignored) -> new MetronomeMenu(id, inventory, metronome),
            Component.translatable("block.tempad.metronome")), buffer -> {
                buffer.writeBlockPos(metronome.getBlockPos());
                buffer.writeBoolean(metronome.getOwner() != null && metronome.getOwner().equals(serverPlayer.getUUID()));
            });
    }
}
