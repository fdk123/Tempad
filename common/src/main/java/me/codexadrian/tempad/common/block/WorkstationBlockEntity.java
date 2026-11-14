package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.registry.TempadRegistry;
import me.codexadrian.tempad.common.items.TempadItem;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.data.PortalSetupData;
import me.codexadrian.tempad.common.data.PlayerLocationHandler;
import me.codexadrian.tempad.common.data.TempadLocationHandler;
import me.codexadrian.tempad.common.entity.TimedoorEntity;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.items.ChrononStorage;
import me.codexadrian.tempad.common.items.ChrononContainerItem;
import me.codexadrian.tempad.common.items.TempadUpgrades;
import me.codexadrian.tempad.common.recipe.TempadUpgradeRecipe;
import me.codexadrian.tempad.api.options.impl.TimerOption;
import me.codexadrian.tempad.common.utils.TeleportUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;

import java.util.UUID;

public final class WorkstationBlockEntity extends BlockEntity {
    private ItemStack tempad = ItemStack.EMPTY;
    private UUID owner;
    private boolean locked;
    private boolean powered;
    private UUID timedoorId;
    private int activationCooldown;
    private int sustainTicks;
    private String status = "idle";
    private ResourceLocation pendingUpgrade;
    private int downloadTime;
    private int maxDownloadTime;

