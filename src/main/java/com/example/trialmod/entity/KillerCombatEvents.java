package com.example.trialmod.entity;

import com.example.trialmod.TrialMod;
import com.example.trialmod.capability.PlayerRole;
import com.example.trialmod.capability.SurvivorStatus;
import com.example.trialmod.capability.TrialPlayerData;
import com.example.trialmod.capability.TrialPlayerDataEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = TrialMod.MOD_ID)
public class KillerCombatEvents {

    private static final UUID KILLER_SPEED_UUID = UUID.fromString("da3d8984-7ab6-4b68-b80c-7b249b6b8015");
    private static final AttributeModifier KILLER_SPEED_MOD = 
            new AttributeModifier(KILLER_SPEED_UUID, "Killer Speed Boost", 0.20D, AttributeModifier.Operation.MULTIPLY_TOTAL);

    private static final UUID SURVIVOR_CRAWL_UUID = UUID.fromString("c07d39cc-0357-4148-8df0-e29f8f413d07");
    private static final AttributeModifier SURVIVOR_CRAWL_MOD = 
            new AttributeModifier(SURVIVOR_CRAWL_UUID, "Survivor Crawling", -0.80D, AttributeModifier.Operation.MULTIPLY_TOTAL);

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer attacker && event.getTarget() instanceof ServerPlayer target) {
            TrialPlayerData.getLazy(attacker).ifPresent(attackData -> {
                if (attackData.getRole() == PlayerRole.KILLER) {
                    TrialPlayerData.getLazy(target).ifPresent(targetData -> {
                        if (targetData.getRole() == PlayerRole.SURVIVOR) {
                            SurvivorStatus status = targetData.getSurvivorStatus();
                            if (status == SurvivorStatus.DOWNED || status == SurvivorStatus.HOOKED || status == SurvivorStatus.DEAD) {
                                // Cancel attack to prevent hitting downed/hooked/dead survivors
                                event.setCanceled(true);
                                return;
                            }

                            // Perform status transition
                            if (status == SurvivorStatus.HEALTHY) {
                                targetData.setSurvivorStatus(SurvivorStatus.INJURED);
                                target.sendSystemMessage(Component.literal("§c[!] Kamu terluka! (INJURED)"));
                                attacker.sendSystemMessage(Component.literal("§a[!] Target terluka!"));
                            } else if (status == SurvivorStatus.INJURED) {
                                targetData.setSurvivorStatus(SurvivorStatus.DOWNED);
                                target.sendSystemMessage(Component.literal("§4[!] Kamu sekarat! (DOWNED)"));
                                attacker.sendSystemMessage(Component.literal("§4[!] Target sekarat! Bawa ke Hook!"));
                            }

                            // Sync capability state
                            TrialPlayerDataEvents.syncPlayerData(target);

                            // Play combat sound effects
                            target.level().playSound(null, target.blockPosition(), 
                                    SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.5f, 0.8f);
                        }
                    });
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player target && event.getSource().getEntity() instanceof Player attacker) {
            TrialPlayerData.getLazy(attacker).ifPresent(attackData -> {
                if (attackData.getRole() == PlayerRole.KILLER) {
                    // Set damage to a tiny constant (0.5 hearts) to give visual feedback without depleting actual vanilla health
                    event.setAmount(1.0f);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.player instanceof ServerPlayer player) {
            TrialPlayerData.getLazy(player).ifPresent(data -> {
                var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null) {
                    // Manage Killer speed boost
                    if (data.getRole() == PlayerRole.KILLER) {
                        if (!speedAttr.hasModifier(KILLER_SPEED_MOD)) {
                            speedAttr.addTransientModifier(KILLER_SPEED_MOD);
                        }
                    } else {
                        if (speedAttr.hasModifier(KILLER_SPEED_MOD)) {
                            speedAttr.removeModifier(KILLER_SPEED_MOD);
                        }
                    }

                    // Manage Survivor Downed crawl speed penalty and crawling pose
                    if (data.getRole() == PlayerRole.SURVIVOR && data.getSurvivorStatus() == SurvivorStatus.DOWNED) {
                        if (!speedAttr.hasModifier(SURVIVOR_CRAWL_MOD)) {
                            speedAttr.addTransientModifier(SURVIVOR_CRAWL_MOD);
                        }
                        // If not riding another entity, force vanilla swimming/crawling pose
                        if (!player.isPassenger()) {
                            player.setPose(net.minecraft.world.entity.Pose.SWIMMING);
                        }
                    } else {
                        if (speedAttr.hasModifier(SURVIVOR_CRAWL_MOD)) {
                            speedAttr.removeModifier(SURVIVOR_CRAWL_MOD);
                        }
                    }
                }
            });
        }
    }
}
