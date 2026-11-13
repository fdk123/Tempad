package me.codexadrian.tempad.common.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class LocationData implements Comparable<LocationData>{

    private final UUID id;
    private final String name;
    private final BlockPos blockPos;
    private final ResourceKey<Level> levelKey;
    private boolean isTeleportable;
    private boolean isDeletable;
    private boolean isDownloadable;
    @Nullable private final Vec3 arrivalPosition;
    private final float arrivalYaw;
    private final int portalColor;

    public LocationData(String name, @Nullable ResourceKey<Level> levelKey, BlockPos pos, UUID uuid, boolean isTeleportable, boolean isDeletable, boolean isDownloadable) {
        this(name, levelKey, pos, uuid, isTeleportable, isDeletable, isDownloadable, null, 0.0F, -1);
    }

    public LocationData(String name, @Nullable ResourceKey<Level> levelKey, BlockPos pos, UUID uuid, boolean isTeleportable, boolean isDeletable, boolean isDownloadable, @Nullable Vec3 arrivalPosition, float arrivalYaw, int portalColor) {
        this.name = name;
        this.levelKey = levelKey;
        this.blockPos = pos;
        this.id = uuid;
        this.isTeleportable = isTeleportable;
        this.isDeletable = isDeletable;
        this.isDownloadable = isDownloadable;
        this.arrivalPosition = arrivalPosition;
        this.arrivalYaw = arrivalYaw;
        this.portalColor = portalColor;
    }

    public LocationData(String name, @Nullable ResourceKey<Level> levelKey, BlockPos pos, UUID uuid) {
        this(name, levelKey, pos, uuid, true, true, true);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", id);
        tag.putString("name", name);
        tag.put("blockpos", NbtUtils.writeBlockPos(blockPos));
        if (arrivalPosition != null) {
            tag.putDouble("arrivalX", arrivalPosition.x);
            tag.putDouble("arrivalY", arrivalPosition.y);
            tag.putDouble("arrivalZ", arrivalPosition.z);
            tag.putFloat("arrivalYaw", arrivalYaw);
        }
        if (portalColor >= 0) tag.putInt("portalColor", portalColor);

        if(levelKey != null) {
            Optional<Tag> optTag = Level.RESOURCE_KEY_CODEC.encodeStart(NbtOps.INSTANCE, levelKey).result();
            tag.put("levelKey", optTag.get());
        }

        return tag;
    }

    public static LocationData fromTag(CompoundTag tag) {
        UUID uuid = tag.getUUID("uuid");
        String name = tag.getString("name");
        BlockPos pos = NbtUtils.readBlockPos(tag.getCompound("blockpos"));

        ResourceKey<Level> key = null;
        if (tag.contains("levelKey")) {
            Optional<ResourceKey<Level>> optKey = Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, tag.get("levelKey")).result();
            key = optKey.get();
        }

        Vec3 arrival = tag.contains("arrivalX", Tag.TAG_DOUBLE) && tag.contains("arrivalY", Tag.TAG_DOUBLE) && tag.contains("arrivalZ", Tag.TAG_DOUBLE)
            ? new Vec3(tag.getDouble("arrivalX"), tag.getDouble("arrivalY"), tag.getDouble("arrivalZ")) : null;
        return new LocationData(name, key, pos, uuid, true, true, true, arrival, tag.getFloat("arrivalYaw"), tag.contains("portalColor") ? tag.getInt("portalColor") : -1);
    }

    public UUID getId() {
        return id;
    }

    @Nullable
    public ResourceKey<Level> getLevelKey() {
        return levelKey;
    }

    public String getName() {
        return name;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public boolean hasPreciseArrival() { return arrivalPosition != null; }
    public Vec3 getArrivalPosition() { return arrivalPosition != null ? arrivalPosition : Vec3.atCenterOf(blockPos); }
    public float getArrivalYaw() { return arrivalYaw; }
    public int getPortalColor() { return portalColor; }

    public boolean isTeleportable() {
        return isTeleportable;
    }

    public boolean isDeletable() {
        return isDeletable;
    }

    public boolean isDownloadable() {
        return isDownloadable;
    }

    @Override
    public int compareTo(@NotNull LocationData o) {
        return getName().compareTo(o.getName());
    }
}
