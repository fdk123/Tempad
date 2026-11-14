package me.codexadrian.tempad.common.items;

import me.codexadrian.tempad.common.utils.PlatformUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;

/** Cycles the 3.0 public and optional FTB Teams access modes. */
public final class ScreeningDeviceItem extends Item {
    public static final String PUBLIC = "public";
    public static final String FTB_TEAMS = "ftbteams";
    public static final String GUILD = "guild";
    public static final String PARTY = "party";
    private static final String ACCESS = "AccessId";

    public ScreeningDeviceItem(Properties properties) { super(properties.stacksTo(1)); }

    public static String mode(ItemStack stack) {
        if (!(stack.getItem() instanceof ScreeningDeviceItem) || !stack.hasTag()) return "off";
        String value = stack.getTag().getString(ACCESS);
        if (PUBLIC.equals(value)) return PUBLIC;
        if (FTB_TEAMS.equals(value) && PlatformUtils.isModLoaded("ftbteams")) return FTB_TEAMS;
        if ((GUILD.equals(value) || PARTY.equals(value)) && PlatformUtils.isModLoaded("argonauts")) return value;
        return "off";
    }

    private static String nextMode(String current) {
        List<String> modes = new ArrayList<>(List.of("off", PUBLIC));
        if (PlatformUtils.isModLoaded("ftbteams")) modes.add(FTB_TEAMS);
        if (PlatformUtils.isModLoaded("argonauts")) {
            modes.add(GUILD);
            modes.add(PARTY);
        }
        return modes.get((modes.indexOf(current) + 1) % modes.size());
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            String next = nextMode(mode(stack));
            if ("off".equals(next)) stack.getOrCreateTag().remove(ACCESS);
            else stack.getOrCreateTag().putString(ACCESS, next);
            player.displayClientMessage(Component.translatable("item.tempad.screening_device.mode",
                Component.translatable("item.tempad.screening_device." + next)), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.tempad.screening_device.mode",
            Component.translatable("item.tempad.screening_device." + mode(stack))));
    }
}
