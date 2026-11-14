package me.codexadrian.tempad.common.entity.forge;

import me.codexadrian.tempad.common.forge.TempadTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class TimedoorEntityImpl {
    private static final String COOLDOWN_KEY = "tempad:timedoor_cooldown_until";

    public static Entity teleportEntity(ServerLevel destinationLevel, BlockPos pos, Vec3 deltaMovement, Entity entity) {
        Entity moved = entity.changeDimension(destinationLevel, new TempadTeleporter(pos));
        if (moved != null) {
            moved.setDeltaMovement(deltaMovement);
            moved.hasImpulse = true;
        }
        return moved;
    }

    public static Entity teleportEntityPrecise(ServerLevel destinationLevel, Vec3 pos, float yaw, Vec3 deltaMovement, Entity entity) {
        Entity moved = entity.changeDimension(destinationLevel, new TempadTeleporter(pos, yaw));
        if (moved != null) {
            moved.setDeltaMovement(deltaMovement);
            moved.hasImpulse = true;
        }
        return moved;
    }

    public static boolean isOnCooldown(Entity entity) {
        return entity.getPersistentData().getLong(COOLDOWN_KEY) > entity.level().getGameTime();
    }

    public static void setCooldown(Entity entity, int ticks) {
        entity.getPersistentData().putLong(COOLDOWN_KEY, entity.level().getGameTime() + ticks);
    }
}
