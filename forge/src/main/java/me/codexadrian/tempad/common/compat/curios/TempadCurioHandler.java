package me.codexadrian.tempad.common.compat.curios;

import me.codexadrian.tempad.common.registry.TempadRegistry;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class TempadCurioHandler {
    public static void init() {
        CuriosApi.registerCurio(TempadRegistry.TEMPAD.get(), new TempadCurio());
        CuriosApi.registerCurio(TempadRegistry.CREATIVE_TEMPAD.get(), new TempadCurio());
        ICurioItem accessDevice = new ICurioItem() {};
        CuriosApi.registerCurio(TempadRegistry.LOCATION_BROADCASTER.get(), accessDevice);
        CuriosApi.registerCurio(TempadRegistry.SCREENING_DEVICE.get(), accessDevice);
    }
}
