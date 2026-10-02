package com.lootbeams.helpers;

import com.lootbeams.LootBeams;
import com.mojang.blaze3d.platform.Window;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ViewHelper {
   public ViewHelper() {
   }

   public static boolean shouldRenderOnItem(ItemStack itemStack) {
      boolean shouldRender = false;
      if (LootBeams.config.allItems) {
         shouldRender = true;
      } else {
         if (LootBeams.config.onlyEquipment) {
            shouldRender = ItemHelper.isEquipmentItem(itemStack);
         }

         if (LootBeams.config.onlyRare) {
            shouldRender = RarityHelper.rarityCheck(itemStack, shouldRender);
         }

         if (ItemHelper.isItemInRegistryList(LootBeams.config.whitelist, itemStack.getItem())) {
            shouldRender = true;
         }
      }

      if (ItemHelper.isItemInRegistryList(LootBeams.config.blacklist, itemStack.getItem())) {
         shouldRender = false;
      }

      return shouldRender && fr.poubone.att2.client.hud.HUDConfig.shouldRenderLootBeam(itemStack);
   }

   public static boolean shouldRenderCrosshair(Minecraft client) {
      return LootBeams.config.renderTooltipsInThirdPersonView
            ? (
                  client.options.getCameraType() == CameraType.THIRD_PERSON_FRONT
                        || client.options.getCameraType() == CameraType.THIRD_PERSON_BACK
                        || client.options.getCameraType().isFirstPerson()
            )
                  && !client.options.hideGui
            : client.options.getCameraType().isFirstPerson() && !client.options.hideGui;
   }

   public static void cancelBobView(Vector3f position, float partialTicks) {
      Minecraft mc = Minecraft.getInstance();
      if (Boolean.TRUE.equals(mc.options.bobView().get()) && mc.getCameraEntity() instanceof AbstractClientPlayer player) {
         ClientAvatarState avatar = player.avatarState();
         float stepSize = -avatar.getBackwardsInterpolatedWalkDistance(partialTicks);
         float viewBob = avatar.getInterpolatedBob(partialTicks);
         float pi = (float) Math.PI;
         Quaternionf bobXRotation = Axis.XP.rotationDegrees(Math.abs(Mth.cos(stepSize * pi - 0.2F) * viewBob) * 5.0F);
         Quaternionf bobZRotation = Axis.ZP.rotationDegrees(Mth.sin(stepSize * pi) * viewBob * 3.0F);
         bobXRotation.conjugate();
         bobZRotation.conjugate();
         bobXRotation.transform(position);
         bobZRotation.transform(position);
         position.add(-Mth.sin(stepSize * pi) * viewBob * 0.5F, Math.abs(Mth.cos(stepSize * pi) * viewBob), 0.0F);
      }
   }

   public static Vector3f worldToScreenSpace(Vec3 pos, float partialTicks) {
      Minecraft mc = Minecraft.getInstance();
      Camera camera = mc.gameRenderer.getMainCamera();
      Vec3 cameraPosition = camera.position();
      Vector3f position = new Vector3f(
            (float) (cameraPosition.x - pos.x),
            (float) (cameraPosition.y - pos.y),
            (float) (cameraPosition.z - pos.z));
      Quaternionf cameraRotation = new Quaternionf(camera.rotation());
      cameraRotation.conjugate();
      cameraRotation.transform(position);
      cancelBobView(position, partialTicks);
      Window window = mc.getWindow();
      float fov = mc.options.fov().get().floatValue();
      float screenSize = window.getGuiScaledHeight() / 2.0F / position.z() / (float) Math.tan(Math.toRadians(fov / 2.0F));
      position.mul(-screenSize, screenSize, 1.0F);
      position.add(window.getGuiScaledWidth() / 2.0F, window.getGuiScaledHeight() / 2.0F, 0.0F);
      return position;
   }
}
