package com.lootbeams;

import com.lootbeams.dconfig.DynamicConfig;
import com.lootbeams.dconfig.events.ConfigEvents;
import com.lootbeams.events.PresetEvents;
import com.lootbeams.events.RenderEvents;
import com.lootbeams.events.ResourceEvents;
import com.lootbeams.features.BeamOpacityOnApproach;
import com.lootbeams.features.BeamSizeOnApproach;
import com.lootbeams.features.CustomLootBeamsConfig;
import com.lootbeams.features.CustomRarity;
import com.lootbeams.listeners.ResourceReloadListener;
import com.lootbeams.managers.GlowEffectManager;
import com.lootbeams.managers.ParticleManager;
import com.lootbeams.managers.PresetManager;
import com.lootbeams.managers.RenderManager;
import com.lootbeams.managers.TooltipManager;
import com.lootbeams.renderers.HudRenderer;
import com.lootbeams.renderers.LootBeamRenderer;
import com.lootbeams.screens.LootBeamsPresetManagerScreen;
import com.lootbeams.shaders.LootBeamShaders;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.player.Player;

public class ClientSetup {
   public static KeyMapping keyBinding;
   private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("lootbeams", "lootbeams"));

   public static void registerKeyBindings() {
      keyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "lootbeams.keybindings.savePreset",
            InputConstants.Type.KEYSYM,
            325,
            KEY_CATEGORY));
   }

   public static void registerCoreShaderRegistrationEvents() {
      LootBeamShaders.registerCoreShaders();
      LootBeamShaders.registerDroplightCoreShaders();
   }

   public static void registerHudRenderEvents() {
      RenderEvents.HUD.register((context, tickCounter) -> HudRenderer.onHudRender(context, tickCounter));
   }

   public static void registerWorldRenderEvents() {
      RenderEvents.BEFORE_PARTICLES.register(RenderManager::onWorldRenderBeforeParticles);
      RenderEvents.AFTER_TRANSLUCENT.register(RenderManager::onWorldRenderAfterTranslucent);
      RenderEvents.AFTER_WEATHER.register(RenderManager::onWorldRenderAfterWeather);
      RenderEvents.BEFORE_END.register(RenderManager::onWorldRenderBeforeEnd);
      RenderEvents.END.register(RenderManager::onWorldRenderEnd);
   }

   public static void registerClientEvents() {
      ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(LootBeams.id("reload_resources"), new ResourceReloadListener());
      ClientTickEvents.END_CLIENT_TICK.register(client -> {
         while (keyBinding.consumeClick()) {
            Minecraft.getInstance().setScreen(new LootBeamsPresetManagerScreen(Minecraft.getInstance().screen));
         }
      });
   }

   public static void registerCommands() {
      ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(
            (LiteralArgumentBuilder) ((LiteralArgumentBuilder) ClientCommandManager.literal("lootbeams")
                  .then(ClientCommandManager.literal("save-preset")
                        .then(ClientCommandManager.argument("presetName", StringArgumentType.word()).executes(context -> {
                           String presetName = StringArgumentType.getString(context, "presetName");
                           PresetManager.savePreset(presetName);
                           return 1;
                        }))))
                  .then(ClientCommandManager.literal("item-custom-config")
                        .then(ClientCommandManager.literal("available").executes(context -> {
                           for (DynamicConfig.Control.Field field : LootBeams.configManager.getFieldsByGroup("visual")) {
                              Player player = Minecraft.getInstance().player;
                              if (player == null) {
                                 continue;
                              }
                              MutableComponent fieldText = Component.literal("")
                                    .append(Component.literal(field.saveKey).withStyle(style -> style.withColor(ChatFormatting.BLUE)))
                                    .append(Component.literal(" "));
                              MutableComponent fieldDescriptionText = Component.literal("[ ")
                                    .withStyle(style -> style.withColor(ChatFormatting.WHITE))
                                    .append(Component.literal("type=").withStyle(style -> style.withColor(ChatFormatting.GRAY)));
                              if (field.type == List.class) {
                                 fieldDescriptionText = fieldDescriptionText.append(
                                       Component.literal("List([\"val1\", \"val2\", \"...\"])").withStyle(style -> style.withColor(ChatFormatting.WHITE)));
                              } else if (field.type == ParticleManager.ParticleTexture.class) {
                                 fieldDescriptionText = fieldDescriptionText.append(
                                       Component.literal("String(lootbeams:particle_id)").withStyle(style -> style.withColor(ChatFormatting.WHITE)));
                              } else if (field.type == GlowEffectManager.GlowEffectTexture.class) {
                                 fieldDescriptionText = fieldDescriptionText.append(
                                       Component.literal("String(lootbeams:glow_effect_id)").withStyle(style -> style.withColor(ChatFormatting.WHITE)));
                              } else if (field.type == LootBeamShaders.Shader.class) {
                                 fieldDescriptionText = fieldDescriptionText.append(Component.literal("String("
                                       + String.join(", ", Arrays.stream(LootBeamShaders.Shader.values()).map(shader -> shader.name().toLowerCase()).toList())
                                       + ")").withStyle(style -> style.withColor(ChatFormatting.WHITE)));
                              } else if (field.type == LootBeamShaders.CustomShader.class) {
                                 fieldDescriptionText = fieldDescriptionText.append(Component.literal("String("
                                       + String.join(", ", LootBeamShaders.CustomShader.values().stream().map(shader -> shader.name().toLowerCase()).toList())
                                       + ")").withStyle(style -> style.withColor(ChatFormatting.WHITE)));
                              } else if (field.type == BeamOpacityOnApproach.class) {
                                 fieldDescriptionText = fieldDescriptionText.append(Component.literal("String("
                                       + String.join(", ", Arrays.stream(BeamOpacityOnApproach.values()).map(option -> option.name().toLowerCase()).toList())
                                       + ")").withStyle(style -> style.withColor(ChatFormatting.WHITE)));
                              } else if (field.type == BeamSizeOnApproach.class) {
                                 fieldDescriptionText = fieldDescriptionText.append(Component.literal("String("
                                       + String.join(", ", Arrays.stream(BeamSizeOnApproach.values()).map(option -> option.name().toLowerCase()).toList())
                                       + ")").withStyle(style -> style.withColor(ChatFormatting.WHITE)));
                              } else {
                                 fieldDescriptionText = fieldDescriptionText.append(
                                       Component.literal(field.type.toString()).withStyle(style -> style.withColor(ChatFormatting.WHITE)));
                              }
                              if (field.type == int.class || field.type == float.class || field.type == double.class || field.type == long.class) {
                                 DecimalFormat format = new DecimalFormat("0.#####");
                                 fieldDescriptionText = fieldDescriptionText
                                       .append(Component.literal(", min=").withStyle(style -> style.withColor(ChatFormatting.GRAY)))
                                       .append(Component.literal(format.format(field.minValue)).withStyle(style -> style.withColor(ChatFormatting.RED)))
                                       .append(Component.literal(", max=").withStyle(style -> style.withColor(ChatFormatting.GRAY)))
                                       .append(Component.literal(format.format(field.maxValue)).withStyle(style -> style.withColor(ChatFormatting.GREEN)));
                              }
                              fieldDescriptionText = fieldDescriptionText.append(Component.literal(" ]").withStyle(style -> style.withColor(ChatFormatting.WHITE)));
                              player.displayClientMessage(fieldText.append(fieldDescriptionText), false);
                           }
                           return 1;
                        })))));
   }

   public static void registerCustomEvents() {
      ConfigEvents.SAVE.register(() -> {
         CustomLootBeamsConfig.onConfigurationChange();
         LootBeamRenderer.onConfigurationChange();
      });
      PresetEvents.APPLY_PRESET.register(presetName -> {
         CustomLootBeamsConfig.onConfigurationChange();
         LootBeamRenderer.onConfigurationChange();
      });
      ResourceEvents.RESOURCE_RELOAD.register((synchronizer, resourceManager, prepareExecutor, applyExecutor) -> {
         TooltipManager.onResourcesReload();
         CustomLootBeamsConfig.onResourcesReload();
         CustomRarity.onResourcesReload(resourceManager);
         PresetManager.onResourcesReload(resourceManager);
         LootBeamShaders.onResourcesReload(resourceManager);
         GlowEffectManager.onResourceManagerReload(resourceManager, prepareExecutor);
         ParticleManager.onResourceManagerReload(resourceManager, prepareExecutor);
      });
   }
}
