package me.codexadrian.tempad.common.forge;

import me.codexadrian.tempad.common.data.TravelHistoryData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Forge 1.20.1 equivalents of Tempad 3.0 travel history hooks. */
public final class ForgeTravelHistoryEvents {
    private ForgeTravelHistoryEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.PlayerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.tickCount % 1200 == 0)
                TravelHistoryData.get(player).log(player, null);
        });
        MinecraftForge.EVENT_BUS.addListener((LivingDeathEvent event) -> {
            if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player)
                TravelHistoryData.get(player).log(player, TravelHistoryData.DEATH);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player)
                TravelHistoryData.get(player).log(player, TravelHistoryData.RESPAWN);
        });
        MinecraftForge.EVENT_BUS.addListener((EntityTravelToDimensionEvent event) -> {
            if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player)
                TravelHistoryData.get(player).log(player, TravelHistoryData.DIMENSION_EXIT);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player)
                TravelHistoryData.get(player).log(player, TravelHistoryData.DIMENSION_ENTER);
        });
    }
}
