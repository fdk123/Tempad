package me.codexadrian.tempad.common.utils;

import dev.architectury.injectables.annotations.ExpectPlatform;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.ConfigCache;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.items.TempadItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class TeleportUtils {

    public static boolean mayTeleport(ResourceKey<Level> level, Player player) {
        if (level == null) return false;
        if (player.level().isClientSide()) {
            if (isDimensionBlacklisted(player.level(), level)) return false;
            return level.equals(player.level().dimension()) ? ConfigCache.allowIntradimensionalTravel : ConfigCache.allowInterdimensionalTravel;
        }
        return mayTravel(player.level(), level);
    }

    public static boolean mayTravel(Level sourceLevel, ResourceKey<Level> destination) {
        if (destination == null || isDimensionBlacklisted(sourceLevel, destination)) return false;
        if (!(sourceLevel instanceof ServerLevel serverLevel)) return false;
        ServerLevel targetLevel = serverLevel.getServer().getLevel(destination);
        if (targetLevel == null) return false;
        if (sourceLevel.dimension().equals(destination))
            return TempadConfig.allowIntradimensionalTravel
                && !sourceLevel.dimensionTypeRegistration().is(Tempad.INTRADIMENSIONAL_TRAVEL_NOT_SUPPORTED);
        return TempadConfig.allowInterdimensionalTravel
            && !sourceLevel.dimensionTypeRegistration().is(Tempad.LEAVING_NOT_SUPPORTED)
            && !targetLevel.dimensionTypeRegistration().is(Tempad.ENTERING_NOT_SUPPORTED);
    }

    public static boolean isDimensionBlacklisted(Level sourceLevel, ResourceKey<Level> level) {
        var lookup = sourceLevel.registryAccess().lookup(Registries.DIMENSION);
        if (lookup.isPresent()) {
            var levelReference = lookup.get().get(level);
            if (levelReference.isPresent()) {
                return levelReference.get().is(Tempad.TEMPAD_DIMENSION_BLACKLIST);
            }
        }
        return false;
    }

    public static ItemStack findAndReplaceTempad(Player player, @Nullable ItemStack replacementTempad) {
        AtomicReference<ItemStack> tempad = new AtomicReference<>(ItemStack.EMPTY);
        Consumer<ItemStack> setTempad = null;

        if (isBaubleModLoaded()) {
            setTempad = BaubleUtils.findTempadInBaubles(player, tempad::set);
        }

        if (setTempad != null) {
            if (replacementTempad != null) {
                setTempad.accept(replacementTempad);
            }
            return tempad.get();
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof TempadItem tempadItem) {
                tempad.set(stack);
                int finalI = i;
                setTempad = (itemStack) -> player.getInventory().setItem(finalI, itemStack);

                if (tempadItem.getOption().canTimedoorOpen(player, tempad.get())) {
                    break;
                }
            }
        }

        if (replacementTempad != null && setTempad != null) {
            setTempad.accept(replacementTempad);
        }

        return tempad.get();
    }

    public static ItemStack findTempad(Player player) {
        return findAndReplaceTempad(player, null);
    }

    public static boolean hasTempad(Player player) {
        return !findTempad(player).isEmpty();
    }

    @ExpectPlatform
    public static boolean isBaubleModLoaded() {
        throw new NotImplementedException();
    }
}
