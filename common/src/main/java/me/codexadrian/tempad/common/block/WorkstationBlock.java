package me.codexadrian.tempad.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class WorkstationBlock extends BaseEntityBlock {
    public WorkstationBlock(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WorkstationBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return level.isClientSide ? null : type == me.codexadrian.tempad.common.registry.TempadRegistry.WORKSTATION_BLOCK_ENTITY.get() ? (level1, pos, state1, entity) -> WorkstationBlockEntity.tick(level1, pos, state1, (WorkstationBlockEntity) entity) : null; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WorkstationBlockEntity workstation)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!workstation.canAccess(player)) { player.displayClientMessage(Component.translatable("block.tempad.workstation.locked"), true); return InteractionResult.FAIL; }
        if (player.isShiftKeyDown() && !workstation.getTempad().isEmpty()) { workstation.toggleLock(player); return InteractionResult.CONSUME; }
        ItemStack held = player.getItemInHand(hand);
        if (workstation.getTempad().isEmpty() && workstation.insert(player, held)) { held.shrink(1); return InteractionResult.CONSUME; }
        if (held.isEmpty() && !workstation.getTempad().isEmpty()) { player.setItemInHand(hand, workstation.eject()); return InteractionResult.CONSUME; }
        return InteractionResult.PASS;
    }
}
