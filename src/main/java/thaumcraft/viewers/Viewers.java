package thaumcraft.viewers;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
@Mod(value="thaumcraft_compact",dist=net.neoforged.api.distmarker.Dist.CLIENT)
public final class Viewers {
 public enum Recipes {ALL_WITH_REQUIREMENTS,RESEARCHED_ONLY}
 public enum Aspects {SCANNED_ONLY,ALL}
 public static final ModConfigSpec.EnumValue<Recipes> RECIPES;
 public static final ModConfigSpec.EnumValue<Aspects> ASPECTS;
 static final ModConfigSpec SPEC;
 static {var b=new ModConfigSpec.Builder();RECIPES=b.comment("Show all with requirements, or only known research.").defineEnum("recipes",Recipes.ALL_WITH_REQUIREMENTS);ASPECTS=b.comment("Reveal item aspects after scanning, or immediately.").defineEnum("aspects",Aspects.SCANNED_ONLY);SPEC=b.build();}
 public Viewers(ModContainer container){container.registerConfig(ModConfig.Type.CLIENT,SPEC,"thaumcraft_viewers-client.toml");aop.ui.api.SettingsRegistry.register("thaumcraft_compact", "thaumcraft_viewers","AOP:TC4 Compact",aop.ui.api.UiTheme.COMPACT);}
 public static boolean emi(){return ViewerSelection.select(net.neoforged.fml.ModList.get().isLoaded("jei"),net.neoforged.fml.ModList.get().isLoaded("emi"))==ViewerSelection.EMI;}
}
