package com.example.trialmod;

import com.example.trialmod.capability.TrialPlayerData;
import com.example.trialmod.network.ModMessages;
import com.example.trialmod.registry.ModBlockEntities;
import com.example.trialmod.registry.ModBlocks;
import com.example.trialmod.registry.ModItems;
import com.example.trialmod.registry.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TrialMod.MOD_ID)
public class TrialMod {
    public static final String MOD_ID = "trialmod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TrialMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register registries to mod event bus
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        // Register lifecycle events
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        
        // Register capabilities
        modEventBus.addListener(this::registerCapabilities);
        
        // Register entity attributes
        modEventBus.addListener(this::registerAttributes);

        // Register ourselves to the general Forge event bus
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        LOGGER.info("TrialMod has been initialized!");
    }

    private void onRegisterCommands(final net.minecraftforge.event.RegisterCommandsEvent event) {
        com.example.trialmod.command.TrialModCommands.register(event.getDispatcher());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModMessages::register);
        LOGGER.info("TrialMod common setup complete.");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("TrialMod client setup complete.");
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.register(TrialPlayerData.class);
    }

    private void registerAttributes(final net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(ModEntities.KILLER_BASE.get(), com.example.trialmod.entity.KillerBaseEntity.createAttributes().build());
    }
}

