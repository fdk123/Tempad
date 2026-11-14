package me.codexadrian.tempad.common.items;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** NBT-backed two-row container used by the 3.0.x card wallet. */
public final class WalletContainer implements Container {
    private static final String CONTENTS = "Wallet";
    private final ItemStack wallet;
    private final NonNullList<ItemStack> items = NonNullList.withSize(18, ItemStack.EMPTY);

    public WalletContainer(ItemStack wallet) {
        this.wallet = wallet;
        ListTag contents = wallet.hasTag() ? wallet.getTag().getList(CONTENTS, Tag.TAG_COMPOUND) : new ListTag();
        for (int index = 0; index < contents.size(); index++) {
            CompoundTag entry = contents.getCompound(index);
            int slot = entry.getByte("Slot") & 255;
            if (slot < items.size()) items.set(slot, ItemStack.of(entry.getCompound("Item")));
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : items) if (!item.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = items.set(slot, ItemStack.EMPTY);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!canPlaceItem(slot, stack)) return;
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.isEmpty() || canStore(stack);
    }

    /** Moves as many filled cards as possible into existing or empty slots. */
    public int insert(ItemStack source) {
        if (!canStore(source)) return 0;
        int moved = 0;
        for (int slot = 0; slot < items.size() && !source.isEmpty(); slot++) {
            ItemStack existing = items.get(slot);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, source)) continue;
            int room = source.getMaxStackSize() - existing.getCount();
            if (room <= 0) continue;
            int amount = Math.min(room, source.getCount());
            if (existing.isEmpty()) {
                ItemStack stored = source.copy();
                stored.setCount(amount);
                items.set(slot, stored);
            } else existing.grow(amount);
            source.shrink(amount);
            moved += amount;
        }
        if (moved > 0) setChanged();
        return moved;
    }

    @Override
    public void setChanged() {
        ListTag contents = new ListTag();
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack item = items.get(slot);
            if (item.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) slot);
            entry.put("Item", item.save(new CompoundTag()));
            contents.add(entry);
        }
        wallet.getOrCreateTag().put(CONTENTS, contents);
    }

    @Override
    public boolean stillValid(Player player) {
        return !wallet.isEmpty();
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    public static boolean canStore(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof LocationCard && LocationCard.getLocation(stack) != null;
    }
}
