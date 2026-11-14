package me.codexadrian.tempad.common.forge;

import me.codexadrian.tempad.common.block.MetronomeBlockEntity;
import me.codexadrian.tempad.common.block.MetronomeMenuContainer;
import me.codexadrian.tempad.common.items.ChrononContainerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Eight working slots, network energy and owner lock for the Forge Metronome. */
public final class MetronomeMenu extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData data;
    private final BlockPos blockPos;
    private final boolean canEditLock;
    @Nullable private final MetronomeBlockEntity metronome;

    public MetronomeMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, new SimpleContainer(9), new SimpleContainerData(6),
            buffer.readBlockPos(), buffer.readBoolean(), null);
    }

    public MetronomeMenu(int id, Inventory playerInventory, MetronomeBlockEntity metronome) {
        this(id, playerInventory, metronome.inventory(), liveData(metronome), metronome.getBlockPos(),
            metronome.getOwner() != null && metronome.getOwner().equals(playerInventory.player.getUUID()), metronome);
    }

    private MetronomeMenu(int id, Inventory playerInventory, Container inventory, ContainerData data,
                          BlockPos blockPos, boolean canEditLock, @Nullable MetronomeBlockEntity metronome) {
        super(ForgeTempad.METRONOME_MENU.get(), id);
        this.inventory = inventory;
        this.data = data;
        this.blockPos = blockPos;
        this.canEditLock = canEditLock;
        this.metronome = metronome;
        for (int slot = 0; slot < 8; slot++) {
            int inventorySlot = slot < 4 ? slot : slot + 1;
            int x = 25 + (slot < 4 ? slot : slot + 1) * 18;
            addSlot(new Slot(inventory, inventorySlot, x, 40) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof ChrononContainerItem;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 25 + column * 18, 85 + row * 18));
        }
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(playerInventory, column, 25 + column * 18, 143));
        addDataSlots(data);
    }

    private static ContainerData liveData(MetronomeBlockEntity metronome) {
        return new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> metronome.getChronons() & 0xFFFF;
                    case 1 -> metronome.getChronons() >>> 16;
                    case 2 -> metronome.getCapacity() & 0xFFFF;
                    case 3 -> metronome.getCapacity() >>> 16;
                    case 4 -> metronome.getBootTime();
                    case 5 -> metronome.isLocked() ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return 6; }
        };
    }

    public int stored() { return (data.get(1) << 16) | (data.get(0) & 0xFFFF); }
    public int capacity() { return (data.get(3) << 16) | (data.get(2) & 0xFFFF); }
    public int bootTime() { return data.get(4); }
    public boolean locked() { return data.get(5) != 0; }
    public boolean canEditLock() { return canEditLock; }
    public BlockPos blockPos() { return blockPos; }

    @Override public boolean stillValid(Player player) {
        return metronome == null || inventory.stillValid(player);
    }

    @Override public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != 0 || metronome == null || !canEditLock || !stillValid(player)
            || metronome.getOwner() == null || !metronome.getOwner().equals(player.getUUID())) return false;
        metronome.toggleLock(player);
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack item = slot.getItem();
        ItemStack original = item.copy();
        if (index < 8) {
            if (!moveItemStackTo(item, 8, slots.size(), true)) return ItemStack.EMPTY;
        } else if (item.getItem() instanceof ChrononContainerItem) {
            if (!moveItemStackTo(item, 0, 8, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (item.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
