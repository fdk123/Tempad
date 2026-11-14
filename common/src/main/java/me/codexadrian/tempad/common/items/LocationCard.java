package me.codexadrian.tempad.common.items;

import com.teamresourceful.resourcefullib.common.utils.CommonUtils;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.data.TempadLocationHandler;
import me.codexadrian.tempad.common.data.MarkerIndex;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class LocationCard extends Item {
    public LocationCard(Properties properties) {
        super(properties.stacksTo(16));
    }

    public static void setLocation(ItemStack stack, LocationData locationData, String creator) {
        stack.getOrCreateTag().put("Location", locationData.toTag());
        stack.getOrCreateTag().putString("Creator", creator);
        stack.getOrCreateTag().remove("MarkerId");
    }

    @Nullable
    public static LocationData getLocation(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains("Location")) return null;
        return LocationData.fromTag(stack.getOrCreateTag().getCompound("Location"));
    }

    @Nullable public static LocationData resolveLocation(ItemStack stack, Level level, UUID player) {
        if (stack.hasTag() && stack.getTag().hasUUID("MarkerId") && !level.isClientSide)
            return MarkerIndex.resolve(level, player, stack.getTag().getUUID("MarkerId"));
        return getLocation(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        var location = getLocation(stack);

        if (location != null) {
            tooltipComponents.add(Component.translatable("item.tempad.location_card.shift_toolip", Component.literal("SHIFT").withStyle(Screen.hasShiftDown() ? ChatFormatting.WHITE : ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY));

            if (Screen.hasShiftDown()) {
                tooltipComponents.add(Component.literal(location.getName()).withStyle(ChatFormatting.GRAY));
                tooltipComponents.add(Component.translatable("gui.tempad.x", location.getBlockPos().getX()).withStyle(ChatFormatting.GRAY));
                tooltipComponents.add(Component.translatable("gui.tempad.y", location.getBlockPos().getY()).withStyle(ChatFormatting.GRAY));
                tooltipComponents.add(Component.translatable("gui.tempad.z", location.getBlockPos().getZ()).withStyle(ChatFormatting.GRAY));
                tooltipComponents.add(Component.translatable("gui.tempad.dimension", Component.translatable(location.getLevelKey().location().toLanguageKey("dimension"))).withStyle(ChatFormatting.GRAY));

                if (stack.hasTag() && stack.getTag().contains("Creator")) {
                    tooltipComponents.add(Component.translatable("item.tempad.location_card.creator_toolip", Component.translatable(stack.getTag().getString("Creator")).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
                }
            }
        } else {
            tooltipComponents.add(Component.translatable("item.tempad.location_card.add_location").withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("item.tempad.location_card.set_name").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide) {
            ItemStack card = player.getItemInHand(usedHand);
            var location = resolveLocation(card, level, player.getUUID());
            if (location == null && card.hasTag() && card.getTag().hasUUID("MarkerId")) {
                player.displayClientMessage(Component.translatable("item.tempad.location_card.marker_unavailable"), true);
                return InteractionResultHolder.fail(card);
            }
            if (location != null) {
                if (!player.isShiftKeyDown() && insertIntoWallet(player, card)) {
                    player.displayClientMessage(Component.translatable("item.tempad.location_card.stored"), true);
                    return InteractionResultHolder.success(card);
                }
                if (card.hasTag() && card.getTag().hasUUID("MarkerId")) {
                    player.displayClientMessage(Component.translatable("item.tempad.location_card.marker_live"), true);
                    return InteractionResultHolder.success(card);
                }
                TempadLocationHandler.addLocation(level, player.getUUID(), location);
                card.shrink(1);
                player.displayClientMessage(Component.translatable("item.tempad.location_card.added_location", Component.literal(location.getName()).withStyle(ChatFormatting.GOLD)), true);
            } else {
                var newLocation = new LocationData(card.getHoverName().getString(), level.dimension(), player.blockPosition(), CommonUtils.generate(id -> !TempadLocationHandler.containsLocation(level, player.getUUID(), id), UUID::randomUUID));
                ItemStack written = card;
                if (card.getCount() > 1) {
                    written = card.copy();
                    written.setCount(1);
                    card.shrink(1);
                }
                setLocation(written, newLocation, player.getDisplayName().getString());
                if (written != card) player.getInventory().placeItemBackInInventory(written);
                player.displayClientMessage(Component.translatable("item.tempad.location_card.set_location", Component.literal(newLocation.getName()).withStyle(ChatFormatting.GOLD)), true);
            }
            return InteractionResultHolder.success(card);
        } else {
            return InteractionResultHolder.pass(player.getItemInHand(usedHand));
        }
    }

    private static boolean insertIntoWallet(Player player, ItemStack cards) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack wallet = player.getInventory().getItem(slot);
            if (!(wallet.getItem() instanceof CardWalletItem)) continue;
            if (new WalletContainer(wallet).insert(cards) > 0) {
                player.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }
}
