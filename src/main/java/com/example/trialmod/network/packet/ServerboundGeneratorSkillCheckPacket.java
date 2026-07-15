package com.example.trialmod.network.packet;

import com.example.trialmod.block.GeneratorBlockEntity;
import com.example.trialmod.capability.PlayerRole;
import com.example.trialmod.capability.TrialPlayerData;
import com.example.trialmod.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundGeneratorSkillCheckPacket {
    private final BlockPos pos;
    private final boolean success;

    public ServerboundGeneratorSkillCheckPacket(BlockPos pos, boolean success) {
        this.pos = pos;
        this.success = success;
    }

    public ServerboundGeneratorSkillCheckPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.success = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(success);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof GeneratorBlockEntity generator) {
                if (!generator.isCompleted()) {
                    if (success) {
                        float current = generator.getProgress();
                        generator.setProgress(current + 5.0f);
                        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.2f);
                    } else {
                        float current = generator.getProgress();
                        generator.setProgress(current - 10.0f);

                        // Explode sound
                        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0f, 0.8f);

                        // Explode particles
                        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
                        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.15);

                        // Sync alert to Killers
                        ClientboundExplosionAlertPacket alertPacket = new ClientboundExplosionAlertPacket(pos);
                        for (ServerPlayer otherPlayer : level.players()) {
                            TrialPlayerData.getLazy(otherPlayer).ifPresent(data -> {
                                if (data.getRole() == PlayerRole.KILLER) {
                                    ModMessages.sendToPlayer(alertPacket, otherPlayer);
                                    otherPlayer.sendSystemMessage(Component.literal("§c[!] Generator meledak di koordinat: " + pos.toShortString()));
                                }
                            });
                        }
                    }
                }
            }
        });
        return true;
    }
}
