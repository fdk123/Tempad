package me.codexadrian.tempad.common.data.forge;

import me.codexadrian.tempad.common.data.PlayerLocationAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

/** Forge optional integrations for equipped access devices and FTB Teams. */
public final class PlayerLocationAccessImpl {
    private PlayerLocationAccessImpl() {}

    public static boolean hasCurioAccess(ServerPlayer owner, UUID accessor) {
        if (!ModList.get().isLoaded("curios")) return false;
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

    public static boolean sameFtbTeam(ServerPlayer owner, UUID accessor) {
        if (!ModList.get().isLoaded("ftbteams")) return false;
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.FTBTeamsAPIImpl");
            Object api = apiClass.getField("INSTANCE").get(null);
            Object manager = apiClass.getMethod("getManager").invoke(api);
            return Boolean.TRUE.equals(manager.getClass().getMethod("arePlayersInSameTeam", UUID.class, UUID.class)
                .invoke(manager, owner.getUUID(), accessor));
        } catch (ReflectiveOperationException | RuntimeException missingApi) {
            return false;
        }
    }
}
