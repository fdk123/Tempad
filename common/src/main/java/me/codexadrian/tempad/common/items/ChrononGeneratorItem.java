package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.config.ConfigCache;
import me.codexadrian.tempad.common.config.TempadConfig;

/** Basic TVA chronon generator with independently configurable output. */
public final class ChrononGeneratorItem extends ChronometerItem {
    public ChrononGeneratorItem(Properties properties) {
        super(properties, () -> TempadConfig.chrononGeneratorCapacity, () -> ConfigCache.chrononGeneratorCapacity);
    }

    @Override
    protected int getGenerationInterval() {
        return Math.max(1, TempadConfig.chrononGeneratorGenerationInterval);
    }

    @Override
    protected int getGenerationAmount() {
        return Math.max(0, TempadConfig.chrononGeneratorGenerationAmount);
    }

    @Override protected int displayGenerationInterval() { return ConfigCache.chrononGeneratorGenerationInterval; }
    @Override protected int displayGenerationAmount() { return ConfigCache.chrononGeneratorGenerationAmount; }
}
