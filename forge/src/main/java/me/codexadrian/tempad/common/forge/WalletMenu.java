package me.codexadrian.tempad.common.forge;

import me.codexadrian.tempad.common.items.WalletContainer;
import me.codexadrian.tempad.common.items.CardWalletItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Server-side wallet menu. The client uses the vanilla two-row chest screen. */
public final class WalletMenu extends ChestMenu {
    private final int walletInventorySlot;
    private final int walletMenuSlot;

    public WalletMenu(int id, Inventory inventory, WalletContainer contents, ItemStack wallet) {
        super(MenuType.GENERIC_9x2, id, inventory, contents, 2);
        int inventorySlot = -1;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot) == wallet) {
                inventorySlot = slot;
                break;
            }
        }
        walletInventorySlot = inventorySlot;
        walletMenuSlot = inventorySlot >= 9 && inventorySlot < 36 ? 18 + inventorySlot - 9
            : inventorySlot >= 0 && inventorySlot < 9 ? 45 + inventorySlot : -1;
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (walletMenuSlot >= 0 && slotId == walletMenuSlot || slotId >= 0 && slotId < slots.size()
            && slots.get(slotId).getItem().getItem() instanceof CardWalletItem) return;
        if (clickType == ClickType.SWAP && button >= 0 && button < player.getInventory().getContainerSize()
            && player.getInventory().getItem(button).getItem() instanceof CardWalletItem) return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return index == walletMenuSlot || index >= 0 && index < slots.size()
            && slots.get(index).getItem().getItem() instanceof CardWalletItem
            ? ItemStack.EMPTY : super.quickMoveStack(player, index);
    }
}
