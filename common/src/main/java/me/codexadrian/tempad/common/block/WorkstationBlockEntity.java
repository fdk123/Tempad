package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.registry.TempadRegistry;
import me.codexadrian.tempad.common.items.TempadItem;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.data.TempadLocationHandler;
import me.codexadrian.tempad.common.entity.TimedoorEntity;
import me.codexadrian.tempad.common.utils.TeleportUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class WorkstationBlockEntity extends BlockEntity {
    private ItemStack tempad = ItemStack.EMPTY;
    private UUID owner;
    private boolean locked;
    private boolean powered;
    private UUID timedoorId;
    private int activationCooldown;

    public WorkstationBlockEntity(BlockPos pos, BlockState state) { super(TempadRegistry.WORKSTATION_BLOCK_ENTITY.get(), pos, state); }
    public ItemStack getTempad() { return tempad; }
    public boolean isLocked() { return locked; }
    public boolean canAccess(Player player) { return !locked || owner == null || owner.equals(player.getUUID()); }
    public boolean insert(Player player, ItemStack stack) {
        if (!tempad.isEmpty() || !(stack.getItem() instanceof TempadItem)) return false;
        tempad = stack.copyWithCount(1); owner = player.getUUID(); markChanged(); return true;
    }
    public ItemStack eject() { ItemStack result = tempad; tempad = ItemStack.EMPTY; markChanged(); return result; }
    public boolean toggleLock(Player player) { if (owner == null || !owner.equals(player.getUUID())) return false; locked = !locked; markChanged(); return locked; }
    private void markChanged() { setChanged(); refreshState(); if (level != null) { level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock()); } }
    public void refreshState() { if (level == null || level.isClientSide) return; BlockState current = getBlockState(); BlockState updated = current.setValue(WorkstationBlock.HAS_TEMPAD, !tempad.isEmpty()).setValue(WorkstationBlock.POWERED, powered); if (updated != current) level.setBlock(worldPosition, updated, 3); }
    public void restoreIdentity(ItemStack stack) { if (!stack.hasTag()) return; CompoundTag tag = stack.getTag(); owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : owner; locked = tag.getBoolean("Locked"); if (tag.contains("Tempad")) tempad = ItemStack.of(tag.getCompound("Tempad")); refreshState(); }
    public void writeIdentity(ItemStack stack) { if (owner != null) stack.getOrCreateTag().putUUID("Owner", owner); stack.getOrCreateTag().putBoolean("Locked", locked); if (!tempad.isEmpty()) stack.getOrCreateTag().put("Tempad", tempad.save(new CompoundTag())); }
    public static void tick(Level level, BlockPos pos, BlockState state, WorkstationBlockEntity workstation) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        boolean signal = level.hasNeighborSignal(pos);
        TimedoorEntity timedoor = workstation.getTimedoor(serverLevel);
        if (!signal) {
            if (workstation.powered) { workstation.powered = false; workstation.refreshState(); }
            workstation.activationCooldown = 0;
            if (timedoor != null) workstation.closeTimedoor(timedoor);
            return;
        }
        if (!workstation.powered) { workstation.powered = true; workstation.refreshState(); }
        if (timedoor != null || workstation.activationCooldown > 0) { if (workstation.activationCooldown > 0) workstation.activationCooldown--; return; }
        workstation.activationCooldown = 20;
        workstation.setChanged();
        if (workstation.owner == null || workstation.tempad.isEmpty()) return;
        if (!(serverLevel.getPlayerByUUID(workstation.owner) instanceof ServerPlayer player)) return;
        if (!(workstation.tempad.getItem() instanceof TempadItem tempad)) return;
        LocationData location = TempadLocationHandler.getLocation(serverLevel, player.getUUID(), TempadLocationHandler.getFavorite(serverLevel, player.getUUID()));
        if (location == null || !TeleportUtils.mayTeleport(location.getLevelKey(), player) || !tempad.getOption().canTimedoorOpen(player, workstation.tempad)) return;
        if (!player.getAbilities().instabuild) tempad.getOption().onTimedoorOpen(player, workstation.tempad);
        TimedoorEntity opened = TempadItem.summonTimeDoor(location, player, 0, pos, state.getValue(WorkstationBlock.FACING));
        workstation.timedoorId = opened.getUUID();
        workstation.setChanged();
    }
    private TimedoorEntity getTimedoor(ServerLevel level) { if (timedoorId == null) return null; if (level.getEntity(timedoorId) instanceof TimedoorEntity timedoor && !timedoor.isRemoved()) return timedoor; timedoorId = null; return null; }
    private void closeTimedoor(TimedoorEntity timedoor) { timedoor.setClosingTime(timedoor.tickCount); if (timedoor.getLinkedPortalEntity() != null) timedoor.getLinkedPortalEntity().setClosingTime(timedoor.getLinkedPortalEntity().tickCount); timedoorId = null; setChanged(); }
    @Override public CompoundTag getUpdateTag() { CompoundTag tag = new CompoundTag(); saveAdditional(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); if (!tempad.isEmpty()) tag.put("Tempad", tempad.save(new CompoundTag())); if (owner != null) tag.putUUID("Owner", owner); if (timedoorId != null) tag.putUUID("Timedoor", timedoorId); tag.putInt("ActivationCooldown", activationCooldown); tag.putBoolean("Locked", locked); tag.putBoolean("Powered", powered); }
    @Override public void load(CompoundTag tag) { super.load(tag); tempad = tag.contains("Tempad") ? ItemStack.of(tag.getCompound("Tempad")) : ItemStack.EMPTY; owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null; timedoorId = tag.hasUUID("Timedoor") ? tag.getUUID("Timedoor") : null; activationCooldown = Math.max(0, tag.getInt("ActivationCooldown")); locked = tag.getBoolean("Locked"); powered = tag.getBoolean("Powered"); }
}
