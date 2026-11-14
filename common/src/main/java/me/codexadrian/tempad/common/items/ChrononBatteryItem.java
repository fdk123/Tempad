package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.utils.CarriedStacks;
import net.minecraft.world.entity.Entity;
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
import java.util.function.IntSupplier;

public class ChrononBatteryItem extends Item implements ChrononContainerItem {
    private final IntSupplier capacity;
    private final IntSupplier displayCapacity;

    public ChrononBatteryItem(Properties properties, int capacity) {
        this(properties, () -> capacity, () -> capacity);
    }

    public ChrononBatteryItem(Properties properties, IntSupplier capacity, IntSupplier displayCapacity) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
        this.displayCapacity = displayCapacity;
    }

    @Override
    public int getChrononCapacity(ItemStack stack) {
        return Math.max(1, capacity.getAsInt());
    }

    @Override
    public void inventoryTick(ItemStack source, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(source, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player) || player.tickCount % 10 != 0 || getChronons(source) <= 0) return;
        for (ItemStack target : CarriedStacks.of(player)) {
            if (target == source || target.is(Tempad.CHRONON_BATTERIES)
                || target.is(Tempad.CHRONON_AUTO_CHARGE_BLACKLIST)
                || !(target.getItem() instanceof ChrononContainerItem container)) continue;
            if (ChrononStorage.transfer(source, target, container, getChronons(source)) > 0) break;
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack source = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(source, true);

        int available = getChronons(source);
        if (available <= 0) return InteractionResultHolder.fail(source);

        for (ItemStack target : CarriedStacks.of(player)) {
            if (target == source || target.is(Tempad.CHRONON_AUTO_CHARGE_BLACKLIST) || !(target.getItem() instanceof ChrononContainerItem container)) continue;
            int moved = ChrononStorage.transfer(source, target, container, available);
            if (moved <= 0) continue;
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
            if (ChrononStorage.transfer(source, target, container, getChronons(source)) > 0) slot.setChanged();
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.tempad.chronons", getChronons(stack), Math.max(1, displayCapacity.getAsInt())));
    }
}
