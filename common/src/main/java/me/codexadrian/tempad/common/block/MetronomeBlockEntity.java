package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.ConfigCache;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.items.ChrononContainerItem;
import me.codexadrian.tempad.common.items.ChrononStorage;
import me.codexadrian.tempad.common.registry.TempadRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class MetronomeBlockEntity extends BlockEntity {
    private static final String CHRONONS = "Chronons";
    private int chronons;
    private int ticks;
    private UUID owner;
    private boolean locked;

    public MetronomeBlockEntity(BlockPos pos, BlockState state) {
        super(TempadRegistry.METRONOME_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MetronomeBlockEntity entity) {
        if (level.isClientSide) return;
        if (ConfigCache.metronomeRedstonePause && level.hasNeighborSignal(pos)) return;
        if (entity.distributeToWorkstations(level)) entity.markChargeChanged();
        if (++entity.ticks < ConfigCache.metronomeGenerationInterval) return;
        entity.ticks = 0;
        entity.chronons = Math.min(entity.getCapacity(), entity.chronons + ConfigCache.metronomeGenerationAmount);
        entity.distribute(level);
        entity.markChargeChanged();
    }

    private void distribute(Level level) {
        if (chronons <= 0) return;
        for (Player player : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(worldPosition).inflate(ConfigCache.metronomeDistributionRange))) {
            if (owner != null && !owner.equals(player.getUUID())) continue;
            for (int slot = 0; slot < player.getInventory().getContainerSize() && chronons > 0; slot++) {
                ItemStack target = player.getInventory().getItem(slot);
                if (target.is(Tempad.CHRONON_AUTO_CHARGE_BLACKLIST) || !(target.getItem() instanceof ChrononContainerItem container)) continue;
                int moved = container.insertChronons(target, chronons);
                chronons -= moved;
            }
        }
    }

    private boolean distributeToWorkstations(Level level) {
        if (chronons <= 0 || owner == null) return false;
        Set<BlockPos> visited = new HashSet<>();
        boolean transferred = false;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(direction);
            BlockPos targetPos = neighbor;
            if (level.getBlockState(neighbor).is(TempadRegistry.WORKSTATION_CHILD_BLOCK.get())) {
                Direction facing = level.getBlockState(neighbor).getValue(BlockStateProperties.HORIZONTAL_FACING);
                targetPos = neighbor.relative(facing.getClockWise());
            }
            if (!visited.add(targetPos) || !(level.getBlockEntity(targetPos) instanceof WorkstationBlockEntity workstation)
                    || !workstation.acceptsChrononsFrom(owner)) continue;
            int moved = workstation.receiveChronons(Math.min(chronons, Math.max(1, TempadConfig.metronomeTransferRate)));
            if (moved > 0) {
                chronons -= moved;
                transferred = true;
            }
            if (chronons <= 0) break;
        }
        return transferred;
    }

    public boolean receive(ItemStack source) {
        if (!(source.getItem() instanceof ChrononContainerItem container)) return false;
        int moved = Math.min(chronons < getCapacity() ? getCapacity() - chronons : 0, container.getChronons(source));
        if (moved <= 0) return false;
        container.extractChronons(source, moved);
        chronons += moved;
        markChargeChanged();
        return true;
    }

    private void markChargeChanged() {
        setChanged();
        if (level != null) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean canAccess(Player player) {
        return !locked || owner == null || owner.equals(player.getUUID());
    }

    public boolean isLocked() {
        return locked;
    }

    public void claim(Player player) {
        if (owner == null) owner = player.getUUID();
    }

    public void restoreIdentity(ItemStack stack) {
        if (!stack.hasTag()) return;
        CompoundTag tag = stack.getTag();
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : owner;
        locked = tag.getBoolean("Locked");
    }

    public void writeIdentity(ItemStack stack) {
        if (owner != null) stack.getOrCreateTag().putUUID("Owner", owner);
        stack.getOrCreateTag().putBoolean("Locked", locked);
    }

    public boolean toggleLock(Player player) {
        if (owner == null || !owner.equals(player.getUUID())) return false;
        locked = !locked;
        setChanged();
        return locked;
    }

    public int getChronons() {
        return chronons;
    }

    public int getCapacity() {
        return ConfigCache.metronomeCapacity;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(CHRONONS, chronons);
        tag.putInt("Ticks", ticks);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putBoolean("Locked", locked);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        chronons = Math.max(0, Math.min(tag.getInt(CHRONONS), getCapacity()));
        ticks = Math.max(0, tag.getInt("Ticks"));
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        locked = tag.getBoolean("Locked");
    }
}
