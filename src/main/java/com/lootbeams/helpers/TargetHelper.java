package com.lootbeams.helpers;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class TargetHelper {
   public TargetHelper() {
   }

   public static boolean isLookingAt(Player player, Entity target, double accuracy) {
      Vec3 difference = new Vec3(target.getX() - player.getX(), target.getEyeY() - player.getEyeY(), target.getZ() - player.getZ());
      double length = difference.length();
      double dot = Minecraft.getInstance().getCameraEntity().getLookAngle().normalize().dot(difference.normalize());
      return dot > 1.0 - accuracy / length && player.hasLineOfSight(target);
   }

   public static HitResult getEntityItem(Player player) {
      Minecraft mc = Minecraft.getInstance();
      double distance = player.blockInteractionRange();
      float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
      Vec3 position = player.getEyePosition(partialTicks);
      Vec3 view = player.getViewVector(partialTicks);
      if (mc.hitResult != null && mc.hitResult.getType() != Type.MISS) {
         distance = mc.hitResult.getLocation().distanceTo(position);
      }

      return getEntityItem(player, position, position.add(view.x * distance, view.y * distance, view.z * distance));
   }

   public static HitResult getEntityItem(Player player, Vec3 position, Vec3 look) {
      Vec3 include = look.subtract(position);
      List list = player.level().getEntities(player, player.getBoundingBox().inflate(include.x, include.y, include.z));
      double closestDistance = player.blockInteractionRange();
      ItemEntity closestItem = null;

      for (int i = 0; i < list.size(); i++) {
         Entity entity = (Entity)list.get(i);
         if (entity instanceof ItemEntity itemEntity) {
            AABB itemBox = entity.getBoundingBox().inflate(0.0, 0.3, 0.0);
            Optional<Vec3> intersection = itemBox.clip(position, look);
            if (intersection.isPresent()) {
               double distance = position.distanceTo(intersection.get());
               if (distance < closestDistance) {
                  closestDistance = distance;
                  closestItem = itemEntity;
               }
            }
         }
      }

      return closestItem != null ? new EntityHitResult(closestItem) : null;
   }
}
