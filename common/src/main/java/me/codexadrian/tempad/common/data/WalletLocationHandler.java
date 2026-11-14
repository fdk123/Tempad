package me.codexadrian.tempad.common.data;

import me.codexadrian.tempad.common.items.CardWalletItem;
import me.codexadrian.tempad.common.items.LocationCard;
import me.codexadrian.tempad.common.items.WalletContainer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** First carried wallet exposes its filled cards as live, non-exportable Tempad destinations. */
public final class WalletLocationHandler {
    private WalletLocationHandler() { }

    public static UUID locationId(UUID owner, int slot) {
        return UUID.nameUUIDFromBytes(("tempad:wallet:" + owner + ":" + slot).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isWalletLocation(UUID owner, UUID location) {
        if (owner == null || location == null) return false;
        for (int slot = 0; slot < 18; slot++) if (locationId(owner, slot).equals(location)) return true;
        return false;
    }

    public static Map<UUID, LocationData> visibleLocations(Level level, UUID owner) {
        if (!(level instanceof ServerLevel serverLevel)) return Map.of();
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(owner);
        if (player == null) return Map.of();
        ItemStack wallet = firstWallet(player);
        if (wallet.isEmpty()) return Map.of();
        WalletContainer contents = new WalletContainer(wallet);
        Map<UUID, LocationData> result = new HashMap<>();
        for (int slot = 0; slot < contents.getContainerSize(); slot++) {
            LocationData location = fromSlot(contents, slot, level, owner);
            if (location != null) result.put(location.getId(), location);
        }
        return result;
    }

    public static LocationData resolve(Level level, UUID owner, UUID locationId) {
        if (!(level instanceof ServerLevel serverLevel) || !isWalletLocation(owner, locationId)) return null;
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(owner);
        if (player == null) return null;
        ItemStack wallet = firstWallet(player);
        if (wallet.isEmpty()) return null;
        WalletContainer contents = new WalletContainer(wallet);
        for (int slot = 0; slot < contents.getContainerSize(); slot++) {
            if (locationId(owner, slot).equals(locationId)) return fromSlot(contents, slot, level, owner);
        }
        return null;
    }

    private static ItemStack firstWallet(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof CardWalletItem && !new WalletContainer(stack).isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static LocationData fromSlot(WalletContainer contents, int slot, Level level, UUID owner) {
        ItemStack card = contents.getItem(slot);
        if (!(card.getItem() instanceof LocationCard)) return null;
        LocationData target = LocationCard.resolveLocation(card, level, owner);
        if (target == null || target.getLevelKey() == null) return null;
        return new LocationData(target.getName(), target.getLevelKey(), target.getBlockPos(), locationId(owner, slot),
            target.isTeleportable(), false, false, target.hasPreciseArrival() ? target.getArrivalPosition() : null,
            target.getArrivalYaw(), target.getPortalColor());
    }
}
