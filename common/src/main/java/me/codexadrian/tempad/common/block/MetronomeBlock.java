package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.registry.TempadRegistry;
import me.codexadrian.tempad.common.utils.MetronomeOpener;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public final class MetronomeBlock extends BaseEntityBlock {
    public MetronomeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MetronomeBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof MetronomeBlockEntity metronome && placer instanceof Player player) {
            metronome.restoreIdentity(stack);
            metronome.claim(player);
            metronome.setChanged();
        }
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
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof MetronomeBlockEntity metronome)) return 0;
        return Mth.floor(15.0F * metronome.getChronons() / Math.max(1, metronome.getCapacity()));
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof MetronomeBlockEntity metronome && !drops.isEmpty()) metronome.writeIdentity(drops.get(0));
        return drops;
    }

    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof MetronomeBlockEntity metronome) {
            metronome.prepareRemoval();
            ItemStack drop = new ItemStack(TempadRegistry.METRONOME.get());
            metronome.writeIdentity(drop);
            Block.popResource(level, pos, drop);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof MetronomeBlockEntity metronome) {
            metronome.prepareRemoval();
            Containers.dropContents(level, pos, metronome.inventory());
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof MetronomeBlockEntity metronome)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            metronome.claim(player);
            if (!metronome.canAccess(player)) {
                player.displayClientMessage(Component.translatable("block.tempad.metronome.locked"), true);
                return InteractionResult.FAIL;
            }
            if (player.isShiftKeyDown() && metronome.toggleLock(player)) {
                player.displayClientMessage(Component.translatable("block.tempad.metronome.lock_enabled"), true);
                return InteractionResult.CONSUME;
            } else if (player.isShiftKeyDown()) {
                player.displayClientMessage(Component.translatable("block.tempad.metronome.lock_disabled"), true);
                return InteractionResult.CONSUME;
            }
        }
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide && held.isEmpty()) {
            MetronomeOpener.open(player, metronome);
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide && !held.isEmpty() && metronome.receive(held)) {
            player.displayClientMessage(Component.translatable("block.tempad.metronome.charged", metronome.getChronons()), true);
        } else if (!level.isClientSide && held.isEmpty()) {
            player.displayClientMessage(Component.translatable("block.tempad.metronome.status", metronome.getChronons(), metronome.getCapacity()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
