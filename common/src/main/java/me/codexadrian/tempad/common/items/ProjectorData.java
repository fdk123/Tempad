package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.data.LocationData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Card target shared by portable and placed Timedoor Projectors. */
public final class ProjectorData {
    private static final String TARGET = "PortalTarget";
    private ProjectorData() {}

    @Nullable public static LocationData getTarget(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TARGET)) return null;
        try {
            return LocationData.fromTag(stack.getTag().getCompound(TARGET));
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    @Nullable public static LocationData cardTarget(ItemStack card) {
        try {
            return LocationCard.getLocation(card);
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    public static void setTarget(ItemStack stack, @Nullable LocationData target) {
        if (target == null) {
            if (stack.hasTag()) stack.getTag().remove(TARGET);
        } else stack.getOrCreateTag().put(TARGET, target.toTag());
    }

    public static ItemStack cardFor(LocationData target) {
        ItemStack card = new ItemStack(me.codexadrian.tempad.common.registry.TempadRegistry.LOCATION_CARD.get());
        card.getOrCreateTag().put("Location", target.toTag());
        return card;
    }
}
