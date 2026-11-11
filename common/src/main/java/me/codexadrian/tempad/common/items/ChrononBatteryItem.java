package me.codexadrian.tempad.common.items;

import net.minecraft.network.chat.Component;
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.tempad.chronons", getChronons(stack), capacity));
    }
}
