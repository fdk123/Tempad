package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.config.ConfigCache;

/** Basic TVA chronon generator with independently configurable output. */
public final class ChrononGeneratorItem extends ChronometerItem {
    public ChrononGeneratorItem(Properties properties) {
        super(properties, ConfigCache.chrononGeneratorCapacity);
    }

    @Override
    protected int getGenerationInterval() {
        return ConfigCache.chrononGeneratorGenerationInterval;
    }

    @Override
    protected int getGenerationAmount() {
        return ConfigCache.chrononGeneratorGenerationAmount;
    }
}
