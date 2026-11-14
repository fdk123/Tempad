package me.codexadrian.tempad.common.data;

import me.codexadrian.tempad.common.items.TempadUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Live, non-exportable destinations for players who allow access. */
public final class PlayerLocationHandler {
    private PlayerLocationHandler() {}

    public static UUID locationId(UUID player) {
        return UUID.nameUUIDFromBytes(("tempad:player:" + player).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isPlayerLocation(Level level, UUID location) {
        if (!(level instanceof ServerLevel serverLevel) || location == null) return false;
        return playerForLocation(serverLevel.getServer(), location) != null;
    }

    public static Map<UUID, LocationData> visibleLocations(Level level, UUID accessor) {
        if (!(level instanceof ServerLevel serverLevel)) return Map.of();
        ServerPlayer viewer = serverLevel.getServer().getPlayerList().getPlayer(accessor);
        if (viewer == null || !TempadUpgrades.canTeleportToPlayers(viewer)) return Map.of();
        Map<UUID, LocationData> locations = new HashMap<>();
        for (ServerPlayer target : serverLevel.getServer().getPlayerList().getPlayers()) {
            LocationData location = live(serverLevel, accessor, target.getUUID());
            if (location != null) locations.put(location.getId(), location);
        }
        return locations;
    }

    public static LocationData resolveForDevice(Level level, UUID accessor, UUID location) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        ServerPlayer target = playerForLocation(serverLevel.getServer(), location);
        return target == null ? null : live(serverLevel, accessor, target.getUUID());
    }

    public static UUID playerIdForLocation(Level level, UUID accessor, UUID location) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        ServerPlayer target = playerForLocation(serverLevel.getServer(), location);
        return target != null && live(serverLevel, accessor, target.getUUID()) != null ? target.getUUID() : null;
    }

    public static LocationData live(ServerLevel level, UUID accessor, UUID targetId) {
        if (accessor == null || targetId == null) return null;
        MinecraftServer server = level.getServer();
        ServerPlayer target = server.getPlayerList().getPlayer(targetId);
        if (target == null || !PlayerLocationAccess.visibleTo(target, accessor)) return null;
        return new LocationData("@" + target.getGameProfile().getName(), target.level().dimension(),
            BlockPos.containing(target.position()), locationId(targetId), true, false, false,
            target.position(), target.getYRot(), -1);
    }

    private static ServerPlayer playerForLocation(MinecraftServer server, UUID location) {
        if (location == null) return null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (locationId(player.getUUID()).equals(location)) return player;
        }
        return null;
    }
}
