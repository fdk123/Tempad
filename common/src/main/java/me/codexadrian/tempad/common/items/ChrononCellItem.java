package me.codexadrian.tempad.common.items;

import net.minecraft.world.item.Item;
import me.codexadrian.tempad.common.config.ConfigCache;
import me.codexadrian.tempad.common.config.TempadConfig;

public class ChrononCellItem extends ChrononBatteryItem {
    public ChrononCellItem(Properties properties) {
        super(properties, () -> TempadConfig.chrononCellCapacity, () -> ConfigCache.chrononCellCapacity);
    }
}
