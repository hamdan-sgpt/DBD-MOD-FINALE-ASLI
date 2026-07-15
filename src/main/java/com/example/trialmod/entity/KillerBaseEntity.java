package com.example.trialmod.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class KillerBaseEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> ANIM_STATE = 
            SynchedEntityData.defineId(KillerBaseEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<String> MORI_VICTIM = 
            SynchedEntityData.defineId(KillerBaseEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // RawAnimation definitions for GeckoLib 4 animation states
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation CHASE_ANIM = RawAnimation.begin().thenLoop("chase");
    private static final RawAnimation ATTACK_WINDUP_ANIM = RawAnimation.begin().thenPlay("attack_windup");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation DOWNING_VICTIM_ANIM = RawAnimation.begin().thenPlay("downing_victim");
    private static final RawAnimation KILL_MORI_ANIM = RawAnimation.begin().thenPlay("kill_mori");
    private static final RawAnimation HIT_REACTION_ANIM = RawAnimation.begin().thenPlay("hit_reaction");

    public KillerBaseEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIM_STATE, 0); // Default: 0 (idle)
        this.entityData.define(MORI_VICTIM, ""); // Default: empty string (no victim)
    }

    public int getAnimationState() {
        return this.entityData.get(ANIM_STATE);
    }

    public void setAnimationState(int state) {
        this.entityData.set(ANIM_STATE, state);
    }

    public String getMoriVictim() {
        return this.entityData.get(MORI_VICTIM);
    }

    public void setMoriVictim(String uuidStr) {
        this.entityData.set(MORI_VICTIM, uuidStr == null ? "" : uuidStr);
    }

    public float getTerrorRadius() {
        return 32.0f;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        if (this.level().isClientSide()) {
            // Client-side animation registration with CustomInstruction keyframe triggers
            registrar.add(new AnimationController<>(this, "controller", 5, event -> {
                int state = event.getAnimatable().getAnimationState();
                switch (state) {
                    case 1:
                        return event.setAndContinue(WALK_ANIM);
                    case 2:
                        return event.setAndContinue(CHASE_ANIM);
                    case 3:
                        return event.setAndContinue(ATTACK_WINDUP_ANIM);
                    case 4:
                        return event.setAndContinue(ATTACK_ANIM);
                    case 5:
                        return event.setAndContinue(DOWNING_VICTIM_ANIM);
                    case 6:
                        return event.setAndContinue(KILL_MORI_ANIM);
                    case 7:
                        return event.setAndContinue(HIT_REACTION_ANIM);
                    default:
                        return event.setAndContinue(IDLE_ANIM);
                }
            }).setCustomInstructionKeyframeHandler(event -> {
                String instruction = event.getKeyframeData().getInstructions();
                if ("hit".equals(instruction)) {
                    com.example.trialmod.client.ClientAnimationEvents.handleHitKeyframe(event.getAnimatable());
                } else if ("mori_kill".equals(instruction)) {
                    com.example.trialmod.client.ClientAnimationEvents.handleMoriKeyframe(event.getAnimatable());
                }
            }));
        } else {
            // Server-side safe skeleton animation controller
            registrar.add(new AnimationController<>(this, "controller", 5, event -> 
                    software.bernie.geckolib.core.object.PlayState.CONTINUE));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
