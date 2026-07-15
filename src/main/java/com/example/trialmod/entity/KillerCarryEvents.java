package com.example.trialmod.entity;

import com.example.trialmod.TrialMod;
import com.example.trialmod.capability.PlayerRole;
import com.example.trialmod.capability.SurvivorStatus;
import com.example.trialmod.capability.TrialPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrialMod.MOD_ID)
public class KillerCarryEvents {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof ServerPlayer killer && event.getTarget() instanceof ServerPlayer survivor) {
            TrialPlayerData killerData = TrialPlayerData.get(killer);
            TrialPlayerData survivorData = TrialPlayerData.get(survivor);

            if (killerData.getRole() == PlayerRole.KILLER && survivorData.getRole() == PlayerRole.SURVIVOR) {
                if (survivorData.getSurvivorStatus() == SurvivorStatus.DOWNED) {
                    if (killer.getPassengers().isEmpty()) {
                        // Pick up survivor
                        survivor.startRiding(killer, true);

                        // Sound cues
                        survivor.level().playSound(null, survivor.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 1.2f, 0.8f);
                        survivor.level().playSound(null, survivor.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.8f, 0.8f);

                        // Alerts
                        killer.sendSystemMessage(Component.literal("§6[!] Anda memikul " + survivor.getScoreboardName() + "! Bawa ke Hook terdekat."));
                        survivor.sendSystemMessage(Component.literal("§c[!] Anda dipikul oleh Killer!"));

                        event.setCancellationResult(InteractionResult.SUCCESS);
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityDismount(EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityMounting() instanceof Player survivor) {
            // Prevent downed or hooked players from voluntarily dismounting/escaping the Killer
            TrialPlayerData survivorData = TrialPlayerData.get(survivor);
            if (survivorData.getSurvivorStatus() == SurvivorStatus.DOWNED || survivorData.getSurvivorStatus() == SurvivorStatus.HOOKED) {
                event.setCanceled(true);
            }
        }
    }
}
