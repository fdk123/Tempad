package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.config.ConfigCache;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Portable form of a Metronome, retaining its share of the owner's network. */
public final class MetronomeItem extends BlockItem implements ChrononContainerItem {
    public MetronomeItem(Block block, Properties properties) { super(block, properties.stacksTo(1)); }
    @Override public int getChrononCapacity(ItemStack stack) { return Math.max(1, ConfigCache.metronomeCapacity); }

    @Override public InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player != null && stack.hasTag() && stack.getTag().hasUUID("Owner")
                && !stack.getTag().getUUID("Owner").equals(player.getUUID())) return InteractionResult.FAIL;
        return super.place(context);
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.tempad.chronons", getChronons(stack), getChrononCapacity(stack)));
    }
}
