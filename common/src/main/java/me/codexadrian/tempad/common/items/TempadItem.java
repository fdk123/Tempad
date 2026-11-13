package me.codexadrian.tempad.common.items;

import dev.architectury.injectables.annotations.PlatformOnly;
import me.codexadrian.tempad.api.options.TempadOption;
import me.codexadrian.tempad.api.options.TempadOptionApi;
import me.codexadrian.tempad.client.config.TempadClientConfig;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.ConfigCache;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.data.TempadLocationHandler;
import me.codexadrian.tempad.common.entity.TimedoorEntity;
import me.codexadrian.tempad.common.network.NetworkHandler;
import me.codexadrian.tempad.common.network.messages.c2s.OpenFavoritedLocationPacket;
import me.codexadrian.tempad.common.network.messages.s2c.OpenTempadScreenPacket;
import me.codexadrian.tempad.common.registry.TempadRegistry;
import me.codexadrian.tempad.common.utils.ClientUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TempadItem extends Item implements TempadPower, ChrononContainerItem {
    public TempadItem(Properties properties) {
        super(properties);
    }

    public TempadOption getOption() {
        return TempadOptionApi.getOption(ConfigCache.tempadFuelType);
    }

    public int getFuelCost() {
        return ConfigCache.tempadFuelConsumptionValue;
    }

    public int getFuelCapacity() {
        return ConfigCache.tempadFuelCapacityValue;
    }

    @Override
    public int getChrononCapacity(ItemStack stack) {
        return getFuelCapacity();
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand interactionHand) {
        ItemStack stack = player.getItemInHand(interactionHand);
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                OpenTempadScreenPacket packet = new OpenTempadScreenPacket(new ArrayList<>(TempadLocationHandler.getLocations(level, player.getUUID()).values()), TempadLocationHandler.getFavorite(level, player.getUUID()));
                NetworkHandler.CHANNEL.sendToPlayer(packet, player);
                return InteractionResultHolder.success(stack);
            } else {
                return InteractionResultHolder.pass(stack);
            }
        } else {
            if (level.isClientSide) {
                ClientUtils.openFavorited();
                return InteractionResultHolder.pass(stack);
            } else {
                return InteractionResultHolder.success(stack);
            }
        }
    }

    public static TimedoorEntity summonTimeDoor(LocationData locationData, Player player, int color) {
        return summonTimeDoor(locationData, player, color, player.position(), player.getDirection(), player.level());
    }

    public static TimedoorEntity summonTimeDoor(LocationData locationData, Player player, int color, BlockPos origin, Direction direction) {
        return summonTimeDoor(locationData, player, color, Vec3.atCenterOf(origin), direction, player.level());
    }

    public static TimedoorEntity summonTimeDoor(LocationData locationData, Player player, int color, Vec3 origin, Direction direction) {
        return summonTimeDoor(locationData, player, color, origin, direction, player.level());
    }

    public static TimedoorEntity summonTimeDoor(LocationData locationData, Player player, int color, BlockPos origin, Direction direction, Level sourceLevel) {
        return summonTimeDoor(locationData, player, color, Vec3.atCenterOf(origin), direction, sourceLevel);
    }

    private static TimedoorEntity summonTimeDoor(LocationData locationData, Player player, int color, Vec3 origin, Direction direction, Level sourceLevel) {
        TimedoorEntity timedoor = new TimedoorEntity(TempadRegistry.TIMEDOOR_ENTITY.get(), sourceLevel);
        timedoor.setColor(color);
        timedoor.setLocation(locationData);
        timedoor.setOwner(player.getUUID());
        timedoor.setPos(origin.x() + direction.getStepX() * TempadConfig.distanceFromPlayer, origin.y(), origin.z() + direction.getStepZ() * TempadConfig.distanceFromPlayer);
        timedoor.setYRot(direction.getOpposite().toYRot());
        if (!sourceLevel.addFreshEntity(timedoor)) return null;
        timedoor.playSound(Tempad.TIMEDOOR_SOUND.get());
        return timedoor;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> components, @NotNull TooltipFlag flag) {
        this.getOption().addToolTip(stack, level, components, flag);
    }

    @Override
    public int getBarColor(@NotNull ItemStack $$0) {
        return TempadClientConfig.color;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return this.getOption().isDurabilityBarVisible(stack);
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return this.getOption().durabilityBarWidth(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        this.getOption().tick(stack, entity);
    }

    @PlatformOnly(PlatformOnly.FABRIC)
    public boolean allowNbtUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    @PlatformOnly(PlatformOnly.FORGE)
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }
}
