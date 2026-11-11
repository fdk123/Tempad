package me.codexadrian.tempad.common.items;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.ConfigCache;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChronometerItem extends ChrononBatteryItem {
    public ChronometerItem(Properties properties) {
        this(properties, 1000);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.tempad.generator_rate", getGenerationAmount(), getGenerationInterval()));
    }

    protected ChronometerItem(Properties properties, int capacity) {
        super(properties, capacity);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player) || player.tickCount % getGenerationInterval() != 0 || isClashing(player, stack)) return;
        insertChronons(stack, getGenerationAmount());
        if (getChronons(stack) <= 0) return;

        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            ItemStack target = player.getInventory().getItem(index);
            if (target == stack || target.is(Tempad.CHRONON_AUTO_CHARGE_BLACKLIST) || !(target.getItem() instanceof ChrononContainerItem container)) continue;
            int available = getChronons(stack);
            ChrononStorage.transfer(stack, target, container, available);
            if (getChronons(stack) == 0) break;
        }
    }

    protected int getGenerationInterval() {
        return ConfigCache.chronometerGenerationInterval;
    }

    protected int getGenerationAmount() {
        return ConfigCache.chronometerGenerationAmount;
    }

    private boolean isClashing(Player player, ItemStack source) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.is(Tempad.CHRONON_GENERATORS) && candidate != source) return true;
            if (candidate == source) return false;
        }
        return false;
    }
}
