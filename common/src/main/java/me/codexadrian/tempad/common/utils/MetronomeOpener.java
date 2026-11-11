package me.codexadrian.tempad.common.utils;

import dev.architectury.injectables.annotations.ExpectPlatform;
import me.codexadrian.tempad.common.block.MetronomeBlockEntity;
import net.minecraft.world.entity.player.Player;

public final class MetronomeOpener {
    private MetronomeOpener() { }
    @ExpectPlatform
    public static void open(Player player, MetronomeBlockEntity metronome) {
        throw new AssertionError("Platform implementation missing");
    }
}
