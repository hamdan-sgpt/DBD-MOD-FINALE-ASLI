package com.example.trialmod.command;

import com.example.trialmod.capability.PlayerRole;
import com.example.trialmod.capability.SurvivorStatus;
import com.example.trialmod.capability.TrialPlayerData;
import com.example.trialmod.capability.TrialPlayerDataEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TrialModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trialmod")
            .requires(source -> source.hasPermission(2)) // Require cheats enabled (OP level 2)
            .then(Commands.literal("role")
                .then(Commands.argument("roleName", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String roleName = StringArgumentType.getString(context, "roleName").toUpperCase();

                        try {
                            PlayerRole role = PlayerRole.valueOf(roleName);
                            TrialPlayerData.getLazy(player).ifPresent(data -> {
                                data.setRole(role);
                                TrialPlayerDataEvents.syncPlayerData(player);
                                player.sendSystemMessage(Component.literal("§a[!] Peran Anda berhasil diubah menjadi: §e" + role.name()));
                            });
                        } catch (IllegalArgumentException e) {
                            player.sendSystemMessage(Component.literal("§c[!] Peran tidak valid. Pilihan: SURVIVOR, KILLER, SPECTATOR"));
                        }
                        return 1;
                    })
                )
            )
            .then(Commands.literal("status")
                .then(Commands.argument("statusName", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String statusName = StringArgumentType.getString(context, "statusName").toUpperCase();

                        try {
                            SurvivorStatus status = SurvivorStatus.valueOf(statusName);
                            TrialPlayerData.getLazy(player).ifPresent(data -> {
                                if (data.getRole() == PlayerRole.SURVIVOR) {
                                    data.setSurvivorStatus(status);
                                    TrialPlayerDataEvents.syncPlayerData(player);
                                    player.sendSystemMessage(Component.literal("§a[!] Status Survivor Anda berhasil diubah menjadi: §e" + status.name()));
                                } else {
                                    player.sendSystemMessage(Component.literal("§c[!] Anda harus berada dalam peran SURVIVOR untuk mengubah status ini."));
                                }
                            });
                        } catch (IllegalArgumentException e) {
                            player.sendSystemMessage(Component.literal("§c[!] Status tidak valid. Pilihan: HEALTHY, INJURED, DOWNED, HOOKED, DEAD"));
                        }
                        return 1;
                    })
                )
            )
        );
    }
}
