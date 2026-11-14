package me.codexadrian.tempad.common.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.data.MarkerIndex;
import me.codexadrian.tempad.common.data.PlayerLocationHandler;
import me.codexadrian.tempad.common.data.WalletLocationHandler;
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
    private UUID targetPlayer = null;
    private UUID targetMarker = null;
    private UUID targetWallet = null;
    private UUID linkedPortalId = null;
    private TimedoorEntity linkedPortalEntity = null;
    private int missingLinkTicks = 0;
    private boolean receivingPortal = false;
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
        this.tickCount = Math.max(0, compoundTag.getInt("age"));
        if (compoundTag.contains("location"))
            this.setLocation(LocationData.fromTag(compoundTag.getCompound("location")));
        this.setClosingTime(compoundTag.getInt("closing_time"));
        lifetimeTicks = compoundTag.contains("lifetime_ticks") ? Math.max(1, compoundTag.getInt("lifetime_ticks")) : TempadConfig.timedoorWait;
        if (compoundTag.hasUUID("owner")) this.setOwner(compoundTag.getUUID("owner"));
        if (compoundTag.hasUUID("target_player")) this.setTargetPlayer(compoundTag.getUUID("target_player"));
        if (compoundTag.hasUUID("target_marker")) this.setTargetMarker(compoundTag.getUUID("target_marker"));
        if (compoundTag.hasUUID("target_wallet")) this.setTargetWallet(compoundTag.getUUID("target_wallet"));
        this.setColor(compoundTag.getInt("outline_color"));
        this.setFloorMode(compoundTag.getBoolean("floor_mode"));
        if (compoundTag.contains("linked_portal")) {
            this.setLinkedPortalId(compoundTag.getUUID("linked_portal"));
        }
        receivingPortal = compoundTag.getBoolean("receiving_portal");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        compoundTag.putInt("age", tickCount);
        if (locationData != null) {
            compoundTag.put("location", locationData.toTag());
        }
        compoundTag.putInt("closing_time", getClosingTime());
        compoundTag.putInt("lifetime_ticks", lifetimeTicks);
        if (owner != null) compoundTag.putUUID("owner", getOwner());
        if (targetPlayer != null) compoundTag.putUUID("target_player", targetPlayer);
        if (targetMarker != null) compoundTag.putUUID("target_marker", targetMarker);
        if (targetWallet != null) compoundTag.putUUID("target_wallet", targetWallet);
        compoundTag.putInt("outline_color", getColor());
        compoundTag.putBoolean("floor_mode", isFloorMode());
        if (getLinkedPortalId() != null) {
            compoundTag.putUUID("linked_portal", getLinkedPortalId());
        }
        compoundTag.putBoolean("receiving_portal", receivingPortal);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        if ((targetPlayer != null || targetMarker != null || targetWallet != null) && !refreshDynamicTarget()) {
            if (tickCount > getClosingTime() + ANIMATION_LENGTH) closePair();
            return;
        }
        if (getClosingTime() != -1 && tickCount > getClosingTime() + ANIMATION_LENGTH) {
            closePair();
            return;
        }
        if (!receivingPortal && getLocation() != null && TeleportUtils.mayTravel(level(), getLocation().getLevelKey()))
            ensureReceivingPortal();
        if (tickCount < 26 + ANIMATION_LENGTH || (getClosingTime() != -1 && tickCount >= getClosingTime())) return;
        AABB box;
        if (isFloorMode()) {
            box = new AABB(getX() - 0.625, getY() - 0.1, getZ() - 0.625,
                getX() + 0.625, getY() + 0.3, getZ() + 0.625);
        } else {
            box = new AABB(getX() - 1.1, getY(), getZ() - 1.1,
                getX() + 1.1, getY() + 2.3, getZ() + 1.1);
        }
        if (getLocation() != null) {
            TimedoorEntity existingExit = getLinkedPortalEntity();
            if (!receivingPortal && existingExit != null && existingExit.getLocation() == null) {
                existingExit.setLocation(returnLocation());
                existingExit.receivingPortal = true;
                existingExit.setYRot(getLocation().hasPreciseArrival() ? getLocation().getArrivalYaw() + 180.0F : getYRot());
            }
            Predicate<Entity> b = (entity) -> (((entity instanceof LivingEntity || entity instanceof ItemEntity)
                && !entity.getType().is(Tempad.TEMPAD_ENTITY_BLACKLIST)) || entity.getType().is(Tempad.TEMPAD_ENTITY_WHITELIST))
                && !entity.getType().is(Tempad.TELEPORTING_NOT_SUPPORTED)
                && isInsideDoor(entity) && !isOnCooldown(entity);
            List<Entity> entities = this.level().getEntitiesOfClass(Entity.class, box, b);
            boolean destinationAllowed = receivingPortal || TeleportUtils.mayTravel(this.level(), getLocation().getLevelKey());
            if (destinationAllowed && !entities.isEmpty() && !level().isClientSide()) {
                ServerLevel destinationLevel = Objects.requireNonNull(level().getServer()).getLevel(getLocation().getLevelKey());
                if (destinationLevel != null && destinationLevel.getWorldBorder().isWithinBounds(BlockPos.containing(getLocation().getArrivalPosition()))) {
                    List<Entity> travelers = entities.stream().map(Entity::getRootVehicle).distinct()
                        .flatMap(Entity::getSelfAndPassengers).distinct()
                        .filter(entity -> !entity.getType().is(Tempad.TELEPORTING_NOT_SUPPORTED) && !isOnCooldown(entity)).toList();
                    for (Entity entity : travelers) {
                        if (!TimedoorEvents.canEnter(this, entity)) continue;
                        entity.ejectPassengers();
                        Vec3 deltaMovement = entity.getDeltaMovement();
                        var pos = getLocation().getBlockPos();
                        Vec3 arrival = getLocation().getArrivalPosition();
                        float arrivalYaw = getLocation().hasPreciseArrival() ? getLocation().getArrivalYaw() : entity.getYRot();
                        if (getLocation().hasPreciseArrival()) {
                            deltaMovement = deltaMovement.yRot((float) Math.toRadians(getYRot() - arrivalYaw));
                        }
                        int cooldownTicks = level().dimension().equals(destinationLevel.dimension()) ? 30 : 60;
                        setCooldown(entity, cooldownTicks);
                        Entity teleported;
                        if (!getLocation().getLevelKey().location().equals(this.level().dimension().location())) {
                            if (getLocation().hasPreciseArrival()) teleported = teleportEntityPrecise(destinationLevel, arrival, arrivalYaw, deltaMovement, entity);
                            else teleported = teleportEntity(destinationLevel, pos, deltaMovement, entity);
                        } else {
                            if (getLocation().hasPreciseArrival()) entity.setYRot(arrivalYaw);
                            entity.teleportToWithTicket(arrival.x, arrival.y, arrival.z);
                            entity.setDeltaMovement(deltaMovement);
                            entity.hasImpulse = true;
                            teleported = entity;
                        }
                        if (teleported == null) continue;
                        setCooldown(teleported, cooldownTicks);
                        TimedoorEntity linked = getLinkedPortalEntity();
                        if (linked != null) TimedoorEvents.exited(linked, teleported);
                        int minimumReturnTime = cooldownTicks + ANIMATION_LENGTH + 20;
                        if (getClosingTime() != -1)
                            setClosingTime(tickCount + Math.max(lifetimeTicks, minimumReturnTime));
                        if (linked != null && linked.getClosingTime() != -1)
                            linked.setClosingTime(linked.tickCount + Math.max(linked.lifetimeTicks, minimumReturnTime));
                        if (teleported instanceof Player player && player.getUUID().equals(getOwner())) {
                            if (getClosingTime() != -1)
                                this.setClosingTime(this.tickCount + Math.max(TempadConfig.timedoorAddWaitTime, minimumReturnTime));
                            if (linked != null && linked.getClosingTime() != -1)
                                linked.setClosingTime(linked.tickCount + Math.max(TempadConfig.timedoorAddWaitTime, minimumReturnTime));
                        }
                    }
                }
            }
        }
    }

    private void ensureReceivingPortal() {
        if (receivingPortal || getLocation() == null || level().getServer() == null) return;
        ServerLevel destinationLevel = level().getServer().getLevel(getLocation().getLevelKey());
        if (destinationLevel == null) return;
        Vec3 exitPos = recipientPortalPosition(getLocation());
        if (!destinationLevel.getWorldBorder().isWithinBounds(BlockPos.containing(exitPos))) return;
        destinationLevel.getChunkAt(BlockPos.containing(exitPos));
        if (getLinkedPortalEntity() != null) {
            missingLinkTicks = 0;
            return;
        }
        if (getLinkedPortalId() != null && ++missingLinkTicks < 20) return;
        missingLinkTicks = 0;
        setLinkedPortalId(null);
        TimedoorEntity recipientPortal = new TimedoorEntity(TempadRegistry.TIMEDOOR_ENTITY.get(), destinationLevel);
        recipientPortal.setOwner(getOwner());
        recipientPortal.setLocation(returnLocation());
        recipientPortal.setColor(getColor());
        recipientPortal.setFloorMode(isFloorMode());
        recipientPortal.setLifetimeTicks(lifetimeTicks);
        recipientPortal.setClosingTime(getClosingTime() == -1 ? -1
            : recipientPortal.tickCount + Math.max(1, getClosingTime() - tickCount));
        recipientPortal.setPos(exitPos);
        recipientPortal.setYRot(getLocation().hasPreciseArrival() ? getLocation().getArrivalYaw() + 180.0F : getYRot());
        recipientPortal.receivingPortal = true;
        if (destinationLevel.addFreshEntity(recipientPortal)) {
            setLinkedPortalId(recipientPortal.getUUID());
            recipientPortal.setLinkedPortalId(getUUID());
        }
    }

    private void closePair() {
        TimedoorEntity linked = getLinkedPortalEntity();
        if (linked != null) {
            linked.setLinkedPortalId(null);
            if (!receivingPortal) {
                TimedoorEvents.closed(linked);
                linked.discard();
            }
        }
        setLinkedPortalId(null);
        TimedoorEvents.closed(this);
        discard();
    }

    public void setLocation(LocationData location) {
        this.locationData = location;
    }

    public void setTargetPlayer(@Nullable UUID player) { targetPlayer = player; }
    public void setTargetMarker(@Nullable UUID marker) { targetMarker = marker; }
    public void setTargetWallet(@Nullable UUID wallet) { targetWallet = wallet; }

    private boolean refreshDynamicTarget() {
        if (!(level() instanceof ServerLevel serverLevel) || owner == null) return closeUnreachableTarget();
        var accessor = serverLevel.getServer().getPlayerList().getPlayer(owner);
        LocationData live = targetPlayer != null ? PlayerLocationHandler.live(serverLevel, owner, targetPlayer)
            : targetMarker != null ? MarkerIndex.resolve(serverLevel, owner, targetMarker)
            : WalletLocationHandler.resolve(serverLevel, owner, targetWallet);
        if (accessor == null || live == null || !TeleportUtils.mayTeleport(live.getLevelKey(), accessor))
            return closeUnreachableTarget();
        setLocation(live);
        TimedoorEntity linked = getLinkedPortalEntity();
        if (linked != null && !linked.level().dimension().equals(live.getLevelKey())) {
            TimedoorEvents.closed(linked);
            linked.discard();
            setLinkedPortalId(null);
        } else if (linked != null) {
            placeRecipientPortal(linked, live);
            linked.setYRot(live.hasPreciseArrival() ? live.getArrivalYaw() + 180.0F : getYRot());
        }
        return true;
    }

    private boolean closeUnreachableTarget() {
        setLocation(null);
        if (getClosingTime() == -1 || getClosingTime() > tickCount) setClosingTime(tickCount);
        TimedoorEntity linked = getLinkedPortalEntity();
        if (linked != null && (linked.getClosingTime() == -1 || linked.getClosingTime() > linked.tickCount))
            linked.setClosingTime(linked.tickCount);
        return false;
    }

    private void placeRecipientPortal(TimedoorEntity recipient, LocationData location) {
        recipient.setPos(recipientPortalPosition(location));
    }

    private Vec3 recipientPortalPosition(LocationData location) {
        Vec3 arrival = location.getArrivalPosition();
        if (isFloorMode()) return arrival.add(0, 2.4, 0);
        else if (location.hasPreciseArrival())
            return arrival.add(getDirection().getStepX(), 0, getDirection().getStepZ());
        else {
            BlockPos position = location.getBlockPos().relative(getDirection(), 1);
            return new Vec3(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        }
    }

    private LocationData returnLocation() {
        double angle = Math.toRadians(getYRot() + 90.0F);
        Vec3 arrival = position().add(Math.cos(angle), 0, Math.sin(angle));
        if (!level().getWorldBorder().isWithinBounds(BlockPos.containing(arrival)))
            arrival = position().add(-Math.cos(angle), 0, -Math.sin(angle));
        if (!level().getWorldBorder().isWithinBounds(BlockPos.containing(arrival)))
            arrival = position();
        return new LocationData(level().dimension().location().toString(), level().dimension(),
            BlockPos.containing(arrival), UUID.randomUUID(), true, true, true, arrival, getYRot(), getColor());
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
    public static Entity teleportEntity(ServerLevel destinationLevel, BlockPos pos, Vec3 deltaMovement, Entity entity) {
        throw new NotImplementedException();
    }

    @ExpectPlatform
    public static Entity teleportEntityPrecise(ServerLevel destinationLevel, Vec3 pos, float yaw, Vec3 deltaMovement, Entity entity) {
        throw new NotImplementedException();
    }

    @ExpectPlatform
    public static boolean isOnCooldown(Entity entity) {
        throw new NotImplementedException();
    }

    @ExpectPlatform
    public static void setCooldown(Entity entity, int ticks) {
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
        missingLinkTicks = 0;
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
