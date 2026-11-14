package me.codexadrian.tempad.common.compat.jade;

import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.block.MetronomeBlock;
import me.codexadrian.tempad.common.block.MetronomeBlockEntity;
import me.codexadrian.tempad.common.block.TimedoorMarkerBlock;
import me.codexadrian.tempad.common.block.TimedoorMarkerBlockEntity;
import me.codexadrian.tempad.common.block.TimedoorProjectorBlock;
import me.codexadrian.tempad.common.block.TimedoorProjectorBlockEntity;
import me.codexadrian.tempad.common.block.WorkstationBlock;
import me.codexadrian.tempad.common.block.WorkstationBlockEntity;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.data.MarkerIndex;
import me.codexadrian.tempad.common.entity.TimedoorEntity;
import me.codexadrian.tempad.common.items.ChrononContainerItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import java.util.UUID;

/** Optional Jade 1.20.1 readouts for the devices represented in the 3.0 plugin. */
@WailaPlugin
public final class TempadJadePlugin implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MetronomeInfo.INSTANCE, MetronomeBlockEntity.class);
        registration.registerBlockDataProvider(WorkstationInfo.INSTANCE, WorkstationBlockEntity.class);
        registration.registerBlockDataProvider(ProjectorInfo.INSTANCE, TimedoorProjectorBlockEntity.class);
        registration.registerBlockDataProvider(MarkerInfo.INSTANCE, TimedoorMarkerBlockEntity.class);
        registration.registerEntityDataProvider(TimedoorInfo.INSTANCE, TimedoorEntity.class);
    }

    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MetronomeInfo.INSTANCE, MetronomeBlock.class);
        registration.registerBlockComponent(WorkstationInfo.INSTANCE, WorkstationBlock.class);
        registration.registerBlockComponent(ProjectorInfo.INSTANCE, TimedoorProjectorBlock.class);
        registration.registerBlockComponent(MarkerInfo.INSTANCE, TimedoorMarkerBlock.class);
        registration.registerEntityComponent(TimedoorInfo.INSTANCE, TimedoorEntity.class);
    }

    private static ResourceLocation id(String path) { return new ResourceLocation(Tempad.MODID, "jade_" + path); }

    private static void addOwner(ITooltip tooltip, CompoundTag data) {
        if (data.contains("Owner")) tooltip.add(Component.translatable("jade.tempad.owner", data.getString("Owner")));
    }

    private static void saveOwner(CompoundTag data, BlockAccessor accessor, @Nullable UUID owner) {
        if (owner == null) return;
        ServerPlayer online = accessor.getLevel().getServer() == null ? null
            : accessor.getLevel().getServer().getPlayerList().getPlayer(owner);
        data.putString("Owner", online == null ? owner.toString() : online.getGameProfile().getName());
    }

    private static void addCharge(ITooltip tooltip, CompoundTag data) {
        if (data.contains("Capacity"))
            tooltip.add(Component.translatable("jade.tempad.chronons", data.getInt("Charge"), data.getInt("Capacity")));
    }

    private static void saveCharge(CompoundTag data, int charge, int capacity) {
        data.putInt("Charge", Math.max(0, charge));
        data.putInt("Capacity", Math.max(1, capacity));
    }

    private static void saveTarget(CompoundTag data, LocationData target) {
        if (target == null || target.getLevelKey() == null) return;
        data.putString("Target", target.getName());
        data.putString("Dimension", target.getLevelKey().location().toString());
        data.putInt("X", target.getBlockPos().getX());
        data.putInt("Y", target.getBlockPos().getY());
        data.putInt("Z", target.getBlockPos().getZ());
    }

    private static void addTarget(ITooltip tooltip, CompoundTag data) {
        if (!data.contains("Target")) return;
        tooltip.add(Component.translatable("jade.tempad.target", data.getString("Target")));
        ResourceLocation dimension = ResourceLocation.tryParse(data.getString("Dimension"));
        if (dimension != null) tooltip.add(Component.translatable("jade.tempad.destination",
            Component.translatable(dimension.toLanguageKey("dimension")), data.getInt("X"), data.getInt("Y"), data.getInt("Z")));
    }

    private enum MetronomeInfo implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;
        @Override public ResourceLocation getUid() { return id("metronome"); }
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof MetronomeBlockEntity metronome)) return;
            saveCharge(data, metronome.getChronons(), metronome.getCapacity());
            data.putInt("BootTime", metronome.getBootTime());
            data.putBoolean("Locked", metronome.isLocked());
            saveOwner(data, accessor, metronome.getOwner());
        }
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains("BootTime") && data.getInt("BootTime") > 0)
                tooltip.add(Component.translatable("jade.tempad.metronome.booting", 100 - data.getInt("BootTime")));
            addCharge(tooltip, data);
            if (data.contains("Locked")) tooltip.add(Component.translatable(data.getBoolean("Locked")
                ? "jade.tempad.locked" : "jade.tempad.public"));
            addOwner(tooltip, data);
        }
    }

    private enum WorkstationInfo implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;
        @Override public ResourceLocation getUid() { return id("workstation"); }
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof WorkstationBlockEntity station)) return;
            data.putBoolean("HasTempad", !station.getTempad().isEmpty());
            data.putString("Status", station.getStatus());
            data.putInt("Download", station.getDownloadTime());
            data.putInt("DownloadMax", station.getMaxDownloadTime());
            ItemStack tempad = station.getTempad();
            if (tempad.getItem() instanceof ChrononContainerItem container)
                saveCharge(data, container.getChronons(tempad), container.getChrononCapacity(tempad));
            saveOwner(data, accessor, station.getOwner());
        }
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("HasTempad")) return;
            tooltip.add(Component.translatable(data.getBoolean("HasTempad")
                ? "jade.tempad.workstation.loaded" : "jade.tempad.workstation.empty"));
            int maximum = data.getInt("DownloadMax");
            if (maximum > 0) tooltip.add(Component.translatable("jade.tempad.workstation.installing",
                Math.max(0, Math.min(100, (maximum - data.getInt("Download")) * 100 / maximum))));
            else tooltip.add(Component.translatable("jade.tempad.workstation.status",
                Component.translatable("block.tempad.workstation.status." + data.getString("Status"))));
            addCharge(tooltip, data);
            addOwner(tooltip, data);
        }
    }

    private enum ProjectorInfo implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;
        @Override public ResourceLocation getUid() { return id("projector"); }
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof TimedoorProjectorBlockEntity projector)) return;
            saveCharge(data, projector.getChronons(), projector.getCapacity());
            saveOwner(data, accessor, projector.getOwner());
            if (!projector.canAccess(accessor.getPlayer())) return;
            LocationData target = projector.getMarkerLink() == null ? projector.getTarget()
                : MarkerIndex.resolve(accessor.getLevel(), accessor.getPlayer().getUUID(), projector.getMarkerLink());
            saveTarget(data, target);
        }
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            addTarget(tooltip, data);
            addCharge(tooltip, data);
            addOwner(tooltip, data);
        }
    }

    private enum MarkerInfo implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;
        @Override public ResourceLocation getUid() { return id("marker"); }
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof TimedoorMarkerBlockEntity marker)) return;
            MarkerIndex.Entry snapshot = marker.snapshot();
            data.putString("Name", snapshot.name());
            data.putString("Access", snapshot.accessMode());
            if (accessor.showDetails()) data.putUUID("MarkerId", marker.getMarkerId());
            saveOwner(data, accessor, marker.getOwner());
        }
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains("Name")) tooltip.add(Component.literal(data.getString("Name")));
            if (data.contains("Access")) tooltip.add(Component.translatable("jade.tempad.access",
                Component.translatable("screen.tempad.marker." + data.getString("Access"))));
            if (data.hasUUID("MarkerId")) tooltip.add(Component.translatable("jade.tempad.marker_id", data.getUUID("MarkerId")));
            addOwner(tooltip, data);
        }
    }

    private enum TimedoorInfo implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
        INSTANCE;
        @Override public ResourceLocation getUid() { return id("timedoor"); }
        @Override public void appendServerData(CompoundTag data, EntityAccessor accessor) {
            if (!(accessor.getEntity() instanceof TimedoorEntity door)) return;
            int closing = door.getClosingTime();
            data.putInt("Remaining", closing < 0 ? -1 : Math.max(0, closing - door.tickCount));
            saveTarget(data, door.getLocation());
        }
        @Override public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains("Remaining")) {
                int remaining = data.getInt("Remaining");
                tooltip.add(remaining < 0 ? Component.translatable("jade.tempad.timedoor.open")
                    : Component.translatable("jade.tempad.timedoor.closes", (remaining + 19) / 20));
            }
            addTarget(tooltip, data);
        }
    }
}
