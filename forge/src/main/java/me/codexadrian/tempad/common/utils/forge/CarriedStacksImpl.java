package me.codexadrian.tempad.common.utils.forge;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

public final class CarriedStacksImpl {
    private CarriedStacksImpl() {}

    public static List<ItemStack> of(Player player) {
        List<ItemStack> carried = new ArrayList<>();
        if (ModList.get().isLoaded("curios")) CuriosCarriedStacks.add(player, carried);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) carried.add(stack);
        }
        return carried;
    }
}
