package me.codexadrian.tempad.common.entity.forge;

import me.codexadrian.tempad.api.event.TimedoorEvent;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.entity.TimedoorEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

public final class TimedoorEventsImpl {
    private TimedoorEventsImpl() {}

    public static boolean canOpen(TimedoorEntity door, Player opener, LocationData target,
                                  @Nullable ItemStack item, @Nullable BlockEntity block) {
        TimedoorEvent.Open event = item != null ? new TimedoorEvent.OpenWithItem(door, opener, target, item)
            : block != null ? new TimedoorEvent.OpenWithBlock(door, opener, target, block)
            : new TimedoorEvent.Open(door, opener, target, null, null);
        return !MinecraftForge.EVENT_BUS.post(event);
    }

    public static boolean canEnter(TimedoorEntity door, Entity traveler) {
        return !MinecraftForge.EVENT_BUS.post(new TimedoorEvent.Enter(door, traveler));
    }

    public static void exited(TimedoorEntity door, Entity traveler) {
        MinecraftForge.EVENT_BUS.post(new TimedoorEvent.Exit(door, traveler));
    }

    public static void closed(TimedoorEntity door) {
        MinecraftForge.EVENT_BUS.post(new TimedoorEvent.Close(door));
    }
}
