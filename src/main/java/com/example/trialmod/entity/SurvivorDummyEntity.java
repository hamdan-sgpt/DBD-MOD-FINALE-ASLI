package com.example.trialmod.entity;

import com.example.trialmod.capability.SurvivorStatus;
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

/**
 * Invisible "dummy" entity used only for rendering a GeckoLib survivor model 
 * in place of the vanilla player model. Never actually spawned in the world.
 */
public class SurvivorDummyEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Integer> ANIM_STATE =
            SynchedEntityData.defineId(SurvivorDummyEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // --- Animation constants ---
    // 0 = idle, 1 = walk, 2 = run, 3 = downed_crawl, 4 = hooked_struggle, 5 = injured_limp
    public static final int STATE_IDLE           = 0;
    public static final int STATE_WALK           = 1;
    public static final int STATE_RUN            = 2;
    public static final int STATE_DOWNED_CRAWL   = 3;
    public static final int STATE_HOOKED_STRUGGLE = 4;
    public static final int STATE_INJURED_LIMP   = 5;

    private static final RawAnimation ANIM_IDLE           = RawAnimation.begin().thenLoop("survivor.idle");
    private static final RawAnimation ANIM_WALK           = RawAnimation.begin().thenLoop("survivor.walk");
    private static final RawAnimation ANIM_RUN            = RawAnimation.begin().thenLoop("survivor.run");
    private static final RawAnimation ANIM_DOWNED_CRAWL   = RawAnimation.begin().thenLoop("survivor.downed_crawl");
    private static final RawAnimation ANIM_HOOKED_STRUGGLE = RawAnimation.begin().thenLoop("survivor.hooked_struggle");
    private static final RawAnimation ANIM_INJURED_LIMP   = RawAnimation.begin().thenLoop("survivor.injured_limp");

    public SurvivorDummyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoAi(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIM_STATE, STATE_IDLE);
    }

    public int getAnimationState() {
        return this.entityData.get(ANIM_STATE);
    }

    public void setAnimationState(int state) {
        this.entityData.set(ANIM_STATE, state);
    }

    private java.util.UUID boundPlayerUuid;

    public java.util.UUID getBoundPlayerUuid() {
        return boundPlayerUuid;
    }

    public void setBoundPlayerUuid(java.util.UUID uuid) {
        this.boundPlayerUuid = uuid;
    }

    /**
     * Convenience method: set the animation state from the survivor's current status
     * and movement.
     */
    public void syncFromSurvivorStatus(SurvivorStatus status, boolean isMoving, boolean isSprinting) {
        switch (status) {
            case DOWNED -> setAnimationState(STATE_DOWNED_CRAWL);
            case HOOKED -> setAnimationState(STATE_HOOKED_STRUGGLE);
            case INJURED -> setAnimationState(STATE_INJURED_LIMP);
            default -> { // HEALTHY, DEAD, etc.
                if (isSprinting) {
                    setAnimationState(STATE_RUN);
                } else if (isMoving) {
                    setAnimationState(STATE_WALK);
                } else {
                    setAnimationState(STATE_IDLE);
                }
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        if (this.level().isClientSide()) {
            registrar.add(new AnimationController<>(this, "survivor_controller", 5, event -> {
                int state = event.getAnimatable().getAnimationState();
                return switch (state) {
                    case STATE_WALK           -> event.setAndContinue(ANIM_WALK);
                    case STATE_RUN            -> event.setAndContinue(ANIM_RUN);
                    case STATE_DOWNED_CRAWL   -> event.setAndContinue(ANIM_DOWNED_CRAWL);
                    case STATE_HOOKED_STRUGGLE -> event.setAndContinue(ANIM_HOOKED_STRUGGLE);
                    case STATE_INJURED_LIMP   -> event.setAndContinue(ANIM_INJURED_LIMP);
                    default                   -> event.setAndContinue(ANIM_IDLE);
                };
            }));
        } else {
            registrar.add(new AnimationController<>(this, "survivor_controller", 5, event ->
                    software.bernie.geckolib.core.object.PlayState.CONTINUE));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
