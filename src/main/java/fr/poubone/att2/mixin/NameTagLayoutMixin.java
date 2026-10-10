package fr.poubone.att2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fr.poubone.att2.client.renderer.ItemNameTags;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dropped items' name tags are drawn from their kept layout; see {@link ItemNameTags}. */
@Mixin(NameTagFeatureRenderer.class)
public abstract class NameTagLayoutMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void att2$nextFrame(CallbackInfo ci) {
        ItemNameTags.nextFrame();
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"))
    private void att2$keptLayout(Font font, Component text, float x, float y, int color, boolean shadow, Matrix4f pose,
                                 MultiBufferSource buffers, Font.DisplayMode mode, int background, int light, Operation<Void> original) {
        Font.PreparedText prepared = ItemNameTags.prepared(font, text, x, y, color, shadow, background);
        if (prepared == null) {
            original.call(font, text, x, y, color, shadow, pose, buffers, mode, background, light);
            return;
        }
        prepared.visit(Font.GlyphVisitor.forMultiBufferSource(buffers, pose, mode, light));
    }
}
