package thaumcraft.viewers.bridge;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import thaumcraft.api.crafting.RecipeDisplay;
import thaumcraft.viewers.Catalog;

/**
 * Adapter for the core Thaumcraft Arcane Workbench.
 * Uses public WorkbenchApi, Catalog research checks, TransferPlanner and ArcaneTransferRequest.
 */
public final class WorkbenchTransferAdapter implements MenuTransferAdapter {
    public static final ResourceLocation MENU_ID = ResourceLocation.parse("thaumcraft:arcane_workbench");

    @Override
    public boolean matchesMenu(AbstractContainerMenu menu) {
        return Catalog.arcane(menu);
    }

    @Override
    public boolean supportsArcane(AbstractContainerMenu menu, RecipeDisplay recipe) {
        return matchesMenu(menu) && recipe != null && recipe.kind() == RecipeDisplay.Kind.ARCANE && recipe.inputs().size() == 9 && recipe.id() != null;
    }

    @Override
    public TransferResult canTransferArcane(AbstractContainerMenu menu, RecipeDisplay recipe, Player player, boolean maximum) {
        if (!matchesMenu(menu) || player == null || !menu.stillValid(player)) {
            return TransferResult.fail(Component.translatable("thaumcraft_viewers.invalid_menu"));
        }
        if (!supportsArcane(menu, recipe)) {
            return TransferResult.fail(Component.translatable("thaumcraft_viewers.invalid_recipe"));
        }
        if (!Catalog.known(recipe)) {
            return TransferResult.fail(Component.translatable("thaumcraft_viewers.research_required"));
        }
        if (Catalog.plan(menu, recipe, maximum).isEmpty()) {
            return TransferResult.fail(Component.translatable("thaumcraft_viewers.transfer_failed"));
        }
        return TransferResult.ok();
    }

    @Override
    public boolean transferArcane(AbstractContainerMenu menu, RecipeDisplay recipe, Player player, boolean maximum) {
        if (!canTransferArcane(menu, recipe, player, maximum).success()) {
            return false;
        }
        Catalog.transfer(menu, recipe, maximum);
        return true;
    }

    @Override
    public boolean supportsCrafting(AbstractContainerMenu menu, CraftingRecipe recipe) {
        return false;
    }

    @Override
    public TransferResult canTransferCrafting(AbstractContainerMenu menu, RecipeHolder<CraftingRecipe> recipe, Player player, boolean maximum) {
        return TransferResult.fail(Component.translatable("thaumcraft_viewers.invalid_recipe"));
    }

    @Override
    public boolean transferCrafting(AbstractContainerMenu menu, RecipeHolder<CraftingRecipe> recipe, Player player, boolean maximum) {
        return false;
    }
}
