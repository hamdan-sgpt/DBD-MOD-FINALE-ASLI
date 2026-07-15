package com.example.trialmod.registry;

import com.example.trialmod.TrialMod;
import com.example.trialmod.block.GeneratorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = 
            DeferredRegister.create(ForgeRegistries.BLOCKS, TrialMod.MOD_ID);

    public static final RegistryObject<Block> GENERATOR = BLOCKS.register("generator",
            () -> new GeneratorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3.5f).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<Block> HOOK = BLOCKS.register("hook",
            () -> new com.example.trialmod.block.HookBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f).requiresCorrectToolForDrops().noOcclusion()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
