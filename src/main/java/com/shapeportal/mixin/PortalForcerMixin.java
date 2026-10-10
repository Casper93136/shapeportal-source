package com.shapeportal.mixin;

import com.shapeportal.ShapePortals;
import com.shapeportal.portal.PortalContext;
import com.shapeportal.portal.PortalGenerator;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin {
    @Shadow @Final private ServerLevel level;

    @Inject(method = "createPortal", at = @At("HEAD"), cancellable = true)
    private void shapeportal$create(BlockPos pos, Direction.Axis axis,
                                    CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        ServerLevel src = PortalContext.level();
        BlockPos entrance = PortalContext.entrance();
        if (src == null || entrance == null) return;
        try {
            Optional<BlockUtil.FoundRectangle> r = PortalGenerator.generate(src, entrance, this.level, pos, axis);
            if (r.isPresent()) cir.setReturnValue(r);
        } catch (Throwable t) {
            ShapePortals.LOGGER.error("Shape portal generation failed, falling back to vanilla", t);
        }
    }
}
