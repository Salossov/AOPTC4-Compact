package thaumcraft.viewers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid="thaumcraft_compact",value=Dist.CLIENT)
public final class Updates {
 private static boolean catalog,dirty;private static int wait;private static int signature;
 @SubscribeEvent public static void catalog(thaumcraft.api.addon.CatalogChangedEvent e){if(!e.client())return;AspectRecipes.invalidate();catalog=true;dirty=true;wait=10;}
 @SubscribeEvent public static void knowledge(thaumcraft.api.addon.KnowledgeChangedEvent e){if(!e.client())return;var k=e.knowledge();int value=java.util.Objects.hash(k.completedResearch(),k.aspectPools().keySet(),k.scanned(thaumcraft.api.knowledge.PlayerKnowledge.ScanKind.OBJECT));if(value!=signature){signature=value;AspectRecipes.invalidate();dirty=true;wait=10;}}
 @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){if(!dirty||wait-->0)return;dirty=false;if(Viewers.emi())thaumcraft.viewers.emi.EmiPlugin.refresh();else if(net.neoforged.fml.ModList.get().isLoaded("jei"))thaumcraft.viewers.jei.JeiPlugin.refresh(catalog);catalog=false;}
 @EventBusSubscriber(modid="thaumcraft_compact",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
 public static final class Config {
  @SubscribeEvent public static void changed(net.neoforged.fml.event.config.ModConfigEvent.Reloading event){if(event.getConfig().getModId().equals("thaumcraft_compact")){catalog=true;dirty=true;wait=10;}}
 }
}
