package me.codexadrian.tempad.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.codexadrian.tempad.common.block.WorkstationBlock;
import me.codexadrian.tempad.common.block.WorkstationBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.core.Direction;

public final class WorkstationRenderer implements BlockEntityRenderer<WorkstationBlockEntity> {
    private final BlockEntityRendererProvider.Context context;

    public WorkstationRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public void render(WorkstationBlockEntity workstation, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (workstation.getTempad().isEmpty()) return;
        pose.pushPose();
        Direction facing = workstation.getBlockState().getValue(WorkstationBlock.FACING);
        pose.translate(0.5D, 0.34D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.scale(0.58F, 0.58F, 0.58F);
        Minecraft.getInstance().getItemRenderer().renderStatic(workstation.getTempad(), ItemDisplayContext.FIXED, light, overlay, pose, buffers, workstation.getLevel(), 0);
        pose.popPose();
    }
}
