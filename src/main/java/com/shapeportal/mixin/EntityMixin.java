package com.shapeportal.mixin;

import com.shapeportal.portal.PortalContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow protected BlockPos portalEntrancePos;
    @Shadow public abstract Level level();

    @Inject(method = "findDimensionEntryPoint", at = @At("HEAD"))
    private void shapeportal$begin(ServerLevel dest, CallbackInfoReturnable<PortalInfo> cir) {
        PortalContext.begin(this.level(), this.portalEntrancePos);
    }

    @Inject(method = "findDimensionEntryPoint", at = @At("RETURN"))
    private void shapeportal$end(ServerLevel dest, CallbackInfoReturnable<PortalInfo> cir) {
        PortalContext.end();
    }
}
