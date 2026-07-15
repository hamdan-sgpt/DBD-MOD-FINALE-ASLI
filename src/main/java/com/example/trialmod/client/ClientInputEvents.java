package com.example.trialmod.client;

import com.example.trialmod.TrialMod;
import com.example.trialmod.capability.SurvivorStatus;
import com.example.trialmod.capability.TrialPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TrialMod.MOD_ID, value = Dist.CLIENT)
public class ClientInputEvents {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getKey() == GLFW.GLFW_KEY_SPACE && event.getAction() == GLFW.GLFW_PRESS) {
            if (GeneratorRepairOverlay.skillCheckActive) {
                GeneratorRepairOverlay.handleKeyPress();
            } else if (StruggleOverlay.struggleSkillCheckActive) {
                StruggleOverlay.handleKeyPress();
            }
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // Prevent jumping and movement during an active skill check, repairing, or when hooked
        TrialPlayerData.getLazy(mc.player).ifPresent(data -> {
            if (GeneratorRepairOverlay.skillCheckActive || isRepairing(mc) || 
                    data.getSurvivorStatus() == SurvivorStatus.HOOKED || 
                    StruggleOverlay.struggleSkillCheckActive) {
                event.getInput().jumping = false;
                event.getInput().leftImpulse = 0.0f;
                event.getInput().forwardImpulse = 0.0f;
            }
        });
    }

    private static boolean isRepairing(Minecraft mc) {
        if (!mc.options.keyUse.isDown()) return false;
        
        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            return mc.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof com.example.trialmod.block.GeneratorBlock;
        }
        return false;
    }
}
