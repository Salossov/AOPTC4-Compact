package thaumcraft.viewers;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.crafting.RecipeDisplay;
import thaumcraft.viewers.bridge.MenuTransferAdapter;
import thaumcraft.viewers.bridge.TheTerminalTransferAdapter;
import thaumcraft.viewers.bridge.TransferResult;
import thaumcraft.viewers.bridge.ViewerTransferBridge;
import thaumcraft.viewers.bridge.WorkbenchTransferAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless verification fixture for viewer bridge routing, menu matching, and contract invariants.
 * Tests real positive and negative registry-ID, adapter routing, recipe constraints, and dry-run safety.
 * Does not instantiate Minecraft world or network.
 */
public final class BridgeRoutingTest {
    public static void main(String[] args) {
        testRegistryIds();
        testAdaptersRegistered();
        testWorkbenchAdapterInvariants();
        testTerminalAdapterInvariants();
        testRecipeDisplayInvariants();
        testDryRunSafety();
        System.out.println("BRIDGE_ROUTING_TEST_PASS");
    }

    private static void testRegistryIds() {
        ResourceLocation expectedTerminalId = ResourceLocation.parse("thaumicenergistics:arcane_crafting_terminal");
        if (!TheTerminalTransferAdapter.TERMINAL_MENU_ID.equals(expectedTerminalId)) {
            throw new AssertionError("TheTerminalTransferAdapter.TERMINAL_MENU_ID must be " + expectedTerminalId
                    + " but was " + TheTerminalTransferAdapter.TERMINAL_MENU_ID);
        }

        ResourceLocation expectedWorkbenchId = ResourceLocation.parse("thaumcraft:arcane_workbench");
        if (!WorkbenchTransferAdapter.MENU_ID.equals(expectedWorkbenchId)) {
            throw new AssertionError("WorkbenchTransferAdapter.MENU_ID must be " + expectedWorkbenchId
                    + " but was " + WorkbenchTransferAdapter.MENU_ID);
        }
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
        TransferResult nullMenuArcane = adapter.canTransferArcane(null, null, null, false);
        if (nullMenuArcane.success()) {
            throw new AssertionError("Workbench adapter must reject null menu for arcane transfer");
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
        TransferResult nullMenuCrafting = adapter.canTransferCrafting(null, null, null, false);
        if (nullMenuCrafting.success()) {
            throw new AssertionError("Terminal adapter must reject null menu for crafting transfer");
        }
    }

    private static void testRecipeDisplayInvariants() {
        WorkbenchTransferAdapter workbench = ViewerTransferBridge.workbench();
        TheTerminalTransferAdapter terminal = ViewerTransferBridge.theTerminal();

        List<List<ItemStack>> nineInputs = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            nineInputs.add(List.of(ItemStack.EMPTY));
        }
        List<List<ItemStack>> eightInputs = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            eightInputs.add(List.of(ItemStack.EMPTY));
        }

        RecipeDisplay arcane9WithId = new RecipeDisplay(
                RecipeDisplay.Kind.ARCANE,
                ResourceLocation.parse("thaumcraft:test_recipe"),
                nineInputs,
                ItemStack.EMPTY,
                List.of(),
                0,
                0,
                Component.empty(),
                null
        );

        RecipeDisplay arcane9WithoutId = new RecipeDisplay(
                RecipeDisplay.Kind.ARCANE,
                null,
                nineInputs,
                ItemStack.EMPTY,
                List.of(),
                0,
                0,
                Component.empty(),
                null
        );

        RecipeDisplay arcane8WithId = new RecipeDisplay(
                RecipeDisplay.Kind.ARCANE,
                ResourceLocation.parse("thaumcraft:test_recipe"),
                eightInputs,
                ItemStack.EMPTY,
                List.of(),
                0,
                0,
                Component.empty(),
                null
        );

        RecipeDisplay crucibleWithId = new RecipeDisplay(
                RecipeDisplay.Kind.CRUCIBLE,
                ResourceLocation.parse("thaumcraft:test_crucible"),
                nineInputs,
                ItemStack.EMPTY,
                List.of(),
                0,
                0,
                Component.empty(),
                null
        );

        // Recipe inputs must be exactly 9
        if (workbench.supportsArcane(null, arcane8WithId)) {
            throw new AssertionError("8-input recipe must not be supported for arcane transfer");
        }
        if (terminal.supportsArcane(null, arcane8WithId)) {
            throw new AssertionError("8-input recipe must not be supported for terminal transfer");
        }

        // Recipe ID must not be null
        if (workbench.supportsArcane(null, arcane9WithoutId)) {
            throw new AssertionError("Null recipe ID must not be supported for arcane transfer");
        }
        if (terminal.supportsArcane(null, arcane9WithoutId)) {
            throw new AssertionError("Null recipe ID must not be supported for terminal transfer");
        }

        // Recipe kind must be ARCANE
        if (workbench.supportsArcane(null, crucibleWithId)) {
            throw new AssertionError("Crucible recipe must not be supported by arcane workbench transfer");
        }
        if (terminal.supportsArcane(null, crucibleWithId)) {
            throw new AssertionError("Crucible recipe must not be supported by terminal transfer");
        }
    }

    private static void testDryRunSafety() {
        WorkbenchTransferAdapter workbench = ViewerTransferBridge.workbench();
        TheTerminalTransferAdapter terminal = ViewerTransferBridge.theTerminal();

        // Dry-run checks should never throw or mutate state
        TransferResult wbRes = workbench.canTransferArcane(null, null, null, false);
        if (wbRes.success() || wbRes.error() == null) {
            throw new AssertionError("Dry-run on invalid workbench state must return failure with error component");
        }

        TransferResult termArcaneRes = terminal.canTransferArcane(null, null, null, false);
        if (termArcaneRes.success() || termArcaneRes.error() == null) {
            throw new AssertionError("Dry-run on invalid terminal state must return failure with error component");
        }

        TransferResult termCraftRes = terminal.canTransferCrafting(null, null, null, false);
        if (termCraftRes.success() || termCraftRes.error() == null) {
            throw new AssertionError("Dry-run on invalid crafting terminal state must return failure with error component");
        }

        // Transfer execution on invalid inputs must safely return false
        if (workbench.transferArcane(null, null, null, false)) {
            throw new AssertionError("transferArcane on null must return false");
        }
        if (workbench.transferCrafting(null, null, null, false)) {
            throw new AssertionError("transferCrafting on workbench must return false");
        }
        if (terminal.transferArcane(null, null, null, false)) {
            throw new AssertionError("transferArcane on null terminal must return false");
        }
        if (terminal.transferCrafting(null, null, null, false)) {
            throw new AssertionError("transferCrafting on null terminal must return false");
        }
    }

    private BridgeRoutingTest() {}
}
