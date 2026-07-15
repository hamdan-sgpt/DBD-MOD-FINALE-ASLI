package com.example.trialmod.client;

import com.example.trialmod.TrialMod;
import com.example.trialmod.capability.PlayerRole;
import com.example.trialmod.capability.SurvivorStatus;
import com.example.trialmod.capability.TrialPlayerData;
import com.example.trialmod.entity.KillerBaseEntity;
import com.example.trialmod.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
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
        return DUMMY_KILLERS.computeIfAbsent(playerUuid, uuid -> {
            KillerBaseEntity dummy = new KillerBaseEntity(ModEntities.KILLER_BASE.get(), level);
            dummy.setId(Math.abs(uuid.hashCode()) + 1000000);
            return dummy;
        });
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) return;

        TrialPlayerData.getLazy(player).ifPresent(data -> {
            PlayerRole role = data.getRole();

            if (role == PlayerRole.KILLER) {
                // Render Killer as 3D GeckoLib model with 256x256 texture
                renderAsKiller(event, player);
            } else if (role == PlayerRole.SURVIVOR) {
                // Use native player skin with custom poses for INJURED, DOWNED, and HOOKED
                handleSurvivorPlayerRender(event, player, data.getSurvivorStatus());
            }
        });
    }

    private static void renderAsKiller(RenderPlayerEvent.Pre event, Player player) {
        event.setCanceled(true); // Stop rendering standard player model

        KillerBaseEntity dummy = getOrCreateDummyKiller(player.getUUID(), player.level());
        
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
        // DO NOT CANCEL EVENT - Preserve player's custom skin, layers, and model!
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        PoseStack poseStack = event.getPoseStack();

        if (status == SurvivorStatus.DOWNED) {
            // Force native crawling pose (player lies flat on ground with legs/arms crawling using their own skin)
            if (!player.isPassenger()) {
                player.setPose(Pose.SWIMMING);
            }
        } else if (status == SurvivorStatus.INJURED) {
            // Pose left arm holding stomach/chest (holding wound) while standing or walking
            model.leftArm.xRot = -0.85F;
            model.leftArm.yRot = 0.65F;
            model.leftArm.zRot = 0.15F;
        } else if (status == SurvivorStatus.HOOKED) {
            // Elevate position and raise arms up hanging on hook
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.5D, 0.0D);
            model.leftArm.xRot = -3.14F;
            model.rightArm.xRot = -3.14F;
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
