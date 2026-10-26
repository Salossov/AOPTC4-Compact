package thaumcraft.viewers.tooltip.the;

import mcp.mobius.waila.api.*;
import mcp.mobius.waila.api.component.WrappedComponent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
                var snapshot = accessor.getData().raw().copy();
                // AE2's WTHIT legacy registrar installs this body unconditionally;
                // unlike Jade, it exposes no per-grid-status config key. Presence
                // of the native payload below is the authoritative fallback gate.
                boolean nativeStatus = true;
                TheTooltip.withoutDuplicateNetworkStatus(TheTooltip.readOrLocal(snapshot, accessor.getBlockEntity(), side, hitPos), snapshot, nativeStatus)
                        .forEach(line -> tooltip.addLine(new TargetedLine(line, snapshot)));
            }
        }
    }

    /** WTHIT caches its built lines between updates. Validate again at draw time,
     * after Minecraft's current frame has picked a potentially different part. */
    private static final class TargetedLine extends WrappedComponent {
        private final CompoundTag snapshot;

        TargetedLine(Component text, CompoundTag snapshot) {
            super(text);
            this.snapshot = snapshot.copy();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof TargetedLine line && super.equals(other) && snapshot.equals(line.snapshot);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + snapshot.hashCode();
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, DeltaTracker delta) {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                    || !minecraft.level.hasChunkAt(hit.getBlockPos())) return;
            var current = minecraft.level.getBlockEntity(hit.getBlockPos());
            if (TheTooltip.supports(snapshot, current, hit.getDirection(), hit.getLocation())) {
                super.render(graphics, x, y, delta);
            }
        }
    }
}
