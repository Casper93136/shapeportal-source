package com.shapeportal.mixin;

import com.shapeportal.portal.PortalUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla removes a portal block when the surroundings are not a perfect rectangle.
 * For free-form portals a block survives as long as its in-plane neighbour is
 * another portal block or a frame block.
 */
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin {
    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
    private void shapeportal$update(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                    BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
        Direction.Axis axis = state.getValue(NetherPortalBlock.AXIS);
        boolean outOfPlane = dir.getAxis() != axis && dir.getAxis().isHorizontal();
        if (outOfPlane || neighbor.is(Blocks.NETHER_PORTAL) || PortalUtil.isFrame(neighbor)) {
            cir.setReturnValue(state);
        } else {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }
}
