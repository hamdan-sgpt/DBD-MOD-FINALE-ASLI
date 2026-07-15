package com.example.trialmod.client;

import com.example.trialmod.block.GeneratorBlockEntity;
import com.example.trialmod.network.ModMessages;
import com.example.trialmod.network.packet.ServerboundGeneratorSkillCheckPacket;
import com.example.trialmod.network.packet.ServerboundInteractGeneratorPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class GeneratorRepairOverlay {
    private static BlockPos currentGenPos = null;
    public static boolean skillCheckActive = false;
    public static float skillCheckProgress = 0.0f;
    public static float skillCheckTargetMin = 0.0f;
    public static float skillCheckTargetMax = 0.0f;
    private static int ticksSinceLastCheck = 0;

    public static final IGuiOverlay HUD = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        BlockPos targetPos = getLookingAtGenerator(mc);
        if (targetPos == null) {
            resetSkillCheck();
            return;
        }

        BlockEntity be = mc.level.getBlockEntity(targetPos);
        if (!(be instanceof GeneratorBlockEntity generator) || generator.isCompleted()) {
            resetSkillCheck();
            return;
        }

        // Render progress bar
        renderProgressBar(guiGraphics, width, height, generator.getProgress(), mc.font);

        // Interact logic
        if (mc.options.keyUse.isDown()) {
            ModMessages.sendToServer(new ServerboundInteractGeneratorPacket(targetPos));
            tickSkillCheck(targetPos, mc);
        } else {
            if (skillCheckActive) {
                failSkillCheck(targetPos);
            }
        }

        // Render active skill check
        if (skillCheckActive) {
            renderSkillCheck(guiGraphics, width, height, mc.font);
        }
    };

    private static BlockPos getLookingAtGenerator(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            if (mc.level.getBlockState(pos).getBlock() instanceof com.example.trialmod.block.GeneratorBlock) {
                if (mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 25.0) {
                    return pos;
                }
            }
        }
        return null;
    }

    private static void resetSkillCheck() {
        skillCheckActive = false;
        currentGenPos = null;
    }

    private static void tickSkillCheck(BlockPos pos, Minecraft mc) {
        currentGenPos = pos;
        ticksSinceLastCheck++;

        if (!skillCheckActive) {
            // Chance to trigger skill check (0.5% per tick, min 4 seconds / 80 ticks since last check)
            if (ticksSinceLastCheck > 80 && mc.level.random.nextFloat() < 0.005f) {
                skillCheckActive = true;
                skillCheckProgress = 0.0f;
                skillCheckTargetMin = 0.55f + mc.level.random.nextFloat() * 0.2f;
                skillCheckTargetMax = skillCheckTargetMin + 0.12f;

                mc.level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(),
                        SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.BLOCKS, 1.5f, 1.5f, false);
            }
        } else {
            // Progresses 5% per tick (lasts 20 ticks = 1 second)
            skillCheckProgress += 0.05f;
            if (skillCheckProgress >= 1.0f) {
                failSkillCheck(pos);
            }
        }
    }

    public static void handleKeyPress() {
        if (skillCheckActive && currentGenPos != null) {
            if (skillCheckProgress >= skillCheckTargetMin && skillCheckProgress <= skillCheckTargetMax) {
                ModMessages.sendToServer(new ServerboundGeneratorSkillCheckPacket(currentGenPos, true));
            } else {
                failSkillCheck(currentGenPos);
            }
            skillCheckActive = false;
            ticksSinceLastCheck = 0;
        }
    }

    private static void failSkillCheck(BlockPos pos) {
        ModMessages.sendToServer(new ServerboundGeneratorSkillCheckPacket(pos, false));
        skillCheckActive = false;
        ticksSinceLastCheck = 0;
    }

    private static void renderProgressBar(GuiGraphics guiGraphics, int width, int height, float progress, Font font) {
        int barWidth = 140;
        int barHeight = 8;
        int x = (width - barWidth) / 2;
        int y = height - 70;

        // Dark background
        guiGraphics.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0x88000000);
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF333333);

        // Progress fill (cyan)
        int fillWidth = (int) (barWidth * (progress / 100.0f));
        guiGraphics.fill(x, y, x + fillWidth, y + barHeight, 0xFF55FFFF);

        // Label
        String text = String.format("Repairing Generator: %.1f%%", progress);
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
        int targetXStart = (int) (barWidth * skillCheckTargetMin);
        int targetXEnd = (int) (barWidth * skillCheckTargetMax);
        guiGraphics.fill(x + targetXStart, y, x + targetXEnd, y + barHeight, 0xFF00FF00);

        // Indicator cursor (red)
        int cursorX = (int) (barWidth * skillCheckProgress);
        guiGraphics.fill(x + cursorX - 1, y - 2, x + cursorX + 2, y + barHeight + 2, 0xFFFF0000);

        // Key warning label
        guiGraphics.drawCenteredString(font, "§e§lSKILL CHECK! §7[SPACEBAR]", width / 2, y - 14, 0xFFFFFF55);
    }
}
