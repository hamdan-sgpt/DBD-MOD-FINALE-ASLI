package com.example.trialmod.block;

import com.example.trialmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GeneratorBlockEntity extends BlockEntity {
    private float progress = 0.0f;
    private boolean isCompleted = false;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENERATOR_BE.get(), pos, state);
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0.0f, Math.min(100.0f, progress));
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("Progress", progress);
        tag.putBoolean("IsCompleted", isCompleted);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.progress = tag.getFloat("Progress");
        this.isCompleted = tag.getBoolean("IsCompleted");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) {
            if (isCompleted) {
                // Spawns fire and smoke when completed (100%)
                if (level.random.nextFloat() < 0.2f) {
                    double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.8;
                    double y = pos.getY() + 1.2 + level.random.nextDouble() * 0.5;
                    double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.8;
                    level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.05, 0.0);
                    level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.1, z, 0.0, 0.05, 0.0);
                }
            }
        }
    }
}
