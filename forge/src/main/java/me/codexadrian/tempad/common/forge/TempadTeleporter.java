package me.codexadrian.tempad.common.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class TempadTeleporter implements ITeleporter {
    private final Vec3 position;
    private final Float yaw;

    public TempadTeleporter(BlockPos pos) {
        this.position = Vec3.atCenterOf(pos);
        this.yaw = null;
    }

    public TempadTeleporter(Vec3 position, float yaw) {
        this.position = position;
        this.yaw = yaw;
    }

    @Nullable
    @Override
    public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        return new PortalInfo(position, entity.getDeltaMovement(), yaw == null ? entity.getYRot() : yaw, entity.getXRot());
    }

    @Override
    public boolean isVanilla() {
        return false;
    }

    @Override
    public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
        return repositionEntity.apply(false);
    }
}
