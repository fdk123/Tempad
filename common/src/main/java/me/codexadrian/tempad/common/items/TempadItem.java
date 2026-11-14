package me.codexadrian.tempad.common.items;

import dev.architectury.injectables.annotations.PlatformOnly;
import me.codexadrian.tempad.api.options.TempadOption;
import me.codexadrian.tempad.api.options.TempadOptionApi;
import me.codexadrian.tempad.client.config.TempadClientConfig;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.ConfigCache;
import me.codexadrian.tempad.common.config.TempadConfig;
import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.data.PortalSetupData;
import me.codexadrian.tempad.common.data.PlayerLocationHandler;
import me.codexadrian.tempad.common.data.MarkerIndex;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.SlotAccess;
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
        return getFuelCapacity() + (TimeTwisterData.equipped(stack) ? TempadConfig.timeTwisterCapacity : 0);
    }

    @Override public int getChronons(ItemStack stack) { return ChrononStorage.get(stack) + TimeTwisterData.reserve(stack); }

    @Override public int insertChronons(ItemStack stack, int amount) {
        int main = ChrononStorage.insert(stack, amount, getFuelCapacity());
        return main + TimeTwisterData.insert(stack, amount - main);
    }

    @Override public int extractChronons(ItemStack stack, int amount) {
        int main = ChrononStorage.extract(stack, amount);
        return main + TimeTwisterData.extract(stack, amount - main);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand interactionHand) {
        ItemStack stack = player.getItemInHand(interactionHand);
        if (!player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                TempadSettings.openDefault(serverPlayer, stack);
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
        return summonTimeDoor(locationData, player, color, player.position(), player.getDirection(), player.level(), null, player.getXRot() > 45.0F);
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

    public static TimedoorEntity summonTimeDoor(LocationData locationData, Player player, int color, BlockPos origin,
                                                Direction direction, Level sourceLevel, PortalSetupData placement) {
        return summonTimeDoor(locationData, player, color, Vec3.atCenterOf(origin), direction, sourceLevel, placement);
    }

    private static TimedoorEntity summonTimeDoor(LocationData locationData, Player player, int color, Vec3 origin, Direction direction, Level sourceLevel) {
        return summonTimeDoor(locationData, player, color, origin, direction, sourceLevel, null);
    }

    private static TimedoorEntity summonTimeDoor(LocationData locationData, Player player, int color, Vec3 origin,
                                                 Direction direction, Level sourceLevel, @Nullable PortalSetupData placement) {
        return summonTimeDoor(locationData, player, color, origin, direction, sourceLevel, placement, false);
    }

    private static TimedoorEntity summonTimeDoor(LocationData locationData, Player player, int color, Vec3 origin,
                                                 Direction direction, Level sourceLevel, @Nullable PortalSetupData placement,
                                                 boolean handheldFloor) {
        TimedoorEntity timedoor = new TimedoorEntity(TempadRegistry.TIMEDOOR_ENTITY.get(), sourceLevel);
        timedoor.setColor(locationData.getPortalColor() >= 0 ? locationData.getPortalColor() : color);
        timedoor.setLocation(locationData);
        timedoor.setOwner(player.getUUID());
        timedoor.setTargetPlayer(PlayerLocationHandler.playerIdForLocation(sourceLevel, player.getUUID(), locationData.getId()));
        if (MarkerIndex.resolve(sourceLevel, player.getUUID(), locationData.getId()) != null)
            timedoor.setTargetMarker(locationData.getId());
        Vec3 offset = placement == null
            ? new Vec3(direction.getStepX() * TempadConfig.distanceFromPlayer, 0, direction.getStepZ() * TempadConfig.distanceFromPlayer)
            : placement.normalized().offset(direction);
        Vec3 doorPos = origin.add(offset);
        if (!sourceLevel.getWorldBorder().isWithinBounds(BlockPos.containing(doorPos))) return null;
        timedoor.setPos(doorPos.x, doorPos.y, doorPos.z);
        timedoor.setYRot(direction.getOpposite().toYRot() + (placement == null ? 0 : placement.angle()));
        timedoor.setFloorMode(placement == null ? handheldFloor : !placement.upright());
        if (!sourceLevel.addFreshEntity(timedoor)) return null;
        timedoor.playSound(Tempad.TIMEDOOR_SOUND.get());
        return timedoor;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> components, @NotNull TooltipFlag flag) {
        this.getOption().addToolTip(stack, level, components, flag);
        var installed = TempadUpgrades.installed(stack);
        for (int i = 0; i < installed.size(); i++) {
            net.minecraft.resources.ResourceLocation upgrade = net.minecraft.resources.ResourceLocation.tryParse(installed.getString(i));
            if (upgrade != null) components.add(Component.translatable("upgrade." + upgrade.getNamespace() + "." + upgrade.getPath()));
        }
        if (TimeTwisterData.equipped(stack)) components.add(Component.translatable("item.tempad.time_twister.installed", TimeTwisterData.reserve(stack), ConfigCache.timeTwisterCapacity));
    }

    @Override public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        if (TimeTwisterData.equipped(stack) && !slot.hasItem()) {
            slot.set(TimeTwisterData.uninstall(stack));
            return true;
        }
        if (!TimeTwisterData.equipped(stack) && slot.getItem().getItem() instanceof TimeTwisterItem) {
            if (TimeTwisterData.install(stack, slot.getItem())) { slot.set(ItemStack.EMPTY); return true; }
        }
        return false;
    }

    @Override public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !(other.getItem() instanceof TimeTwisterItem)) return false;
        if (!TimeTwisterData.install(stack, other)) return false;
        access.set(ItemStack.EMPTY);
        slot.setChanged();
        return true;
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
