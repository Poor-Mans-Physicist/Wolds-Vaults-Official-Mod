package xyz.iwolfking.woldsvaults.client.screens.gods;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.iwolfking.woldsvaults.WoldsVaults;

import java.io.IOException;

// Shaders the gods tab loads with the game's own. They are null until the first resource load finishes.
@Mod.EventBusSubscriber(modid = WoldsVaults.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GodTreeShaders {
    //position_tex plus a brightness/contrast/gamma/vibrance grade, for the backdrop's textured layers.
    public static ShaderInstance backdropGrade;
    //the animated cloud round the transfer slots on the God's Mastery idols, see GodIdolCloud.
    public static ShaderInstance idolCloud;

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceManager(), WoldsVaults.id("backdrop_grade"),
                DefaultVertexFormat.POSITION_TEX), shader -> backdropGrade = shader);
        event.registerShader(new ShaderInstance(event.getResourceManager(), WoldsVaults.id("idol_cloud"),
                DefaultVertexFormat.POSITION_TEX), shader -> idolCloud = shader);
    }
}
