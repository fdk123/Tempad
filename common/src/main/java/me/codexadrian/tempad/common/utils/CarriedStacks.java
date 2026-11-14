package me.codexadrian.tempad.common.utils;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Live item stacks carried in inventory or optional equipment slots. */
public final class CarriedStacks {
    private CarriedStacks() {}

    @ExpectPlatform
    public static List<ItemStack> of(Player player) {
        throw new AssertionError("Not implemented");
    }

    public static boolean contains(Player player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (ItemStack carried : of(player)) if (carried == stack) return true;
        return false;
    }
}
