package com.example.trialmod.capability;

import com.example.trialmod.TrialMod;
import com.example.trialmod.network.ModMessages;
import com.example.trialmod.network.packet.ClientboundPlayerDataSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrialMod.MOD_ID)
public class TrialPlayerDataEvents {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(TrialPlayerData.PLAYER_DATA).isPresent()) {
                event.addCapability(new ResourceLocation(TrialMod.MOD_ID, "player_data"), new TrialPlayerDataProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Copy capability data from old player to new player upon death/respawn
        event.getOriginal().getCapability(TrialPlayerData.PLAYER_DATA).ifPresent(oldStore -> {
            event.getEntity().getCapability(TrialPlayerData.PLAYER_DATA).ifPresent(newStore -> {
                newStore.copyFrom(oldStore);
            });
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayerData(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayerData(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayerData(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer trackedPlayer) {
            if (event.getEntity() instanceof ServerPlayer trackingPlayer) {
                // Sync tracked player's data to the tracking player's client
                trackedPlayer.getCapability(TrialPlayerData.PLAYER_DATA).ifPresent(data -> {
                    CompoundTag tag = new CompoundTag();
                    data.saveNBTData(tag);
                    ModMessages.sendToPlayer(new ClientboundPlayerDataSyncPacket(trackedPlayer.getUUID(), tag), trackingPlayer);
                });
            }
        }
    }

    public static void syncPlayerData(ServerPlayer player) {
        player.getCapability(TrialPlayerData.PLAYER_DATA).ifPresent(data -> {
            CompoundTag tag = new CompoundTag();
            data.saveNBTData(tag);
            // Sync to the player themselves and tracking clients
            ModMessages.sendToPlayerAndTracking(new ClientboundPlayerDataSyncPacket(player.getUUID(), tag), player);
        });
    }
}
