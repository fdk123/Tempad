package me.codexadrian.tempad.common.data;

import me.codexadrian.tempad.common.block.TimedoorMarkerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cross-dimensional, persisted index of both 3.0 marker types. */
public final class MarkerIndex extends SavedData {
    private final Map<UUID, Entry> markers = new HashMap<>();

    public record Entry(UUID id, ResourceKey<Level> dimension, BlockPos blockPos, UUID owner,
                        String name, int color, boolean publicAccess, boolean locked,
                        boolean chronomark, Direction facing, boolean ceiling, float yOffset, int angle) {
        public boolean visibleTo(UUID player) { return publicAccess || owner.equals(player); }

        public LocationData location() {
            Vec3 arrival;
            float yaw;
            if (chronomark) {
                double rad = Math.toRadians(angle + 90.0);
                arrival = Vec3.atBottomCenterOf(blockPos).add(Math.cos(rad), yOffset, Math.sin(rad));
                yaw = angle;
            } else {
                arrival = Vec3.atBottomCenterOf(blockPos).add(facing.getStepX(), ceiling ? -2.0 : 0.5, facing.getStepZ());
                yaw = facing.toYRot();
            }
            return new LocationData(name, dimension, BlockPos.containing(arrival), id,
                true, false, true, arrival, yaw, color);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            tag.putString("Dimension", dimension.location().toString());
            tag.putLong("Pos", blockPos.asLong());
            tag.putUUID("Owner", owner);
            tag.putString("Name", name);
            tag.putInt("Color", color);
            tag.putBoolean("Public", publicAccess);
            tag.putBoolean("Locked", locked);
            tag.putBoolean("Chronomark", chronomark);
            tag.putInt("Facing", facing.get2DDataValue());
            tag.putBoolean("Ceiling", ceiling);
            tag.putFloat("YOffset", yOffset);
            tag.putInt("Angle", angle);
            return tag;
        }

        @Nullable public static Entry load(CompoundTag tag) {
            try {
                if (!tag.hasUUID("Id") || !tag.hasUUID("Owner")) return null;
                ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
                if (dimension == null) return null;
                return new Entry(tag.getUUID("Id"), ResourceKey.create(Registries.DIMENSION, dimension),
                    BlockPos.of(tag.getLong("Pos")), tag.getUUID("Owner"), tag.getString("Name"),
                    tag.getInt("Color"), tag.getBoolean("Public"), tag.getBoolean("Locked"),
                    tag.getBoolean("Chronomark"), Direction.from2DDataValue(tag.getInt("Facing")),
                    tag.getBoolean("Ceiling"), tag.getFloat("YOffset"), tag.getInt("Angle"));
            } catch (RuntimeException malformed) { return null; }
        }
    }

    private MarkerIndex() {}

    private static MarkerIndex load(CompoundTag tag) {
        MarkerIndex index = new MarkerIndex();
        for (Tag entry : tag.getList("Markers", Tag.TAG_COMPOUND)) {
            Entry marker = Entry.load((CompoundTag) entry);
            if (marker != null) index.markers.put(marker.id(), marker);
        }
        return index;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        markers.values().forEach(marker -> entries.add(marker.save()));
        tag.put("Markers", entries);
        return tag;
    }

    @Nullable public static MarkerIndex get(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().overworld().getDataStorage().computeIfAbsent(MarkerIndex::load, MarkerIndex::new, "tempad_markers");
    }

    public static Map<UUID, LocationData> visibleLocations(Level level, UUID player) {
        MarkerIndex index = get(level);
        Map<UUID, LocationData> visible = new HashMap<>();
        if (index != null) index.markers.forEach((id, marker) -> {
            if (marker.visibleTo(player)) visible.put(id, marker.location());
        });
        return visible;
    }

    @Nullable public static LocationData resolve(Level level, UUID player, UUID id) {
        MarkerIndex index = get(level);
        Entry entry = index == null ? null : index.markers.get(id);
        return entry != null && entry.visibleTo(player) ? entry.location() : null;
    }

    public static void publish(TimedoorMarkerBlockEntity marker) {
        if (marker.getLevel() == null || marker.getOwner() == null) return;
        MarkerIndex index = get(marker.getLevel());
        if (index == null) return;
        UUID id = marker.getMarkerId();
        Entry prior = index.markers.get(id);
        if (prior != null && (!prior.dimension().equals(marker.getLevel().dimension()) || !prior.blockPos().equals(marker.getBlockPos()))) {
            id = UUID.randomUUID();
            marker.setMarkerId(id);
        }
        index.markers.put(id, marker.snapshot());
        index.setDirty();
    }

    public static void remove(Level level, UUID id, BlockPos pos) {
        MarkerIndex index = get(level);
        if (index == null) return;
        Entry current = index.markers.get(id);
        if (current != null && current.dimension().equals(level.dimension()) && current.blockPos().equals(pos)) {
            index.markers.remove(id);
            index.setDirty();
        }
    }
}
