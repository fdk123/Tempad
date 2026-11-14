package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.data.TempadLocationHandler;
import me.codexadrian.tempad.common.data.TravelHistoryData;
import me.codexadrian.tempad.common.data.PortalSetupData;
import me.codexadrian.tempad.common.network.NetworkHandler;
import me.codexadrian.tempad.common.network.messages.s2c.OpenTempadScreenPacket;
import me.codexadrian.tempad.common.network.messages.s2c.OpenTimeTwisterPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import me.codexadrian.tempad.common.utils.TeleportUtils;

import java.util.ArrayList;
import java.util.UUID;

/** Per-device settings; values from the network are restricted to known options. */
public final class TempadSettings {
    private static final String APP = "DefaultApp";
    private static final String MACRO = "DefaultMacro";
    private static final String ORDER = "LocationOrder";

    private TempadSettings() {}

    public static UUID id(ItemStack stack) {
        if (!stack.getOrCreateTag().hasUUID("SettingsId")) stack.getOrCreateTag().putUUID("SettingsId", UUID.randomUUID());
        return stack.getTag().getUUID("SettingsId");
    }

    public static ItemStack find(Player player, UUID id) {
        if (matches(player.getMainHandItem(), id)) return player.getMainHandItem();
        if (matches(player.getOffhandItem(), id)) return player.getOffhandItem();
        ItemStack curio = TeleportUtils.findTempad(player);
        if (matches(curio, id)) return curio;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (matches(stack, id)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean matches(ItemStack stack, UUID id) {
        return stack.getItem() instanceof TempadItem && stack.hasTag() && stack.getTag().hasUUID("SettingsId")
            && stack.getTag().getUUID("SettingsId").equals(id);
    }

    public static String app(ItemStack stack) { return option(stack, APP, "locations", "locations", "timeline", "guide"); }
    public static String macro(ItemStack stack) { return option(stack, MACRO, "favorite", "favorite", "none"); }
    public static String order(ItemStack stack) { return option(stack, ORDER, "alphabetical", "alphabetical", "dimension", "type"); }

    private static String option(ItemStack stack, String key, String fallback, String... allowed) {
        if (!stack.hasTag()) return fallback;
        String value = stack.getTag().getString(key);
        for (String candidate : allowed) if (candidate.equals(value)) return value;
        return fallback;
    }

    public static void save(ItemStack stack, String app, String macro, String order) {
        if (!(stack.getItem() instanceof TempadItem)) return;
        stack.getOrCreateTag().putString(APP, valid(app, "locations", "locations", "timeline", "guide"));
        stack.getOrCreateTag().putString(MACRO, valid(macro, "favorite", "favorite", "none"));
        stack.getOrCreateTag().putString(ORDER, valid(order, "alphabetical", "alphabetical", "dimension", "type"));
    }

    private static String valid(String value, String fallback, String... allowed) {
        for (String candidate : allowed) if (candidate.equals(value)) return candidate;
        return fallback;
    }

    public static void openDefault(ServerPlayer player, ItemStack stack) {
        String app = app(stack);
        if ("timeline".equals(app) && TimeTwisterData.equipped(stack)) {
            int cost = Math.max(0, TempadConfig.timeTwisterBacktrackCost);
            NetworkHandler.CHANNEL.sendToPlayer(new OpenTimeTwisterPacket(
                TravelHistoryData.get(player).relevant(player.getUUID()), true, TimeTwisterData.reserve(stack), cost, id(stack)), player);
            return;
        }
        openMain(player, stack, "guide".equals(app) && TempadUpgrades.has(stack, TempadUpgrades.KNOWLEDGE_REPOSITORY));
    }

    public static void openMain(ServerPlayer player, ItemStack stack) { openMain(player, stack, false); }

    private static void openMain(ServerPlayer player, ItemStack stack, boolean startGuide) {
        NetworkHandler.CHANNEL.sendToPlayer(new OpenTempadScreenPacket(
            new ArrayList<>(TempadLocationHandler.getLocations(player.level(), player.getUUID()).values()),
            TempadLocationHandler.getFavorite(player.level(), player.getUUID()), id(stack), TempadUpgrades.canSaveLocations(player),
            TempadUpgrades.has(stack, TempadUpgrades.KNOWLEDGE_REPOSITORY), TimeTwisterData.equipped(stack),
            app(stack), macro(stack), order(stack), startGuide, PortalSetupData.get(stack), PortalSetupData.selected(stack)), player);
    }
}
