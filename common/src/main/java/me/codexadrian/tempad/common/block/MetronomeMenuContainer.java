package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.items.ChrononContainerItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class MetronomeMenuContainer implements Container {
    private final MetronomeBlockEntity metronome;
    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

    public MetronomeMenuContainer(MetronomeBlockEntity metronome) {
        this.metronome = metronome;
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return true; }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot != 4 || !(stack.getItem() instanceof ChrononContainerItem)) return;
        metronome.receive(stack);
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 4 && stack.getItem() instanceof ChrononContainerItem; }
    @Override public void setChanged() { metronome.setChanged(); }
    @Override public boolean stillValid(Player player) {
        return metronome.canAccess(player)
            && player.distanceToSqr(metronome.getBlockPos().getX() + 0.5, metronome.getBlockPos().getY() + 0.5, metronome.getBlockPos().getZ() + 0.5) <= 64;
    }
    @Override public void clearContent() { }
}
