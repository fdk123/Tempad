package me.codexadrian.tempad.common.data;

import me.codexadrian.tempad.common.block.MetronomeBlockEntity;
import me.codexadrian.tempad.common.config.TempadConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The 3.0 Metronome pool: one persistent chronon store per owner across dimensions. */
public final class MetronomeNetworkData extends SavedData {
    private static final class Network {
        private final Set<GlobalPos> positions = new HashSet<>();
        private int stored;
    }

    private final Map<UUID, Network> networks = new HashMap<>();

    private MetronomeNetworkData() {}

    public static MetronomeNetworkData get(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().overworld().getDataStorage()
            .computeIfAbsent(MetronomeNetworkData::load, MetronomeNetworkData::new, "tempad_metronome_network");
    }

    private static MetronomeNetworkData load(CompoundTag tag) {
        MetronomeNetworkData data = new MetronomeNetworkData();
        for (Tag raw : tag.getList("Networks", Tag.TAG_COMPOUND)) {
            CompoundTag networkTag = (CompoundTag) raw;
            if (!networkTag.hasUUID("Owner")) continue;
            Network network = new Network();
            for (Tag positionRaw : networkTag.getList("Positions", Tag.TAG_COMPOUND)) {
                CompoundTag position = (CompoundTag) positionRaw;
                ResourceLocation dimension = ResourceLocation.tryParse(position.getString("Dimension"));
                if (dimension != null) network.positions.add(GlobalPos.of(
                    ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(position.getLong("Pos"))));
            }
            if (network.positions.isEmpty()) continue;
            network.stored = Math.min(capacity(network), Math.max(0, networkTag.getInt("Stored")));
            data.networks.put(networkTag.getUUID("Owner"), network);
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        networks.forEach((owner, network) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Owner", owner);
            entry.putInt("Stored", network.stored);
            ListTag positions = new ListTag();
            for (GlobalPos position : network.positions) {
                CompoundTag point = new CompoundTag();
                point.putString("Dimension", position.dimension().location().toString());
                point.putLong("Pos", position.pos().asLong());
                positions.add(point);
            }
            entry.put("Positions", positions);
            entries.add(entry);
        });
        tag.put("Networks", entries);
        return tag;
    }

    private static int capacity(Network network) {
        return (int) Math.min(Integer.MAX_VALUE, (long) network.positions.size() * Math.max(1, TempadConfig.metronomeCapacity));
    }

    public int stored(UUID owner) {
        Network network = networks.get(owner);
        return network == null ? 0 : network.stored;
    }

    public int capacity(UUID owner) {
        Network network = networks.get(owner);
        return network == null ? 0 : capacity(network);
    }

    public void register(UUID owner, GlobalPos pos, int initialChronons) {
        Network network = networks.computeIfAbsent(owner, ignored -> new Network());
        if (network.positions.add(pos)) {
            network.stored = (int) Math.min(capacity(network), (long) network.stored + Math.max(0, initialChronons));
            setDirty();
        }
    }

    /** Returns the fair share that travels with a broken Metronome, or null if it was not registered. */
    public Integer unregister(UUID owner, GlobalPos pos) {
        Network network = networks.get(owner);
        if (network == null || !network.positions.contains(pos)) return null;
        int share = network.positions.size() == 1 ? network.stored : network.stored / network.positions.size();
        network.stored -= share;
        network.positions.remove(pos);
        if (network.positions.isEmpty()) networks.remove(owner);
        setDirty();
        return share;
    }

    public int insert(UUID owner, int amount) {
        Network network = networks.get(owner);
        if (network == null || amount <= 0) return 0;
        int moved = (int) Math.min(amount, Math.max(0L, (long) capacity(network) - network.stored));
        if (moved > 0) { network.stored += moved; setDirty(); }
        return moved;
    }

    public int extract(UUID owner, int amount) {
        Network network = networks.get(owner);
        if (network == null || amount <= 0) return 0;
        int moved = Math.min(amount, network.stored);
        if (moved > 0) { network.stored -= moved; setDirty(); }
        return moved;
    }

    public void syncOwner(MinecraftServer server, UUID owner) {
        Network network = networks.get(owner);
        if (network == null) return;
        for (GlobalPos pos : network.positions) {
            ServerLevel level = server.getLevel(pos.dimension());
            if (level == null || !level.hasChunkAt(pos.pos())) continue;
            BlockState state = level.getBlockState(pos.pos());
            level.updateNeighbourForOutputSignal(pos.pos(), state.getBlock());
            level.sendBlockUpdated(pos.pos(), state, state, 3);
        }
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % Math.max(1, TempadConfig.metronomeGenerationInterval) != 0) return;
        MetronomeNetworkData data = get(server.overworld());
        if (data == null) return;
        for (Map.Entry<UUID, Network> entry : data.networks.entrySet()) {
            Network network = entry.getValue();
            int active = 0;
            for (GlobalPos pos : network.positions) {
                ServerLevel level = server.getLevel(pos.dimension());
                if (level != null && level.hasChunkAt(pos.pos())) {
                    if (!(level.getBlockEntity(pos.pos()) instanceof MetronomeBlockEntity metronome)
                            || !entry.getKey().equals(metronome.getOwner()) || !metronome.isBooted()
                            || (TempadConfig.metronomeRedstonePause && level.hasNeighborSignal(pos.pos()))) continue;
                }
                active++;
            }
            if (active == 0) continue;
            long generated = (long) Math.max(0, TempadConfig.metronomeGenerationAmount)
                * (TempadConfig.metronomeScaleGeneration ? active : 1);
            int next = (int) Math.min(capacity(network), (long) network.stored + generated);
            if (next != network.stored) {
                network.stored = next;
                data.setDirty();
                data.syncOwner(server, entry.getKey());
            }
        }
    }
}
