package me.codexadrian.tempad.common.compat.cadmus;

import earth.terrarium.cadmus.api.claims.admin.FlagApi;
import earth.terrarium.cadmus.api.claims.admin.flags.BooleanFlag;
import earth.terrarium.cadmus.common.claims.AdminClaimHandler;
import earth.terrarium.cadmus.common.claims.ClaimHandler;
import earth.terrarium.cadmus.common.teams.TeamHelper;
import me.codexadrian.tempad.api.event.TimedoorEvent;
import me.codexadrian.tempad.common.data.MarkerIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;

/** Cadmus 1.20.1 claim protection for Timedoor creation. */
public final class CadmusCompat {
    private static final String FLAG = "timedoors-summoning";
    private static GameRules.Key<GameRules.BooleanValue> allowTimedoorsInClaims;

    private CadmusCompat() {}

    public static void init() {
        allowTimedoorsInClaims = GameRules.register("allowTimedoorsInClaims", GameRules.Category.MISC,
            GameRules.BooleanValue.create(true));
        FlagApi.API.register(FLAG, new BooleanFlag(true));
        MinecraftForge.EVENT_BUS.addListener(CadmusCompat::onOpen);
    }

    private static void onOpen(TimedoorEvent.Open event) {
        if (!(event.getOpener() instanceof ServerPlayer player)
                || !(event.getTimedoor().level() instanceof ServerLevel source)) return;
        ServerLevel target = source.getServer().getLevel(event.getDestination().getLevelKey());
        if (target == null) return;
        if (!canSummon(source, event.getTimedoor().blockPosition(), player)) {
            player.displayClientMessage(Component.translatable("tempad.cadmus.timedoor.starting"), true);
            event.setCanceled(true);
            return;
        }
        // Existing marker destinations are managed by their own access rules.
        BlockPos arrival = event.getDestination().hasPreciseArrival()
            ? BlockPos.containing(event.getDestination().getArrivalPosition())
            : event.getDestination().getBlockPos();
        if (!MarkerIndex.isKnownMarker(source, event.getDestination().getId())
                && !canSummon(target, arrival, player)) {
            player.displayClientMessage(Component.translatable("tempad.cadmus.timedoor.target"), true);
            event.setCanceled(true);
        }
    }

    private static boolean canSummon(ServerLevel level, BlockPos pos, ServerPlayer player) {
        var claim = ClaimHandler.getClaim(level, new ChunkPos(pos));
        if (claim == null || player.hasPermissions(2)) return true;
        String id = claim.getFirst();
        if (id.startsWith(ClaimHandler.ADMIN_PREFIX))
            return AdminClaimHandler.getBooleanFlag(level.getServer(), id, FLAG);
        return level.getGameRules().getBoolean(allowTimedoorsInClaims)
            || TeamHelper.isMember(id, level.getServer(), player.getUUID());
    }
}
