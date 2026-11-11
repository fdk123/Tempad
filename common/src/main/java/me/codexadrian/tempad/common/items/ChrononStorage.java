package me.codexadrian.tempad.common.items;

import net.minecraft.world.item.ItemStack;

/** Small NBT-backed storage used until the full 3.0 context API is ported. */
public final class ChrononStorage {
    private static final String CHRONONS = "Chronons";

    private ChrononStorage() {
    }

    public static int get(ItemStack stack) {
        if (!stack.hasTag()) return 0;
        return Math.max(0, stack.getTag().getInt(CHRONONS));
    }

    public static int insert(ItemStack stack, int amount, int capacity) {
        if (amount <= 0 || capacity <= 0) return 0;
        int current = Math.min(get(stack), Math.max(0, capacity));
        int inserted = Math.min(amount, Math.max(0, capacity - current));
        if (inserted > 0) stack.getOrCreateTag().putInt(CHRONONS, current + inserted);
        return inserted;
    }

    public static int extract(ItemStack stack, int amount) {
        if (amount <= 0) return 0;
        int extracted = Math.min(amount, get(stack));
        if (extracted > 0) stack.getOrCreateTag().putInt(CHRONONS, get(stack) - extracted);
        return extracted;
    }
}
