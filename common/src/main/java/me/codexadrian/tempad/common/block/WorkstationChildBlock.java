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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.List;

public final class WorkstationChildBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public WorkstationChildBlock(Properties properties) { super(properties); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) { return List.of(); }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState facingState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise() && !facingState.is(TempadRegistry.WORKSTATION_BLOCK.get())) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, facingState, level, pos, neighborPos);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos parent = pos.relative(facing.getCounterClockWise());
            if (level.getBlockState(parent).is(TempadRegistry.WORKSTATION_BLOCK.get())) level.destroyBlock(parent, true);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
