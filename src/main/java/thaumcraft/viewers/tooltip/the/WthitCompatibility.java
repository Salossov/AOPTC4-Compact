package thaumcraft.viewers.tooltip.the;

import mcp.mobius.waila.api.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class WthitCompatibility implements IWailaCommonPlugin {
    public static final Provider PROVIDER = new Provider();

    @Override
    public void register(ICommonRegistrar registrar) {
        registrar.blockData(PROVIDER, BlockEntity.class);
    }

    public static final class Provider implements IDataProvider<BlockEntity> {
        @Override
        public void appendData(IDataWriter data, IServerAccessor<BlockEntity> accessor, IPluginConfig config) {
            HitResult hit = accessor.getHitResult();
            Vec3 hitPos = hit != null ? hit.getLocation() : null;
            Direction side = hit instanceof BlockHitResult bhr ? bhr.getDirection() : null;
            TheTooltip.write(data.raw(), accessor.getTarget(), side, hitPos);
        }
    }
}
