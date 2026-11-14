package me.codexadrian.tempad.client.forge;

import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.entity.TimedoorEntity;
import me.codexadrian.tempad.common.registry.TempadRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

/** Registers the Timedoor with Ars Nouveau's 1.20.1 dynamic light manager. */
public final class ArsCompat {
    private ArsCompat() {}

    public static void init() {
        try {
            Class<?> manager = Class.forName("com.hollingsworth.arsnouveau.common.light.LightManager");
            Function<Entity, Integer> brightness = entity -> Math.min(5,
                Math.max(0, entity.tickCount * 5 / Math.max(1, TimedoorEntity.ANIMATION_LENGTH)));
            manager.getMethod("register", EntityType.class, Function.class)
                .invoke(null, TempadRegistry.TIMEDOOR_ENTITY.get(), brightness);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException error) {
            Tempad.LOG.warn("Could not register Timedoor lighting with Ars Nouveau", error);
        }
    }
}
