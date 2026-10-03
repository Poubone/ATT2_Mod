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

    private GlobeShader() {
    }

    /** Registers the pipeline during client init, so it's compiled with the other shaders on resource load. */
    public static void init() {
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
