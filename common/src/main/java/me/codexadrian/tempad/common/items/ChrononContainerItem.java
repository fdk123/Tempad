package me.codexadrian.tempad.common.items;

import net.minecraft.world.item.ItemStack;

/** A 1.20.1-compatible storage contract for the 3.0.x chronon devices. */
public interface ChrononContainerItem {
    int getChrononCapacity(ItemStack stack);

    default int getChronons(ItemStack stack) {
        return ChrononStorage.get(stack);
    }

    default int insertChronons(ItemStack stack, int amount) {
        return ChrononStorage.insert(stack, amount, getChrononCapacity(stack));
    }

    default int extractChronons(ItemStack stack, int amount) {
        return ChrononStorage.extract(stack, amount);
    }
}
