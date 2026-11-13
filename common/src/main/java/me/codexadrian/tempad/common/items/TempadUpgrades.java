package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.TempadConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import me.codexadrian.tempad.common.utils.TeleportUtils;

/** Installed 3.0 upgrades stored directly on the device so they survive workstation pickup. */
public final class TempadUpgrades {
    public static final ResourceLocation NEW_LOCATION = new ResourceLocation(Tempad.MODID, "new_location");
    public static final ResourceLocation KNOWLEDGE_REPOSITORY = new ResourceLocation(Tempad.MODID, "knowledge_repository");
    private static final String KEY = "InstalledUpgrades";

    private TempadUpgrades() {}

    public static boolean has(ItemStack stack, ResourceLocation upgrade) {
        if (!(stack.getItem() instanceof TempadItem) || !stack.hasTag()) return false;
        ListTag list = stack.getTag().getList(KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) if (upgrade.toString().equals(list.getString(i))) return true;
        return false;
    }

    public static boolean install(ItemStack stack, ResourceLocation upgrade) {
        if (!(stack.getItem() instanceof TempadItem) || has(stack, upgrade)) return false;
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = tag.getList(KEY, Tag.TAG_STRING);
        list.add(StringTag.valueOf(upgrade.toString()));
        tag.put(KEY, list);
        return true;
    }

    public static ListTag installed(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getList(KEY, Tag.TAG_STRING) : new ListTag();
    }

    public static boolean canSaveLocations(ItemStack stack) {
        return !TempadConfig.requireNewLocationUpgrade || has(stack, NEW_LOCATION);
    }

    /** Saved locations belong to the player in 1.20.1, so any carried upgraded device grants access. */
    public static boolean canSaveLocations(Player player) {
        if (!TempadConfig.requireNewLocationUpgrade) return true;
        if (has(player.getMainHandItem(), NEW_LOCATION) || has(player.getOffhandItem(), NEW_LOCATION)
                || has(TeleportUtils.findTempad(player), NEW_LOCATION)) return true;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (has(player.getInventory().getItem(i), NEW_LOCATION)) return true;
        }
        return false;
    }
}
