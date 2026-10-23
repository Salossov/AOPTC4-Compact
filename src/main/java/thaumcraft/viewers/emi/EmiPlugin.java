package thaumcraft.viewers.emi;
import java.util.*;
import dev.emi.emi.api.*;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.recipe.handler.*;
import net.minecraft.world.inventory.*;
import thaumcraft.api.crafting.RecipeDisplay;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.viewers.*;
@EmiEntrypoint
public final class EmiPlugin implements dev.emi.emi.api.EmiPlugin {
 private static volatile boolean registered;
 public void initialize(EmiInitRegistry registry){registry.addIngredientSerializer(EmiAspect.class,new EmiAspect.Serializer());}
 @SuppressWarnings("unchecked") public void register(EmiRegistry registry){
  var categories=new EnumMap<RecipeDisplay.Kind,EmiRecipeCategory>(RecipeDisplay.Kind.class);for(var kind:RecipeDisplay.Kind.values()){var station=Catalog.workstations().getOrDefault(kind,List.of());var category=new EmiRecipeCategory(Catalog.id(kind),station.isEmpty()?new EmiAspect(new AspectValue("aer",false,1)):EmiStack.of(station.getFirst())){public net.minecraft.network.chat.Component getName(){return Catalog.title(kind);}};categories.put(kind,category);registry.addCategory(category);}
  for(var value:AspectRecipes.values())registry.addEmiStack(new EmiAspect(value));
  var aspects=new EnumMap<AspectRecipes.Kind,EmiRecipeCategory>(AspectRecipes.Kind.class);for(var kind:AspectRecipes.Kind.values()){var category=new EmiRecipeCategory(net.minecraft.resources.ResourceLocation.parse("thaumcraft_viewers:aspects/"+kind.name().toLowerCase(Locale.ROOT)),new EmiAspect(new AspectValue("aer",false,1))){public net.minecraft.network.chat.Component getName(){return AspectPages.title(kind);}};aspects.put(kind,category);registry.addCategory(category);}for(var entry:AspectRecipes.entries())registry.addRecipe(new EmiAspectPage(entry,aspects.get(entry.kind())));
  for(var r:Catalog.all())if(Catalog.visible(r))registry.addRecipe(new EmiDisplay(r,categories.get(r.kind())));
  Catalog.workstations().forEach((kind,stacks)->stacks.forEach(stack->registry.addWorkstation(categories.get(kind),EmiStack.of(stack))));
  var menu=(MenuType<AbstractContainerMenu>)(MenuType<?>)net.minecraft.core.registries.BuiltInRegistries.MENU.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:arcane_workbench"));
  registry.addRecipeHandler(menu,new EmiRecipeHandler<AbstractContainerMenu>(){
   public EmiPlayerInventory getInventory(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<AbstractContainerMenu> screen){return EmiPlayerInventory.of(net.minecraft.client.Minecraft.getInstance().player);}
   public boolean supportsRecipe(EmiRecipe recipe){return recipe instanceof EmiDisplay r&&r.display.kind()==RecipeDisplay.Kind.ARCANE;}
   public boolean canCraft(EmiRecipe recipe,EmiCraftContext<AbstractContainerMenu> context){
    if(!supportsRecipe(recipe)||context.getDestination()!=EmiCraftContext.Destination.NONE)return false;
    var adapter=thaumcraft.viewers.bridge.ViewerTransferBridge.workbench();
    return adapter.canTransferArcane(context.getScreenHandler(),((EmiDisplay)recipe).display,net.minecraft.client.Minecraft.getInstance().player,context.getAmount()>1).success();
   }
   public boolean craft(EmiRecipe recipe,EmiCraftContext<AbstractContainerMenu> context){
    if(!canCraft(recipe,context))return false;
    var adapter=thaumcraft.viewers.bridge.ViewerTransferBridge.workbench();
    return adapter.transferArcane(context.getScreenHandler(),((EmiDisplay)recipe).display,net.minecraft.client.Minecraft.getInstance().player,context.getAmount()>1);
   }
  });
  thaumcraft.viewers.bridge.TheTerminalTransferAdapter.getMenuType().ifPresent(terminalType->{
   var tMenu=(MenuType<AbstractContainerMenu>)(MenuType<?>)terminalType;
   registry.addRecipeHandler(tMenu,new EmiRecipeHandler<AbstractContainerMenu>(){
    public EmiPlayerInventory getInventory(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<AbstractContainerMenu> screen){return EmiPlayerInventory.of(net.minecraft.client.Minecraft.getInstance().player);}
    public boolean supportsRecipe(EmiRecipe recipe){
     if(recipe instanceof EmiDisplay r)return r.display.kind()==RecipeDisplay.Kind.ARCANE;
     var catId=recipe.getCategory()!=null?recipe.getCategory().getId():null;
     return catId!=null&&catId.getNamespace().equals("minecraft")&&catId.getPath().equals("crafting");
    }
    public boolean canCraft(EmiRecipe recipe,EmiCraftContext<AbstractContainerMenu> context){
     if(!supportsRecipe(recipe)||context.getDestination()!=EmiCraftContext.Destination.NONE)return false;
     var adapter=thaumcraft.viewers.bridge.ViewerTransferBridge.theTerminal();
     if(!adapter.matchesMenu(context.getScreenHandler()))return false;
     if(recipe instanceof EmiDisplay r){
      return adapter.canTransferArcane(context.getScreenHandler(),r.display,net.minecraft.client.Minecraft.getInstance().player,context.getAmount()>1).success();
     }
     return true;
    }
    public boolean craft(EmiRecipe recipe,EmiCraftContext<AbstractContainerMenu> context){
     if(!canCraft(recipe,context))return false;
     var adapter=thaumcraft.viewers.bridge.ViewerTransferBridge.theTerminal();
     var player=net.minecraft.client.Minecraft.getInstance().player;
     if(recipe instanceof EmiDisplay r){
      return adapter.transferArcane(context.getScreenHandler(),r.display,player,context.getAmount()>1);
     }
     var id=recipe.getId();
     if(id!=null&&player!=null&&player.level()!=null){
      var holder=player.level().getRecipeManager().byKey(id).orElse(null);
      if(holder!=null&&holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe){
       @SuppressWarnings("unchecked")
       var craftingHolder=(net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>)holder;
       return adapter.transferCrafting(context.getScreenHandler(),craftingHolder,player,context.getAmount()>1);
      }
     }
     return false;
    }
   });
  });registered=true;
 }
 /** EMI 1.1.x exposes no public invalidation method. Resolve the stable reload hook without linking internals. */
 public static void refresh(){if(!registered||net.minecraft.client.Minecraft.getInstance().level==null)return;try{Class.forName("dev.emi.emi.runtime.EmiReloadManager").getMethod("reload").invoke(null);}catch(ReflectiveOperationException failure){org.slf4j.LoggerFactory.getLogger("thaumcraft_viewers").error("Cannot refresh EMI 1.1.x catalog",failure);}}
}
