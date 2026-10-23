package thaumcraft.viewers.bridge;

import java.lang.reflect.Constructor;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.crafting.RecipeDisplay;
import thaumcraft.viewers.Catalog;

/**
 * Adapter for the Thaumic Energistics Arcane Crafting Terminal.
 * Supports both Arcane recipes (via RecipeDisplay.Kind.ARCANE) and vanilla Crafting recipes.
 * Dispatches intent to the server via action 10 without leaking menu or recipe state.
 */
public final class TheTerminalTransferAdapter implements MenuTransferAdapter {
    public static final ResourceLocation TERMINAL_MENU_ID = ResourceLocation.parse("thaumicenergistics:arcane_terminal");
    private static final String TERMINAL_MENU_CLASS = "thaumicenergistics.menu.ArcaneTerminalMenu";
    private static final String ACTION_PAYLOAD_CLASS = "thaumicenergistics.network.ArcaneTerminalActionPayload";

    @Override
    public boolean matchesMenu(AbstractContainerMenu menu) {
        if (menu == null) return false;
        ResourceLocation id = BuiltInRegistries.MENU.getKey(menu.getType());
        if (id != null && id.equals(TERMINAL_MENU_ID)) {
            return true;
        }
        return menu.getClass().getName().equals(TERMINAL_MENU_CLASS);
    }

    public static Optional<MenuType<?>> getMenuType() {
        MenuType<?> type = BuiltInRegistries.MENU.get(TERMINAL_MENU_ID);
        return Optional.ofNullable(type);
    }

    @Override
    public boolean supportsArcane(AbstractContainerMenu menu, RecipeDisplay recipe) {
        return matchesMenu(menu) && recipe != null && recipe.kind() == RecipeDisplay.Kind.ARCANE && recipe.inputs().size() == 9;
    }

    @Override
    public TransferResult canTransferArcane(AbstractContainerMenu menu, RecipeDisplay recipe, Player player, boolean maximum) {
        if (!matchesMenu(menu)) {
            return TransferResult.fail(Component.translatable("thaumcraft_viewers.invalid_menu"));
        }
        if (!Catalog.known(recipe)) {
            return TransferResult.fail(Component.translatable("thaumcraft_viewers.research_required"));
        }
        return TransferResult.ok();
    }

    @Override
    public boolean transferArcane(AbstractContainerMenu menu, RecipeDisplay recipe, Player player, boolean maximum) {
        if (!canTransferArcane(menu, recipe, player, maximum).success()) {
            return false;
        }
        return sendAction(menu, recipe.id());
    }

    @Override
    public boolean supportsCrafting(AbstractContainerMenu menu, CraftingRecipe recipe) {
        return matchesMenu(menu) && recipe != null && (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe);
    }

    @Override
    public TransferResult canTransferCrafting(AbstractContainerMenu menu, RecipeHolder<CraftingRecipe> recipe, Player player, boolean maximum) {
        if (!matchesMenu(menu)) {
            return TransferResult.fail(Component.translatable("thaumcraft_viewers.invalid_menu"));
        }
        if (recipe == null || !supportsCrafting(menu, recipe.value())) {
            return TransferResult.fail(Component.translatable("thaumcraft_viewers.invalid_recipe"));
        }
        return TransferResult.ok();
    }

    @Override
    public boolean transferCrafting(AbstractContainerMenu menu, RecipeHolder<CraftingRecipe> recipe, Player player, boolean maximum) {
        if (!canTransferCrafting(menu, recipe, player, maximum).success()) {
            return false;
        }
        return sendAction(menu, recipe.id());
    }

    public static RecipeHolder<CraftingRecipe> findHolder(Player player, CraftingRecipe recipe) {
        if (player == null || player.level() == null || recipe == null) return null;
        for (RecipeHolder<CraftingRecipe> holder : player.level().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (holder.value() == recipe || holder.value().equals(recipe)) {
                return holder;
            }
        }
        return null;
    }

    public static boolean sendAction(AbstractContainerMenu menu, ResourceLocation recipeId) {
        if (menu == null || recipeId == null) return false;
        try {
            Class<?> payloadClass = Class.forName(ACTION_PAYLOAD_CLASS);
            Constructor<?> ctor = payloadClass.getConstructor(
                    int.class, int.class, int.class, int.class, boolean.class, ItemStack.class, String.class
            );
            CustomPacketPayload payload = (CustomPacketPayload) ctor.newInstance(
                    menu.containerId, 10, 0, 0, false, ItemStack.EMPTY, recipeId.toString()
            );
            PacketDistributor.sendToServer(payload);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
