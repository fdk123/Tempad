package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.Tempad;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChrononBatteryItem extends Item implements ChrononContainerItem {
    private final int capacity;

    public ChrononBatteryItem(Properties properties, int capacity) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
    }

    @Override
    public int getChrononCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack source = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(source, true);

        int available = getChronons(source);
        if (available <= 0) return InteractionResultHolder.fail(source);

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack target = player.getInventory().getItem(slot);
            if (target == source || target.is(Tempad.CHRONON_AUTO_CHARGE_BLACKLIST) || !(target.getItem() instanceof ChrononContainerItem container)) continue;
            int moved = container.insertChronons(target, available);
            if (moved <= 0) continue;
            extractChronons(source, moved);
            player.displayClientMessage(Component.translatable("item.tempad.chronons.transferred", moved), true);
            break;
        }

        return InteractionResultHolder.sidedSuccess(source, false);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack source, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !slot.hasItem()) return false;
        ItemStack target = slot.getItem();
        if (target == source || target.is(Tempad.CHRONON_AUTO_CHARGE_BLACKLIST) || !(target.getItem() instanceof ChrononContainerItem container)) return false;
        if (!player.level().isClientSide) {
            int moved = container.insertChronons(target, getChronons(source));
            if (moved > 0) extractChronons(source, moved);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.tempad.chronons", getChronons(stack), capacity));
    }
}
