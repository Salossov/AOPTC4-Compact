package thaumcraft.viewers.tooltip.the;

import mcp.mobius.waila.api.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class WthitClientCompatibility implements IWailaClientPlugin {
    public static final Provider PROVIDER = new Provider();

    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(PROVIDER, Block.class);
    }

    public static final class Provider implements IBlockComponentProvider {
        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            BlockHitResult hit = accessor.getBlockHitResult();
            Vec3 hitPos = hit != null ? hit.getLocation() : null;
            Direction side = accessor.getSide() != null ? accessor.getSide() : (hit != null ? hit.getDirection() : null);
            if (TheTooltip.supports(accessor.getData().raw(), accessor.getBlockEntity(), side, hitPos)) {
                TheTooltip.readOrLocal(accessor.getData().raw(), accessor.getBlockEntity(), side, hitPos).forEach(tooltip::addLine);
            }
        }
    }
}
