package com.example.trialmod.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundExplosionAlertPacket {
    private final BlockPos pos;

    public ClientboundExplosionAlertPacket(BlockPos pos) {
        this.pos = pos;
    }

    public ClientboundExplosionAlertPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ClientPacketHandler.handleExplosionAlert(pos);
        });
        return true;
    }

    private static class ClientPacketHandler {
        private static void handleExplosionAlert(BlockPos pos) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                // Play loud local bell warning at position of generator explosion
                mc.level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(),
                        SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 5.0f, 0.5f, false);
                // Direct notification sound to the killer player
                mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0f, 0.5f, false);
            }
        }
    }
}
