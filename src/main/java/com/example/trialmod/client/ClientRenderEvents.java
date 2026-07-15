package com.example.trialmod.client;

import com.example.trialmod.TrialMod;
import com.example.trialmod.capability.PlayerRole;
import com.example.trialmod.capability.TrialPlayerData;
import com.example.trialmod.entity.KillerBaseEntity;
import com.example.trialmod.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TrialMod.MOD_ID, value = Dist.CLIENT)
public class ClientRenderEvents {

    private static final Map<UUID, KillerBaseEntity> DUMMY_KILLERS = new HashMap<>();

    public static KillerBaseEntity getOrCreateDummy(UUID playerUuid, Level level) {
        return DUMMY_KILLERS.computeIfAbsent(playerUuid, uuid -> 
                new KillerBaseEntity(ModEntities.KILLER_BASE.get(), level));
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            // Fetch role
            TrialPlayerData.getLazy(player).ifPresent(data -> {
                if (data.getRole() == PlayerRole.KILLER) {
                    event.setCanceled(true); // Stop rendering standard player

                    // Retrieve dummy Killer representation
                    KillerBaseEntity dummy = getOrCreateDummy(player.getUUID(), player.level());

                    // Synchronize tick counts and client positions
                    dummy.tickCount = player.tickCount;
                    dummy.setPos(player.getX(), player.getY(), player.getZ());
                    dummy.xo = player.xo; dummy.yo = player.yo; dummy.zo = player.zo;
                    dummy.setYRot(player.getYRot()); dummy.setXRot(player.getXRot());
                    dummy.yRotO = player.yRotO; dummy.xRotO = player.xRotO;
                    dummy.yBodyRot = player.yBodyRot; dummy.yBodyRotO = player.yBodyRotO;
                    dummy.yHeadRot = player.getYHeadRot(); dummy.yHeadRotO = player.yHeadRotO;

                    // Choose animation state
                    boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 0.001;
                    if (player.swingTime > 0) {
                        dummy.setAnimationState(3); // ATTACK_WINDUP
                    } else if (isMoving) {
                        dummy.setAnimationState(player.isSprinting() ? 2 : 1); // CHASE (sprint) vs WALK
                    } else {
                        dummy.setAnimationState(0); // IDLE
                    }

                    // Pass 0.0D coordinates since event.getPoseStack() is already translated to the player's position
                    float rotation = player.getViewYRot(event.getPartialTick());

                    Minecraft.getInstance().getEntityRenderDispatcher().render(
                            dummy,
                            0.0D,
                            0.0D,
                            0.0D,
                            rotation,
                            event.getPartialTick(),
                            event.getPoseStack(),
                            event.getMultiBufferSource(),
                            event.getPackedLight()
                    );
                }
            });
        }
    }
}
