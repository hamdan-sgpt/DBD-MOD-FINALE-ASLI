package com.example.trialmod.client.renderer;

import com.example.trialmod.client.model.KillerBaseModel;
import com.example.trialmod.entity.KillerBaseEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KillerBaseRenderer extends GeoEntityRenderer<KillerBaseEntity> {
    public KillerBaseRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new KillerBaseModel());
        this.shadowRadius = 0.5f;
    }
}
