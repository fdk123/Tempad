package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.config.ConfigCache;
import me.codexadrian.tempad.common.items.ChrononContainerItem;
import me.codexadrian.tempad.common.items.ChrononStorage;
import me.codexadrian.tempad.common.registry.TempadRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MetronomeBlockEntity extends BlockEntity {
    private static final String CHRONONS = "Chronons";
    private int chronons;
    private int ticks;

    public MetronomeBlockEntity(BlockPos pos, BlockState state) {
        super(TempadRegistry.METRONOME_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MetronomeBlockEntity entity) {
        if (level.isClientSide) return;
        if (++entity.ticks < ConfigCache.metronomeGenerationInterval) return;
        entity.ticks = 0;
        entity.chronons = Math.min(entity.getCapacity(), entity.chronons + ConfigCache.metronomeGenerationAmount);
        entity.distribute(level);
        entity.setChanged();
    }

    private void distribute(Level level) {
        if (chronons <= 0) return;
        for (Player player : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(worldPosition).inflate(8))) {
            for (int slot = 0; slot < player.getInventory().getContainerSize() && chronons > 0; slot++) {
                ItemStack target = player.getInventory().getItem(slot);
                if (target.is(Tempad.CHRONON_AUTO_CHARGE_BLACKLIST) || !(target.getItem() instanceof ChrononContainerItem container)) continue;
                int moved = container.insertChronons(target, chronons);
                chronons -= moved;
            }
        }
    }

    public boolean receive(ItemStack source) {
        if (!(source.getItem() instanceof ChrononContainerItem container)) return false;
        int moved = Math.min(chronons < getCapacity() ? getCapacity() - chronons : 0, container.getChronons(source));
        if (moved <= 0) return false;
        container.extractChronons(source, moved);
        chronons += moved;
        setChanged();
        return true;
    }

    public int getChronons() {
        return chronons;
    }

    public int getCapacity() {
        return ConfigCache.metronomeCapacity;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(CHRONONS, chronons);
        tag.putInt("Ticks", ticks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        chronons = Math.max(0, Math.min(tag.getInt(CHRONONS), getCapacity()));
        ticks = Math.max(0, tag.getInt("Ticks"));
    }
}
