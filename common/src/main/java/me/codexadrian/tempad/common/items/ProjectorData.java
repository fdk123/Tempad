package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.data.MarkerIndex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

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
        if (stack.hasTag()) stack.getTag().remove("MarkerId");
        if (target == null) {
            if (stack.hasTag()) stack.getTag().remove(TARGET);
        } else stack.getOrCreateTag().put(TARGET, target.toTag());
    }

    @Nullable public static UUID markerId(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID("MarkerId") ? stack.getTag().getUUID("MarkerId") : null;
    }

    public static void setFromCard(ItemStack projector, ItemStack card) {
        setTarget(projector, cardTarget(card));
        UUID id = markerId(card);
        if (id != null) projector.getOrCreateTag().putUUID("MarkerId", id);
    }

    @Nullable public static LocationData resolveTarget(ItemStack stack, Level level, UUID player) {
        UUID id = markerId(stack);
        return id != null && !level.isClientSide ? MarkerIndex.resolve(level, player, id) : getTarget(stack);
    }

    public static ItemStack cardFor(LocationData target) {
        return cardFor(target, null);
    }

    public static ItemStack cardFor(LocationData target, @Nullable UUID markerId) {
        ItemStack card = new ItemStack(me.codexadrian.tempad.common.registry.TempadRegistry.LOCATION_CARD.get());
        card.getOrCreateTag().put("Location", target.toTag());
        if (markerId != null) card.getOrCreateTag().putUUID("MarkerId", markerId);
        return card;
    }
}
