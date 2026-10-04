package fr.poubone.att2.client.hud;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.slf4j.LoggerFactory;

import java.util.OptionalInt;

/** Renders a {@link ManaGlobe} liquid texture on the GPU ({@code att2:shaders/core/globe.fsh}). */
public final class GlobeShader {
    /** std140 layout of the {@code GlobeParams} block: two vec4. */
    static final int PARAMS_SIZE = new Std140SizeCalculator().putVec4().putVec4().get();

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder()
                    .withLocation(Identifier.fromNamespaceAndPath("att2", "pipeline/globe"))
                    .withVertexShader("core/screenquad")
                    .withFragmentShader(Identifier.fromNamespaceAndPath("att2", "core/globe"))
                    .withUniform("GlobeParams", UniformType.UNIFORM_BUFFER)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                    .build()
    );
    private static final OptionalInt CLEAR_TRANSPARENT = OptionalInt.of(0);
    private static boolean failureLogged;

    private GlobeShader() {
    }

    /** Registers the pipeline during client init, so it's compiled with the other shaders on resource load. */
    public static void init() {
    }

    /** False when the shader failed to compile (e.g. a driver issue); the orbs then use the CPU path. */
    static boolean isAvailable() {
        // The device caches compiled pipelines and clears that cache on resource reload.
        // Query the current pipeline so a corrected or newly broken shader changes the fallback too.
        boolean available = RenderSystem.getDevice().precompilePipeline(PIPELINE).isValid();
        if (!available && !failureLogged) {
            LoggerFactory.getLogger("att2").warn("Orb shader failed to compile, falling back to CPU orb rendering");
        }
        failureLogged = !available;
        return available;
    }

    static void render(GpuTextureView target, GpuBuffer params) {
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "att2 globe", target, CLEAR_TRANSPARENT)) {
            pass.setPipeline(PIPELINE);
            pass.setUniform("GlobeParams", params);
            pass.draw(0, 3);
        }
    }
}
