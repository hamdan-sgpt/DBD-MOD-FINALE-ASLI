package com.example.trialmod.client;

import com.example.trialmod.TrialMod;
import com.example.trialmod.client.renderer.KillerBaseRenderer;
import com.example.trialmod.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrialMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("generator_repair", GeneratorRepairOverlay.HUD);
        event.registerAboveAll("struggle_hud", StruggleOverlay.HUD);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.KILLER_BASE.get(), KillerBaseRenderer::new);
    }
}
