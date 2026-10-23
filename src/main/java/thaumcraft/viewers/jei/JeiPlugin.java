package thaumcraft.viewers.jei;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import thaumcraft.api.crafting.RecipeDisplay;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.viewers.*;
import mezz.jei.api.*;
import mezz.jei.api.registration.*;
import mezz.jei.api.recipe.RecipeType;
@mezz.jei.api.JeiPlugin
public final class JeiPlugin implements IModPlugin {
 private static mezz.jei.api.runtime.IJeiRuntime runtime;
 private static List<RecipeDisplay> registered=List.of();
 private static Set<AspectRecipes.Entry> aspectPages=new LinkedHashSet<>();
 private static RecipeType<RecipeDisplay> type(RecipeDisplay.Kind kind){return new RecipeType<>(Catalog.id(kind),RecipeDisplay.class);}
 public ResourceLocation getPluginUid(){return ResourceLocation.parse("thaumcraft_viewers:jei");}
 public void registerItemSubtypes(ISubtypeRegistration r){if(Viewers.emi())return;for(var item:thaumcraft.api.aspects.AspectApi.variants().stream().map(net.minecraft.world.item.ItemStack::getItem).distinct().toList())r.registerSubtypeInterpreter(item,(stack,context)->stack.getComponentsPatch().toString());}
 public void registerIngredients(IModIngredientRegistration r){if(Viewers.emi())return;var values=AspectRecipes.values();var a=new JeiAspects();r.register(JeiAspects.TYPE,values,a,a);}
 public void registerCategories(IRecipeCategoryRegistration r){if(Viewers.emi())return;for(var k:RecipeDisplay.Kind.values())r.addRecipeCategories(new JeiCategory(k));for(var k:AspectRecipes.Kind.values())r.addRecipeCategories(new JeiAspectCategory(k));}
 public void registerRecipes(IRecipeRegistration r){if(Viewers.emi())return;registered=Catalog.all();for(var k:RecipeDisplay.Kind.values())r.addRecipes(type(k),registered.stream().filter(d->d.kind()==k).toList());aspectPages=new LinkedHashSet<>(AspectRecipes.entries());for(var k:AspectRecipes.Kind.values())r.addRecipes(JeiAspectCategory.type(k),aspectPages.stream().filter(e->e.kind()==k).toList());}
 public void registerRecipeCatalysts(IRecipeCatalystRegistration r){if(Viewers.emi())return;Catalog.workstations().forEach((kind,stacks)->stacks.forEach(stack->r.addRecipeCatalyst(stack,type(kind))));}
 public void registerRecipeTransferHandlers(IRecipeTransferRegistration r){
  if(Viewers.emi())return;
  var helper=r.getTransferHelper();
  r.addRecipeTransferHandler(new mezz.jei.api.recipe.transfer.IRecipeTransferHandler<AbstractContainerMenu,RecipeDisplay>(){
   public Class<? extends AbstractContainerMenu> getContainerClass(){return thaumcraft.api.crafting.WorkbenchApi.menuClass();}
   @SuppressWarnings("unchecked") public Optional<net.minecraft.world.inventory.MenuType<AbstractContainerMenu>> getMenuType(){return Optional.of((net.minecraft.world.inventory.MenuType<AbstractContainerMenu>)thaumcraft.api.crafting.WorkbenchApi.menuType());}
   public RecipeType<RecipeDisplay> getRecipeType(){return type(RecipeDisplay.Kind.ARCANE);}
   public mezz.jei.api.recipe.transfer.IRecipeTransferError transferRecipe(AbstractContainerMenu menu,RecipeDisplay recipe,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.world.entity.player.Player player,boolean maximum,boolean transfer){
    var adapter=thaumcraft.viewers.bridge.ViewerTransferBridge.workbench();
    if(!adapter.matchesMenu(menu))return helper.createInternalError();
    var check=adapter.canTransferArcane(menu,recipe,player,maximum);
    if(!check.success())return helper.createUserErrorWithTooltip(check.error());
    if(transfer)adapter.transferArcane(menu,recipe,player,maximum);
    return null;
   }
  },type(RecipeDisplay.Kind.ARCANE));
  thaumcraft.viewers.bridge.TheTerminalTransferAdapter.getMenuType().ifPresent(terminalType->{
   r.addRecipeTransferHandler(new mezz.jei.api.recipe.transfer.IRecipeTransferHandler<AbstractContainerMenu,RecipeDisplay>(){
    public Class<? extends AbstractContainerMenu> getContainerClass(){return AbstractContainerMenu.class;}
    @SuppressWarnings("unchecked") public Optional<net.minecraft.world.inventory.MenuType<AbstractContainerMenu>> getMenuType(){return Optional.of((net.minecraft.world.inventory.MenuType<AbstractContainerMenu>)(net.minecraft.world.inventory.MenuType<?>)terminalType);}
    public RecipeType<RecipeDisplay> getRecipeType(){return type(RecipeDisplay.Kind.ARCANE);}
    public mezz.jei.api.recipe.transfer.IRecipeTransferError transferRecipe(AbstractContainerMenu menu,RecipeDisplay recipe,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.world.entity.player.Player player,boolean maximum,boolean transfer){
     var adapter=thaumcraft.viewers.bridge.ViewerTransferBridge.theTerminal();
     if(!adapter.matchesMenu(menu))return helper.createInternalError();
     var check=adapter.canTransferArcane(menu,recipe,player,maximum);
     if(!check.success())return helper.createUserErrorWithTooltip(check.error());
     if(transfer)adapter.transferArcane(menu,recipe,player,maximum);
     return null;
    }
   },type(RecipeDisplay.Kind.ARCANE));
   r.addRecipeTransferHandler(new mezz.jei.api.recipe.transfer.IRecipeTransferHandler<AbstractContainerMenu,net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>>(){
    public Class<? extends AbstractContainerMenu> getContainerClass(){return AbstractContainerMenu.class;}
    @SuppressWarnings("unchecked") public Optional<net.minecraft.world.inventory.MenuType<AbstractContainerMenu>> getMenuType(){return Optional.of((net.minecraft.world.inventory.MenuType<AbstractContainerMenu>)(net.minecraft.world.inventory.MenuType<?>)terminalType);}
    public RecipeType<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> getRecipeType(){return mezz.jei.api.constants.RecipeTypes.CRAFTING;}
    public mezz.jei.api.recipe.transfer.IRecipeTransferError transferRecipe(AbstractContainerMenu menu,net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe> recipe,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.world.entity.player.Player player,boolean maximum,boolean transfer){
     var adapter=thaumcraft.viewers.bridge.ViewerTransferBridge.theTerminal();
     if(!adapter.matchesMenu(menu))return helper.createInternalError();
     if(!adapter.supportsCrafting(menu,recipe.value()))return helper.createInternalError();
     if(transfer){
      adapter.transferCrafting(menu,recipe,player,maximum);
     }
     return null;
    }
   },mezz.jei.api.constants.RecipeTypes.CRAFTING);
  });
 }
 public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime r){if(Viewers.emi())return;runtime=r;r.getIngredientManager().addIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,thaumcraft.api.aspects.AspectApi.variants());refresh(false);}
 public void onRuntimeUnavailable(){runtime=null;registered=List.of();aspectPages=new LinkedHashSet<>();}
 public static void refresh(boolean catalog){if(runtime==null||Viewers.emi())return;var manager=runtime.getRecipeManager();
  var desired=AspectRecipes.entries();var added=desired.stream().filter(e->!aspectPages.contains(e)).toList();
  for(var k:AspectRecipes.Kind.values()){manager.addRecipes(JeiAspectCategory.type(k),added.stream().filter(e->e.kind()==k).toList());manager.hideRecipes(JeiAspectCategory.type(k),aspectPages.stream().filter(e->e.kind()==k&&!desired.contains(e)).toList());manager.unhideRecipes(JeiAspectCategory.type(k),desired.stream().filter(e->e.kind()==k).toList());}aspectPages.addAll(added);
  var ingredients=runtime.getIngredientManager();var values=AspectRecipes.values();ingredients.removeIngredientsAtRuntime(JeiAspects.TYPE,values);ingredients.addIngredientsAtRuntime(JeiAspects.TYPE,values);
  if(catalog){for(var k:RecipeDisplay.Kind.values())manager.hideRecipes(type(k),registered.stream().filter(d->d.kind()==k).toList());registered=Catalog.all();for(var k:RecipeDisplay.Kind.values())manager.addRecipes(type(k),registered.stream().filter(d->d.kind()==k).toList());}for(var k:RecipeDisplay.Kind.values()){var recipes=registered.stream().filter(d->d.kind()==k).toList();manager.hideRecipes(type(k),recipes.stream().filter(d->!Catalog.visible(d)).toList());manager.unhideRecipes(type(k),recipes.stream().filter(Catalog::visible).toList());}}
}
