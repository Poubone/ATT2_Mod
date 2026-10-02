package com.lootbeams.vfx;

import com.lootbeams.LootBeams;
import com.lootbeams.config.Configuration;
import com.lootbeams.features.CustomLootBeamsConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class Trail {
   private Vec3[] points;
   private Vec3[] rotations;
   private Vec3 centerPoint;
   private int color;
   private Function<Float, Float> widthFunction;
   private int length = 100;
   private boolean billboard = true;
   private Trail.TilingMode tilingMode = Trail.TilingMode.STRETCH;
   private int frequency = 1;
   private float minDistance = 0.0F;
   private Identifier texture = null;
   private boolean parentRotation = false;
   private ItemStack stack = null;
   private Configuration config;

   public Trail(Vec3[] points, int color, Function<Float, Float> widthFunction) {
      this.config = LootBeams.config;
      this.points = points;
      this.color = color;
      this.widthFunction = widthFunction;
   }

   public Trail(int color, Function<Float, Float> widthFunction) {
      this(new Vec3[]{Vec3.ZERO}, color, widthFunction);
   }

   public void setParentRotation(boolean parentRotation) {
      this.parentRotation = parentRotation;
   }

   public void setTilingMode(Trail.TilingMode tilingMode) {
      this.tilingMode = tilingMode;
   }

   public void setTexture(Identifier texture) {
      this.texture = texture;
   }

   public void setFrequency(int frequency) {
      this.frequency = frequency;
   }

   public void setMinDistance(float minDistance) {
      this.minDistance = minDistance;
   }

   public void setCenterPoint(Vec3 centerPoint) {
      this.centerPoint = centerPoint;
   }

   public void setStack(ItemStack stack) {
      this.stack = stack;
      this.setConfig(CustomLootBeamsConfig.fromItemStack(this.stack));
   }

   public void setConfig(Configuration config) {
      this.config = config;
   }

   public void setPoints(Vec3[] points) {
      if (points.length > this.length) {
         Vec3[] newPoints = new Vec3[this.length];
         System.arraycopy(points, points.length - this.length, newPoints, 0, this.length);
         points = newPoints;
      }

      this.points = points;
   }

   public void setColor(int color) {
      this.color = color;
   }

   public void setColor(float r, float g, float b, float a) {
      this.color = (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F) | (int)(a * 255.0F) << 24;
   }

   public void setLength(int length) {
      this.length = length;
   }

   public void setBillboard(boolean billboard) {
      this.billboard = billboard;
   }

   public void setWidthFunction(Function<Float, Float> widthFunction) {
      this.widthFunction = widthFunction;
   }

   public Identifier getTexture() {
      return this.texture;
   }

   public int getLength() {
      return this.length;
   }

   public void pushPoint(Vec3 point) {
      if (this.points.length == 0) {
         this.points = new Vec3[]{point};
      } else if (!(this.points[this.points.length - 1].distanceTo(point) < this.minDistance)) {
         if (!this.points[this.points.length - 1].equals(point)) {
            if (this.points[0] == Vec3.ZERO) {
               this.points[0] = point;
            } else {
               Vec3[] newPoints = new Vec3[this.points.length + 1];
               System.arraycopy(this.points, 0, newPoints, 0, this.points.length);
               newPoints[this.points.length] = point;
               if (newPoints.length > this.length) {
                  Vec3[] newPoints2 = new Vec3[this.length];
                  System.arraycopy(newPoints, 1, newPoints2, 0, this.length);
                  newPoints = newPoints2;
               }

               this.points = newPoints;
            }
         }
      }
   }

   public void pushRotatedPoint(Vec3 point, Vec3 rotation) {
      if (this.points.length == 0) {
         this.points = new Vec3[]{point};
         this.rotations = new Vec3[]{rotation};
      } else if (this.points[0] == Vec3.ZERO) {
         this.points[0] = point;
         this.rotations = new Vec3[]{rotation};
      } else if (!(this.points[this.points.length - 1].distanceTo(point) < this.minDistance)) {
         if (this.points.length <= 0 || !this.points[this.points.length - 1].equals(point)) {
            if (this.rotations == null) {
               this.rotations = new Vec3[]{rotation};
            }

            Vec3[] newPoints = new Vec3[this.points.length + 1];
            Vec3[] newRotations = new Vec3[this.points.length + 1];
            System.arraycopy(this.points, 0, newPoints, 0, this.points.length);
            System.arraycopy(this.rotations, 0, newRotations, 0, this.rotations.length);
            newPoints[this.points.length] = point;
            newRotations[this.rotations.length] = rotation;
            if (newPoints.length > this.length) {
               Vec3[] newPoints2 = new Vec3[this.length];
               Vec3[] newRotations2 = new Vec3[this.length];
               System.arraycopy(newPoints, 1, newPoints2, 0, this.length);
               System.arraycopy(newRotations, 1, newRotations2, 0, this.length);
               newPoints = newPoints2;
               newRotations = newRotations2;
            }

            this.points = newPoints;
            this.rotations = newRotations;
         }
      }
   }

   public void render(PoseStack stack, VertexConsumer consumer) {
      if (this.centerPoint == null || this.points == null || this.points.length < 2) {
         return;
      }
      stack.pushPose();
      double maxY = Arrays.stream(this.points).mapToDouble(p -> p.y).max().orElse(1.0);
      double minY = Arrays.stream(this.points).mapToDouble(p -> p.y).min().orElse(0.0);
      double pathHeight = maxY - minY;
      if (pathHeight <= 0.0) {
         pathHeight = 1.0E-4;
      }

      double scaleHeight = this.config.trailScaleHeightEqualsBeamHeight ? this.config.beamHeight : this.config.trailScaleHeight;
      double scaleY = scaleHeight / pathHeight;
      scaleY = Math.min(scaleY, scaleHeight);
      double baseY = this.centerPoint.y;
      Vector3f[][] corners = new Vector3f[this.points.length][2];

      for (int i = 0; i < this.points.length; i++) {
         if (i % this.frequency == 0) {
            double pointY = baseY + (this.points[i].y - baseY) * (this.config.trailUseScale ? scaleY : 1.0);
            Vec3 scaledPoint = new Vec3(this.points[i].x, pointY, this.points[i].z);
            float width = this.widthFunction.apply((float) i / (this.points.length - 1));
            Vector3f topOffset = new Vector3f(0.0F, width / 2.0F, 0.0F);
            Vector3f bottomOffset = new Vector3f(0.0F, -(width / 2.0F), 0.0F);
            if (this.billboard) {
               Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
               Vec3 directionToCamera = cameraPos.subtract(scaledPoint).normalize();
               Vec3 dirToNext = this.points[Math.min(i + this.frequency, this.points.length - 1)].subtract(this.points[i]).normalize();
               Vector3f dirToNextPoint = new Vector3f((float) dirToNext.x, (float) dirToNext.y, (float) dirToNext.z);
               Vector3f axis = new Vector3f((float) directionToCamera.x, (float) directionToCamera.y, (float) directionToCamera.z);
               axis.mul(-1.0F);
               axis.cross(dirToNextPoint);
               axis.normalize();
               topOffset = new Vector3f(axis);
               topOffset.mul(width / 2.0F);
               bottomOffset = new Vector3f(axis);
               bottomOffset.mul(-width / 2.0F);
            }

            topOffset.add((float) scaledPoint.x, (float) scaledPoint.y, (float) scaledPoint.z);
            bottomOffset.add((float) scaledPoint.x, (float) scaledPoint.y, (float) scaledPoint.z);
            corners[i / this.frequency][0] = topOffset;
            corners[i / this.frequency][1] = bottomOffset;
         }
      }

      this.renderPoints(stack, consumer, corners, this.color);
      stack.popPose();
   }

   private void renderPoints(PoseStack stack, VertexConsumer consumer, Vector3f[][] corners, int color) {
      stack.pushPose();
      float scaleHeight = this.config.trailScaleHeightEqualsBeamHeight ? this.config.beamHeight : this.config.trailScaleHeight;
      float yOffset = this.config.beamYOffset;
      double baseY = this.centerPoint.y();
      double maxScaleHeight = baseY + yOffset + scaleHeight;
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float a = (color >> 24 & 0xFF) / 255.0F;

      for (int i = 0; i < corners.length - 1; i++) {
         Vector3f top = corners[i][0];
         Vector3f bottom = corners[i][1];
         Vector3f nextTop = corners[i + 1][0];
         Vector3f nextBottom = corners[i + 1][1];
         if (nextTop != null && nextBottom != null && top != null && bottom != null) {
            float u = 0.0F;
            float u1 = 1.0F;
            if (this.tilingMode == Trail.TilingMode.STRETCH) {
               u = (float)i / (corners.length - 1);
               u1 = (float)(i + 1) / (corners.length - 1);
            }

            float alpha = a;
            float relativeHeightBottom = (float)(bottom.y() - baseY) / (float)(maxScaleHeight - baseY);
            float relativeHeightTop = (float)(top.y() - baseY) / (float)(maxScaleHeight - baseY);
            if (relativeHeightTop > 0.75F || relativeHeightBottom > 0.75F) {
               float fadeFactorTop = relativeHeightTop > 0.75F ? (relativeHeightTop - 0.75F) / 0.25F : 0.0F;
               float fadeFactorBottom = relativeHeightBottom > 0.75F ? (relativeHeightBottom - 0.75F) / 0.25F : 0.0F;
               float fadeFactor = Math.max(fadeFactorTop, fadeFactorBottom);
               alpha = a * (1.0F - fadeFactor);
               alpha = Math.min(Math.max(alpha, 0.0F), a);
            }

            consumer.addVertex(stack.last().pose(), bottom.x(), bottom.y() + yOffset, bottom.z())
               .setColor(r, g, b, alpha)
               .setUv(u, 0.0F)
               .setOverlay(OverlayTexture.NO_OVERLAY)
               .setLight(15728880)
               .setNormal(0.0F, 1.0F, 0.0F);
            consumer.addVertex(stack.last().pose(), top.x(), top.y() + yOffset, top.z())
               .setColor(r, g, b, alpha)
               .setUv(u, 1.0F)
               .setOverlay(OverlayTexture.NO_OVERLAY)
               .setLight(15728880)
               .setNormal(0.0F, 1.0F, 0.0F);
            consumer.addVertex(stack.last().pose(), nextTop.x(), nextTop.y() + yOffset, nextTop.z())
               .setColor(r, g, b, alpha)
               .setUv(u1, 1.0F)
               .setOverlay(OverlayTexture.NO_OVERLAY)
               .setLight(15728880)
               .setNormal(0.0F, 1.0F, 0.0F);
            consumer.addVertex(stack.last().pose(), nextBottom.x(), nextBottom.y() + yOffset, nextBottom.z())
               .setColor(r, g, b, alpha)
               .setUv(u1, 0.0F)
               .setOverlay(OverlayTexture.NO_OVERLAY)
               .setLight(15728880)
               .setNormal(0.0F, 1.0F, 0.0F);
         }
      }

      stack.popPose();
   }

   public static enum TilingMode {
      NONE,
      STRETCH,
      REPEAT;

      private TilingMode() {
      }
   }
}
