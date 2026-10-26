package thaumcraft.viewers;

import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.crafting.RecipeDisplay;
import thaumcraft.viewers.bridge.*;

/** Registry-backed routing and recipe-shape checks. Player/network scenarios remain native checks. */
public final class BridgeRoutingTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static MenuType<FixtureMenu> register(String id) {
        return Registry.register(BuiltInRegistries.MENU, ResourceLocation.parse(id),
                new MenuType<>((containerId, inventory) -> new FixtureMenu(null, containerId), FeatureFlags.DEFAULT_FLAGS));
    }

    private static RecipeDisplay recipe(String id, RecipeDisplay.Kind kind, int size) {
        return new RecipeDisplay(ResourceLocation.parse(id), ResourceLocation.parse(id), kind, List.of(),
                Collections.nCopies(size, Ingredient.EMPTY), List.of(), Map.of(), Map.of(), Map.of(),
                0, 0, List.of(), List.of());
    }

    public static void main(String[] args) {
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), new ArrayList<>(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Only this standalone fixture JVM opens its menu registry for test entries.
        ((net.minecraft.core.MappedRegistry<MenuType<?>>) BuiltInRegistries.MENU).unfreeze();
        var workbenchType = register("thaumcraft:arcane_workbench");
        var terminalType = register("thaumicenergistics:arcane_crafting_terminal");
        BuiltInRegistries.MENU.freeze();
        var workbenchMenu = new FixtureMenu(workbenchType, 1);
        var terminalMenu = new FixtureMenu(terminalType, 2);
        var otherMenu = new FixtureMenu(MenuType.GENERIC_9x1, 3);
        var workbench = ViewerTransferBridge.workbench();
        var terminal = ViewerTransferBridge.theTerminal();

        check(TheTerminalTransferAdapter.TERMINAL_MENU_ID.equals(ResourceLocation.parse("thaumicenergistics:arcane_crafting_terminal")), "Real terminal ID");
        check(WorkbenchTransferAdapter.MENU_ID.equals(ResourceLocation.parse("thaumcraft:arcane_workbench")), "Real workbench ID");
        check(ViewerTransferBridge.findAdapter(workbenchMenu) == workbench, "Workbench routes to workbench adapter");
        check(ViewerTransferBridge.findAdapter(terminalMenu) == terminal, "Terminal routes to terminal adapter");
        check(ViewerTransferBridge.findAdapter(otherMenu) == null, "Unrelated menu does not route");
        check(ViewerTransferBridge.findAdapter(null) == null, "Null menu does not route");
        check(!workbench.matchesMenu(null) && !terminal.matchesMenu(null), "Adapters reject null menus");
        check(!workbench.matchesMenu(terminalMenu) && !terminal.matchesMenu(workbenchMenu), "Adapters stay isolated");

        var arcane9 = recipe("test:arcane", RecipeDisplay.Kind.ARCANE, 9);
        var arcane8 = recipe("test:short_arcane", RecipeDisplay.Kind.ARCANE, 8);
        var crucible9 = recipe("test:crucible", RecipeDisplay.Kind.CRUCIBLE, 9);
        check(workbench.supportsArcane(workbenchMenu, arcane9), "Workbench accepts nine-slot arcane recipe");
        check(terminal.supportsArcane(terminalMenu, arcane9), "Terminal accepts nine-slot arcane recipe");
        check(!workbench.supportsArcane(workbenchMenu, arcane8), "Workbench rejects short arcane recipe");
        check(!terminal.supportsArcane(terminalMenu, arcane8), "Terminal rejects short arcane recipe");
        check(!workbench.supportsArcane(workbenchMenu, crucible9), "Workbench rejects crucible recipe");
        check(!terminal.supportsArcane(terminalMenu, crucible9), "Terminal rejects crucible recipe");
        check(!workbench.supportsArcane(workbenchMenu, null), "Workbench rejects absent recipe");
        check(!terminal.supportsArcane(terminalMenu, null), "Terminal rejects absent recipe");
        check(!terminal.supportsArcane(otherMenu, arcane9), "Valid recipe does not bypass menu identity");

        boolean rejectedNullId = false;
        try {
            new RecipeDisplay(null, null, RecipeDisplay.Kind.ARCANE, List.of(), List.of(), List.of(),
                    Map.of(), Map.of(), Map.of(), 0, 0, List.of(), List.of());
        } catch (NullPointerException expected) {
            rejectedNullId = true;
        }
        check(rejectedNullId, "Public recipe contract rejects null display ID at construction");

        // These calls must reject missing players before attempting any client network send.
        var before = terminalMenu.getSlot(0).getItem().copy();
        var carried = terminalMenu.getCarried().copy();
        var wbCheck = workbench.canTransferArcane(workbenchMenu, arcane9, null, false);
        var teCheck = terminal.canTransferArcane(terminalMenu, arcane9, null, false);
        check(!wbCheck.success() && wbCheck.error() != null, "Workbench dry-run reports invalid player");
        check(!teCheck.success() && teCheck.error() != null, "Terminal dry-run reports invalid player");
        check(!terminal.canTransferCrafting(terminalMenu, null, null, false).success(), "Null crafting holder rejected");
        check(!workbench.supportsCrafting(workbenchMenu, null), "Workbench never claims vanilla crafting");
        check(!workbench.transferArcane(workbenchMenu, arcane9, null, false), "Invalid workbench execution sends nothing");
        check(!terminal.transferArcane(terminalMenu, arcane9, null, false), "Invalid terminal execution sends nothing");
        check(!terminal.transferCrafting(terminalMenu, null, null, false), "Invalid vanilla execution sends nothing");
        check(ItemStack.matches(before, terminalMenu.getSlot(0).getItem()), "Dry-run preserves slot contents");
        check(ItemStack.matches(carried, terminalMenu.getCarried()), "Dry-run preserves carried stack");
        System.out.println("BRIDGE_ROUTING_TEST_PASS " + checks);
    }

    private static final class FixtureMenu extends AbstractContainerMenu {
        FixtureMenu(MenuType<?> type, int id) {
            super(type, id);
            var inventory = new SimpleContainer(1);
            inventory.setItem(0, new ItemStack(Items.STONE, 7));
            addSlot(new Slot(inventory, 0, 0, 0));
        }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    }

    private BridgeRoutingTest() {}
}
