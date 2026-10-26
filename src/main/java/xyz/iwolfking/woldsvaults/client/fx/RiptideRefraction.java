package xyz.iwolfking.woldsvaults.client.fx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import xyz.iwolfking.woldsvaults.WoldsVaults;

import java.lang.reflect.Method;

/**
 * Heat-haze pass for the shock cone: copies the scene drawn so far, then draws the cone shell with a shader that
 * samples that copy at a rippling offset, so the air inside the shock front appears to warp. Skipped, with a
 * logged reason, when the shader failed to load or an Iris/Oculus shader pack owns the pipeline.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = WoldsVaults.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RiptideRefraction {
    private static ShaderInstance shader;
    private static TextureTarget sceneCopy;
    private static boolean irisResolved;
    private static Object irisApi;
    private static Method shaderPackInUse;
    private static boolean irisQueryFailed;
    private static String lastSkipReason;

    private RiptideRefraction() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceManager(), WoldsVaults.id("riptide_refraction"), DefaultVertexFormat.POSITION_COLOR_TEX),
                    loaded -> shader = loaded);
        } catch (Exception e) {
            shader = null;
            WoldsVaults.LOGGER.error("Riptide refraction shader failed to load; shock cones fall back to the additive shell.", e);
        }
    }

    /**
     * Copies the scene and binds the refraction shader. Returns false, leaving GL state untouched, when the pass
     * cannot run; the caller then draws the additive shell instead.
     */
    static boolean begin() {
        String skip = skipReason();
        if (skip != null) {
            if (!skip.equals(lastSkipReason)) {
                WoldsVaults.LOGGER.warn("Riptide shock cone refraction skipped ({}); drawing the additive shell instead.", skip);
                lastSkipReason = skip;
            }
            return false;
        }
        lastSkipReason = null;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        if (sceneCopy == null) {
            sceneCopy = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
            sceneCopy.setFilterMode(GL11.GL_LINEAR);
        } else if (sceneCopy.width != main.width || sceneCopy.height != main.height) {
            sceneCopy.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        int bound = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sceneCopy.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, sceneCopy.width, sceneCopy.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, bound);

        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, sceneCopy.getColorTextureId());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        Tesselator.getInstance().getBuilder().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        return true;
    }

    static BufferBuilder builder() {
        return Tesselator.getInstance().getBuilder();
    }

    static void end() {
        Tesselator.getInstance().end();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static String skipReason() {
        if (shader == null) {
            return "refraction shader not loaded";
        }
        if (shaderPackActive()) {
            return "an Iris/Oculus shader pack is active";
        }
        return null;
    }

    private static boolean shaderPackActive() {
        if (!irisResolved) {
            irisResolved = true;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = api.getMethod("getInstance").invoke(null);
                shaderPackInUse = api.getMethod("isShaderPackInUse");
            } catch (ClassNotFoundException e) {
                irisApi = null;
            } catch (ReflectiveOperationException | LinkageError e) {
                irisApi = null;
                WoldsVaults.LOGGER.error("Riptide refraction could not query the Iris API; assuming no shader pack is active.", e);
            }
        }
        if (irisQueryFailed) {
            return true;
        }
        if (irisApi == null) {
            return false;
        }
        try {
            return (Boolean) shaderPackInUse.invoke(irisApi);
        } catch (ReflectiveOperationException e) {
            WoldsVaults.LOGGER.error("Riptide refraction Iris shader pack query failed; treating a pack as active from now on.", e);
            irisQueryFailed = true;
            return true;
        }
    }
}
