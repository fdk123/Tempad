package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.items.TempadItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Nine-slot view used by the vanilla chest menu; the device occupies slot 4. */
public final class WorkstationMenuContainer implements Container {
    private final Player player;
    private final WorkstationBlockEntity workstation;
    private final NonNullList<ItemStack> empty = NonNullList.withSize(9, ItemStack.EMPTY);

    public WorkstationMenuContainer(Player player, WorkstationBlockEntity workstation) {
        this.player = player;
        this.workstation = workstation;
    }

    @Override public int getContainerSize() { return 9; }
    @Override public boolean isEmpty() { return workstation.getTempad().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 4 ? workstation.getTempad() : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) { return slot == 4 && amount > 0 ? workstation.eject() : ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return slot == 4 ? workstation.eject() : ItemStack.EMPTY; }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot == 4 && workstation.getTempad().isEmpty() && stack.getItem() instanceof TempadItem && workstation.insert(player, stack)) stack.shrink(1);
    }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 4 && stack.getItem() instanceof TempadItem && workstation.getTempad().isEmpty() && workstation.canAccess(player); }
    @Override public void setChanged() { workstation.setChanged(); }
    @Override public boolean stillValid(Player player) { return workstation.canAccess(player) && player.distanceToSqr(workstation.getBlockPos().getX() + 0.5, workstation.getBlockPos().getY() + 0.5, workstation.getBlockPos().getZ() + 0.5) <= 64; }
    @Override public void clearContent() { if (!workstation.getTempad().isEmpty()) workstation.eject(); }
}
