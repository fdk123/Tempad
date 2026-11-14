package me.codexadrian.tempad.common.items;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.ConfigCache;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.utils.CarriedStacks;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntSupplier;

public class ChronometerItem extends ChrononBatteryItem {
    public ChronometerItem(Properties properties) {
        this(properties, () -> TempadConfig.chronometerCapacity, () -> ConfigCache.chronometerCapacity);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.tempad.generator_rate", displayGenerationAmount(), displayGenerationInterval()));
    }

    protected ChronometerItem(Properties properties, IntSupplier capacity, IntSupplier displayCapacity) {
        super(properties, capacity, displayCapacity);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player) || player.tickCount % getGenerationInterval() != 0 || isClashing(player, stack)) return;
        insertChronons(stack, getGenerationAmount());
        if (getChronons(stack) <= 0) return;

        for (ItemStack target : CarriedStacks.of(player)) {
            if (target == stack || target.is(Tempad.CHRONON_AUTO_CHARGE_BLACKLIST) || !(target.getItem() instanceof ChrononContainerItem container)) continue;
            int available = getChronons(stack);
            if (ChrononStorage.transfer(stack, target, container, available) > 0) break;
        }
    }

    protected int getGenerationInterval() {
        return Math.max(1, TempadConfig.chronometerGenerationInterval);
    }

    protected int getGenerationAmount() {
        return Math.max(0, TempadConfig.chronometerGenerationAmount);
    }

    protected int displayGenerationInterval() { return ConfigCache.chronometerGenerationInterval; }
    protected int displayGenerationAmount() { return ConfigCache.chronometerGenerationAmount; }

    private boolean isClashing(Player player, ItemStack source) {
        for (ItemStack candidate : CarriedStacks.of(player)) {
            if (candidate.is(Tempad.CHRONON_GENERATORS) && candidate != source) return true;
        }
        return false;
    }
}
