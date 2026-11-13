package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.data.LocationData;
import me.codexadrian.tempad.common.items.LocationCard;
import me.codexadrian.tempad.common.items.ProjectorData;
import me.codexadrian.tempad.common.registry.TempadRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The placed form of the Timedoor Projector. A redstone rising edge opens its card target. */
public final class TimedoorProjectorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    public static final BooleanProperty HAS_CARD = BooleanProperty.create("has_card");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public TimedoorProjectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, false).setValue(HAS_CARD, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, TRIGGERED, HAS_CARD); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TimedoorProjectorBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(HAS_CARD, ProjectorData.getTarget(context.getItemInHand()) != null);
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TimedoorProjectorBlockEntity projector) {
            projector.restoreFrom(stack);
            if (placer instanceof Player player) projector.claim(player);
            if (level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above())) {
                level.setBlock(pos, level.getBlockState(pos).setValue(TRIGGERED, true), 3);
                level.scheduleTick(pos, this, 4);
            }
        }
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TimedoorProjectorBlockEntity projector)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        projector.claim(player);
        if (!projector.canAccess(player)) {
            player.displayClientMessage(Component.translatable("block.tempad.timedoor_projector.locked"), true);
            return InteractionResult.FAIL;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof LocationCard) {
            LocationData target = ProjectorData.cardTarget(held);
            if (target != null) {
                if (projector.getTarget() != null) player.getInventory().placeItemBackInInventory(ProjectorData.cardFor(projector.getTarget(), projector.getMarkerLink()));
                projector.setTarget(target, ProjectorData.markerId(held));
                if (!player.getAbilities().instabuild) held.shrink(1);
                return InteractionResult.CONSUME;
            }
        }
        if (!held.isEmpty() && projector.receiveFrom(held) > 0) {
            player.displayClientMessage(Component.translatable("block.tempad.timedoor_projector.charge", projector.getChronons(), projector.getCapacity()), true);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty()) {
            if (player.isShiftKeyDown()) {
                projector.stopTimedoor();
                ItemStack portable = new ItemStack(TempadRegistry.TIMEDOOR_PROJECTOR.get());
                projector.writeTo(portable);
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 35);
                player.getInventory().placeItemBackInInventory(portable);
            } else if (projector.getTarget() != null) {
                player.getInventory().placeItemBackInInventory(ProjectorData.cardFor(projector.getTarget(), projector.getMarkerLink()));
                projector.setTarget(null, null);
            } else player.displayClientMessage(Component.translatable("block.tempad.timedoor_projector.charge", projector.getChronons(), projector.getCapacity()), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos fromPos, boolean moving) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        if (powered && !state.getValue(TRIGGERED)) {
            level.setBlock(pos, state.setValue(TRIGGERED, true), 3);
            level.scheduleTick(pos, this, 4);
        } else if (!powered && state.getValue(TRIGGERED)) level.setBlock(pos, state.setValue(TRIGGERED, false), 3);
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(TRIGGERED) && (level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above()))
                && level.getBlockEntity(pos) instanceof TimedoorProjectorBlockEntity projector) projector.openTimedoor();
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof TimedoorProjectorBlockEntity projector && !drops.isEmpty()) projector.writeTo(drops.get(0));
        return drops;
    }

    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof TimedoorProjectorBlockEntity projector) {
            ItemStack portable = new ItemStack(TempadRegistry.TIMEDOOR_PROJECTOR.get());
            projector.writeTo(portable);
            Block.popResource(level, pos, portable);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof TimedoorProjectorBlockEntity projector) projector.stopTimedoor();
        super.onRemove(state, level, pos, next, moving);
    }

    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof TimedoorProjectorBlockEntity projector)) return 0;
        return Mth.floor(15.0F * projector.getChronons() / projector.getCapacity());
    }
}
