package me.codexadrian.tempad.common.block;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Limits the workstation slot to ordinary pickup and placement. */
public final class WorkstationMenu extends ChestMenu {
    public WorkstationMenu(int id, Inventory inventory, Container container) {
        super(MenuType.GENERIC_9x1, id, inventory, container, 1);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType != ClickType.PICKUP || (slotId >= 0 && slotId < 9 && slotId != 4)) return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
