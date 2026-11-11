package me.codexadrian.tempad.common.utils;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class WalletOpener {
    private WalletOpener() {
    }

    @ExpectPlatform
    public static void open(Player player, ItemStack stack) {
        throw new AssertionError("Platform implementation missing");
    }
}
