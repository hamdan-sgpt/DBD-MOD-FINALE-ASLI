package com.example.trialmod.client.model;

import com.example.trialmod.TrialMod;
import com.example.trialmod.entity.SurvivorDummyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SurvivorModel extends GeoModel<SurvivorDummyEntity> {
    @Override
    public ResourceLocation getModelResource(SurvivorDummyEntity animatable) {
        return new ResourceLocation(TrialMod.MOD_ID, "geo/survivor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SurvivorDummyEntity animatable) {
        return new ResourceLocation(TrialMod.MOD_ID, "textures/entity/survivor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SurvivorDummyEntity animatable) {
        return new ResourceLocation(TrialMod.MOD_ID, "animations/survivor.animation.json");
    }
}
