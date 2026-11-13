package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.items.TempadItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Nine-slot view used by the vanilla chest menu; the device occupies slot 4. */
public final class WorkstationMenuContainer implements Container {
    private final Player player;
    private final WorkstationBlockEntity workstation;

    public WorkstationMenuContainer(Player player, WorkstationBlockEntity workstation) {
        this.player = player;
        this.workstation = workstation;
    }

    @Override public int getContainerSize() { return 9; }
    @Override public boolean isEmpty() { return workstation.getTempad().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 4 ? workstation.getTempad() : ItemStack.EMPTY; }
    private boolean canOperate() {
        return !workstation.isRemoved() && workstation.getLevel() != null
            && workstation.getLevel().getBlockEntity(workstation.getBlockPos()) == workstation
            && workstation.canAccess(player)
            && player.distanceToSqr(workstation.getBlockPos().getX() + 0.5,
                workstation.getBlockPos().getY() + 0.5, workstation.getBlockPos().getZ() + 0.5) <= 64;
    }
    @Override public ItemStack removeItem(int slot, int amount) { return slot == 4 && amount > 0 && canOperate() ? workstation.eject() : ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return slot == 4 && canOperate() ? workstation.eject() : ItemStack.EMPTY; }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot == 4 && canOperate() && workstation.getTempad().isEmpty() && stack.getItem() instanceof TempadItem) workstation.insert(player, stack);
    }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 4 && stack.getItem() instanceof TempadItem && workstation.getTempad().isEmpty() && canOperate(); }
    @Override public void setChanged() { workstation.notifyTempadChanged(); }
    @Override public boolean stillValid(Player player) { return this.player == player && canOperate(); }
    @Override public void clearContent() { }
}
