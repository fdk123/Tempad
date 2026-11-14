package me.codexadrian.tempad.common.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.registry.TempadRegistry;
import me.codexadrian.tempad.common.utils.TeleportUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

public class TimedoorEntity extends Entity {
    public static final int ANIMATION_LENGTH = 8;
    private static final EntityDataAccessor<Integer> CLOSING_TIME = SynchedEntityData.defineId(TimedoorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(TimedoorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLOOR_MODE = SynchedEntityData.defineId(TimedoorEntity.class, EntityDataSerializers.BOOLEAN);
    private LocationData locationData = null;
    private UUID owner = null;
    private UUID linkedPortalId = null;
    private TimedoorEntity linkedPortalEntity = null;
    private int lifetimeTicks = TempadConfig.timedoorWait;

    public TimedoorEntity(EntityType<TimedoorEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(CLOSING_TIME, TempadConfig.timedoorWait);
        entityData.define(COLOR, Tempad.ORANGE);
        entityData.define(FLOOR_MODE, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        if (compoundTag.contains("location"))
            this.setLocation(LocationData.fromTag(compoundTag.getCompound("location")));
        this.setClosingTime(compoundTag.getInt("closing_time"));
        lifetimeTicks = compoundTag.contains("lifetime_ticks") ? Math.max(1, compoundTag.getInt("lifetime_ticks")) : TempadConfig.timedoorWait;
        if (compoundTag.hasUUID("owner")) this.setOwner(compoundTag.getUUID("owner"));
        this.setColor(compoundTag.getInt("outline_color"));
        this.setFloorMode(compoundTag.getBoolean("floor_mode"));
        if (compoundTag.contains("linked_portal")) {
            this.setLinkedPortalId(compoundTag.getUUID("linked_portal"));
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        if (locationData != null) {
            compoundTag.put("location", locationData.toTag());
        }
        compoundTag.putInt("closing_time", getClosingTime());
        compoundTag.putInt("lifetime_ticks", lifetimeTicks);
        if (owner != null) compoundTag.putUUID("owner", getOwner());
        compoundTag.putInt("outline_color", getColor());
        compoundTag.putBoolean("floor_mode", isFloorMode());
        if (getLinkedPortalId() != null) {
            compoundTag.putUUID("linked_portal", getLinkedPortalId());
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void tick() {
        AABB box;
        if (isFloorMode()) {
            box = new AABB(getX() - 0.625, getY() - 0.1, getZ() - 0.625,
                getX() + 0.625, getY() + 0.3, getZ() + 0.625);
        } else {
            box = new AABB(getX() - 1.1, getY(), getZ() - 1.1,
                getX() + 1.1, getY() + 2.3, getZ() + 1.1);
        }
        if (getLocation() != null) {
            Predicate<Entity> b = (entity) -> (((entity instanceof LivingEntity || entity instanceof ItemEntity)
                && !entity.getType().is(Tempad.TEMPAD_ENTITY_BLACKLIST)) || entity.getType().is(Tempad.TEMPAD_ENTITY_WHITELIST))
                && isInsideDoor(entity);
            List<Entity> entities = this.level().getEntitiesOfClass(Entity.class, box, b);
            boolean destinationAllowed = !TeleportUtils.isDimensionBlacklisted(this.level(), getLocation().getLevelKey());
            if (destinationAllowed && !entities.isEmpty() && !level().isClientSide()) {
                ServerLevel destinationLevel = Objects.requireNonNull(level().getServer()).getLevel(getLocation().getLevelKey());
                if (destinationLevel != null) {
                    entities.stream().flatMap(entity -> entity.getRootVehicle().getSelfAndPassengers()).distinct().forEach(entity -> {
                        entity.ejectPassengers();
                        Vec3 deltaMovement = entity.getDeltaMovement();
                        var pos = getLocation().getBlockPos();
                        Vec3 arrival = getLocation().getArrivalPosition();
                        if (!getLocation().getLevelKey().location().equals(this.level().dimension().location())) {
                            if (getLocation().hasPreciseArrival()) teleportEntityPrecise(destinationLevel, arrival, getLocation().getArrivalYaw(), deltaMovement, entity);
                            else teleportEntity(destinationLevel, pos, deltaMovement, entity);
                        } else {
                            if (getLocation().hasPreciseArrival()) entity.setYRot(getLocation().getArrivalYaw());
                            entity.teleportToWithTicket(arrival.x, arrival.y, arrival.z);
                            entity.setDeltaMovement(deltaMovement);
                            entity.hasImpulse = true;
                        }
                        TimedoorEntity linked = getLinkedPortalEntity();
                        if (linked != null) linked.resetClosingTime();
                        this.resetClosingTime();
                        if (entity instanceof Player player && player.getUUID().equals(getOwner())) {
                            this.setClosingTime(this.tickCount + TempadConfig.timedoorAddWaitTime);
                            if (linked != null) linked.setClosingTime(linked.tickCount + TempadConfig.timedoorAddWaitTime);
                        }
                    });
                    if (getLinkedPortalEntity() == null) {
                        TimedoorEntity recipientPortal = new TimedoorEntity(TempadRegistry.TIMEDOOR_ENTITY.get(), destinationLevel);
                        recipientPortal.setOwner(this.getOwner());
                        recipientPortal.setClosingTime(TempadConfig.timedoorAddWaitTime);
                        recipientPortal.setLocation(null);
                        recipientPortal.setColor(this.getColor());
                        recipientPortal.setFloorMode(isFloorMode());
                        recipientPortal.setLifetimeTicks(lifetimeTicks);
                        if (isFloorMode()) {
                            Vec3 arrival = getLocation().getArrivalPosition();
                            recipientPortal.setPos(arrival.x, arrival.y + 2.4, arrival.z);
                        } else if (getLocation().hasPreciseArrival()) {
                            Vec3 arrival = getLocation().getArrivalPosition();
                            recipientPortal.setPos(arrival.x + this.getDirection().getStepX(), arrival.y, arrival.z + this.getDirection().getStepZ());
                        } else {
                            var position = getLocation().getBlockPos().relative(this.getDirection(), 1);
                            recipientPortal.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
                        }
                        recipientPortal.setYRot(this.getYRot());
                        if (destinationLevel.addFreshEntity(recipientPortal)) {
                            this.setLinkedPortalId(recipientPortal.getUUID());
                            recipientPortal.setLinkedPortalId(this.getUUID());
                        }
                    }
                }
            }
        }
        if (this.tickCount > getClosingTime() + ANIMATION_LENGTH && getClosingTime() != -1) {
            if (this.getLinkedPortalEntity() != null) this.getLinkedPortalEntity().setLinkedPortalId(null);
            this.setLinkedPortalId(null);
            this.discard();
        }

    }

    public void setLocation(LocationData location) {
        this.locationData = location;
    }

    @Nullable
    public LocationData getLocation() {
        return locationData;
    }

    public int getClosingTime() {
        return entityData.get(CLOSING_TIME);
    }

    public void setClosingTime(int closingTime) {
        entityData.set(CLOSING_TIME, closingTime);
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public UUID getLinkedPortalId() {
        return linkedPortalId;
    }

    @ExpectPlatform
    public static void teleportEntity(ServerLevel destinationLevel, BlockPos pos, Vec3 deltaMovement, Entity entity) {
        throw new NotImplementedException();
    }

    @ExpectPlatform
    public static void teleportEntityPrecise(ServerLevel destinationLevel, Vec3 pos, float yaw, Vec3 deltaMovement, Entity entity) {
        throw new NotImplementedException();
    }

    public TimedoorEntity getLinkedPortalEntity() {
        if (linkedPortalId == null || level().isClientSide()) return null;
        if (linkedPortalEntity != null && !linkedPortalEntity.isRemoved()) return linkedPortalEntity;
        linkedPortalEntity = null;
        if (level().getServer() != null) {
            for (ServerLevel serverLevel : level().getServer().getAllLevels()) {
                if (serverLevel.getEntity(linkedPortalId) instanceof TimedoorEntity portal) {
                    linkedPortalEntity = portal;
                    break;
                }
            }
        }
        return linkedPortalEntity;
    }

    public void setLinkedPortalId(UUID id) {
        this.linkedPortalId = id;
        this.linkedPortalEntity = null;
    }

    public void setColor(int color) {
        entityData.set(COLOR, color);
    }

    public int getColor() {
        return entityData.get(COLOR);
    }

    public boolean isFloorMode() { return entityData.get(FLOOR_MODE); }
    public void setFloorMode(boolean floor) { entityData.set(FLOOR_MODE, floor); }

    private boolean isInsideDoor(Entity entity) {
        if (isFloorMode()) return true;
        double angle = Math.toRadians(getYRot());
        double dx = entity.getX() - getX();
        double dz = entity.getZ() - getZ();
        double normal = dx * Math.sin(angle) + dz * Math.cos(angle);
        double lateral = dx * Math.cos(angle) - dz * Math.sin(angle);
        double radius = entity.getBbWidth() * 0.5;
        return Math.abs(normal) <= 0.2 + radius && Math.abs(lateral) <= 0.7 + radius;
    }

    public void resetClosingTime() {
        if (getClosingTime() != -1) {
            this.setClosingTime(this.tickCount + lifetimeTicks);
        }
    }

    public void setLifetimeTicks(int ticks) {
        lifetimeTicks = Math.max(1, ticks);
        resetClosingTime();
    }
}
