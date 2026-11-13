package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.entity.TimedoorEntity;
import me.codexadrian.tempad.common.items.ChrononContainerItem;
import me.codexadrian.tempad.common.items.ProjectorData;
import me.codexadrian.tempad.common.items.TempadItem;
import me.codexadrian.tempad.common.registry.TempadRegistry;
import me.codexadrian.tempad.common.utils.TeleportUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Power and location storage for a placed projector. */
public final class TimedoorProjectorBlockEntity extends BlockEntity {
    private int chronons;
    private UUID owner;
    private LocationData target;
    private UUID timedoorId;

    public TimedoorProjectorBlockEntity(BlockPos pos, BlockState state) { super(TempadRegistry.TIMEDOOR_PROJECTOR_BLOCK_ENTITY.get(), pos, state); }
    public int getChronons() { return chronons; }
    public int getCapacity() { return Math.max(1, TempadConfig.projectorCapacity); }
    public UUID getOwner() { return owner; }
    @Nullable public LocationData getTarget() { return target; }
    public boolean canAccess(Player player) { return owner == null || owner.equals(player.getUUID()); }
    public boolean acceptsChrononsFrom(UUID sourceOwner) { return owner != null && owner.equals(sourceOwner); }
    public void claim(Player player) { if (owner == null) { owner = player.getUUID(); markChanged(); } }

    public int receiveChronons(int amount) {
        if (level == null || level.isClientSide || amount <= 0) return 0;
        int moved = Math.min(amount, Math.max(0, getCapacity() - chronons));
        if (moved > 0) { chronons += moved; markChanged(); }
        return moved;
    }

    public int receiveFrom(ItemStack source) {
        if (!(source.getItem() instanceof ChrononContainerItem container)) return 0;
        int moved = receiveChronons(container.getChronons(source));
        if (moved > 0) container.extractChronons(source, moved);
        return moved;
    }

    public void setTarget(@Nullable LocationData location) {
        if (target != null) stopTimedoor();
        target = location;
        if (level != null && !level.isClientSide) level.setBlock(worldPosition, getBlockState().setValue(TimedoorProjectorBlock.HAS_CARD, target != null), 3);
        markChanged();
    }

    public void openTimedoor() {
        if (!(level instanceof ServerLevel serverLevel) || owner == null || target == null || target.getLevelKey() == null || !target.isTeleportable()) return;
        if (!(serverLevel.getPlayerByUUID(owner) instanceof ServerPlayer player)) return;
        if (!player.getAbilities().instabuild && (TeleportUtils.isDimensionBlacklisted(serverLevel, target.getLevelKey())
                || (!TempadConfig.allowInterdimensionalTravel && !serverLevel.dimension().equals(target.getLevelKey())))) return;
        if (serverLevel.getServer().getLevel(target.getLevelKey()) == null) return;
        if (timedoorId != null && serverLevel.getEntity(timedoorId) instanceof TimedoorEntity existing && !existing.isRemoved()) return;
        timedoorId = null;
        int cost = Math.max(0, TempadConfig.projectorDoorCost);
        if (chronons < cost) return;
        TimedoorEntity opened = TempadItem.summonTimeDoor(target, player, Tempad.ORANGE, worldPosition, getBlockState().getValue(TimedoorProjectorBlock.FACING), serverLevel);
        if (opened == null) return;
        opened.setLifetimeTicks(Math.max(1, TempadConfig.projectorDoorDuration));
        chronons -= cost;
        timedoorId = opened.getUUID();
        markChanged();
    }

    public void stopTimedoor() {
        if (level instanceof ServerLevel serverLevel && timedoorId != null && serverLevel.getEntity(timedoorId) instanceof TimedoorEntity door) {
            door.setClosingTime(door.tickCount);
            TimedoorEntity linked = door.getLinkedPortalEntity();
            if (linked != null) linked.setClosingTime(linked.tickCount);
        }
        timedoorId = null;
    }

    public void restoreFrom(ItemStack stack) {
        if (!stack.hasTag()) return;
        CompoundTag tag = stack.getTag();
        chronons = Math.max(0, Math.min(getCapacity(), tag.getInt("Chronons")));
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : owner;
        target = ProjectorData.getTarget(stack);
        markChanged();
    }

    public void writeTo(ItemStack stack) {
        if (chronons > 0) stack.getOrCreateTag().putInt("Chronons", chronons);
        if (owner != null) stack.getOrCreateTag().putUUID("Owner", owner);
        ProjectorData.setTarget(stack, target);
    }

    private void markChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Chronons", chronons);
        if (owner != null) tag.putUUID("Owner", owner);
        if (target != null) tag.put("PortalTarget", target.toTag());
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        chronons = Math.max(0, Math.min(getCapacity(), tag.getInt("Chronons")));
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        try {
            target = tag.contains("PortalTarget") ? LocationData.fromTag(tag.getCompound("PortalTarget")) : null;
        } catch (RuntimeException malformed) {
            target = null;
        }
        timedoorId = null;
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag tag = new CompoundTag(); saveAdditional(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
