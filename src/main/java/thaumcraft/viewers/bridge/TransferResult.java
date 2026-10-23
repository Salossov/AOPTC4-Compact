package thaumcraft.viewers.bridge;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public record TransferResult(boolean success, @Nullable Component error) {
    private static final TransferResult OK = new TransferResult(true, null);

    public static TransferResult ok() {
        return OK;
    }

    public static TransferResult fail(Component error) {
        return new TransferResult(false, error);
    }
}
