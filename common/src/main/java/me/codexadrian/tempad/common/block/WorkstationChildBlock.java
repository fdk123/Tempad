package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.registry.TempadRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import java.util.List;

public final class WorkstationChildBlock extends Block {
    private static final VoxelShape NORTH_SHAPE = Shapes.or(Block.box(1, 0, 0, 15, 2, 16), Block.box(1, 2, 0, 15, 11, 10));
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(Block.box(1, 0, 0, 15, 2, 16), Block.box(1, 2, 6, 15, 11, 16));
    private static final VoxelShape WEST_SHAPE = Shapes.or(Block.box(0, 0, 1, 16, 2, 15), Block.box(0, 2, 1, 10, 11, 15));
    private static final VoxelShape EAST_SHAPE = Shapes.or(Block.box(0, 0, 1, 16, 2, 15), Block.box(6, 2, 1, 16, 11, 15));

    public WorkstationChildBlock(Properties properties) { super(properties); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) { return List.of(); }

    private static BlockPos parentPos(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise());
    }

    private static boolean matchesParent(BlockState child, BlockState parent) {
        return parent.is(TempadRegistry.WORKSTATION_BLOCK.get())
            && parent.getValue(BlockStateProperties.HORIZONTAL_FACING) == child.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos parentPos = parentPos(state, pos);
        BlockState parent = level.getBlockState(parentPos);
        if (!matchesParent(state, parent)) return InteractionResult.PASS;
        return ((WorkstationBlock) parent.getBlock()).use(parent, level, parentPos, player, hand, hit);
    }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState facingState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise() && !matchesParent(state, facingState)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, facingState, level, pos, neighborPos);
    }

    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos parentPos = parentPos(state, pos);
            if (matchesParent(state, level.getBlockState(parentPos))) {
                if (player.isCreative() && level.getBlockEntity(parentPos) instanceof WorkstationBlockEntity workstation
                        && !workstation.getTempad().isEmpty()) {
                    Block.popResource(level, parentPos, workstation.eject());
                }
                level.destroyBlock(parentPos, !player.isCreative());
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos parent = parentPos(state, pos);
            if (matchesParent(state, level.getBlockState(parent)) && level.getBlockEntity(parent) instanceof WorkstationBlockEntity) level.destroyBlock(parent, true);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
