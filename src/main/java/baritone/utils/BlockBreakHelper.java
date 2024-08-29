/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import baritone.Baritone;
import baritone.api.utils.IPlayerContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;


/**
 * @author Brady
 * @since 8/25/2018
 */
public final class BlockBreakHelper {

    private final IPlayerContext ctx;
    private boolean didSwingArmLastTick;

    private int coolDownTicksLeft; // Hot fix for 2b2t
    private int swingArmTicks = 0;
    BlockBreakHelper(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public void stopBreakingBlock() {
        // The player controller will never be null, but the player can be
        if (ctx.player() != null && didSwingArmLastTick) {
            if (!ctx.playerController().hasBrokenBlock()) {
                // insane bypass to check breaking succeeded
                ctx.playerController().setHittingBlock(true);
            }
            ctx.playerController().resetBlockRemoving();
            didSwingArmLastTick = false;
        }
    }

    public void tick(boolean isLeftClick) {
        HitResult trace = ctx.objectMouseOver();
        boolean isBlockTrace = trace != null && trace.getType() == HitResult.Type.BLOCK;

        if (coolDownTicksLeft != 0) {
            coolDownTicksLeft--;
        } else if (isLeftClick && isBlockTrace) {
            if (!didSwingArmLastTick) {
                // Start breaking the block
                swingArmTicks = 0;
                ctx.playerController().syncHeldItem();
                ctx.playerController().clickBlock(((BlockHitResult) trace).getBlockPos(), ((BlockHitResult) trace).getDirection());
                ctx.player().swing(InteractionHand.MAIN_HAND);
            }

            // Attempt to break the block
            if (ctx.playerController().onPlayerDamageBlock(((BlockHitResult) trace).getBlockPos(), ((BlockHitResult) trace).getDirection())) {
                ctx.player().swing(InteractionHand.MAIN_HAND);
                swingArmTicks += 1;
            }

            ctx.playerController().setHittingBlock(false);

            didSwingArmLastTick = true;
        } else if (didSwingArmLastTick) { //Finished breaking the block
            final int INSTA_BREAK_TICKS = 1;
            if (swingArmTicks > INSTA_BREAK_TICKS) {
                coolDownTicksLeft = Baritone.settings().extraBreakTicks.value;
            }
            stopBreakingBlock();
            didSwingArmLastTick = false;
        }
    }
}