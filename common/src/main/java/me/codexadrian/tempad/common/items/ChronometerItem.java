package me.codexadrian.tempad.common.items;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import me.codexadrian.tempad.common.Tempad;

public class ChronometerItem extends ChrononBatteryItem {
    private static final int GENERATION_INTERVAL = 24;
    private static final int GENERATION_AMOUNT = 1;

    public ChronometerItem(Properties properties) {
        super(properties, 1000);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player) || player.tickCount % GENERATION_INTERVAL != 0 || isClashing(player, stack)) return;
        insertChronons(stack, GENERATION_AMOUNT);
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
