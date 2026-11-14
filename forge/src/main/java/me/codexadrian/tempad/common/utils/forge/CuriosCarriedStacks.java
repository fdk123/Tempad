package me.codexadrian.tempad.common.utils.forge;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.List;

/** Loaded only when the optional Curios mod is present. */
final class CuriosCarriedStacks {
    private CuriosCarriedStacks() {}

    static void add(Player player, List<ItemStack> carried) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getCurios().values().forEach(group -> {
            IDynamicStackHandler stacks = group.getStacks();
            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                ItemStack stack = stacks.getStackInSlot(slot);
                if (!stack.isEmpty()) carried.add(stack);
            }
        }));
    }
}