    public WorkstationBlockEntity(BlockPos pos, BlockState state) { super(TempadRegistry.WORKSTATION_BLOCK_ENTITY.get(), pos, state); }
    public ItemStack getTempad() { return tempad; }
    public UUID getOwner() { return owner; }
    public boolean isLocked() { return locked; }
    public boolean isInstallingUpgrade() { return downloadTime > 0; }
    public int getDownloadTime() { return downloadTime; }
    public int getMaxDownloadTime() { return maxDownloadTime; }
    public boolean startUpgrade(Player player, ItemStack ingredient) {
        if (level == null || level.isClientSide || !canAccess(player) || tempad.isEmpty() || isInstallingUpgrade() || ingredient.isEmpty()) return false;
        for (TempadUpgradeRecipe recipe : level.getRecipeManager().getAllRecipesFor(TempadRegistry.UPGRADE_TYPE.get())) {
            if (recipe.item().test(ingredient) && !TempadUpgrades.has(tempad, recipe.upgrade())) {
                pendingUpgrade = recipe.upgrade();
                downloadTime = recipe.downloadTime();
                maxDownloadTime = downloadTime;
                if (!player.getAbilities().instabuild) ingredient.shrink(1);
                level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7F, 1.2F);
                markChanged();
                return true;
            }
        }
        return false;
    }
    public boolean canAccess(Player player) { return !locked || owner == null || owner.equals(player.getUUID()); }
    public boolean acceptsChrononsFrom(UUID sourceOwner) {
        return owner != null && owner.equals(sourceOwner);
    }
    public int receiveChronons(int amount) {
        if (level == null || level.isClientSide || amount <= 0 || !(tempad.getItem() instanceof ChrononContainerItem container)) return 0;
        int moved = container.insertChronons(tempad, amount);
        if (moved > 0) markChanged();
        return moved;
    }
    public int receiveFrom(ItemStack source) {
        if (level == null || level.isClientSide || source == tempad || !(source.getItem() instanceof ChrononContainerItem)
                || !(tempad.getItem() instanceof ChrononContainerItem container)) return 0;
        int moved = ChrononStorage.transfer(source, tempad, container, ChrononStorage.get(source));
        if (moved > 0) markChanged();
        return moved;
    }
    public boolean insert(Player player, ItemStack stack) {
        if (!canAccess(player) || !tempad.isEmpty() || !(stack.getItem() instanceof TempadItem)) return false;
        tempad = stack.copyWithCount(1); owner = player.getUUID(); markChanged(); return true;
    }
    public ItemStack eject() {
        if (tempad.isEmpty() || isInstallingUpgrade()) return ItemStack.EMPTY;
        stopTimedoor();
        ItemStack result = tempad;
        tempad = ItemStack.EMPTY;
        markChanged();
        return result;
    }
    public boolean toggleLock(Player player) { if (owner == null || !owner.equals(player.getUUID())) return false; locked = !locked; markChanged(); return locked; }
    private void markChanged() { setChanged(); refreshState(); if (level != null) { level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock()); } }
    public void notifyTempadChanged() { markChanged(); }
    public void refreshState() { if (level == null || level.isClientSide) return; BlockState current = getBlockState(); BlockState updated = current.setValue(WorkstationBlock.HAS_TEMPAD, !tempad.isEmpty()).setValue(WorkstationBlock.POWERED, powered); if (updated != current) level.setBlock(worldPosition, updated, 3); }
    public void restoreIdentity(ItemStack stack) { if (!stack.hasTag()) return; CompoundTag tag = stack.getTag(); owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : owner; locked = tag.getBoolean("Locked"); if (tag.contains("Tempad")) tempad = ItemStack.of(tag.getCompound("Tempad")); if (tag.contains("PendingUpgrade")) pendingUpgrade = ResourceLocation.tryParse(tag.getString("PendingUpgrade")); downloadTime = Math.max(0, tag.getInt("DownloadTime")); maxDownloadTime = Math.max(downloadTime, tag.getInt("MaxDownloadTime")); refreshState(); }
    public void writeIdentity(ItemStack stack) { if (owner != null) stack.getOrCreateTag().putUUID("Owner", owner); stack.getOrCreateTag().putBoolean("Locked", locked); if (!tempad.isEmpty()) stack.getOrCreateTag().put("Tempad", tempad.save(new CompoundTag())); if (pendingUpgrade != null) stack.getOrCreateTag().putString("PendingUpgrade", pendingUpgrade.toString()); stack.getOrCreateTag().putInt("DownloadTime", downloadTime); stack.getOrCreateTag().putInt("MaxDownloadTime", maxDownloadTime); }
    public static void tick(Level level, BlockPos pos, BlockState state, WorkstationBlockEntity workstation) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (workstation.downloadTime > 0) {
            workstation.downloadTime--;
            if (workstation.downloadTime == 0) {
                if (workstation.pendingUpgrade != null && !workstation.tempad.isEmpty()) TempadUpgrades.install(workstation.tempad, workstation.pendingUpgrade);
                workstation.pendingUpgrade = null;
                workstation.maxDownloadTime = 0;
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0);
                level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5F, 1.5F);
            } else if (workstation.downloadTime % 5 == 0) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 2, 0.15, 0.1, 0.15, 0);
            }
            workstation.markChanged();
        }
        if (!workstation.tempad.isEmpty() && workstation.tempad.getItem() instanceof TempadItem stored
                && stored.getOption() instanceof TimerOption timer) {
            timer.tick(workstation.tempad, null);
            if (level.getGameTime() % 20 == 0) workstation.setChanged();
        }
        Direction facing = state.getValue(WorkstationBlock.FACING);
        BlockPos childPos = pos.relative(facing.getCounterClockWise());
        BlockState childState = level.getBlockState(childPos);
        if (!childState.is(TempadRegistry.WORKSTATION_CHILD_BLOCK.get())
                || childState.getValue(WorkstationBlock.FACING) != facing) {
            serverLevel.destroyBlock(pos, true);
            return;
        }
        boolean signal = level.hasNeighborSignal(pos)
            || level.hasNeighborSignal(childPos);
        TimedoorEntity timedoor = workstation.getTimedoor(serverLevel);
        if (!signal) {
            if (workstation.powered) { workstation.powered = false; workstation.refreshState(); }
            workstation.activationCooldown = 0;
            workstation.sustainTicks = 0;
            workstation.setStatus("idle");
            if (timedoor != null) workstation.closeTimedoor(timedoor);
            return;
        }
        if (!workstation.powered) { workstation.powered = true; workstation.refreshState(); }
        if (timedoor != null) {
            if (workstation.tempad.isEmpty()) {
                workstation.closeTimedoor(timedoor);
                workstation.setStatus("no_tempad");
                return;
            }
            if (++workstation.sustainTicks >= 20) {
                workstation.sustainTicks = 0;
                int cost = Math.max(0, TempadConfig.workstationPortalSustainCost);
                if (ChrononStorage.get(workstation.tempad) < cost) {
                    workstation.closeTimedoor(timedoor);
                    workstation.activationCooldown = 20;
                    workstation.setStatus("no_chronons");
                    return;
                }
                ChrononStorage.extract(workstation.tempad, cost);
                workstation.markChanged();
                timedoor.setClosingTime(timedoor.tickCount + Math.max(40, TempadConfig.timedoorWait));
                TimedoorEntity linked = timedoor.getLinkedPortalEntity();
                if (linked != null) linked.setClosingTime(linked.tickCount + Math.max(40, TempadConfig.timedoorWait));
            }
            workstation.setStatus("active");
            return;
        }
        if (workstation.activationCooldown > 0) { workstation.activationCooldown--; return; }
        workstation.activationCooldown = 20;
        workstation.setChanged();
        if (workstation.owner == null) { workstation.setStatus("no_owner"); return; }
        if (workstation.tempad.isEmpty()) { workstation.setStatus("no_tempad"); return; }
        if (!(serverLevel.getPlayerByUUID(workstation.owner) instanceof ServerPlayer player)) { workstation.setStatus("owner_offline"); return; }
        if (!(workstation.tempad.getItem() instanceof TempadItem tempad)) { workstation.setStatus("invalid_tempad"); return; }
        UUID selected = PortalSetupData.selected(workstation.tempad);
        if (selected == null) selected = TempadLocationHandler.getFavorite(serverLevel, player.getUUID());
        boolean playerTarget = selected != null && PlayerLocationHandler.isPlayerLocation(serverLevel, selected);
        if (playerTarget && !TempadUpgrades.has(workstation.tempad, TempadUpgrades.PLAYER_TELEPORT)) {
            workstation.setStatus("blocked");
            return;
        }
        LocationData location = selected == null ? null : playerTarget
            ? PlayerLocationHandler.resolveForDevice(serverLevel, player.getUUID(), selected)
            : TempadLocationHandler.getLocation(serverLevel, player.getUUID(), selected);
        if (location == null) { workstation.setStatus(PortalSetupData.selected(workstation.tempad) == null ? "no_favorite" : "no_target"); return; }
        if (!TeleportUtils.mayTeleport(location.getLevelKey(), player)) { workstation.setStatus("blocked"); return; }
        if (!tempad.getOption().canTimedoorOpen(player, workstation.tempad)) { workstation.setStatus("no_fuel"); return; }
        if (ChrononStorage.get(workstation.tempad) < Math.max(0, TempadConfig.workstationPortalSustainCost)) { workstation.setStatus("no_chronons"); return; }
        TimedoorEntity opened = TempadItem.summonTimeDoor(location, player, 0, pos, state.getValue(WorkstationBlock.FACING), serverLevel, PortalSetupData.get(workstation.tempad));
        if (opened == null) { workstation.setStatus("blocked"); return; }
        if (!player.getAbilities().instabuild) tempad.getOption().onTimedoorOpen(player, workstation.tempad);
        workstation.timedoorId = opened.getUUID();
        workstation.sustainTicks = 0;
        workstation.setStatus("active");
        workstation.markChanged();
    }
    public String getStatus() { return status; }
    private void setStatus(String next) { if (status.equals(next)) return; status = next; setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    private TimedoorEntity getTimedoor(ServerLevel level) {
        if (timedoorId == null) return null;
        if (level.getEntity(timedoorId) instanceof TimedoorEntity timedoor && !timedoor.isRemoved()) return timedoor;
        timedoorId = null;
        setChanged();
        return null;
    }
    public void stopTimedoor() {
        if (level instanceof ServerLevel serverLevel) {
            TimedoorEntity timedoor = getTimedoor(serverLevel);
            if (timedoor != null) closeTimedoor(timedoor);
        }
    }
    private void closeTimedoor(TimedoorEntity timedoor) {
        timedoor.setClosingTime(timedoor.tickCount);
        TimedoorEntity linked = timedoor.getLinkedPortalEntity();
        if (linked != null) linked.setClosingTime(linked.tickCount);
        timedoorId = null;
        sustainTicks = 0;
        setChanged();
    }
    @Override public CompoundTag getUpdateTag() { CompoundTag tag = new CompoundTag(); saveAdditional(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); if (!tempad.isEmpty()) tag.put("Tempad", tempad.save(new CompoundTag())); if (owner != null) tag.putUUID("Owner", owner); if (timedoorId != null) tag.putUUID("Timedoor", timedoorId); if (pendingUpgrade != null) tag.putString("PendingUpgrade", pendingUpgrade.toString()); tag.putInt("DownloadTime", downloadTime); tag.putInt("MaxDownloadTime", maxDownloadTime); tag.putString("Status", status); tag.putInt("ActivationCooldown", activationCooldown); tag.putInt("SustainTicks", sustainTicks); tag.putBoolean("Locked", locked); tag.putBoolean("Powered", powered); }
    @Override public void load(CompoundTag tag) { super.load(tag); tempad = tag.contains("Tempad") ? ItemStack.of(tag.getCompound("Tempad")) : ItemStack.EMPTY; owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null; timedoorId = tag.hasUUID("Timedoor") ? tag.getUUID("Timedoor") : null; pendingUpgrade = tag.contains("PendingUpgrade") ? ResourceLocation.tryParse(tag.getString("PendingUpgrade")) : null; downloadTime = pendingUpgrade == null ? 0 : Math.max(0, tag.getInt("DownloadTime")); maxDownloadTime = Math.max(downloadTime, tag.getInt("MaxDownloadTime")); status = tag.contains("Status") ? tag.getString("Status") : "idle"; activationCooldown = Math.max(0, tag.getInt("ActivationCooldown")); sustainTicks = Math.max(0, Math.min(19, tag.getInt("SustainTicks"))); locked = tag.getBoolean("Locked"); powered = tag.getBoolean("Powered"); }
}
