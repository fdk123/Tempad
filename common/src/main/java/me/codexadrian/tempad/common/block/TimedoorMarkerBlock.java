package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.data.MarkerIndex;
import me.codexadrian.tempad.common.items.LocationCard;
import me.codexadrian.tempad.common.registry.TempadRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Shared block behavior for the 3.0 Timedoor Marker and Chronomark. */
public final class TimedoorMarkerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CEILING = BooleanProperty.create("ceiling");
    private static final VoxelShape FLOOR_SHAPE = Block.box(0, 0, 0, 16, 4, 16);
    private static final VoxelShape CEILING_SHAPE = Block.box(0, 12, 0, 16, 16, 16);

    public TimedoorMarkerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CEILING, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, CEILING); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TimedoorMarkerBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return state.getValue(CEILING) ? CEILING_SHAPE : FLOOR_SHAPE; }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(CEILING, context.getClickedFace() == Direction.DOWN);
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof TimedoorMarkerBlockEntity marker) marker.restoreFrom(stack, player);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TimedoorMarkerBlockEntity marker)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!player.mayInteract(level, pos)) return InteractionResult.FAIL;
        marker.claim(player);
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof LocationCard) {
            var linked = MarkerIndex.resolve(level, player.getUUID(), marker.getMarkerId());
            if (linked == null) {
                player.displayClientMessage(Component.translatable("item.tempad.location_card.marker_unavailable"), true);
                return InteractionResult.FAIL;
            }
            LocationCard.setLocation(held, linked, player.getDisplayName().getString());
            held.getOrCreateTag().putUUID("MarkerId", marker.getMarkerId());
            player.displayClientMessage(Component.translatable("block.tempad.marker.card_bound"), true);
            return InteractionResult.CONSUME;
        }
        if (!marker.canEdit(player)) {
            player.displayClientMessage(Component.translatable("block.tempad.marker.locked"), true);
            return InteractionResult.FAIL;
        }
        if (held.getItem() instanceof DyeItem dye) {
            marker.dye(player, dye.getDyeColor().getTextColor());
            if (!player.getAbilities().instabuild) held.shrink(1);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty()) {
            marker.openScreen(player);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof TimedoorMarkerBlockEntity marker && !drops.isEmpty()) marker.writeTo(drops.get(0));
        return drops;
    }

    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof TimedoorMarkerBlockEntity marker) {
            ItemStack drop = new ItemStack(state.is(TempadRegistry.CHRONOMARK_BLOCK.get()) ? TempadRegistry.CHRONOMARK.get() : TempadRegistry.TIMEDOOR_MARKER.get());
            marker.writeTo(drop);
            Block.popResource(level, pos, drop);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof TimedoorMarkerBlockEntity marker) MarkerIndex.remove(level, marker.getMarkerId(), pos);
        super.onRemove(state, level, pos, next, moved);
    }
}
