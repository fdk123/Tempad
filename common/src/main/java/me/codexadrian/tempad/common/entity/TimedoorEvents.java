package me.codexadrian.tempad.common.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import me.codexadrian.tempad.common.data.LocationData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Platform bridge for the Forge Timedoor event API. */
public final class TimedoorEvents {
    private TimedoorEvents() {}

    @ExpectPlatform
    public static boolean canOpen(TimedoorEntity door, Player opener, LocationData target,
                                  @Nullable ItemStack item, @Nullable BlockEntity block) {
        throw new AssertionError("Not implemented");
    }

    @ExpectPlatform
    public static boolean canEnter(TimedoorEntity door, Entity traveler) {
        throw new AssertionError("Not implemented");
    }

    @ExpectPlatform
    public static void exited(TimedoorEntity door, Entity traveler) {
        throw new AssertionError("Not implemented");
    }

    @ExpectPlatform
    public static void closed(TimedoorEntity door) {
        throw new AssertionError("Not implemented");
    }
}
