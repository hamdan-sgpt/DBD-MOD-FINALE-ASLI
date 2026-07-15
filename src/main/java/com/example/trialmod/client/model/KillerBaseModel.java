package com.example.trialmod.client.model;

import com.example.trialmod.TrialMod;
import com.example.trialmod.entity.KillerBaseEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class KillerBaseModel extends GeoModel<KillerBaseEntity> {
    @Override
    public ResourceLocation getModelResource(KillerBaseEntity animatable) {
        return new ResourceLocation(TrialMod.MOD_ID, "geo/killer.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KillerBaseEntity animatable) {
        return new ResourceLocation(TrialMod.MOD_ID, "textures/entity/killer.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KillerBaseEntity animatable) {
        return new ResourceLocation(TrialMod.MOD_ID, "animations/killer.animation.json");
    }
}
