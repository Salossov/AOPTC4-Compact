package thaumcraft.viewers.tooltip;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin("thaumcraft_compact")
public final class JadeCoreCompatibility implements IWailaPlugin {
    public static final Provider PROVIDER = new Provider();
    @Override public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(PROVIDER, BlockEntity.class);
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PROVIDER, Block.class);
    }
    public static final class Provider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        @Override public ResourceLocation getUid() { return ResourceLocation.parse("thaumcraft_viewers:essentia"); }
        @Override public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            CoreTooltip.write(tag, accessor.getBlockEntity(), accessor.getSide());
        }
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (CoreTooltip.supports(accessor.getBlockEntity()))
                CoreTooltip.readOrLocal(accessor.getServerData(), accessor.getBlockEntity(), accessor.getSide()).forEach(tooltip::add);
        }
    }
}
