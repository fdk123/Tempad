package me.codexadrian.tempad.common.data;

import dev.architectury.injectables.annotations.ExpectPlatform;
import me.codexadrian.tempad.common.items.LocationBroadcasterItem;
import me.codexadrian.tempad.common.items.ScreeningDeviceItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Server-side visibility supplied by carried or equipped access devices. */
public final class PlayerLocationAccess {
    private PlayerLocationAccess() {}

    public static boolean allows(ServerPlayer owner, UUID accessor, ItemStack stack) {
        if (LocationBroadcasterItem.enabled(stack)) return true;
        if (!(stack.getItem() instanceof ScreeningDeviceItem)) return false;
        return switch (ScreeningDeviceItem.mode(stack)) {
            case ScreeningDeviceItem.PUBLIC -> true;
            case ScreeningDeviceItem.FTB_TEAMS -> sameFtbTeam(owner.getUUID(), accessor);
            default -> false;
        };
    }

    public static boolean visibleTo(ServerPlayer owner, UUID accessor) {
        if (owner.isSpectator() || owner.getUUID().equals(accessor)) return false;
        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            if (allows(owner, accessor, owner.getInventory().getItem(slot))) return true;
        }
        return hasCurioAccess(owner, accessor);
    }

    @ExpectPlatform public static boolean hasCurioAccess(ServerPlayer owner, UUID accessor) {
        throw new AssertionError("Platform implementation missing");
    }

    @ExpectPlatform public static boolean sameFtbTeam(UUID owner, UUID accessor) {
        throw new AssertionError("Platform implementation missing");
    }
}
