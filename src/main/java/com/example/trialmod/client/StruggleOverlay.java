package com.example.trialmod.client;

import com.example.trialmod.capability.SurvivorStatus;
import com.example.trialmod.capability.TrialPlayerData;
import com.example.trialmod.network.ModMessages;
import com.example.trialmod.network.packet.ServerboundStruggleSkillCheckPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class StruggleOverlay {
    public static boolean struggleSkillCheckActive = false;
    public static float struggleSkillCheckProgress = 0.0f;
    public static float struggleSkillCheckTargetMin = 0.0f;
    public static float struggleSkillCheckTargetMax = 0.0f;
    private static int ticksSinceLastStruggleCheck = 0;

    public static final IGuiOverlay HUD = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        TrialPlayerData data = TrialPlayerData.get(mc.player);
        if (data.getSurvivorStatus() != SurvivorStatus.HOOKED) {
            resetStruggleSkillCheck();
            return;
        }

        // Render progress bar representing struggle progress (0-100)
        renderStruggleBar(guiGraphics, width, height, data.getStruggleProgress(), mc.font);

        // Update skill check logic
        ticksSinceLastStruggleCheck++;

        if (struggleSkillCheckActive) {
            struggleSkillCheckProgress += 0.05f; // lasts 20 ticks (1 second)
            if (struggleSkillCheckProgress >= 1.0f) {
                failStruggleSkillCheck(mc.player.blockPosition());
            }
        } else {
            // Chance to trigger struggle check (4% per tick after 6 seconds / 120 ticks)
            if (ticksSinceLastStruggleCheck > 120 && mc.level.random.nextFloat() < 0.03f) {
                struggleSkillCheckActive = true;
                struggleSkillCheckProgress = 0.0f;
                struggleSkillCheckTargetMin = 0.5f + mc.level.random.nextFloat() * 0.25f;
                struggleSkillCheckTargetMax = struggleSkillCheckTargetMin + 0.12f;

                mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.PLAYERS, 1.5f, 1.5f, false);
            }
        }

        // Render active skill check
        if (struggleSkillCheckActive) {
            renderSkillCheck(guiGraphics, width, height, mc.font);
        }
    };

    private static void resetStruggleSkillCheck() {
        struggleSkillCheckActive = false;
        ticksSinceLastStruggleCheck = 0;
    }

    public static void handleKeyPress() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (struggleSkillCheckActive) {
            if (struggleSkillCheckProgress >= struggleSkillCheckTargetMin && struggleSkillCheckProgress <= struggleSkillCheckTargetMax) {
                ModMessages.sendToServer(new ServerboundStruggleSkillCheckPacket(mc.player.blockPosition(), true));
            } else {
                failStruggleSkillCheck(mc.player.blockPosition());
            }
            struggleSkillCheckActive = false;
            ticksSinceLastStruggleCheck = 0;
        }
    }

    private static void failStruggleSkillCheck(BlockPos pos) {
        ModMessages.sendToServer(new ServerboundStruggleSkillCheckPacket(pos, false));
        struggleSkillCheckActive = false;
        ticksSinceLastStruggleCheck = 0;
    }

    private static void renderStruggleBar(GuiGraphics guiGraphics, int width, int height, float progress, Font font) {
        int barWidth = 140;
        int barHeight = 8;
        int x = (width - barWidth) / 2;
        int y = height - 70;

        // Dark background
        guiGraphics.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0x88000000);
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF333333);

        // Progress fill (red/orange for warning struggle)
        int fillWidth = (int) (barWidth * (progress / 100.0f));
        guiGraphics.fill(x, y, x + fillWidth, y + barHeight, 0xFFFF3333);

        // Label
        String text = String.format("§c§lSTRUGGLE: %.1f%%", progress);
        guiGraphics.drawCenteredString(font, text, width / 2, y - 12, 0xFFFFFFFF);
    }

    private static void renderSkillCheck(GuiGraphics guiGraphics, int width, int height, Font font) {
        int barWidth = 120;
        int barHeight = 10;
        int x = (width - barWidth) / 2;
        int y = height - 100;

        // Dark background
        guiGraphics.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0xAA000000);
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF222222);

        // Success zone (green)
        int targetXStart = (int) (barWidth * struggleSkillCheckTargetMin);
        int targetXEnd = (int) (barWidth * struggleSkillCheckTargetMax);
        guiGraphics.fill(x + targetXStart, y, x + targetXEnd, y + barHeight, 0xFF00FF00);

        // Indicator cursor (red)
        int cursorX = (int) (barWidth * struggleSkillCheckProgress);
        guiGraphics.fill(x + cursorX - 1, y - 2, x + cursorX + 2, y + barHeight + 2, 0xFFFF0000);

        // Key warning label
        guiGraphics.drawCenteredString(font, "§c§lSTRUGGLE! §e[SPACEBAR]", width / 2, y - 14, 0xFFFFFF55);
    }
}
