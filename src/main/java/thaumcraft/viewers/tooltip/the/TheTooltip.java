package thaumcraft.viewers.tooltip.the;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartHelper;
import appeng.api.parts.SelectedPart;
import appeng.api.util.AEColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    public static final String TAG_TARGET_KIND = "target_kind";
    public static final String KIND_MONITOR = "monitor";
    public static final String KIND_PROVIDER = "provider";
    public static final String KIND_VIBRATION_CHAMBER = "vibration_chamber";
    public static final String KIND_ASSEMBLER = "arcane_assembler"; // Extension seam for upcoming assembler

    public static final String TAG_POS = "target_pos";
    public static final String TAG_DIM = "target_dim";
    public static final String TAG_PART_SIDE = "part_side";
    public static final String TAG_PART_TYPE = "part_type";
    public static final String TAG_LOCKED = "Locked";
    public static final String TAG_ASPECT = "Aspect";
    public static final String TAG_AMOUNT = "Amount";
    public static final String TAG_COLOR = "Color";
    public static final String TAG_ACTIVE = "Active";
    public static final String TAG_STORED_AMOUNT = "StoredAmount";
    public static final String TAG_STORED_ASPECT = "StoredAspect";
    public static final String TAG_PROCESSING_ASPECT = "ProcessingAspect";

    private static final Set<String> PROVIDER_CLASSES = Set.of(
            "thaumicenergistics.blocks.ProviderBlockEntity",
            "thaumicenergistics.blocks.EssentiaProviderBlockEntity",
            "thaumicenergistics.blocks.InfusionProviderBlockEntity"
    );

    private static final String VIBRATION_CHAMBER_CLASS = "thaumicenergistics.blocks.VibrationChamberBlockEntity";

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
        if (tile == null) return false;
        if (data != null && data.contains(KEY, Tag.TAG_COMPOUND)) {
            CompoundTag values = data.getCompound(KEY);
            if (isStale(values, tile)) {
                return false;
            }
            String kind = values.getString(TAG_TARGET_KIND);
            if (KIND_PROVIDER.equals(kind) || values.contains(TAG_COLOR, Tag.TAG_STRING)) {
                return isProviderBlockEntity(tile);
            }
            if (KIND_VIBRATION_CHAMBER.equals(kind)) {
                return isVibrationChamberBlockEntity(tile);
            }
            IPart currentPart = resolvePart(tile, side, hitPos);
            if (!(currentPart instanceof EssentiaStorageMonitorPart monitor)) {
                return false;
            }
            if (values.contains(TAG_PART_SIDE, Tag.TAG_STRING)) {
                String snapshotSide = values.getString(TAG_PART_SIDE);
                Direction currentSide = monitor.getSide();
                if (currentSide != null && !snapshotSide.isEmpty() && !currentSide.getName().equals(snapshotSide)) {
                    return false;
                }
            }
            if (values.contains(TAG_PART_TYPE, Tag.TAG_STRING)) {
                String snapshotType = values.getString(TAG_PART_TYPE);
                if (!snapshotType.isEmpty() && !monitor.getClass().getName().equals(snapshotType)) {
                    return false;
                }
            }
            return true;
        }
        return supports(tile, side, hitPos);
    }

    public static boolean supports(BlockEntity tile, Direction side, Vec3 hitPos) {
        if (tile == null) return false;
        if (isProviderBlockEntity(tile) || isVibrationChamberBlockEntity(tile)) {
            return true;
        }
        return resolvePart(tile, side, hitPos) instanceof EssentiaStorageMonitorPart;
    }

    public static void write(CompoundTag data, BlockEntity tile, Direction side, Vec3 hitPos) {
        if (tile == null || data == null) return;
        data.remove(KEY);

        if (isProviderBlockEntity(tile)) {
            CompoundTag values = new CompoundTag();
            values.putString(TAG_TARGET_KIND, KIND_PROVIDER);
            writePosAndDim(values, tile);
            Boolean active = getProviderActive(tile);
            if (active != null) {
                values.putBoolean(TAG_ACTIVE, active);
            }
            String color = getProviderColor(tile);
            if (color != null) {
                values.putString(TAG_COLOR, color);
            }
            data.put(KEY, values);
            return;
        }

        if (isVibrationChamberBlockEntity(tile)) {
            CompoundTag values = new CompoundTag();
            values.putString(TAG_TARGET_KIND, KIND_VIBRATION_CHAMBER);
            writePosAndDim(values, tile);
            writeVibrationChamberData(values, tile);
            data.put(KEY, values);
            return;
        }

        IPart part = resolvePart(tile, side, hitPos);
        if (part instanceof EssentiaStorageMonitorPart monitor) {
            CompoundTag values = new CompoundTag();
            values.putString(TAG_TARGET_KIND, KIND_MONITOR);
            writePosAndDim(values, tile);
            Direction partSide = monitor.getSide();
            if (partSide != null) {
                values.putString(TAG_PART_SIDE, partSide.getName());
            } else if (side != null) {
                values.putString(TAG_PART_SIDE, side.getName());
            }
            values.putString(TAG_PART_TYPE, monitor.getClass().getName());
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
            // Point 10: Do not query or mutate server world on client thread. Missing authoritative snapshot displays nothing.
            return List.of();
        }
        CompoundTag values = data.getCompound(KEY);
        if (isStale(values, tile)) {
            return List.of();
        }

        var result = new ArrayList<Component>();
        String kind = values.getString(TAG_TARGET_KIND);

        if (KIND_PROVIDER.equals(kind) || values.contains(TAG_COLOR, Tag.TAG_STRING)) {
            if (!isProviderBlockEntity(tile)) {
                return List.of();
            }
            if (values.contains(TAG_ACTIVE, Tag.TAG_BYTE)) {
                boolean active = values.getBoolean(TAG_ACTIVE);
                result.add(Component.translatable(active ? "thaumcraft_viewers.the.online" : "thaumcraft_viewers.the.offline"));
            }
            if (values.contains(TAG_COLOR, Tag.TAG_STRING)) {
                String colorName = values.getString(TAG_COLOR);
                Component colorComp;
                colorComp = Component.translatable(colorName);
                result.add(Component.translatable("thaumcraft_viewers.the.color", colorComp));
            }
            return List.copyOf(result);
        }

        if (KIND_VIBRATION_CHAMBER.equals(kind)) {
            if (!isVibrationChamberBlockEntity(tile)) {
                return List.of();
            }
            if (values.contains(TAG_STORED_AMOUNT, Tag.TAG_INT) && values.contains(TAG_STORED_ASPECT, Tag.TAG_STRING)) {
                int amount = values.getInt(TAG_STORED_AMOUNT);
                String aspectTag = values.getString(TAG_STORED_ASPECT);
                Aspect aspect = Aspect.getAspect(aspectTag);
                if (aspect != null) {
                    Component name = !ResearchApi.clientKnowledge().knows(aspect)
                            ? Component.translatable("thaumcraft_viewers.unknown_aspect")
                            : aspect.translationKey() == null ? Component.literal(aspect.getName()) : Component.translatable(aspect.translationKey());
                    result.add(Component.translatable("thaumcraft_viewers.the.vibration_stored", amount, name));
                }
            }
            if (values.contains(TAG_PROCESSING_ASPECT, Tag.TAG_STRING)) {
                String aspectTag = values.getString(TAG_PROCESSING_ASPECT);
                Aspect aspect = Aspect.getAspect(aspectTag);
                if (aspect != null) {
                    Component name = !ResearchApi.clientKnowledge().knows(aspect)
                            ? Component.translatable("thaumcraft_viewers.unknown_aspect")
                            : aspect.translationKey() == null ? Component.literal(aspect.getName()) : Component.translatable(aspect.translationKey());
                    result.add(Component.translatable("thaumcraft_viewers.the.vibration_processing", name));
                }
            }
            return List.copyOf(result);
        }

        // Validate part under crosshair against snapshot on CableBus:
        IPart currentPart = resolvePart(tile, side, hitPos);
        if (!(currentPart instanceof EssentiaStorageMonitorPart monitor)) {
            return List.of();
        }
        if (values.contains(TAG_PART_SIDE, Tag.TAG_STRING)) {
            String snapshotSide = values.getString(TAG_PART_SIDE);
            Direction currentSide = monitor.getSide();
            if (currentSide != null && !snapshotSide.isEmpty() && !currentSide.getName().equals(snapshotSide)) {
                return List.of();
            }
        }
        if (values.contains(TAG_PART_TYPE, Tag.TAG_STRING)) {
            String snapshotType = values.getString(TAG_PART_TYPE);
            if (!snapshotType.isEmpty() && !monitor.getClass().getName().equals(snapshotType)) {
                return List.of();
            }
        }

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
        if (values.contains(TAG_LOCKED, Tag.TAG_BYTE)) {
            boolean locked = values.getBoolean(TAG_LOCKED);
            result.add(Component.translatable(locked ? "thaumcraft_viewers.the.locked" : "thaumcraft_viewers.the.unlocked"));
        }
        return List.copyOf(result);
    }

    public static boolean isProviderBlockEntity(BlockEntity tile) {
        if (tile == null) return false;
        Class<?> clazz = tile.getClass();
        while (clazz != null && clazz != Object.class) {
            if (PROVIDER_CLASSES.contains(clazz.getName())) return true;
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    public static boolean isVibrationChamberBlockEntity(BlockEntity tile) {
        if (tile == null) return false;
        Class<?> clazz = tile.getClass();
        while (clazz != null && clazz != Object.class) {
            if (VIBRATION_CHAMBER_CLASS.equals(clazz.getName())) return true;
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    private static boolean isStale(CompoundTag values, BlockEntity tile) {
        if (tile == null) return true;
        if (values.contains(TAG_POS, Tag.TAG_LONG)) {
            if (tile.getBlockPos().asLong() != values.getLong(TAG_POS)) {
                return true;
            }
        }
        if (values.contains(TAG_DIM, Tag.TAG_STRING) && tile.getLevel() != null) {
            String dim = values.getString(TAG_DIM);
            if (!dim.isEmpty() && !tile.getLevel().dimension().location().toString().equals(dim)) {
                return true;
            }
        }
        return false;
    }

    private static void writePosAndDim(CompoundTag values, BlockEntity tile) {
        values.putLong(TAG_POS, tile.getBlockPos().asLong());
        if (tile.getLevel() != null) {
            values.putString(TAG_DIM, tile.getLevel().dimension().location().toString());
        }
    }

    private static String getProviderColor(BlockEntity tile) {
        if (tile == null) return null;
        try {
            var method = tile.getClass().getMethod("getColor");
            Object color = method.invoke(tile);
            return color instanceof AEColor aeColor ? aeColor.translationKey : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static Boolean getProviderActive(BlockEntity tile) {
        if (tile == null) return null;
        try {
            var method = tile.getClass().getMethod("isActive");
            Object active = method.invoke(tile);
            return active instanceof Boolean b ? b : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static void writeVibrationChamberData(CompoundTag tag, BlockEntity tile) {
        try {
            var storedMethod = tile.getClass().getMethod("stored");
            Object stored = storedMethod.invoke(tile);
            if (stored != null) {
                int amount = (int) stored.getClass().getMethod("amount").invoke(stored);
                String aspectTag = (String) stored.getClass().getMethod("tag").invoke(stored);
                if (amount > 0 && aspectTag != null && !aspectTag.isEmpty()) {
                    tag.putInt(TAG_STORED_AMOUNT, amount);
                    tag.putString(TAG_STORED_ASPECT, aspectTag);
                }
            }
            var burnMethod = tile.getClass().getMethod("burn");
            Object burn = burnMethod.invoke(tile);
            if (burn != null) {
                Object aspectObj = burn.getClass().getMethod("aspect").invoke(burn);
                if (aspectObj instanceof Aspect a) {
                    tag.putString(TAG_PROCESSING_ASPECT, a.getTag());
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private TheTooltip() {}
}
