package fr.poubone.att2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fr.poubone.att2.client.renderer.ItemNameTags;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Dropped items' name tags are measured once; see {@link ItemNameTags}. */
@Mixin(NameTagFeatureRenderer.Storage.class)
public abstract class NameTagWidthMixin {
    @WrapOperation(method = "add", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/network/chat/FormattedText;)I"))
    private int att2$keptWidth(Font font, FormattedText text, Operation<Integer> original) {
        int width = text instanceof Component tag ? ItemNameTags.width(font, tag) : -1;
        return width >= 0 ? width : original.call(font, text);
    }
}
