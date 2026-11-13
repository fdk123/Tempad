package me.codexadrian.tempad.common.items;

import dev.architectury.injectables.annotations.PlatformOnly;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.block.TimedoorProjectorBlock;
import me.codexadrian.tempad.common.config.ConfigCache;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.entity.TimedoorEntity;
import me.codexadrian.tempad.common.utils.TeleportUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Portable form of the 3.0 Timedoor Projector. Sneak-use places the block. */
public final class TimedoorProjectorItem extends BlockItem implements ChrononContainerItem {
    public TimedoorProjectorItem(TimedoorProjectorBlock block, Properties properties) { super(block, properties.stacksTo(1)); }

    @Override public int getChrononCapacity(ItemStack stack) { return Math.max(1, TempadConfig.projectorCapacity); }

    @Override public InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player != null && stack.hasTag() && stack.getTag().hasUUID("Owner")
                && !stack.getTag().getUUID("Owner").equals(player.getUUID())) {
            if (!context.getLevel().isClientSide) player.displayClientMessage(Component.translatable("block.tempad.timedoor_projector.owner_mismatch"), true);
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) open(player, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) return super.useOn(context);
        if (!context.getLevel().isClientSide && context.getPlayer() != null) open(context.getPlayer(), context.getItemInHand());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    private static void open(Player player, ItemStack stack) {
        LocationData target = ProjectorData.getTarget(stack);
        if (target == null || target.getLevelKey() == null || !target.isTeleportable()) {
            player.displayClientMessage(Component.translatable("item.tempad.timedoor_projector.no_target"), true);
            return;
        }
        if (!TeleportUtils.mayTeleport(target.getLevelKey(), player) || player.getServer() == null || player.getServer().getLevel(target.getLevelKey()) == null) {
            player.displayClientMessage(Component.translatable("block.tempad.workstation.status.blocked"), true);
            return;
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) return;
        int cost = Math.max(0, TempadConfig.projectorDoorCost);
        if (!player.getAbilities().instabuild && ChrononStorage.get(stack) < cost) {
            player.displayClientMessage(Component.translatable("item.tempad.timedoor_projector.no_chronons"), true);
            return;
        }
        TimedoorEntity door = TempadItem.summonTimeDoor(target, player, Tempad.ORANGE);
        if (door == null) return;
        door.setLifetimeTicks(Math.max(1, TempadConfig.projectorDoorDuration));
        if (!player.getAbilities().instabuild) ChrononStorage.extract(stack, cost);
        player.getCooldowns().addCooldown(stack.getItem(), 40);
    }

    @Override public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        LocationData current = ProjectorData.getTarget(stack);
        if (current != null && !slot.hasItem()) {
            slot.set(ProjectorData.cardFor(current));
            ProjectorData.setTarget(stack, null);
            return true;
        }
        if (current == null && slot.hasItem() && slot.getItem().getItem() instanceof LocationCard) {
            LocationData target = ProjectorData.cardTarget(slot.getItem());
            if (target == null) return false;
            ProjectorData.setTarget(stack, target);
            slot.remove(1);
            slot.setChanged();
            return true;
        }
        return false;
    }

    @Override public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || ProjectorData.getTarget(stack) != null || !(other.getItem() instanceof LocationCard)) return false;
        LocationData target = ProjectorData.cardTarget(other);
        if (target == null) return false;
        ProjectorData.setTarget(stack, target);
        ItemStack remainder = other.copy();
        remainder.shrink(1);
        access.set(remainder);
        slot.setChanged();
        return true;
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        LocationData target = ProjectorData.getTarget(stack);
        tooltip.add(Component.translatable(target == null ? "item.tempad.timedoor_projector.no_target" : "item.tempad.timedoor_projector.target", target == null ? "" : target.getName()));
        tooltip.add(Component.translatable("item.tempad.chronons", ChrononStorage.get(stack), ConfigCache.projectorCapacity));
        tooltip.add(Component.translatable("item.tempad.timedoor_projector.portal", ConfigCache.projectorDoorCost, ConfigCache.projectorDoorDuration / 20));
        tooltip.add(Component.translatable("item.tempad.timedoor_projector.card_controls"));
        tooltip.add(Component.translatable("item.tempad.timedoor_projector.controls"));
    }

    @PlatformOnly(PlatformOnly.FORGE)
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) { return false; }
}
