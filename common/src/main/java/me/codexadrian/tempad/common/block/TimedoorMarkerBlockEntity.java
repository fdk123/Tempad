package me.codexadrian.tempad.common.block;

import me.codexadrian.tempad.common.data.MarkerIndex;
import me.codexadrian.tempad.common.network.NetworkHandler;
import me.codexadrian.tempad.common.network.messages.s2c.OpenMarkerScreenPacket;
import me.codexadrian.tempad.common.registry.TempadRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Shared state for Timedoor Markers and configurable Chronomarks. */
public final class TimedoorMarkerBlockEntity extends BlockEntity {
    private UUID markerId = UUID.randomUUID();
    private UUID owner;
    private String name = "Marker";
    private int color = 0xFF6F00;
    private boolean publicAccess;
    private boolean locked = true;
    private float yOffset;
    private int angle;

    public TimedoorMarkerBlockEntity(BlockPos pos, BlockState state) { super(TempadRegistry.MARKER_BLOCK_ENTITY.get(), pos, state); }
    public UUID getMarkerId() { return markerId; }
    public void setMarkerId(UUID id) { markerId = id; setChanged(); }
    @Nullable public UUID getOwner() { return owner; }
    public boolean isChronomark() { return getBlockState().is(TempadRegistry.CHRONOMARK_BLOCK.get()); }
    public boolean canEdit(Player player) { return owner == null || owner.equals(player.getUUID()) || !locked; }

    public void claim(Player player) {
        if (owner == null) { owner = player.getUUID(); changed(); MarkerIndex.publish(this); }
    }

    public void restoreFrom(ItemStack stack, Player placer) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID("MarkerId")) markerId = tag.getUUID("MarkerId");
        if (tag != null && tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
        if (owner == null) owner = placer.getUUID();
        if (tag != null && tag.contains("MarkerName")) name = tag.getString("MarkerName");
        else name = isChronomark() ? "Chronomark" : "Timedoor Marker";
        if (tag != null && tag.contains("MarkerColor")) color = tag.getInt("MarkerColor") & 0xFFFFFF;
        if (tag != null) {
            publicAccess = tag.getBoolean("PublicAccess");
            locked = !tag.contains("Locked") || tag.getBoolean("Locked");
            yOffset = tag.contains("YOffset") ? tag.getFloat("YOffset") : (getBlockState().getValue(TimedoorMarkerBlock.CEILING) ? -2.0F : 0.5F);
            angle = tag.contains("Angle") ? tag.getInt("Angle") : (int) getBlockState().getValue(TimedoorMarkerBlock.FACING).toYRot();
        } else {
            yOffset = getBlockState().getValue(TimedoorMarkerBlock.CEILING) ? -2.0F : 0.5F;
            angle = (int) getBlockState().getValue(TimedoorMarkerBlock.FACING).toYRot();
        }
        changed();
        MarkerIndex.publish(this);
    }

    public MarkerIndex.Entry snapshot() {
        return new MarkerIndex.Entry(markerId, getLevel().dimension(), getBlockPos(), owner,
            name, color, publicAccess, locked, isChronomark(),
            getBlockState().getValue(TimedoorMarkerBlock.FACING), getBlockState().getValue(TimedoorMarkerBlock.CEILING), yOffset, angle);
    }

    public void update(Player player, String name, int color, boolean publicAccess, boolean lock, float yOffset, int angle) {
        if (!canEdit(player)) return;
        this.name = name.strip().substring(0, Math.min(48, name.strip().length()));
        if (this.name.isEmpty()) this.name = isChronomark() ? "Chronomark" : "Timedoor Marker";
        this.color = color & 0xFFFFFF;
        this.publicAccess = publicAccess;
        this.locked = owner != null && owner.equals(player.getUUID()) && lock;
        if (isChronomark()) {
            this.yOffset = Math.max(-5.0F, Math.min(5.0F, yOffset));
            this.angle = Math.floorMod(angle, 360);
        }
        changed();
        MarkerIndex.publish(this);
    }

    public void openScreen(Player player) {
        NetworkHandler.CHANNEL.sendToPlayer(new OpenMarkerScreenPacket(getBlockPos(), isChronomark(), name,
            color, publicAccess, locked, yOffset, angle), player);
    }

    public void dye(Player player, int color) {
        if (!canEdit(player)) return;
        this.color = color & 0xFFFFFF;
        changed();
        MarkerIndex.publish(this);
    }

    public void writeTo(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID("MarkerId", markerId);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("MarkerName", name);
        tag.putInt("MarkerColor", color);
        tag.putBoolean("PublicAccess", publicAccess);
        tag.putBoolean("Locked", locked);
        tag.putFloat("YOffset", yOffset);
        tag.putInt("Angle", angle);
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putUUID("MarkerId", markerId);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("MarkerName", name);
        tag.putInt("MarkerColor", color);
        tag.putBoolean("PublicAccess", publicAccess);
        tag.putBoolean("Locked", locked);
        tag.putFloat("YOffset", yOffset);
        tag.putInt("Angle", angle);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        markerId = tag.hasUUID("MarkerId") ? tag.getUUID("MarkerId") : UUID.randomUUID();
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        name = tag.contains("MarkerName") ? tag.getString("MarkerName") : "Marker";
        color = tag.contains("MarkerColor") ? tag.getInt("MarkerColor") & 0xFFFFFF : 0xFF6F00;
        publicAccess = tag.getBoolean("PublicAccess");
        locked = !tag.contains("Locked") || tag.getBoolean("Locked");
        yOffset = tag.getFloat("YOffset");
        angle = tag.getInt("Angle");
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag tag = new CompoundTag(); saveAdditional(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
