package me.codexadrian.tempad.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Block;
import me.codexadrian.tempad.common.utils.WorkstationOpener;
import me.codexadrian.tempad.common.registry.TempadRegistry;

public final class WorkstationBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty HAS_TEMPAD = BooleanProperty.create("has_tempad");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 1, 14, 4, 15);
    private static final VoxelShape SOUTH_SHAPE = Block.box(2, 0, 1, 16, 4, 15);
    private static final VoxelShape WEST_SHAPE = Block.box(1, 0, 2, 15, 4, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(1, 0, 0, 15, 4, 14);
    public WorkstationBlock(Properties properties) { super(properties); }
    { registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HAS_TEMPAD, false).setValue(POWERED, false)); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WorkstationBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return level.isClientSide ? null : type == me.codexadrian.tempad.common.registry.TempadRegistry.WORKSTATION_BLOCK_ENTITY.get() ? (level1, pos, state1, entity) -> WorkstationBlockEntity.tick(level1, pos, state1, (WorkstationBlockEntity) entity) : null; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, HAS_TEMPAD, POWERED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos child = childPos(context.getClickedPos(), facing);
        return context.getLevel().getBlockState(child).canBeReplaced(context) ? defaultBlockState().setValue(FACING, facing) : null;
    }
    private static BlockPos childPos(BlockPos pos, Direction facing) { return pos.relative(facing.getCounterClockWise()); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) level.setBlock(childPos(pos, state.getValue(FACING)), TempadRegistry.WORKSTATION_CHILD_BLOCK.get().defaultBlockState().setValue(FACING, state.getValue(FACING)), 3);
        if (level.getBlockEntity(pos) instanceof WorkstationBlockEntity workstation) { workstation.restoreIdentity(stack); workstation.setChanged(); }
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) { case SOUTH -> SOUTH_SHAPE; case WEST -> WEST_SHAPE; case EAST -> EAST_SHAPE; default -> NORTH_SHAPE; };
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState facingState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getCounterClockWise() && !facingState.is(TempadRegistry.WORKSTATION_CHILD_BLOCK.get())) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, facingState, level, pos, neighborPos);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos child = childPos(pos, state.getValue(FACING));
            if (level.getBlockState(child).is(TempadRegistry.WORKSTATION_CHILD_BLOCK.get())) level.setBlock(child, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 35);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    @Override public java.util.List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        java.util.List<ItemStack> drops = super.getDrops(state, builder);
        BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof WorkstationBlockEntity workstation && !drops.isEmpty()) workstation.writeIdentity(drops.get(0));
        return drops;
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof WorkstationBlockEntity workstation)) return 0;
        return workstation.getTempad().isEmpty() ? 0 : 15;
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WorkstationBlockEntity workstation)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!workstation.canAccess(player)) { player.displayClientMessage(Component.translatable("block.tempad.workstation.locked"), true); return InteractionResult.FAIL; }
        if (player.isShiftKeyDown() && !workstation.getTempad().isEmpty()) { workstation.toggleLock(player); return InteractionResult.CONSUME; }
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) { WorkstationOpener.open(player, workstation); return InteractionResult.CONSUME; }
        if (workstation.getTempad().isEmpty() && workstation.insert(player, held)) { held.shrink(1); return InteractionResult.CONSUME; }
        return InteractionResult.PASS;
    }
}
