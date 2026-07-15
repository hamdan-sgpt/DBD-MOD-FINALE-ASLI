package com.example.trialmod.block;

import com.example.trialmod.capability.PlayerRole;
import com.example.trialmod.capability.SurvivorStatus;
import com.example.trialmod.capability.TrialPlayerData;
import com.example.trialmod.capability.TrialPlayerDataEvents;
import com.example.trialmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class HookBlockEntity extends BlockEntity {
    private UUID hookedPlayerUuid = null;
    private int syncCooldown = 0;

    public HookBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOOK_BE.get(), pos, state);
    }

    public UUID getHookedPlayerUuid() {
        return hookedPlayerUuid;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HookBlockEntity be) {
        if (level.isClientSide()) return;

        if (be.hookedPlayerUuid != null) {
            ServerLevel serverLevel = (ServerLevel) level;
            ServerPlayer hookedPlayer = (ServerPlayer) serverLevel.getPlayerByUUID(be.hookedPlayerUuid);

            if (hookedPlayer == null || hookedPlayer.isRemoved()) {
                be.hookedPlayerUuid = null;
                be.setChanged();
                return;
            }

            TrialPlayerData data = TrialPlayerData.get(hookedPlayer);
            if (data.getSurvivorStatus() != SurvivorStatus.HOOKED) {
                be.hookedPlayerUuid = null;
                be.setChanged();
                return;
            }

            // Lock position: teleport player to hanging position on the hook
            hookedPlayer.teleportTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            hookedPlayer.setDeltaMovement(0, 0, 0);

            // Decay struggle progress
            float currentStruggle = data.getStruggleProgress();
            float newStruggle = currentStruggle - 0.1f; // 2% per second (decaying to 0 in 50 seconds)
            data.setStruggleProgress(newStruggle);

            // Sync periodically to avoid massive packet spam
            be.syncCooldown++;
            if (be.syncCooldown >= 10) {
                TrialPlayerDataEvents.syncPlayerData(hookedPlayer);
                be.syncCooldown = 0;
            }

            // Check if player died on hook
            if (newStruggle <= 0.0f) {
                data.setSurvivorStatus(SurvivorStatus.DEAD);
                data.setRole(PlayerRole.SPECTATOR); // Automatically make them spectator
                TrialPlayerDataEvents.syncPlayerData(hookedPlayer);

                // Play death scream and wither death sound globally
                level.playSound(null, pos, SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 2.0f, 0.8f);
                level.playSound(null, pos, SoundEvents.PLAYER_DEATH, SoundSource.PLAYERS, 1.5f, 0.9f);

                // Broadcast death
                level.players().forEach(p -> p.sendSystemMessage(
                        Component.literal("§c[!] " + hookedPlayer.getScoreboardName() + " telah mati di Hook!")
                ));

                be.hookedPlayerUuid = null;
                be.setChanged();
            }
        }
    }

    public InteractionResult handleInteraction(Player player, InteractionHand hand) {
        if (level == null || level.isClientSide()) return InteractionResult.PASS;

        TrialPlayerData data = TrialPlayerData.get(player);

        if (hookedPlayerUuid != null) {
            // Unhook interaction: only other Survivors can rescue
            if (data.getRole() == PlayerRole.SURVIVOR && !player.getUUID().equals(hookedPlayerUuid)) {
                ServerPlayer hookedPlayer = (ServerPlayer) ((ServerLevel) level).getPlayerByUUID(hookedPlayerUuid);
                if (hookedPlayer != null) {
                    TrialPlayerData hookedData = TrialPlayerData.get(hookedPlayer);
                    hookedData.setSurvivorStatus(SurvivorStatus.INJURED);
                    hookedData.setStruggleProgress(100.0f);
                    TrialPlayerDataEvents.syncPlayerData(hookedPlayer);

                    // Teleport slightly offset from hook to free them
                    hookedPlayer.teleportTo(worldPosition.getX() + 0.5D, worldPosition.getY(), worldPosition.getZ() + 1.2D);

                    // Sound cues
                    level.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.2f, 1.1f);
                    level.playSound(null, worldPosition, SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0f, 1.4f);

                    // Broadcast saving event
                    level.players().forEach(p -> p.sendSystemMessage(
                            Component.literal("§a[!] " + player.getScoreboardName() + " menyelamatkan " + hookedPlayer.getScoreboardName() + " dari Hook!")
                    ));
                }

                hookedPlayerUuid = null;
                setChanged();
                return InteractionResult.SUCCESS;
            }
        } else {
            // Hook interaction: Killer hangs carried downed survivor on Hook
            if (data.getRole() == PlayerRole.KILLER) {
                Entity carriedEntity = player.getFirstPassenger();
                if (carriedEntity instanceof ServerPlayer carriedPlayer) {
                    TrialPlayerData carriedData = TrialPlayerData.get(carriedPlayer);
                    if (carriedData.getSurvivorStatus() == SurvivorStatus.DOWNED) {
                        // Dismount and hook
                        carriedPlayer.stopRiding();
                        hookedPlayerUuid = carriedPlayer.getUUID();
                        carriedData.setSurvivorStatus(SurvivorStatus.HOOKED);
                        carriedData.setStruggleProgress(100.0f);
                        TrialPlayerDataEvents.syncPlayerData(carriedPlayer);

                        // Mount player on hook
                        carriedPlayer.teleportTo(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D);

                        // Sounds: metallic clash and loud scream
                        level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.2f, 0.8f);
                        level.playSound(null, worldPosition, SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 1.8f, 0.8f);
                        level.playSound(null, worldPosition, SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.5f, 0.8f);

                        // Broadcast hooking event
                        level.players().forEach(p -> p.sendSystemMessage(
                                Component.literal("§c[!] Killer menggantung " + carriedPlayer.getScoreboardName() + " di Hook!")
                        ));

                        setChanged();
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (hookedPlayerUuid != null) {
            tag.putUUID("hookedPlayerUuid", hookedPlayerUuid);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("hookedPlayerUuid")) {
            hookedPlayerUuid = tag.getUUID("hookedPlayerUuid");
        } else {
            hookedPlayerUuid = null;
        }
    }
}
