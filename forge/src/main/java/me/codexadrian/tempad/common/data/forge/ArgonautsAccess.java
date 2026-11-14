package me.codexadrian.tempad.common.data.forge;

import earth.terrarium.argonauts.api.guild.GuildApi;
import earth.terrarium.argonauts.api.party.PartyApi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** Loaded only when Argonauts is installed. */
final class ArgonautsAccess {
    private ArgonautsAccess() {}

    static boolean sameGroup(Level level, UUID owner, UUID accessor, String mode) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if ("guild".equals(mode)) {
            var guild = GuildApi.API.getPlayerGuild(serverLevel.getServer(), owner);
            return guild != null && (guild.members().isMember(accessor) || guild.members().isAllied(accessor));
        }
        if ("party".equals(mode)) {
            var party = PartyApi.API.getPlayerParty(owner);
            return party != null && party.members().isMember(accessor);
        }
        return false;
    }
}
