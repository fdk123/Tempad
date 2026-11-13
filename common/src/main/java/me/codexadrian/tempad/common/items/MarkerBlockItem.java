package me.codexadrian.tempad.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/** Keeps an owned marker from silently changing hands when it is moved. */
public final class MarkerBlockItem extends BlockItem {
    public MarkerBlockItem(Block block, Properties properties) { super(block, properties.stacksTo(1)); }

    @Override public InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player != null && stack.hasTag() && stack.getTag().hasUUID("Owner")
                && !stack.getTag().getUUID("Owner").equals(player.getUUID())) {
            if (!context.getLevel().isClientSide) player.displayClientMessage(Component.translatable("block.tempad.marker.owner_mismatch"), true);
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }
}
