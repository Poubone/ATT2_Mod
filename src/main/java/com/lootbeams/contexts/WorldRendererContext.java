package com.lootbeams.contexts;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import org.joml.Matrix4f;

public class WorldRendererContext {
   private LevelRenderer worldRenderer;
   private DeltaTracker tickCounter;
   private PoseStack matrixStack;
   private boolean blockOutlines;
   private Camera camera;
   private GameRenderer gameRenderer;
   private LightTexture lightmapTextureManager;
   private Matrix4f projectionMatrix;
   private Matrix4f positionMatrix;
   private BufferSource consumers;
   private boolean advancedTranslucency;
   private ClientLevel world;

   public WorldRendererContext() {
   }

   public WorldRendererContext prepare(
      LevelRenderer worldRenderer,
      DeltaTracker delta,
      boolean blockOutlines,
      Camera camera,
      GameRenderer gameRenderer,
      Matrix4f projectionMatrix,
      Matrix4f positionMatrix,
      BufferSource consumers,
      boolean advancedTranslucency,
      ClientLevel world
   ) {
      this.worldRenderer = worldRenderer;
      this.tickCounter = delta;
      this.matrixStack = null;
      this.blockOutlines = blockOutlines;
      this.camera = camera;
      this.gameRenderer = gameRenderer;
      this.projectionMatrix = projectionMatrix;
      this.positionMatrix = positionMatrix;
      this.consumers = consumers;
      this.advancedTranslucency = advancedTranslucency;
      this.world = world;
      return this;
   }

   public PoseStack getMatrixStack() {
      return this.matrixStack;
   }

   public WorldRendererContext setMatrixStack(PoseStack matrixStack) {
      this.matrixStack = matrixStack;
      return this;
   }

   public Camera getCamera() {
      return this.camera;
   }

   public WorldRendererContext setCamera(Camera camera) {
      this.camera = camera;
      return this;
   }

   public LightTexture getLightmapTextureManager() {
      return this.lightmapTextureManager;
   }

   public WorldRendererContext setLightmapTextureManager(LightTexture lightmapTextureManager) {
      this.lightmapTextureManager = lightmapTextureManager;
      return this;
   }

   public LevelRenderer getWorldRenderer() {
      return this.worldRenderer;
   }

   public WorldRendererContext setWorldRenderer(LevelRenderer worldRenderer) {
      this.worldRenderer = worldRenderer;
      return this;
   }

   public DeltaTracker getTickCounter() {
      return this.tickCounter;
   }

   public WorldRendererContext setTickCounter(DeltaTracker tickCounter) {
      this.tickCounter = tickCounter;
      return this;
   }

   public boolean isBlockOutlines() {
      return this.blockOutlines;
   }

   public WorldRendererContext setBlockOutlines(boolean blockOutlines) {
      this.blockOutlines = blockOutlines;
      return this;
   }

   public GameRenderer getGameRenderer() {
      return this.gameRenderer;
   }

   public WorldRendererContext setGameRenderer(GameRenderer gameRenderer) {
      this.gameRenderer = gameRenderer;
      return this;
   }

   public Matrix4f getProjectionMatrix() {
      return this.projectionMatrix;
   }

   public WorldRendererContext setProjectionMatrix(Matrix4f projectionMatrix) {
      this.projectionMatrix = projectionMatrix;
      return this;
   }

   public Matrix4f getPositionMatrix() {
      return this.positionMatrix;
   }

   public WorldRendererContext setPositionMatrix(Matrix4f positionMatrix) {
      this.positionMatrix = positionMatrix;
      return this;
   }

   public BufferSource getConsumers() {
      return this.consumers;
   }

   public WorldRendererContext setConsumers(BufferSource consumers) {
      this.consumers = consumers;
      return this;
   }

   public boolean isAdvancedTranslucency() {
      return this.advancedTranslucency;
   }

   public WorldRendererContext setAdvancedTranslucency(boolean advancedTranslucency) {
      this.advancedTranslucency = advancedTranslucency;
      return this;
   }

   public ClientLevel getWorld() {
      return this.world;
   }

   public WorldRendererContext setWorld(ClientLevel world) {
      this.world = world;
      return this;
   }
}
