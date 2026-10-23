package thaumcraft.viewers.tooltip.the;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin("thaumicenergistics")
public final class JadeCompatibility implements IWailaPlugin {
    public static final Provider PROVIDER = new Provider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(PROVIDER, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PROVIDER, Block.class);
    }

    public static final class Provider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        public static final ResourceLocation UID = ResourceLocation.parse("thaumicenergistics:devices");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            Vec3 hitPos = accessor.getHitResult() != null ? accessor.getHitResult().getLocation() : null;
            TheTooltip.write(tag, accessor.getBlockEntity(), accessor.getSide(), hitPos);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Vec3 hitPos = accessor.getHitResult() != null ? accessor.getHitResult().getLocation() : null;
            if (TheTooltip.supports(accessor.getServerData(), accessor.getBlockEntity(), accessor.getSide(), hitPos)) {
                TheTooltip.readOrLocal(accessor.getServerData(), accessor.getBlockEntity(), accessor.getSide(), hitPos).forEach(tooltip::add);
            }
        }
    }
}
