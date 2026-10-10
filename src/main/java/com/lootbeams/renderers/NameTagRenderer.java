package com.lootbeams.renderers;

import com.lootbeams.LootBeams;
import com.lootbeams.config.Configuration;
import com.lootbeams.helpers.RarityHelper;
import com.lootbeams.helpers.TargetHelper;
import com.lootbeams.helpers.TextColorHelper;
import com.lootbeams.managers.TooltipManager;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

public class NameTagRenderer {
   public NameTagRenderer() {
   }

   public static void renderNameTags(
      MultiBufferSource buffer,
      PoseStack matrixStack,
      ItemEntity itemEntity,
      Configuration itemConfig,
      TextColor color,
      float fadeAlpha,
      float currentGroundTime,
      long worldtime,
      float pticks
   ) {
      if (!showsFor(itemEntity, itemConfig)) {
         return;
      }
      ItemStack itemStack = itemEntity.getItem();
      float foregroundAlpha = itemConfig.nametagTextAlpha;
      float backgroundAlpha = itemConfig.nametagBackgroundAlpha;
      double yOffset = itemConfig.nametagYOffset;
      int foregroundColor = color.getValue() & 16777215 | (int)(255.0F * foregroundAlpha) << 24;
      int backgroundColor = color.getValue() & 16777215 | (int)(255.0F * backgroundAlpha) << 24;
      matrixStack.pushPose();
      matrixStack.translate(0.0, Math.min(1.0, Minecraft.getInstance().player.distanceToSqr(itemEntity) * 0.025) + yOffset, 0.0);
      Quaternionf entityRotationQuaternion = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
      matrixStack.mulPose(entityRotationQuaternion);
      matrixStack.mulPose(new Quaternionf(0.0, Math.toRadians(90.0), 0.0, 0.0));
      float nametagScale = itemConfig.nametagScale;
      float nametagScaleCompensation = 0.25F;
      matrixStack.scale(
         -0.02F * nametagScale * nametagScaleCompensation,
         -0.02F * nametagScale * nametagScaleCompensation,
         0.02F * nametagScale * nametagScaleCompensation
      );
      Font fontrenderer = Minecraft.getInstance().font;
      String itemName = StringUtil.stripColor(itemStack.getHoverName().getString());
      if (itemConfig.renderStackcount && fr.poubone.att2.client.hud.HUDConfig.get().renderStackcount) {
         int count = itemStack.getCount();
         if (count > 1) {
            itemName = itemName + " x" + count;
         }
      }

      matrixStack.translate(0.0F, 0.0F, -10.0F);
      renderText(itemStack, itemConfig, fontrenderer, matrixStack, buffer, itemName, foregroundColor, backgroundColor, backgroundAlpha);
      boolean alwaysRenderRarityOnItem = RarityHelper.alwaysHasRarity(itemStack);
      if (itemConfig.renderItemRarity || alwaysRenderRarityOnItem) {
         renderRarity(itemStack, itemConfig, foregroundAlpha, backgroundAlpha, fontrenderer, matrixStack, buffer, alwaysRenderRarityOnItem);
      }

      matrixStack.popPose();
   }

   /** Whether {@link #renderNameTags} draws this item's tag now: shown while crouching, or on look. */
   public static boolean showsFor(ItemEntity itemEntity, Configuration itemConfig) {
      return itemConfig.renderNametags && fr.poubone.att2.client.hud.HUDConfig.get().renderNametags && !itemConfig.advancedTooltips
         && (Minecraft.getInstance().player.isCrouching()
            || itemConfig.renderNametagsOnlook && TargetHelper.isLookingAt(Minecraft.getInstance().player, itemEntity, itemConfig.nametagLookSensitivity));
   }

   private static void renderText(
      ItemStack itemStack,
      Configuration itemConfig,
      Font fontRenderer,
      PoseStack stack,
      MultiBufferSource buffer,
      String text,
      int foregroundColor,
      int backgroundColor,
      float backgroundAlpha
   ) {
      if (itemConfig.borders) {
         float w = -fontRenderer.width(text) / 2.0F;
         int bg = new Color(0, 0, 0, (int)(255.0F * backgroundAlpha)).getRGB();
         Component orderedText = Component.nullToEmpty(text);
         fontRenderer.drawInBatch8xOutline(orderedText.getVisualOrderText(), w, 0.0F, foregroundColor, bg, stack.last().pose(), buffer, 15728880);
      } else {
         fontRenderer.drawInBatch(
            text,
            (float)(-fontRenderer.width(text) / 2.0),
            0.0F,
            foregroundColor,
            false,
            stack.last().pose(),
            buffer,
            DisplayMode.NORMAL,
            backgroundColor,
            15728864
         );
      }
   }

   private static void renderRarity(
      ItemStack itemStack,
      Configuration itemConfig,
      float foregroundAlpha,
      float backgroundAlpha,
      Font fontRenderer,
      PoseStack stack,
      MultiBufferSource buffer,
      boolean alwaysRenderRarityOnItem
   ) {
      stack.translate(0.0, 10.0, 0.0);
      stack.scale(0.75F, 0.75F, 0.75F);
      List<Component> tooltip = TooltipManager.getTooltipFromCache(itemStack);
      if (!tooltip.isEmpty()) {
         if (tooltip.size() > 1) {
            Component tooltipRarity = tooltip.get(1);
            String rarityString = tooltipRarity.getString();
            if (itemConfig.customRarities.contains(rarityString) || alwaysRenderRarityOnItem) {
               TextColor rarityColor = LootBeams.config.whiteRarities
                  ? TextColor.fromLegacyFormat(ChatFormatting.WHITE)
                  : TextColorHelper.getRawColor(tooltipRarity);
               int foregroundColor = rarityColor.getValue() & 16777215 | (int)(255.0F * foregroundAlpha) << 24;
               int backgroundColor = rarityColor.getValue() & 16777215 | (int)(255.0F * backgroundAlpha) << 24;
               renderText(itemStack, itemConfig, fontRenderer, stack, buffer, rarityString, foregroundColor, backgroundColor, backgroundAlpha);
               return;
            }
         }

         if (itemConfig.vanillaRarities || alwaysRenderRarityOnItem) {
            String rarity = RarityHelper.getRarity(itemStack);
            TextColor rarityColor = LootBeams.config.whiteRarities ? TextColor.fromLegacyFormat(ChatFormatting.WHITE) : TextColorHelper.getItemColor(itemStack);
            float R = (rarityColor.getValue() >> 16 & 0xFF) / 255.0F;
            float G = (rarityColor.getValue() >> 8 & 0xFF) / 255.0F;
            float B = (rarityColor.getValue() & 0xFF) / 255.0F;
            int foregroundColor = new Color(R, G, B, (float)((int)foregroundAlpha)).getRGB();
            int backgroundColor = new Color(R, G, B, (float)((int)backgroundAlpha)).getRGB();
            renderText(itemStack, itemConfig, fontRenderer, stack, buffer, rarity, foregroundColor, backgroundColor, backgroundAlpha);
         }
      }
   }
}
