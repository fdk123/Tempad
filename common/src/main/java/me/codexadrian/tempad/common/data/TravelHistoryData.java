package me.codexadrian.tempad.common.data;

import me.codexadrian.tempad.common.config.TempadConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Persistent per-player travel history for Time Twister and Tempad timeline. */
public final class TravelHistoryData extends SavedData {
    public static final ResourceLocation DEATH = new ResourceLocation("tempad", "death");
    public static final ResourceLocation RESPAWN = new ResourceLocation("tempad", "respawn");
    public static final ResourceLocation DIMENSION_EXIT = new ResourceLocation("tempad", "dimension_exit");
    public static final ResourceLocation DIMENSION_ENTER = new ResourceLocation("tempad", "dimension_enter");

    public record Entry(long time, @Nullable ResourceLocation marker, ResourceKey<Level> dimension, Vec3 position) {
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Time", time);
            if (marker != null) tag.putString("Marker", marker.toString());
            tag.putString("Dimension", dimension.location().toString());
            tag.putDouble("X", position.x);
            tag.putDouble("Y", position.y);
            tag.putDouble("Z", position.z);
            return tag;
        }

        @Nullable private static Entry load(CompoundTag tag) {
            try {
                ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
                if (dimension == null) return null;
                ResourceLocation marker = tag.contains("Marker") ? ResourceLocation.tryParse(tag.getString("Marker")) : null;
                Vec3 pos = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
                if (!Double.isFinite(pos.x) || !Double.isFinite(pos.y) || !Double.isFinite(pos.z)) return null;
                return new Entry(tag.getLong("Time"), marker, ResourceKey.create(Registries.DIMENSION, dimension), pos);
            } catch (RuntimeException malformed) { return null; }
        }
    }

    private final Map<UUID, TreeMap<Long, Entry>> histories = new java.util.HashMap<>();
    private TravelHistoryData() {}

    private static TravelHistoryData load(CompoundTag tag) {
        TravelHistoryData data = new TravelHistoryData();
        CompoundTag players = tag.getCompound("Players");
        for (String key : players.getAllKeys()) {
            try {
                UUID player = UUID.fromString(key);
                TreeMap<Long, Entry> history = new TreeMap<>();
                for (Tag element : players.getList(key, Tag.TAG_COMPOUND)) {
                    Entry entry = Entry.load((CompoundTag) element);
                    if (entry != null) history.put(entry.time(), entry);
                }
                data.histories.put(player, history);
            } catch (IllegalArgumentException ignored) {}
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        CompoundTag players = new CompoundTag();
        histories.forEach((player, history) -> {
            ListTag entries = new ListTag();
            history.values().forEach(entry -> entries.add(entry.save()));
            players.put(player.toString(), entries);
        });
        tag.put("Players", players);
        return tag;
    }

    public static TravelHistoryData get(ServerPlayer player) {
        return player.getServer().overworld().getDataStorage().computeIfAbsent(TravelHistoryData::load, TravelHistoryData::new, "tempad_travel_history");
    }

    public void log(ServerPlayer player, @Nullable ResourceLocation marker) {
        if (player.isSpectator() || player.getAbilities().instabuild) return;
        TreeMap<Long, Entry> history = histories.computeIfAbsent(player.getUUID(), ignored -> new TreeMap<>());
        if (marker == null && !history.isEmpty()) {
            Entry last = history.lastEntry().getValue();
            if (last.dimension().equals(player.level().dimension()) && last.position().distanceToSqr(player.position()) < 64) return;
        }
        long time = System.currentTimeMillis();
        while (history.containsKey(time)) time++;
        history.put(time, new Entry(time, marker, player.level().dimension(), player.position()));
        int max = Math.max(1, TempadConfig.maxHistorySize);
        while (history.size() > max) history.pollFirstEntry();
        setDirty();
    }

    public List<Entry> relevant(UUID player) {
        TreeMap<Long, Entry> history = histories.get(player);
        if (history == null) return List.of();
        List<Entry> result = new ArrayList<>();
        for (Entry entry : history.descendingMap().values()) {
            if (entry.marker() != null) result.add(entry);
            if (result.size() == 8) break;
        }
        return result;
    }

    @Nullable public Entry find(UUID player, long time) {
        TreeMap<Long, Entry> history = histories.get(player);
        Entry entry = history == null ? null : history.get(time);
        return entry != null && entry.marker() != null ? entry : null;
    }

    public boolean backtrack(ServerPlayer player, long time) {
        Entry entry = find(player.getUUID(), time);
        if (entry == null) return false;
        ServerLevel destination = player.getServer().getLevel(entry.dimension());
        if (destination == null || !destination.getWorldBorder().isWithinBounds(BlockPos.containing(entry.position()))) return false;
        player.teleportTo(destination, entry.position().x, entry.position().y, entry.position().z, player.getYRot(), player.getXRot());
        TreeMap<Long, Entry> history = histories.get(player.getUUID());
        if (history != null) history.tailMap(time, true).clear();
        setDirty();
        return true;
    }
}
