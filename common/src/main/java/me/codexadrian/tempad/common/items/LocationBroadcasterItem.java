package me.codexadrian.tempad.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The 3.0 broadcaster exposes its carrier as a live player destination. */
public final class LocationBroadcasterItem extends Item {
    public LocationBroadcasterItem(Properties properties) { super(properties.stacksTo(1)); }

    public static boolean enabled(ItemStack stack) {
        return stack.getItem() instanceof LocationBroadcasterItem
            && stack.hasTag() && stack.getTag().getBoolean("Enabled");
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean next = !enabled(stack);
            stack.getOrCreateTag().putBoolean("Enabled", next);
            player.displayClientMessage(Component.translatable(next ? "item.tempad.location_broadcaster.on" : "item.tempad.location_broadcaster.off"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable(enabled(stack) ? "item.tempad.location_broadcaster.on" : "item.tempad.location_broadcaster.off"));
    }
}
