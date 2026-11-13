package me.codexadrian.tempad.common.network.messages.s2c;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.ConfigCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record InitConfigPacket(
    boolean allowInterdimensionalTravel,
    boolean allowExporting,
    boolean consumeCooldown,
    String tempadFuelType,
    int tempadFuelConsumptionValue,
    int tempadFuelCapacityValue,
    String advancedTempadFuelType,
    int advancedTempadfuelConsumptionValue,
    int advancedTempadfuelCapacityValue,
    int chronometerGenerationInterval,
    int chronometerGenerationAmount,
    int chrononGeneratorCapacity,
    int chrononGeneratorGenerationInterval,
    int chrononGeneratorGenerationAmount,
    int metronomeCapacity,
    int metronomeGenerationInterval,
    int metronomeGenerationAmount,
    int metronomeDistributionRange,
    boolean metronomeRedstonePause
) implements Packet<InitConfigPacket> {
    public static Handler HANDLER = new Handler();
    public static final ResourceLocation ID = new ResourceLocation(Tempad.MODID, "sync_config");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public PacketHandler<InitConfigPacket> getHandler() {
        return HANDLER;
    }

    private static class Handler implements PacketHandler<InitConfigPacket> {

        @Override
        public void encode(InitConfigPacket message, FriendlyByteBuf buffer) {
            buffer.writeBoolean(message.allowInterdimensionalTravel);
            buffer.writeBoolean(message.allowExporting);
            buffer.writeBoolean(message.consumeCooldown);
            buffer.writeUtf(message.tempadFuelType);
            buffer.writeInt(message.tempadFuelConsumptionValue);
            buffer.writeInt(message.tempadFuelCapacityValue);
            buffer.writeUtf(message.advancedTempadFuelType);
            buffer.writeInt(message.advancedTempadfuelConsumptionValue);
            buffer.writeInt(message.advancedTempadfuelCapacityValue);
            buffer.writeInt(message.chronometerGenerationInterval);
            buffer.writeInt(message.chronometerGenerationAmount);
            buffer.writeInt(message.chrononGeneratorCapacity);
            buffer.writeInt(message.chrononGeneratorGenerationInterval);
            buffer.writeInt(message.chrononGeneratorGenerationAmount);
            buffer.writeInt(message.metronomeCapacity);
            buffer.writeInt(message.metronomeGenerationInterval);
            buffer.writeInt(message.metronomeGenerationAmount);
            buffer.writeInt(message.metronomeDistributionRange);
            buffer.writeBoolean(message.metronomeRedstonePause);
        }

        @Override
        public InitConfigPacket decode(FriendlyByteBuf buffer) {
            return new InitConfigPacket(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readUtf(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readUtf(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readBoolean()
            );
        }

        @Override
        public PacketContext handle(InitConfigPacket message) {
            return (player, level) -> {
                ConfigCache.allowInterdimensionalTravel = message.allowInterdimensionalTravel;
                ConfigCache.allowExporting = message.allowExporting;
                ConfigCache.consumeCooldown = message.consumeCooldown;
                ConfigCache.tempadFuelType = message.tempadFuelType;
                ConfigCache.tempadFuelConsumptionValue = message.tempadFuelConsumptionValue;
                ConfigCache.tempadFuelCapacityValue = message.tempadFuelCapacityValue;
                ConfigCache.advancedTempadFuelType = message.advancedTempadFuelType;
                ConfigCache.advancedTempadfuelConsumptionValue = message.advancedTempadfuelConsumptionValue;
                ConfigCache.advancedTempadfuelCapacityValue = message.advancedTempadfuelCapacityValue;
                ConfigCache.chronometerGenerationInterval = message.chronometerGenerationInterval;
                ConfigCache.chronometerGenerationAmount = message.chronometerGenerationAmount;
                ConfigCache.chrononGeneratorCapacity = message.chrononGeneratorCapacity;
                ConfigCache.chrononGeneratorGenerationInterval = message.chrononGeneratorGenerationInterval;
                ConfigCache.chrononGeneratorGenerationAmount = message.chrononGeneratorGenerationAmount;
                ConfigCache.metronomeCapacity = message.metronomeCapacity;
                ConfigCache.metronomeGenerationInterval = message.metronomeGenerationInterval;
                ConfigCache.metronomeGenerationAmount = message.metronomeGenerationAmount;
                ConfigCache.metronomeDistributionRange = message.metronomeDistributionRange;
                ConfigCache.metronomeRedstonePause = message.metronomeRedstonePause;
            };
        }
    }
}
