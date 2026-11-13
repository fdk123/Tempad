package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.registry.TempadRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import me.codexadrian.tempad.common.utils.TeleportUtils;

/** Separate chronon reserve and preserved item metadata for a Twister installed in a Tempad. */
public final class TimeTwisterData {
    private static final String EQUIPPED = "TwisterEquipped";
    private static final String RESERVE = "TwisterChronons";
    private static final String ITEM = "TwisterItem";
    private TimeTwisterData() {}

    public static boolean equipped(ItemStack tempad) { return tempad.getItem() instanceof TempadItem && tempad.hasTag() && tempad.getTag().getBoolean(EQUIPPED); }
    public static int reserve(ItemStack tempad) { return equipped(tempad) ? Math.max(0, tempad.getTag().getInt(RESERVE)) : 0; }

    public static int insert(ItemStack tempad, int amount) {
        if (!equipped(tempad) || amount <= 0) return 0;
        int moved = Math.min(amount, Math.max(0, TempadConfig.timeTwisterCapacity - reserve(tempad)));
        if (moved > 0) tempad.getOrCreateTag().putInt(RESERVE, reserve(tempad) + moved);
        return moved;
    }

    public static int extract(ItemStack tempad, int amount) {
        int taken = Math.min(Math.max(0, amount), reserve(tempad));
        if (taken > 0) tempad.getOrCreateTag().putInt(RESERVE, reserve(tempad) - taken);
        return taken;
    }

    public static boolean install(ItemStack tempad, ItemStack twister) {
        if (!(tempad.getItem() instanceof TempadItem) || !(twister.getItem() instanceof TimeTwisterItem) || equipped(tempad)) return false;
        CompoundTag tag = tempad.getOrCreateTag();
        tag.putBoolean(EQUIPPED, true);
        tag.putInt(RESERVE, Math.min(TempadConfig.timeTwisterCapacity, ChrononStorage.get(twister)));
        ItemStack clean = twister.copyWithCount(1);
        if (clean.hasTag()) clean.getTag().remove("Chronons");
        tag.put(ITEM, clean.save(new CompoundTag()));
        return true;
    }

    public static ItemStack uninstall(ItemStack tempad) {
        if (!equipped(tempad)) return ItemStack.EMPTY;
        CompoundTag tag = tempad.getTag();
        ItemStack twister = tag.contains(ITEM) ? ItemStack.of(tag.getCompound(ITEM)) : new ItemStack(TempadRegistry.TIME_TWISTER.get());
        if (!(twister.getItem() instanceof TimeTwisterItem)) twister = new ItemStack(TempadRegistry.TIME_TWISTER.get());
        if (reserve(tempad) > 0) twister.getOrCreateTag().putInt("Chronons", reserve(tempad));
        tag.remove(EQUIPPED);
        tag.remove(RESERVE);
        tag.remove(ITEM);
        return twister;
    }

    public static ItemStack findSource(Player player, boolean embedded) {
        return findSource(player, embedded, 0);
    }

    public static ItemStack findSource(Player player, boolean embedded, int minimumCharge) {
        if (matches(player.getMainHandItem(), embedded, minimumCharge)) return player.getMainHandItem();
        if (matches(player.getOffhandItem(), embedded, minimumCharge)) return player.getOffhandItem();
        if (embedded) {
            ItemStack curio = TeleportUtils.findTempad(player);
            if (matches(curio, true, minimumCharge)) return curio;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (matches(stack, embedded, minimumCharge)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean matches(ItemStack stack, boolean embedded, int minimumCharge) {
        return (embedded ? equipped(stack) : stack.getItem() instanceof TimeTwisterItem)
            && available(stack, embedded) >= minimumCharge;
    }

    public static int available(ItemStack source, boolean embedded) {
        return embedded ? reserve(source) : ChrononStorage.get(source);
    }

    public static int consume(ItemStack source, boolean embedded, int amount) {
        return embedded ? extract(source, amount) : ChrononStorage.extract(source, amount);
    }
}
