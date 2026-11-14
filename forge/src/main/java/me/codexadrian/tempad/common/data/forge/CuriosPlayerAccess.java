package me.codexadrian.tempad.common.data.forge;

import me.codexadrian.tempad.common.data.PlayerLocationAccess;
import net.minecraft.server.level.ServerPlayer;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

/** Loaded only when Curios is installed. */
final class CuriosPlayerAccess {
    private CuriosPlayerAccess() {}

    static boolean hasAccess(ServerPlayer owner, UUID accessor) {
        var optional = CuriosApi.getCuriosInventory(owner);
        if (!optional.isPresent()) return false;
        var inventory = optional.orElseThrow(IllegalStateException::new);
        for (var curio : inventory.getCurios().values()) {
            var stacks = curio.getStacks();
            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                if (PlayerLocationAccess.allows(owner, accessor, stacks.getStackInSlot(slot))) return true;
            }
        }
        return false;
    }
}
