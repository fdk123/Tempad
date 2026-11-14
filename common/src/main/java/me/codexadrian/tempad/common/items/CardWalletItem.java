package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.utils.WalletOpener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CardWalletItem extends Item {
    public CardWalletItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) WalletOpener.open(player, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public boolean overrideStackedOnOther(ItemStack wallet, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !slot.hasItem()) return false;
        if (new WalletContainer(wallet).insert(slot.getItem()) == 0) return false;
        if (slot.getItem().isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return true;
    }

    @Override public boolean overrideOtherStackedOnMe(ItemStack wallet, ItemStack cards, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || new WalletContainer(wallet).insert(cards) == 0) return false;
        access.set(cards);
        slot.setChanged();
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int stored = 0;
        WalletContainer contents = new WalletContainer(stack);
        for (int slot = 0; slot < contents.getContainerSize(); slot++) if (!contents.getItem(slot).isEmpty()) stored++;
        tooltip.add(Component.translatable("item.tempad.card_wallet.count", stored, 18));
    }
}
