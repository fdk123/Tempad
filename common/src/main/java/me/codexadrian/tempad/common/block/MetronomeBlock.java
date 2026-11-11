package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.registry.TempadRegistry;
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

public final class MetronomeBlock extends BaseEntityBlock {
    public MetronomeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MetronomeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == TempadRegistry.METRONOME_BLOCK_ENTITY.get() ? (level1, pos, state1, entity) -> MetronomeBlockEntity.tick(level1, pos, state1, (MetronomeBlockEntity) entity) : null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof MetronomeBlockEntity metronome)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide && !held.isEmpty() && metronome.receive(held)) {
            player.displayClientMessage(Component.translatable("block.tempad.metronome.charged", metronome.getChronons()), true);
        } else if (!level.isClientSide && held.isEmpty()) {
            player.displayClientMessage(Component.translatable("block.tempad.metronome.status", metronome.getChronons(), metronome.getCapacity()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
