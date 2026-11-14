package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.block.MetronomeBlockEntity;
import me.codexadrian.tempad.common.block.TimedoorProjectorBlockEntity;
import me.codexadrian.tempad.common.block.WorkstationBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Creative-only, inexhaustible source matching the 3.0 Creative Chronometer. */
public final class CreativeChronometerItem extends Item implements ChrononContainerItem {
    public CreativeChronometerItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public int getChrononCapacity(ItemStack stack) { return Integer.MAX_VALUE; }
    @Override public int getChronons(ItemStack stack) { return Integer.MAX_VALUE; }
    @Override public int insertChronons(ItemStack stack, int amount) { return 0; }
    @Override public int extractChronons(ItemStack stack, int amount) { return Math.max(0, amount); }

    @Override public void inventoryTick(ItemStack source, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(source, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player) || player.tickCount % 10 != 0) return;
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            ItemStack target = player.getInventory().getItem(index);
            if (target == source || target.is(Tempad.CHRONON_AUTO_CHARGE_BLACKLIST)
                    || !(target.getItem() instanceof ChrononContainerItem container)) continue;
            container.insertChronons(target, Integer.MAX_VALUE);
        }
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        var block = context.getLevel().getBlockEntity(context.getClickedPos());
        if (block instanceof MetronomeBlockEntity metronome) {
            if (!metronome.canAccess(player)) return InteractionResult.FAIL;
            if (!context.getLevel().isClientSide) metronome.receiveChronons(Integer.MAX_VALUE);
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (block instanceof WorkstationBlockEntity workstation) {
            if (!workstation.canAccess(player)) return InteractionResult.FAIL;
            if (!context.getLevel().isClientSide) workstation.receiveChronons(Integer.MAX_VALUE);
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (block instanceof TimedoorProjectorBlockEntity projector) {
            if (!projector.canAccess(player)) return InteractionResult.FAIL;
            if (!context.getLevel().isClientSide) projector.receiveChronons(Integer.MAX_VALUE);
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.tempad.creative_chronometer.tooltip"));
    }
}
