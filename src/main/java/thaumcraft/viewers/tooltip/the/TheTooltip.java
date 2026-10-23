package thaumcraft.viewers.tooltip.the;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartHelper;
import appeng.api.parts.SelectedPart;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchApi;
import thaumicenergistics.parts.EssentiaStorageMonitorPart;
import thaumicenergistics.storage.EssentiaKey;

/**
 * Shared snapshot and presentation logic for Thaumic Energistics devices in Jade and WTHIT.
 */
public final class TheTooltip {
    public static final String KEY = "thaumicenergistics:devices";
    public static final String TAG_LOCKED = "Locked";
    public static final String TAG_ASPECT = "Aspect";
    public static final String TAG_AMOUNT = "Amount";

    /**
     * Resolves the part at the targeted hit location or side.
     * When hitPos is available, it uses AE2's selection logic to identify the exact hit part.
     */
    public static IPart resolvePart(BlockEntity tile, Direction side, Vec3 hitPos) {
        if (tile == null) return null;
        IPartHost host = null;
        if (tile instanceof IPartHost partHost) {
            host = partHost;
        } else if (tile.getLevel() != null) {
            host = PartHelper.getPartHost(tile.getLevel(), tile.getBlockPos());
        }
        if (host == null) return null;
        if (hitPos != null) {
            SelectedPart selected = host.selectPartWorld(hitPos);
            if (selected != null && selected.part != null) {
                return selected.part;
            }
            return null;
        }
        return side != null ? host.getPart(side) : null;
    }

    public static boolean supports(CompoundTag data, BlockEntity tile, Direction side, Vec3 hitPos) {
        if (data != null && data.contains(KEY, Tag.TAG_COMPOUND)) return true;
        return supports(tile, side, hitPos);
    }

    public static boolean supports(BlockEntity tile, Direction side, Vec3 hitPos) {
        return resolvePart(tile, side, hitPos) instanceof EssentiaStorageMonitorPart;
    }

    public static void write(CompoundTag data, BlockEntity tile, Direction side, Vec3 hitPos) {
        data.remove(KEY);
        IPart part = resolvePart(tile, side, hitPos);
        if (part instanceof EssentiaStorageMonitorPart monitor) {
            CompoundTag values = new CompoundTag();
            values.putBoolean(TAG_LOCKED, monitor.isLocked());
            EssentiaKey displayed = monitor.getDisplayed();
            if (displayed != null) {
                values.putString(TAG_ASPECT, displayed.tag());
                values.putLong(TAG_AMOUNT, monitor.getAmount());
            }
            data.put(KEY, values);
        }
    }

    public static List<Component> readOrLocal(CompoundTag data, BlockEntity tile, Direction side, Vec3 hitPos) {
        if (data == null || !data.contains(KEY, Tag.TAG_COMPOUND)) {
            data = new CompoundTag();
            write(data, tile, side, hitPos);
        }
        if (!data.contains(KEY, Tag.TAG_COMPOUND)) {
            return List.of();
        }
        var result = new ArrayList<Component>();
        CompoundTag values = data.getCompound(KEY);
        if (values.contains(TAG_ASPECT, Tag.TAG_STRING)) {
            String aspectTag = values.getString(TAG_ASPECT);
            Aspect aspect = Aspect.getAspect(aspectTag);
            long amount = values.getLong(TAG_AMOUNT);
            if (aspect != null) {
                Component name = !ResearchApi.clientKnowledge().knows(aspect)
                        ? Component.translatable("thaumcraft_viewers.unknown_aspect")
                        : aspect.translationKey() == null ? Component.literal(aspect.getName()) : Component.translatable(aspect.translationKey());
                result.add(Component.translatable("thaumcraft_viewers.essentia_contents", name, amount));
            }
        }
        boolean locked = values.getBoolean(TAG_LOCKED);
        result.add(Component.translatable(locked ? "thaumcraft_viewers.the.locked" : "thaumcraft_viewers.the.unlocked"));
        return List.copyOf(result);
    }

    private TheTooltip() {}
}
