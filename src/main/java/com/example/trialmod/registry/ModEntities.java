package com.example.trialmod.registry;

import com.example.trialmod.TrialMod;
import com.example.trialmod.entity.KillerBaseEntity;
import com.example.trialmod.entity.SurvivorDummyEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TrialMod.MOD_ID);

    public static final RegistryObject<EntityType<KillerBaseEntity>> KILLER_BASE = ENTITY_TYPES.register("killer_base",
            () -> EntityType.Builder.of(KillerBaseEntity::new, MobCategory.MONSTER)
                    .sized(0.8f, 2.2f)
                    .build("killer_base"));

    public static final RegistryObject<EntityType<SurvivorDummyEntity>> SURVIVOR_DUMMY = ENTITY_TYPES.register("survivor_dummy",
            () -> EntityType.Builder.of(SurvivorDummyEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .noSummon()
                    .build("survivor_dummy"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}

