package me.codexadrian.tempad.api.options.impl;

import me.codexadrian.tempad.api.options.TempadOption;
import me.codexadrian.tempad.api.options.TempadOptionApi;
import me.codexadrian.tempad.common.items.ChrononStorage;
import me.codexadrian.tempad.common.utils.TeleportUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Uses chronons stored directly in the Tempad as teleport fuel. */
public final class ChrononOption extends TempadOption {
    @Override
    public boolean canTimedoorOpen(Player player, ItemStack stack) {
        return ChrononStorage.get(stack) >= TempadOptionApi.getFuelCost(stack);
    }

    @Override
    public void onTimedoorOpen(Player player) {
        ItemStack stack = TeleportUtils.findTempad(player);
        if (!stack.isEmpty()) ChrononStorage.extract(stack, TempadOptionApi.getFuelCost(stack));
    }

    @Override
    public void onTimedoorOpen(Player player, ItemStack stack) {
        ChrononStorage.extract(stack, TempadOptionApi.getFuelCost(stack));
    }

    @Override
    public void addToolTip(ItemStack stack, Level level, List<Component> components, TooltipFlag flag) {
        components.add(Component.translatable("tempad_option.tempad.chronons",
                ChrononStorage.get(stack), TempadOptionApi.getFuelCapacity(stack)));
    }

    @Override
    public boolean isDurabilityBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public double getPercentage(ItemStack stack) {
        int capacity = TempadOptionApi.getFuelCapacity(stack);
        return capacity <= 0 ? 0 : (double) ChrononStorage.get(stack) / capacity;
    }
}
