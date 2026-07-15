package com.example.trialmod.network.packet;

import com.example.trialmod.block.HookBlockEntity;
import com.example.trialmod.capability.TrialPlayerData;
import com.example.trialmod.capability.TrialPlayerDataEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundStruggleSkillCheckPacket {
    private final BlockPos pos;
    private final boolean success;

    public ServerboundStruggleSkillCheckPacket(BlockPos pos, boolean success) {
        this.pos = pos;
        this.success = success;
    }

    public ServerboundStruggleSkillCheckPacket(FriendlyByteBuf buf) {
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
            if (be instanceof HookBlockEntity hook) {
                if (player.getUUID().equals(hook.getHookedPlayerUuid())) {
                    TrialPlayerData.getLazy(player).ifPresent(data -> {
                        if (success) {
                            // Give minor struggle boost
                            data.setStruggleProgress(data.getStruggleProgress() + 5.0f);
                            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.3f);
                        } else {
                            // Heavy penalty for failing skill check
                            data.setStruggleProgress(data.getStruggleProgress() - 15.0f);
                            level.playSound(null, pos, SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.2f, 0.8f);
                        }
                        TrialPlayerDataEvents.syncPlayerData(player);
                    });
                }
            }
        });
        return true;
    }
}
