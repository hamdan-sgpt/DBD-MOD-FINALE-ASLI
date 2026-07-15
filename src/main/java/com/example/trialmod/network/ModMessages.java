package com.example.trialmod.network;

import com.example.trialmod.TrialMod;
import com.example.trialmod.network.packet.ClientboundPlayerDataSyncPacket;
import com.example.trialmod.network.packet.ClientboundExplosionAlertPacket;
import com.example.trialmod.network.packet.ServerboundGeneratorSkillCheckPacket;
import com.example.trialmod.network.packet.ServerboundInteractGeneratorPacket;
import com.example.trialmod.network.packet.ServerboundMoriKillPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(TrialMod.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // Player Data Sync (Server -> Client)
        net.messageBuilder(ClientboundPlayerDataSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundPlayerDataSyncPacket::new)
                .encoder(ClientboundPlayerDataSyncPacket::toBytes)
                .consumerNetworkThread(ClientboundPlayerDataSyncPacket::handle)
                .add();

        // Interact Generator (Client -> Server)
        net.messageBuilder(ServerboundInteractGeneratorPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerboundInteractGeneratorPacket::new)
                .encoder(ServerboundInteractGeneratorPacket::toBytes)
                .consumerNetworkThread(ServerboundInteractGeneratorPacket::handle)
                .add();

        // Generator Skill Check (Client -> Server)
        net.messageBuilder(ServerboundGeneratorSkillCheckPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerboundGeneratorSkillCheckPacket::new)
                .encoder(ServerboundGeneratorSkillCheckPacket::toBytes)
                .consumerNetworkThread(ServerboundGeneratorSkillCheckPacket::handle)
                .add();

        // Generator Explosion Alert (Server -> Client)
        net.messageBuilder(ClientboundExplosionAlertPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundExplosionAlertPacket::new)
                .encoder(ClientboundExplosionAlertPacket::toBytes)
                .consumerNetworkThread(ClientboundExplosionAlertPacket::handle)
                .add();

        // Mori Kill (Client -> Server)
        net.messageBuilder(ServerboundMoriKillPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerboundMoriKillPacket::new)
                .encoder(ServerboundMoriKillPacket::toBytes)
                .consumerNetworkThread(ServerboundMoriKillPacket::handle)
                .add();

        // Struggle Skill Check (Client -> Server)
        net.messageBuilder(com.example.trialmod.network.packet.ServerboundStruggleSkillCheckPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(com.example.trialmod.network.packet.ServerboundStruggleSkillCheckPacket::new)
                .encoder(com.example.trialmod.network.packet.ServerboundStruggleSkillCheckPacket::toBytes)
                .consumerNetworkThread(com.example.trialmod.network.packet.ServerboundStruggleSkillCheckPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAllClients(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }

    public static <MSG> void sendToTrackingClients(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), message);
    }

    public static <MSG> void sendToPlayerAndTracking(MSG message, ServerPlayer player) {
        sendToPlayer(message, player);
        sendToTrackingClients(message, player);
    }
}
