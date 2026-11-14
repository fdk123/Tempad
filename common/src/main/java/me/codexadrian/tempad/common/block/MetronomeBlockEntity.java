package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.data.MetronomeNetworkData;
import me.codexadrian.tempad.common.items.ChrononContainerItem;
import me.codexadrian.tempad.common.registry.TempadRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Bootable Metronome with eight charge slots and a shared owner network. */
public final class MetronomeBlockEntity extends BlockEntity {
    private final MetronomeMenuContainer inventory = new MetronomeMenuContainer(this);
    private UUID owner;
    private boolean locked = true;
    private int bootTime = 100;
    private int bootChronons;
    private int initialChronons;
    private boolean registered;

    public MetronomeBlockEntity(BlockPos pos, BlockState state) {
        super(TempadRegistry.METRONOME_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MetronomeBlockEntity entity) {
        if (level.isClientSide || entity.owner == null) return;
        if (entity.bootTime > 0) {
            boolean changed = entity.pullFromInputs();
            if (entity.bootChronons >= Math.max(1, TempadConfig.metronomeJumpStartAmount)) {
                entity.bootTime--;
                changed = true;
                if (entity.bootTime == 0) {
                    entity.bootChronons = 0;
                    entity.ensureRegistered();
                }
            }
            if (changed) entity.markChargeChanged();
            return;
        }
        entity.ensureRegistered();
        if (TempadConfig.metronomeRedstonePause && level.hasNeighborSignal(pos)) return;
        boolean changed = entity.pullFromInputs();
        changed |= entity.fillOutputs();
        changed |= entity.distributeToAdjacentDevices(level);
        if (level.getServer().getTickCount() % 20 == 0) changed |= entity.distributeToNearbyPlayer(level);
        if (changed) entity.markChargeChanged();
    }

    private void ensureRegistered() {
        if (registered || owner == null || level == null || level.isClientSide) return;
        MetronomeNetworkData network = MetronomeNetworkData.get(level);
        if (network == null) return;
        network.register(owner, GlobalPos.of(level.dimension(), worldPosition), initialChronons);
        initialChronons = 0;
        registered = true;
        markChargeChanged();
    }

    private boolean pullFromInputs() {
        boolean changed = false;
        for (int slot = 0; slot < 4; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof ChrononContainerItem container)) continue;
            int available = Math.min(Math.max(1, TempadConfig.metronomeTransferRate), container.getChronons(stack));
            int moved = acceptChronons(available);
            if (moved > 0) { container.extractChronons(stack, moved); inventory.setChanged(); changed = true; }
        }
        return changed;
    }

    private boolean fillOutputs() {
        MetronomeNetworkData network = MetronomeNetworkData.get(level);
        if (network == null) return false;
        boolean changed = false;
        for (int slot = 5; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof ChrononContainerItem container)) continue;
            int available = Math.min(Math.max(1, TempadConfig.metronomeTransferRate), network.stored(owner));
            int moved = container.insertChronons(stack, available);
            if (moved > 0) { network.extract(owner, moved); inventory.setChanged(); changed = true; }
        }
        return changed;
    }

    private boolean distributeToNearbyPlayer(Level level) {
        MetronomeNetworkData network = MetronomeNetworkData.get(level);
        if (network == null || network.stored(owner) <= 0) return false;
        boolean changed = false;
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(TempadConfig.metronomeDistributionRange))) {
            if (!owner.equals(player.getUUID())) continue;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.is(Tempad.CHRONON_AUTO_CHARGE_BLACKLIST) || !(stack.getItem() instanceof ChrononContainerItem container)) continue;
                int moved = container.insertChronons(stack, network.stored(owner));
                if (moved > 0) { network.extract(owner, moved); changed = true; }
                if (network.stored(owner) == 0) return changed;
            }
        }
        return changed;
    }

    private boolean distributeToAdjacentDevices(Level level) {
        MetronomeNetworkData network = MetronomeNetworkData.get(level);
        if (network == null || network.stored(owner) <= 0) return false;
        boolean changed = false;
        Set<BlockPos> visited = new HashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(direction);
            BlockPos target = neighbor;
            if (level.getBlockState(neighbor).is(TempadRegistry.WORKSTATION_CHILD_BLOCK.get())) {
                Direction facing = level.getBlockState(neighbor).getValue(BlockStateProperties.HORIZONTAL_FACING);
                target = neighbor.relative(facing.getClockWise());
            }
            if (!visited.add(target)) continue;
            BlockEntity adjacent = level.getBlockEntity(target);
            int amount = Math.min(network.stored(owner), Math.max(1, TempadConfig.metronomeTransferRate));
            int moved = 0;
            if (adjacent instanceof WorkstationBlockEntity workstation && workstation.acceptsChrononsFrom(owner)) moved = workstation.receiveChronons(amount);
            if (adjacent instanceof TimedoorProjectorBlockEntity projector && projector.acceptsChrononsFrom(owner)) moved = projector.receiveChronons(amount);
            if (moved > 0) { network.extract(owner, moved); changed = true; }
            if (network.stored(owner) == 0) return changed;
        }
        return changed;
    }

    public int receiveChronons(int amount) {
        if (level == null || level.isClientSide || amount <= 0 || owner == null) return 0;
        if (bootTime == 0) ensureRegistered();
        int moved = acceptChronons(amount);
        if (moved > 0) markChargeChanged();
        return moved;
    }

    private int acceptChronons(int amount) {
        if (amount <= 0) return 0;
        if (bootTime > 0) {
            int moved = Math.min(amount, Math.max(0, Math.max(1, TempadConfig.metronomeJumpStartAmount) - bootChronons));
            if (moved > 0) bootChronons += moved;
            return moved;
        }
        MetronomeNetworkData network = MetronomeNetworkData.get(level);
        return network == null ? 0 : network.insert(owner, amount);
    }

    public boolean receive(ItemStack source) {
        if (!(source.getItem() instanceof ChrononContainerItem container)) return false;
        int moved = receiveChronons(container.getChronons(source));
        if (moved <= 0) return false;
        container.extractChronons(source, moved);
        return true;
    }

    public void prepareRemoval() {
        if (level == null || level.isClientSide || owner == null || bootTime > 0) return;
        MetronomeNetworkData network = MetronomeNetworkData.get(level);
        Integer share = network == null ? null : network.unregister(owner, GlobalPos.of(level.dimension(), worldPosition));
        if (share != null) initialChronons = share;
        if (network != null) network.syncOwner(level.getServer(), owner);
        registered = false;
        setChanged();
    }

    private void markChargeChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            MetronomeNetworkData network = bootTime == 0 ? MetronomeNetworkData.get(level) : null;
            if (network != null && owner != null) network.syncOwner(level.getServer(), owner);
            else {
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public MetronomeMenuContainer inventory() { return inventory; }
    public UUID getOwner() { return owner; }
    public boolean isBooted() { return bootTime == 0; }
    public int getBootTime() { return bootTime; }
    public int getChronons() {
        if (bootTime > 0) return bootChronons;
        MetronomeNetworkData network = level == null ? null : MetronomeNetworkData.get(level);
        return network == null || owner == null ? initialChronons : network.stored(owner);
    }
    public int getCapacity() {
        if (bootTime > 0) return Math.max(1, TempadConfig.metronomeJumpStartAmount);
        MetronomeNetworkData network = level == null ? null : MetronomeNetworkData.get(level);
        return network == null || owner == null || network.capacity(owner) == 0
            ? Math.max(1, TempadConfig.metronomeCapacity) : network.capacity(owner);
    }
    public boolean canAccess(Player player) { return !locked || owner == null || owner.equals(player.getUUID()); }
    public boolean isLocked() { return locked; }
    public void claim(Player player) { if (owner == null) { owner = player.getUUID(); setChanged(); } }
    public void restoreIdentity(ItemStack stack) {
        if (!stack.hasTag()) return;
        CompoundTag tag = stack.getTag();
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : owner;
        locked = !tag.contains("Locked") || tag.getBoolean("Locked");
        initialChronons = Math.max(0, tag.getInt("Chronons"));
    }
    public void writeIdentity(ItemStack stack) {
        if (owner != null) stack.getOrCreateTag().putUUID("Owner", owner);
        stack.getOrCreateTag().putBoolean("Locked", locked);
        if (initialChronons > 0) stack.getOrCreateTag().putInt("Chronons", initialChronons);
    }
    public boolean toggleLock(Player player) {
        if (owner == null || !owner.equals(player.getUUID())) return false;
        locked = !locked;
        setChanged();
        return locked;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putBoolean("Locked", locked);
        tag.putInt("BootTime", bootTime);
        tag.putInt("BootChronons", bootChronons);
        tag.putInt("InitialChronons", initialChronons);
        CompoundTag items = new CompoundTag();
        inventory.save(items);
        tag.put("Inventory", items);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        locked = !tag.contains("Locked") || tag.getBoolean("Locked");
        bootTime = tag.contains("BootTime") ? Math.max(0, Math.min(100, tag.getInt("BootTime"))) : 0;
        bootChronons = Math.max(0, tag.getInt("BootChronons"));
        initialChronons = Math.max(0, tag.contains("InitialChronons") ? tag.getInt("InitialChronons") : tag.getInt("Chronons"));
        inventory.load(tag.getCompound("Inventory"));
        registered = false;
    }
    @Override public CompoundTag getUpdateTag() { CompoundTag tag = new CompoundTag(); saveAdditional(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
