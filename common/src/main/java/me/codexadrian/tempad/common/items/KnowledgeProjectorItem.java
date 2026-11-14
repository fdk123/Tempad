package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.utils.ClientUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Standalone entry point to the same guide used by the Knowledge Repository upgrade. */
public final class KnowledgeProjectorItem extends Item {
    public KnowledgeProjectorItem(Properties properties) { super(properties); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) ClientUtils.openGuide();
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
