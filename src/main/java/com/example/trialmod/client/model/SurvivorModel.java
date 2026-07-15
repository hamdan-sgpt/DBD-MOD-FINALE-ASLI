package com.example.trialmod.client.model;

import com.example.trialmod.TrialMod;
import com.example.trialmod.entity.SurvivorDummyEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.model.GeoModel;

public class SurvivorModel extends GeoModel<SurvivorDummyEntity> {
    @Override
    public ResourceLocation getModelResource(SurvivorDummyEntity animatable) {
        return new ResourceLocation(TrialMod.MOD_ID, "geo/survivor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SurvivorDummyEntity animatable) {
        if (animatable.getBoundPlayerUuid() != null) {
            Player player = animatable.level().getPlayerByUUID(animatable.getBoundPlayerUuid());
            if (player instanceof AbstractClientPlayer clientPlayer) {
                return clientPlayer.getSkinTextureLocation();
            }
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    public ResourceLocation getAnimationResource(SurvivorDummyEntity animatable) {
        return new ResourceLocation(TrialMod.MOD_ID, "animations/survivor.animation.json");
    }
}
