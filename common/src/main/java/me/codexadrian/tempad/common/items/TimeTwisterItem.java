package me.codexadrian.tempad.common.items;

import dev.architectury.injectables.annotations.PlatformOnly;
import me.codexadrian.tempad.common.config.ConfigCache;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.data.TravelHistoryData;
import me.codexadrian.tempad.common.network.NetworkHandler;
import me.codexadrian.tempad.common.network.messages.s2c.OpenTimeTwisterPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Portable Time Twister. Its charge can also be installed into a Tempad reserve. */
public final class TimeTwisterItem extends Item implements ChrononContainerItem {
    public TimeTwisterItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public int getChrononCapacity(ItemStack stack) { return Math.max(1, TempadConfig.timeTwisterCapacity); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            int cost = Math.max(0, TempadConfig.timeTwisterBacktrackCost);
            if (!player.getAbilities().instabuild && ChrononStorage.get(stack) < cost) {
                player.displayClientMessage(Component.translatable("item.tempad.time_twister.no_chronons"), true);
                return InteractionResultHolder.fail(stack);
            }
            NetworkHandler.CHANNEL.sendToPlayer(new OpenTimeTwisterPacket(
                TravelHistoryData.get(serverPlayer).relevant(player.getUUID()), false, ChrononStorage.get(stack), cost, null), player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.tempad.chronons", ChrononStorage.get(stack), ConfigCache.timeTwisterCapacity));
        tooltip.add(Component.translatable("item.tempad.time_twister.cost", ConfigCache.timeTwisterBacktrackCost));
        tooltip.add(Component.translatable("item.tempad.time_twister.install"));
    }

    @PlatformOnly(PlatformOnly.FORGE)
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) { return false; }
}
