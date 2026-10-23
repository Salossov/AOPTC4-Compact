package thaumcraft.viewers;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.crafting.RecipeDisplay;
import thaumcraft.viewers.bridge.MenuTransferAdapter;
import thaumcraft.viewers.bridge.TheTerminalTransferAdapter;
import thaumcraft.viewers.bridge.TransferResult;
import thaumcraft.viewers.bridge.ViewerTransferBridge;
import thaumcraft.viewers.bridge.WorkbenchTransferAdapter;

import java.util.List;

/**
 * Headless verification fixture for viewer bridge routing, menu matching, and contract invariants.
 * Does not instantiate Minecraft world or network.
 */
public final class BridgeRoutingTest {
    public static void main(String[] args) {
        testAdaptersRegistered();
        testWorkbenchAdapterInvariants();
        testTerminalAdapterInvariants();
        System.out.println("BRIDGE_ROUTING_TEST_PASS");
    }

    private static void testAdaptersRegistered() {
        if (ViewerTransferBridge.workbench() == null) {
            throw new AssertionError("Workbench adapter must not be null");
        }
        if (ViewerTransferBridge.theTerminal() == null) {
            throw new AssertionError("Terminal adapter must not be null");
        }
        if (ViewerTransferBridge.findAdapter(null) != null) {
            throw new AssertionError("Null menu must not match any adapter");
        }
    }

    private static void testWorkbenchAdapterInvariants() {
        WorkbenchTransferAdapter adapter = ViewerTransferBridge.workbench();
        if (adapter.supportsCrafting(null, null)) {
            throw new AssertionError("Workbench adapter must never claim support for vanilla crafting");
        }
        TransferResult result = adapter.canTransferCrafting(null, null, null, false);
        if (result.success()) {
            throw new AssertionError("Workbench adapter must reject vanilla crafting transfer");
        }
        if (adapter.supportsArcane(null, null)) {
            throw new AssertionError("Null recipe must not be supported");
        }
    }

    private static void testTerminalAdapterInvariants() {
        TheTerminalTransferAdapter adapter = ViewerTransferBridge.theTerminal();
        if (adapter.matchesMenu(null)) {
            throw new AssertionError("Null menu must not match terminal adapter");
        }
        if (adapter.supportsArcane(null, null)) {
            throw new AssertionError("Null arcane recipe must not be supported");
        }
        if (adapter.supportsCrafting(null, null)) {
            throw new AssertionError("Null crafting recipe must not be supported");
        }
        TransferResult nullMenuResult = adapter.canTransferArcane(null, null, null, false);
        if (nullMenuResult.success()) {
            throw new AssertionError("Terminal adapter must reject null menu");
        }
    }

    private BridgeRoutingTest() {}
}
