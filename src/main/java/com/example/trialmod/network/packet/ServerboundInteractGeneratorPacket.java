package com.example.trialmod.network.packet;

import com.example.trialmod.block.GeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundInteractGeneratorPacket {
    private final BlockPos pos;

    public ServerboundInteractGeneratorPacket(BlockPos pos) {
        this.pos = pos;
    }

    public ServerboundInteractGeneratorPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            // Verify range (max 6 blocks -> distance squared 36)
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 36.0) {
                return;
            }

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof GeneratorBlockEntity generator) {
                if (!generator.isCompleted()) {
                    float currentProgress = generator.getProgress();
                    float nextProgress = currentProgress + 0.1f; // ~50s solo repair
                    if (nextProgress >= 100.0f) {
                        generator.setProgress(100.0f);
                        generator.setCompleted(true);
                        level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 2.0f, 1.0f);
                    } else {
                        generator.setProgress(nextProgress);
                    }
                }
            }
        });
        return true;
    }
}
