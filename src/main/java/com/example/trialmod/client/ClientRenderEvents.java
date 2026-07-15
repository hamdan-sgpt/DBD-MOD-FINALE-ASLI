package com.example.trialmod.client;

import com.example.trialmod.TrialMod;
import com.example.trialmod.capability.PlayerRole;
import com.example.trialmod.capability.SurvivorStatus;
import com.example.trialmod.capability.TrialPlayerData;
import com.example.trialmod.entity.KillerBaseEntity;
import com.example.trialmod.entity.SurvivorDummyEntity;
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
    private static final Map<UUID, SurvivorDummyEntity> DUMMY_SURVIVORS = new HashMap<>();

    public static KillerBaseEntity getOrCreateDummyKiller(UUID playerUuid, Level level) {
        return DUMMY_KILLERS.computeIfAbsent(playerUuid, uuid ->
                new KillerBaseEntity(ModEntities.KILLER_BASE.get(), level));
    }

    public static SurvivorDummyEntity getOrCreateDummySurvivor(UUID playerUuid, Level level) {
        return DUMMY_SURVIVORS.computeIfAbsent(playerUuid, uuid ->
                new SurvivorDummyEntity(ModEntities.SURVIVOR_DUMMY.get(), level));
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) return;

        TrialPlayerData.getLazy(player).ifPresent(data -> {
            PlayerRole role = data.getRole();

            if (role == PlayerRole.KILLER) {
                renderAsKiller(event, player);
            } else if (role == PlayerRole.SURVIVOR) {
                renderAsSurvivor(event, player, data.getSurvivorStatus());
            }
        });
    }

    // ─── Killer Rendering (existing logic) ──────────────────────────────
    private static void renderAsKiller(RenderPlayerEvent.Pre event, Player player) {
        event.setCanceled(true);

        KillerBaseEntity dummy = getOrCreateDummyKiller(player.getUUID(), player.level());
        syncDummyTransform(dummy, player);

        // Choose animation state
        boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 0.001;
        if (player.swingTime > 0) {
            dummy.setAnimationState(3); // ATTACK_WINDUP
        } else if (isMoving) {
            dummy.setAnimationState(player.isSprinting() ? 2 : 1); // CHASE vs WALK
        } else {
            dummy.setAnimationState(0); // IDLE
        }

        renderDummy(event, dummy, player);
    }

    // ─── Survivor Rendering (new) ───────────────────────────────────────
    private static void renderAsSurvivor(RenderPlayerEvent.Pre event, Player player, SurvivorStatus status) {
        event.setCanceled(true);

        SurvivorDummyEntity dummy = getOrCreateDummySurvivor(player.getUUID(), player.level());
        syncDummyTransform(dummy, player);

        boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 0.001;
        boolean isSprinting = player.isSprinting();
        dummy.syncFromSurvivorStatus(status, isMoving, isSprinting);

        renderDummy(event, dummy, player);
    }

    // ─── Shared helpers ─────────────────────────────────────────────────
    private static void syncDummyTransform(net.minecraft.world.entity.LivingEntity dummy, Player player) {
        dummy.tickCount = player.tickCount;
        dummy.setPos(player.getX(), player.getY(), player.getZ());
        dummy.xo = player.xo; dummy.yo = player.yo; dummy.zo = player.zo;
        dummy.setYRot(player.getYRot()); dummy.setXRot(player.getXRot());
        dummy.yRotO = player.yRotO; dummy.xRotO = player.xRotO;
        dummy.yBodyRot = player.yBodyRot; dummy.yBodyRotO = player.yBodyRotO;
        dummy.yHeadRot = player.getYHeadRot(); dummy.yHeadRotO = player.yHeadRotO;
    }

    private static void renderDummy(RenderPlayerEvent.Pre event, net.minecraft.world.entity.Entity dummy, Player player) {
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
}

