package me.codexadrian.tempad.common.items;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.ConfigCache;

public class ChronometerItem extends ChrononBatteryItem {
    public ChronometerItem(Properties properties) {
        super(properties, 1000);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player) || player.tickCount % ConfigCache.chronometerGenerationInterval != 0 || isClashing(player, stack)) return;
        insertChronons(stack, ConfigCache.chronometerGenerationAmount);
        if (getChronons(stack) <= 0) return;

        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            ItemStack target = player.getInventory().getItem(index);
            if (target == stack || target.is(Tempad.CHRONON_AUTO_CHARGE_BLACKLIST) || !(target.getItem() instanceof ChrononContainerItem container)) continue;
            int available = getChronons(stack);
            int moved = container.insertChronons(target, available);
            if (moved > 0) extractChronons(stack, moved);
            if (getChronons(stack) == 0) break;
        }
    }

    private boolean isClashing(Player player, ItemStack source) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.getItem() instanceof ChronometerItem && candidate != source) return true;
            if (candidate == source) return false;
        }
        return false;
    }
}
