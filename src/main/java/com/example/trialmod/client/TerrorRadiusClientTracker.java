package com.example.trialmod.client;

import com.example.trialmod.TrialMod;
import com.example.trialmod.capability.PlayerRole;
import com.example.trialmod.capability.TrialPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrialMod.MOD_ID, value = Dist.CLIENT)
public class TerrorRadiusClientTracker {
    private static int ticksSinceLastHeartbeat = 0;
    private static int heartbeatDoubleState = 0; // 0 = idle/ready for first beat, 1 = played first beat, waiting to play second

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            // Target player must be a Survivor to experience Terror Radius
            TrialPlayerData.getLazy(mc.player).ifPresent(myCap -> {
                if (myCap.getRole() == PlayerRole.SURVIVOR) {
                    Player closestKiller = null;
                    double closestDistanceSq = Double.MAX_VALUE;

                    for (Player other : mc.level.players()) {
                        if (other == mc.player) continue;

                        var otherCap = TrialPlayerData.get(other);
                        if (otherCap != null && otherCap.getRole() == PlayerRole.KILLER) {
                            double distSq = mc.player.distanceToSqr(other);
                            if (distSq < closestDistanceSq) {
                                closestDistanceSq = distSq;
                                closestKiller = other;
                            }
                        }
                    }

                    if (closestKiller != null) {
                        double distance = Math.sqrt(closestDistanceSq);
                        if (distance <= 32.0) {
                            handleHeartbeat(mc.player, distance);
                            return;
                        }
                    }

                    // Reset counters if Killer is not within 32 blocks
                    heartbeatDoubleState = 0;
                    ticksSinceLastHeartbeat = 0;
                }
            });
        }
    }

    private static void handleHeartbeat(Player player, double distance) {
        ticksSinceLastHeartbeat++;

        // Interpolate beat interval: closer = faster beating
        // At 32 blocks distance: 40 ticks delay (2 seconds)
        // At 2 blocks distance: 10 ticks delay (0.5 seconds)
        float pct = (float) (distance / 32.0f);
        int targetDelay = 10 + (int) (30 * pct);

        // Adjust volume: closer = louder
        float volume = 1.5f - pct; // Ranges from ~0.5f up to 1.5f

        if (heartbeatDoubleState == 0) {
            if (ticksSinceLastHeartbeat >= targetDelay) {
                // Play first heartbeat thud (lub)
                playBeatSound(player, volume, 0.5f);
                heartbeatDoubleState = 1;
                ticksSinceLastHeartbeat = 0;
            }
        } else if (heartbeatDoubleState == 1) {
            // Play second heartbeat thud (dub) 4 ticks later
            if (ticksSinceLastHeartbeat >= 4) {
                playBeatSound(player, volume, 0.45f);
                heartbeatDoubleState = 0;
                ticksSinceLastHeartbeat = 0;
            }
        }
    }

    private static void playBeatSound(Player player, float volume, float pitch) {
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_BASEDRUM.get(), SoundSource.PLAYERS, volume, pitch, false);
    }
}
