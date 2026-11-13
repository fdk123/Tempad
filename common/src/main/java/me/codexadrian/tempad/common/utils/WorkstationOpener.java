package me.codexadrian.tempad.common.utils;

import dev.architectury.injectables.annotations.ExpectPlatform;
import me.codexadrian.tempad.common.block.WorkstationBlockEntity;
import net.minecraft.world.entity.player.Player;

public final class WorkstationOpener {
    private WorkstationOpener() { }
    @ExpectPlatform
    public static void open(Player player, WorkstationBlockEntity workstation) {
        throw new AssertionError("Platform implementation missing");
    }
}
