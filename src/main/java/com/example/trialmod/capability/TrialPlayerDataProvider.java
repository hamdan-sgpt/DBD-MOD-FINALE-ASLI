package com.example.trialmod.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TrialPlayerDataProvider implements ICapabilitySerializable<CompoundTag> {
    private final TrialPlayerData playerData = new TrialPlayerData();
    private final LazyOptional<TrialPlayerData> optional = LazyOptional.of(() -> playerData);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == TrialPlayerData.PLAYER_DATA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        playerData.saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        playerData.loadNBTData(nbt);
    }
}
