package me.codexadrian.tempad.common.data.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

import java.util.UUID;

/** Forge optional integrations for equipped access devices and team access. */
public final class PlayerLocationAccessImpl {
    private PlayerLocationAccessImpl() {}

    public static boolean hasCurioAccess(ServerPlayer owner, UUID accessor) {
        return ModList.get().isLoaded("curios") && CuriosPlayerAccess.hasAccess(owner, accessor);
    }

    public static boolean sameFtbTeam(UUID owner, UUID accessor) {
        if (!ModList.get().isLoaded("ftbteams")) return false;
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.FTBTeamsAPIImpl");
            Object api = apiClass.getField("INSTANCE").get(null);
            Object manager = apiClass.getMethod("getManager").invoke(api);
            return Boolean.TRUE.equals(manager.getClass().getMethod("arePlayersInSameTeam", UUID.class, UUID.class)
                .invoke(manager, owner, accessor));
        } catch (ReflectiveOperationException | RuntimeException missingApi) {
            return false;
        }
    }

    public static boolean sameArgonautsGroup(Level level, UUID owner, UUID accessor, String mode) {
        return ModList.get().isLoaded("argonauts") && ArgonautsAccess.sameGroup(level, owner, accessor, mode);
    }
}
