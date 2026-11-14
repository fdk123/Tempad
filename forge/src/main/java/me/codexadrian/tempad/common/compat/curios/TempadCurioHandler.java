package me.codexadrian.tempad.common.compat.curios;

import me.codexadrian.tempad.common.registry.TempadRegistry;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;
import net.minecraft.world.item.ItemStack;

public class TempadCurioHandler {
    public static void init() {
        CuriosApi.registerCurio(TempadRegistry.TEMPAD.get(), new TempadCurio());
        CuriosApi.registerCurio(TempadRegistry.CREATIVE_TEMPAD.get(), new TempadCurio());
        ICurioItem accessDevice = new ICurioItem() {};
        CuriosApi.registerCurio(TempadRegistry.LOCATION_BROADCASTER.get(), accessDevice);
        CuriosApi.registerCurio(TempadRegistry.SCREENING_DEVICE.get(), accessDevice);
        CuriosApi.registerCurio(TempadRegistry.CARD_WALLET.get(), accessDevice);
        ICurioItem tickingChronons = new ICurioItem() {
            @Override public void curioTick(SlotContext context, ItemStack stack) {
                stack.getItem().inventoryTick(stack, context.entity().level(), context.entity(), -1, false);
            }
        };
        CuriosApi.registerCurio(TempadRegistry.CHRONON_BATTERY.get(), tickingChronons);
        CuriosApi.registerCurio(TempadRegistry.CHRONON_CELL.get(), tickingChronons);
        CuriosApi.registerCurio(TempadRegistry.CHRONOMETER.get(), tickingChronons);
        CuriosApi.registerCurio(TempadRegistry.CHRONON_GENERATOR.get(), tickingChronons);
        CuriosApi.registerCurio(TempadRegistry.CREATIVE_CHRONOMETER.get(), tickingChronons);
    }
}
