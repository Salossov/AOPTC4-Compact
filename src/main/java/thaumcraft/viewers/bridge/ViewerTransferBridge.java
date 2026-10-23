package thaumcraft.viewers.bridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class ViewerTransferBridge {
    private static final List<MenuTransferAdapter> ADAPTERS = new ArrayList<>();
    private static final WorkbenchTransferAdapter WORKBENCH = new WorkbenchTransferAdapter();
    private static final TheTerminalTransferAdapter THE_TERMINAL = new TheTerminalTransferAdapter();

    static {
        ADAPTERS.add(WORKBENCH);
        ADAPTERS.add(THE_TERMINAL);
    }

    public static MenuTransferAdapter findAdapter(AbstractContainerMenu menu) {
        if (menu == null) return null;
        for (MenuTransferAdapter adapter : ADAPTERS) {
            if (adapter.matchesMenu(menu)) {
                return adapter;
            }
        }
        return null;
    }

    public static boolean isSupportedMenu(AbstractContainerMenu menu) {
        return findAdapter(menu) != null;
    }

    public static WorkbenchTransferAdapter workbench() {
        return WORKBENCH;
    }

    public static TheTerminalTransferAdapter theTerminal() {
        return THE_TERMINAL;
    }

    public static List<MenuTransferAdapter> adapters() {
        return Collections.unmodifiableList(ADAPTERS);
    }

    private ViewerTransferBridge() {}
}
