package com.example.trialmod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

import java.util.ArrayList;
import java.util.List;

public class TrialPlayerData {
    public static final Capability<TrialPlayerData> PLAYER_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    private PlayerRole role = PlayerRole.SURVIVOR;
    private SurvivorStatus survivorStatus = SurvivorStatus.HEALTHY;
    private float stamina = 100.0f;
    private float struggleProgress = 100.0f;
    private final List<String> selectedPerks = new ArrayList<>();

    public PlayerRole getRole() {
        return role;
    }

    public void setRole(PlayerRole role) {
        this.role = role;
    }

    public SurvivorStatus getSurvivorStatus() {
        return survivorStatus;
    }

    public void setSurvivorStatus(SurvivorStatus survivorStatus) {
        this.survivorStatus = survivorStatus;
    }

    public float getStamina() {
        return stamina;
    }

    public void setStamina(float stamina) {
        this.stamina = Math.max(0.0f, Math.min(100.0f, stamina));
    }

    public float getStruggleProgress() {
        return struggleProgress;
    }

    public void setStruggleProgress(float struggleProgress) {
        this.struggleProgress = Math.max(0.0f, Math.min(100.0f, struggleProgress));
    }

    public List<String> getSelectedPerks() {
        return selectedPerks;
    }

    public void setPerks(List<String> perks) {
        this.selectedPerks.clear();
        if (perks != null) {
            for (int i = 0; i < Math.min(4, perks.size()); i++) {
                this.selectedPerks.add(perks.get(i));
            }
        }
    }

    public void addPerk(String perkId) {
        if (this.selectedPerks.size() < 4 && !this.selectedPerks.contains(perkId)) {
            this.selectedPerks.add(perkId);
        }
    }

    public void removePerk(String perkId) {
        this.selectedPerks.remove(perkId);
    }

    public void copyFrom(TrialPlayerData source) {
        this.role = source.role;
        this.survivorStatus = source.survivorStatus;
        this.stamina = source.stamina;
        this.struggleProgress = source.struggleProgress;
        this.selectedPerks.clear();
        this.selectedPerks.addAll(source.selectedPerks);
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putString("role", role.name());
        nbt.putString("survivorStatus", survivorStatus.name());
        nbt.putFloat("stamina", stamina);
        nbt.putFloat("struggleProgress", struggleProgress);
        
        ListTag perksList = new ListTag();
        for (String perk : selectedPerks) {
            perksList.add(StringTag.valueOf(perk));
        }
        nbt.put("selectedPerks", perksList);
    }

    public void loadNBTData(CompoundTag nbt) {
        if (nbt.contains("role", Tag.TAG_STRING)) {
            try {
                this.role = PlayerRole.valueOf(nbt.getString("role"));
            } catch (IllegalArgumentException e) {
                this.role = PlayerRole.SURVIVOR;
            }
        }
        if (nbt.contains("survivorStatus", Tag.TAG_STRING)) {
            try {
                this.survivorStatus = SurvivorStatus.valueOf(nbt.getString("survivorStatus"));
            } catch (IllegalArgumentException e) {
                this.survivorStatus = SurvivorStatus.HEALTHY;
            }
        }
        if (nbt.contains("stamina", Tag.TAG_FLOAT)) {
            this.stamina = nbt.getFloat("stamina");
        }
        if (nbt.contains("struggleProgress", Tag.TAG_FLOAT)) {
            this.struggleProgress = nbt.getFloat("struggleProgress");
        } else {
            this.struggleProgress = 100.0f;
        }
        if (nbt.contains("selectedPerks", Tag.TAG_LIST)) {
            this.selectedPerks.clear();
            ListTag perksList = nbt.getList("selectedPerks", Tag.TAG_STRING);
            for (int i = 0; i < Math.min(4, perksList.size()); i++) {
                this.selectedPerks.add(perksList.getString(i));
            }
        }
    }

    public static LazyOptional<TrialPlayerData> getLazy(Player player) {
        return player.getCapability(PLAYER_DATA);
    }

    public static TrialPlayerData get(Player player) {
        return player.getCapability(PLAYER_DATA).orElseThrow(() -> new IllegalStateException("Player capability not found!"));
    }
}
