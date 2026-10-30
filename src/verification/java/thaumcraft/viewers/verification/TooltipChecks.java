package thaumcraft.viewers.verification;

import java.lang.reflect.Proxy;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.*;
import thaumic.tinkerer.block.AnimationTabletContent;
import thaumic.tinkerer.integration.DeviceTooltip;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.viewers.tooltip.CoreTooltip;

@GameTestHolder("thaumcraft_viewers")
@PrefixGameTestTemplate(false)
public final class TooltipChecks {
    static void check(boolean value, String message) {
        if (!value) throw new GameTestAssertException(message);
    }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type, Function<String,Object> values) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(object,method,args)->values.apply(method.getName()));
    }
    @GameTest(template="empty", templateNamespace="thaumcraft_viewers", timeoutTicks=100)
    public static void discoversOptionalTinkererProviders(GameTestHelper helper) {
        if (!ModList.get().isLoaded("thaumictinkerer_aop")) { helper.succeed(); return; }
        var pos = helper.absolutePos(new BlockPos(1,1,1));
        helper.getLevel().setBlockAndUpdate(pos,AnimationTabletContent.BLOCK.get().defaultBlockState());
        var tile=helper.getLevel().getBlockEntity(pos);
        var data=new CompoundTag(); data.putInt("other_mod",31); DeviceTooltip.write(data,tile);
        if (ModList.get().isLoaded("jade")) JadeTooltipChecks.verify(tile,data);
        if (ModList.get().isLoaded("wthit")) WthitTooltipChecks.verify(tile,data);
        helper.succeed();
    }
    @GameTest(template="empty", templateNamespace="thaumcraft_viewers", timeoutTicks=100)
    public static void coreContentsAndClientFallback(GameTestHelper helper) {
        var pos=helper.absolutePos(new BlockPos(1,1,1));
        var jar=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:warded_jar"));
        helper.getLevel().setBlockAndUpdate(pos,jar.defaultBlockState());
        var tile=helper.getLevel().getBlockEntity(pos);
        check(tile instanceof IAspectContainer,"core jar implements public container contract");
        var container=(IAspectContainer)tile;
        container.setAspects(new AspectList().add(Aspect.getAspect("aer"),17));
        var data=new CompoundTag(); data.putInt("other_mod",31);
        CoreTooltip.write(data,tile,Direction.UP);
        check(data.getInt("other_mod")==31,"core provider preserves other providers' data");
        check(data.getCompound(CoreTooltip.KEY).getInt("aer")==17,"server snapshot contains exact jar contents");
        check(CoreTooltip.readOrLocal(new CompoundTag(),tile,Direction.UP).size()==1,"client-only fallback uses synchronized contents");
        container.setAspects(new AspectList());
        var empty=new CompoundTag(); CoreTooltip.write(empty,tile,Direction.UP);
        container.setAspects(new AspectList().add(Aspect.getAspect("aer"),17));
        check(CoreTooltip.readOrLocal(empty,tile,Direction.UP).isEmpty(),"empty authoritative snapshot suppresses stale local contents");
        check(CoreTooltip.readOrLocal(new CompoundTag(),null,Direction.UP).isEmpty(),"unavailable client data produces no invented contents");
        helper.succeed();
    }
}
