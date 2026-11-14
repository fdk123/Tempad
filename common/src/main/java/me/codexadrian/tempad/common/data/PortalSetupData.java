package me.codexadrian.tempad.common.data;

import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.items.TempadItem;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Server validated portal placement and workstation destination stored on a Tempad. */
public record PortalSetupData(float leftRight, float upDown, float forwardBack, int angle, boolean upright) {
    private static final String SETTINGS = "PortalSetup";
    private static final String TARGET = "SelectedPortalLocation";

    public static PortalSetupData defaults() {
        return new PortalSetupData(0, 0, TempadConfig.distanceFromPlayer, 0, true);
    }

    public PortalSetupData normalized() {
        return new PortalSetupData(limit(leftRight), limit(upDown), limit(forwardBack), Math.floorMod(angle, 360), upright);
    }

    private static float limit(float value) { return Float.isFinite(value) ? Mth.clamp(value, -5.0F, 5.0F) : 0.0F; }

    public Vec3 offset(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3(leftRight, upDown, -forwardBack);
            case SOUTH -> new Vec3(-leftRight, upDown, forwardBack);
            case WEST -> new Vec3(-forwardBack, upDown, -leftRight);
            case EAST -> new Vec3(forwardBack, upDown, leftRight);
            default -> Vec3.ZERO;
        };
    }

    public static PortalSetupData get(ItemStack stack) {
        if (!(stack.getItem() instanceof TempadItem) || !stack.hasTag() || !stack.getTag().contains(SETTINGS)) return defaults();
        CompoundTag tag = stack.getTag().getCompound(SETTINGS);
        return new PortalSetupData(tag.getFloat("LeftRight"), tag.getFloat("UpDown"), tag.getFloat("ForwardBack"),
            tag.getInt("Angle"), !tag.contains("Upright") || tag.getBoolean("Upright")).normalized();
    }

    @Nullable public static UUID selected(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID(TARGET) ? stack.getTag().getUUID(TARGET) : null;
    }

    public static void save(ItemStack stack, PortalSetupData settings, @Nullable UUID location) {
        if (!(stack.getItem() instanceof TempadItem)) return;
        PortalSetupData safe = settings.normalized();
        CompoundTag tag = new CompoundTag();
        tag.putFloat("LeftRight", safe.leftRight);
        tag.putFloat("UpDown", safe.upDown);
        tag.putFloat("ForwardBack", safe.forwardBack);
        tag.putInt("Angle", safe.angle);
        tag.putBoolean("Upright", safe.upright);
        stack.getOrCreateTag().put(SETTINGS, tag);
        if (location == null) stack.getTag().remove(TARGET);
        else stack.getTag().putUUID(TARGET, location);
    }
}
