package com.example.trialmod.network.packet;

import com.example.trialmod.capability.TrialPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ClientboundPlayerDataSyncPacket {
    private final UUID playerUuid;
    private final CompoundTag nbt;

    public ClientboundPlayerDataSyncPacket(UUID playerUuid, CompoundTag nbt) {
        this.playerUuid = playerUuid;
        this.nbt = nbt;
    }

    public ClientboundPlayerDataSyncPacket(FriendlyByteBuf buf) {
        this.playerUuid = buf.readUUID();
        this.nbt = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUuid);
        buf.writeNbt(nbt);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ClientPacketHandler.handlePlayerDataSync(playerUuid, nbt);
        });
        return true;
    }

    private static class ClientPacketHandler {
        private static void handlePlayerDataSync(UUID uuid, CompoundTag nbt) {
            Player player = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getPlayerByUUID(uuid) : null;
            if (player != null) {
                player.getCapability(TrialPlayerData.PLAYER_DATA).ifPresent(data -> data.loadNBTData(nbt));
            }
        }
    }
}
