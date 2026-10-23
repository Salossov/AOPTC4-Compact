package thaumcraft.viewers.bridge;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import thaumcraft.api.crafting.RecipeDisplay;

public interface MenuTransferAdapter {
    boolean matchesMenu(AbstractContainerMenu menu);

    boolean supportsArcane(AbstractContainerMenu menu, RecipeDisplay recipe);

    TransferResult canTransferArcane(AbstractContainerMenu menu, RecipeDisplay recipe, Player player, boolean maximum);

    boolean transferArcane(AbstractContainerMenu menu, RecipeDisplay recipe, Player player, boolean maximum);

    boolean supportsCrafting(AbstractContainerMenu menu, CraftingRecipe recipe);

    TransferResult canTransferCrafting(AbstractContainerMenu menu, RecipeHolder<CraftingRecipe> recipe, Player player, boolean maximum);

    boolean transferCrafting(AbstractContainerMenu menu, RecipeHolder<CraftingRecipe> recipe, Player player, boolean maximum);
}
