package fr.poubone.att2.client.hud;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

/**
 * HUD art generated in code once, then drawn as a single textured quad instead of one
 * fill per pixel every frame. Same sampling as GUI resource textures (clamp + nearest).
 */
final class HudTexture extends AbstractTexture {
    private HudTexture(String label, NativeImage image) {
        var device = RenderSystem.getDevice();
        this.texture = device.createTexture(() -> label,
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8, image.getWidth(), image.getHeight(), 1, 1);
        this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        this.textureView = device.createTextureView(this.texture);
        device.createCommandEncoder().writeToTexture(this.texture, image);
    }

    /** Paints a {@code width}×{@code height} image once and registers it under {@code id}. */
    static void register(Identifier id, int width, int height, Consumer<NativeImage> painter) {
        try (NativeImage image = new NativeImage(width, height, true)) {
            painter.accept(image);
            Minecraft.getInstance().getTextureManager().register(id, new HudTexture(id.toString(), image));
        }
    }
}
