package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.items.ChrononContainerItem;
import me.codexadrian.tempad.common.registry.TempadRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class MetronomeMenuContainer implements Container {
    private final MetronomeBlockEntity metronome;
    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

    public MetronomeMenuContainer(MetronomeBlockEntity metronome) {
        this.metronome = metronome;
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = items.set(slot, ItemStack.EMPTY);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (!canPlaceItem(slot, stack)) return;
        items.set(slot, stack);
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != 4 && (stack.isEmpty() || stack.getItem() instanceof ChrononContainerItem);
    }
    @Override public void setChanged() { metronome.setChanged(); }
    @Override public boolean stillValid(Player player) {
        return metronome.canAccess(player)
            && metronome.getLevel() != null
            && metronome.getLevel().getBlockState(metronome.getBlockPos()).is(TempadRegistry.METRONOME_BLOCK.get())
            && player.distanceToSqr(metronome.getBlockPos().getX() + 0.5, metronome.getBlockPos().getY() + 0.5, metronome.getBlockPos().getZ() + 0.5) <= 64;
    }
    @Override public void clearContent() { items.clear(); setChanged(); }

    public void save(CompoundTag tag) { ContainerHelper.saveAllItems(tag, items); }
    public void load(CompoundTag tag) { ContainerHelper.loadAllItems(tag, items); }
}
