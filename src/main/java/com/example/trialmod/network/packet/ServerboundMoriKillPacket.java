package com.example.trialmod.network.packet;

import com.example.trialmod.capability.PlayerRole;
import com.example.trialmod.capability.SurvivorStatus;
import com.example.trialmod.capability.TrialPlayerData;
import com.example.trialmod.capability.TrialPlayerDataEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ServerboundMoriKillPacket {
    private final UUID victimUuid;

    public ServerboundMoriKillPacket(UUID victimUuid) {
        this.victimUuid = victimUuid;
    }

    public ServerboundMoriKillPacket(FriendlyByteBuf buf) {
        this.victimUuid = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(victimUuid);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;

            // Retrieve the victim player
            ServerPlayer victim = sender.serverLevel().getServer().getPlayerList().getPlayer(victimUuid);
            if (victim == null) return;

            // Verify they are within range
            if (sender.distanceToSqr(victim) > 49.0) {
                return; // Too far
            }

            TrialPlayerData.getLazy(victim).ifPresent(data -> {
                if (data.getRole() == PlayerRole.SURVIVOR) {
                    data.setSurvivorStatus(SurvivorStatus.DEAD);
                    TrialPlayerDataEvents.syncPlayerData(victim);

                    // Alert other players
                    sender.serverLevel().getServer().getPlayerList().broadcastSystemMessage(
                            Component.literal("§4§l[!] Killer telah mengeliminasi " + victim.getName().getString() + " dengan Mori!"),
                            false
                    );

                    // Play death sounds
                    sender.serverLevel().playSound(null, victim.blockPosition(), 
                            SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.5f, 0.8f);
                }
            });
        });
        return true;
    }
}
