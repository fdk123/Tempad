package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.config.ConfigCache;

/** Portable Metronome foundation; the workstation block wrapper can use the same device rules. */
public final class MetronomeItem extends ChronometerItem {
    public MetronomeItem(Properties properties) {
        super(properties, ConfigCache.metronomeCapacity);
    }

    @Override
    protected int getGenerationInterval() {
        return ConfigCache.metronomeGenerationInterval;
    }

    @Override
    protected int getGenerationAmount() {
        return ConfigCache.metronomeGenerationAmount;
    }
}
