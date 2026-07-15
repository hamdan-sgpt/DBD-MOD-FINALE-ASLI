package com.example.trialmod.client;

import com.example.trialmod.entity.KillerBaseEntity;
import com.example.trialmod.network.ModMessages;
import com.example.trialmod.network.packet.ServerboundMoriKillPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.UUID;

public class ClientAnimationEvents {

    public static void handleHitKeyframe(KillerBaseEntity killer) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            level.playLocalSound(killer.getX(), killer.getY(), killer.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f, false);
        }
    }

    public static void handleMoriKeyframe(KillerBaseEntity killer) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            String victimUuidStr = killer.getMoriVictim();
            if (!victimUuidStr.isEmpty()) {
                try {
                    UUID victimUuid = UUID.fromString(victimUuidStr);
                    // Send packet to server to finalize the mori death transition
                    ModMessages.sendToServer(new ServerboundMoriKillPacket(victimUuid));
                } catch (IllegalArgumentException e) {
                    // Ignore malformed UUID strings
                }
            }
        }
    }
}
