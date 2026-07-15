package com.example.trialmod.client;

import com.example.trialmod.TrialMod;
import com.example.trialmod.capability.PlayerRole;
import com.example.trialmod.capability.SurvivorStatus;
import com.example.trialmod.capability.TrialPlayerData;
import com.example.trialmod.entity.KillerBaseEntity;
import com.example.trialmod.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Pose;
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

    public static KillerBaseEntity getOrCreateDummyKiller(UUID playerUuid, Level level) {
        return DUMMY_KILLERS.computeIfAbsent(playerUuid, uuid ->
                new KillerBaseEntity(ModEntities.KILLER_BASE.get(), level));
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) return;

        TrialPlayerData.getLazy(player).ifPresent(data -> {
            PlayerRole role = data.getRole();

            if (role == PlayerRole.KILLER) {
                // Render Killer as custom GeckoLib Entity model with proper texture
                renderAsKiller(event, player);
            } else if (role == PlayerRole.SURVIVOR) {
                // Keep the player's OWN skin, but apply custom poses/rotations based on SurvivorStatus
                handleSurvivorPlayerRender(event, player, data.getSurvivorStatus());
            }
        });
    }

    private static void renderAsKiller(RenderPlayerEvent.Pre event, Player player) {
        event.setCanceled(true);

        KillerBaseEntity dummy = getOrCreateDummyKiller(player.getUUID(), player.level());
        
        // Sync transforms
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
            dummy.setAnimationState(player.isSprinting() ? 2 : 1); // CHASE vs WALK
        } else {
            dummy.setAnimationState(0); // IDLE
        }

        float rotation = player.getViewYRot(event.getPartialTick());
        Minecraft.getInstance().getEntityRenderDispatcher().render(
                dummy,
                0.0D, 0.0D, 0.0D,
                rotation,
                event.getPartialTick(),
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight()
        );
    }

    private static void handleSurvivorPlayerRender(RenderPlayerEvent.Pre event, Player player, SurvivorStatus status) {
        // Do NOT cancel event so player's original skin and model (Steve/Alex/Custom) are preserved!
        PoseStack poseStack = event.getPoseStack();

        if (status == SurvivorStatus.DOWNED) {
            // Force crawling pose client-side so player legs/arms stretch out in crawling pose
            if (!player.isPassenger()) {
                player.setPose(Pose.SWIMMING);
            }
        } else if (status == SurvivorStatus.HOOKED) {
            // Elevate player visual position so they appear hanging on the hook block
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.5D, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) return;

        TrialPlayerData.getLazy(player).ifPresent(data -> {
            if (data.getRole() == PlayerRole.SURVIVOR && data.getSurvivorStatus() == SurvivorStatus.HOOKED) {
                event.getPoseStack().popPose();
            }
        });
    }
}
