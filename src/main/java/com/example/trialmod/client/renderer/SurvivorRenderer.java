package com.example.trialmod.client.renderer;

import com.example.trialmod.client.model.SurvivorModel;
import com.example.trialmod.entity.SurvivorDummyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SurvivorRenderer extends GeoEntityRenderer<SurvivorDummyEntity> {
    public SurvivorRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SurvivorModel());
        this.shadowRadius = 0.4f;
    }
}
