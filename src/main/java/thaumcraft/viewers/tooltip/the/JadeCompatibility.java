package thaumcraft.viewers.tooltip.the;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.BlockHitResult;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

@WailaPlugin("thaumicenergistics_aop")
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
                CompoundTag snapshot = accessor.getServerData().copy();
                if (!snapshot.contains(TheTooltip.KEY)) {
                    TheTooltip.write(snapshot, accessor.getBlockEntity(), accessor.getSide(), hitPos);
                }
                boolean nativeStatus = config.getKeys().contains(TheTooltip.AE_NETWORK_STATUS) && config.get(TheTooltip.AE_NETWORK_STATUS);
                TheTooltip.withoutDuplicateNetworkStatus(TheTooltip.readOrLocal(snapshot, accessor.getBlockEntity(), accessor.getSide(), hitPos), snapshot, nativeStatus)
                        .forEach(line -> tooltip.add(new TargetedLine(line, snapshot)));
            }
        }
    }

    /** Jade retains elements between tooltip collections. Validate the captured
     * target against this frame's pick before drawing a cached ThE line. */
    private static final class TargetedLine extends Element {
        private final IElement delegate;
        private final CompoundTag snapshot;

        TargetedLine(Component text, CompoundTag snapshot) {
            this.delegate = IElementHelper.get().text(text);
            this.snapshot = snapshot.copy();
        }

        @Override
        public Vec2 getSize() {
            return delegate.getCachedSize();
        }

        @Override
        public String getMessage() {
            return delegate.getCachedMessage();
        }

        @Override
        public void render(GuiGraphics graphics, float x, float y, float width, float height) {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                    || !minecraft.level.hasChunkAt(hit.getBlockPos())) return;
            var current = minecraft.level.getBlockEntity(hit.getBlockPos());
            if (TheTooltip.supports(snapshot, current, hit.getDirection(), hit.getLocation())) {
                delegate.render(graphics, x, y, width, height);
            }
        }
    }
}
