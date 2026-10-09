package fr.poubone.att2.mixin;

import fr.poubone.att2.client.renderer.ItemNameTags;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Kept name tag layouts point at glyphs, which a font reload or a font option change replaces. */
@Mixin(FontManager.class)
public abstract class FontReloadMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void att2$fontsReloaded(CallbackInfo ci) {
        ItemNameTags.fontsChanged();
    }

    @Inject(method = "updateOptions", at = @At("HEAD"))
    private void att2$fontOptionsChanged(CallbackInfo ci) {
        ItemNameTags.fontsChanged();
    }
}
