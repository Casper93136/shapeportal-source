package com.shapeportal;

import com.shapeportal.portal.CustomPortal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Lets flint & steel / fire charge light a closed obsidian frame of ANY shape. */
@Mod.EventBusSubscriber(modid = ShapePortals.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class IgniteHandler {

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        boolean flint = stack.is(Items.FLINT_AND_STEEL);
        if (!flint && !stack.is(Items.FIRE_CHARGE)) return;
        Level level = event.getLevel();
        if (level.dimension() != Level.OVERWORLD && level.dimension() != Level.NETHER) return;
        Direction face = event.getFace();
        if (face == null) return;

        BlockPos target = event.getPos().relative(face);
        CustomPortal portal = CustomPortal.detect(level, target, Direction.Axis.X);
        if (portal == null) portal = CustomPortal.detect(level, target, Direction.Axis.Z);
        if (portal == null) return; // not a closed frame -> vanilla behaviour

        Player player = event.getEntity();
        if (!level.isClientSide) {
            portal.ignite(level);
            level.playSound(null, target, flint ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRECHARGE_USE,
                SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 0.8F);
            if (!player.getAbilities().instabuild) {
                if (flint) stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
                else stack.shrink(1);
            }
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
    }
}
