package me.codexadrian.tempad.common.network.messages.c2s;

import com.teamresourceful.resourcefullib.common.networking.base.Packet;
import com.teamresourceful.resourcefullib.common.networking.base.PacketContext;
import com.teamresourceful.resourcefullib.common.networking.base.PacketHandler;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.items.TempadItem;
import me.codexadrian.tempad.common.items.TempadSettings;
import me.codexadrian.tempad.common.utils.TeleportUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record SaveTempadSettingsPacket(UUID deviceId, String app, String macro, String order) implements Packet<SaveTempadSettingsPacket> {
    public static final ResourceLocation ID = new ResourceLocation(Tempad.MODID, "save_tempad_settings");
    public static final PacketHandler<SaveTempadSettingsPacket> HANDLER = new Handler();
    @Override public ResourceLocation getID() { return ID; }
    @Override public PacketHandler<SaveTempadSettingsPacket> getHandler() { return HANDLER; }

    private static final class Handler implements PacketHandler<SaveTempadSettingsPacket> {
        @Override public void encode(SaveTempadSettingsPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.deviceId);
            buffer.writeUtf(packet.app, 32);
            buffer.writeUtf(packet.macro, 32);
            buffer.writeUtf(packet.order, 32);
        }
        @Override public SaveTempadSettingsPacket decode(FriendlyByteBuf buffer) {
            return new SaveTempadSettingsPacket(buffer.readUUID(), buffer.readUtf(32), buffer.readUtf(32), buffer.readUtf(32));
        }
        @Override public PacketContext handle(SaveTempadSettingsPacket packet) {
            return (player, level) -> {
                var stack = TempadSettings.find(player, packet.deviceId);
                if (stack.getItem() instanceof TempadItem) TempadSettings.save(stack, packet.app, packet.macro, packet.order);
            };
        }
    }
}
