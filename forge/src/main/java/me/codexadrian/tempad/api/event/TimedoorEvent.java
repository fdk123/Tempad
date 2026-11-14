package me.codexadrian.tempad.api.event;

import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.entity.TimedoorEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.Cancelable;
import org.jetbrains.annotations.Nullable;

/** Events posted on the Forge game bus for a Timedoor's lifecycle. */
public abstract class TimedoorEvent extends EntityEvent {
    protected TimedoorEvent(TimedoorEntity door) {
        super(door);
    }

    public TimedoorEntity getTimedoor() {
        return (TimedoorEntity) getEntity();
    }

    /** Fired before a door is added to the world or its opening cost is paid. */
    @Cancelable
    public static class Open extends TimedoorEvent {
        private final Player opener;
        private final LocationData destination;
        @Nullable private final ItemStack item;
        @Nullable private final BlockEntity block;

        public Open(TimedoorEntity door, Player opener, LocationData destination,
                    @Nullable ItemStack item, @Nullable BlockEntity block) {
            super(door);
            this.opener = opener;
            this.destination = destination;
            this.item = item;
            this.block = block;
        }

        public Player getOpener() { return opener; }
        public LocationData getDestination() { return destination; }
        @Nullable public ItemStack getItem() { return item; }
        @Nullable public BlockEntity getBlock() { return block; }
    }

    @Cancelable
    public static final class OpenWithItem extends Open {
        public OpenWithItem(TimedoorEntity door, Player opener, LocationData destination, ItemStack item) {
            super(door, opener, destination, item, null);
        }
    }

    @Cancelable
    public static final class OpenWithBlock extends Open {
        public OpenWithBlock(TimedoorEntity door, Player opener, LocationData destination, BlockEntity block) {
            super(door, opener, destination, null, block);
        }
    }

    /** Fired before a traveler is dismounted or teleported. */
    @Cancelable
    public static final class Enter extends TimedoorEvent {
        private final Entity traveler;
        public Enter(TimedoorEntity door, Entity traveler) {
            super(door);
            this.traveler = traveler;
        }
        public Entity getTraveler() { return traveler; }
    }

    public static final class Exit extends TimedoorEvent {
        private final Entity traveler;
        public Exit(TimedoorEntity door, Entity traveler) {
            super(door);
            this.traveler = traveler;
        }
        public Entity getTraveler() { return traveler; }
    }

    public static final class Close extends TimedoorEvent {
        public Close(TimedoorEntity door) { super(door); }
    }
}
