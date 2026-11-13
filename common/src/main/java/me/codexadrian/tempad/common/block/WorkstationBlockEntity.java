package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.registry.TempadRegistry;
import me.codexadrian.tempad.common.items.TempadItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public final class WorkstationBlockEntity extends BlockEntity {
    private ItemStack tempad = ItemStack.EMPTY;
    private UUID owner;
    private boolean locked;

    public WorkstationBlockEntity(BlockPos pos, BlockState state) { super(TempadRegistry.WORKSTATION_BLOCK_ENTITY.get(), pos, state); }
    public ItemStack getTempad() { return tempad; }
    public boolean canAccess(Player player) { return !locked || owner == null || owner.equals(player.getUUID()); }
    public boolean insert(Player player, ItemStack stack) {
        if (!tempad.isEmpty() || !(stack.getItem() instanceof TempadItem)) return false;
        tempad = stack.copyWithCount(1); owner = player.getUUID(); setChanged(); return true;
    }
    public ItemStack eject() { ItemStack result = tempad; tempad = ItemStack.EMPTY; setChanged(); return result; }
    public boolean toggleLock(Player player) { if (owner == null || !owner.equals(player.getUUID())) return false; locked = !locked; setChanged(); return locked; }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); if (!tempad.isEmpty()) tag.put("Tempad", tempad.save(new CompoundTag())); if (owner != null) tag.putUUID("Owner", owner); tag.putBoolean("Locked", locked); }
    @Override public void load(CompoundTag tag) { super.load(tag); tempad = tag.contains("Tempad") ? ItemStack.of(tag.getCompound("Tempad")) : ItemStack.EMPTY; owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null; locked = tag.getBoolean("Locked"); }
}
