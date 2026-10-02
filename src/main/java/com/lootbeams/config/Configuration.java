package com.lootbeams.config;

import com.lootbeams.dconfig.DynamicConfig;
import com.lootbeams.features.BeamOpacityOnApproach;
import com.lootbeams.features.BeamSizeOnApproach;
import com.lootbeams.managers.GlowEffectManager;
import com.lootbeams.managers.ParticleManager;
import com.lootbeams.shaders.LootBeamShaders;
import java.util.ArrayList;
import java.util.List;

public class Configuration {
   @DynamicConfig.Field(category = "main", group = "visual")
   public boolean renderNameColor = true;
   @DynamicConfig.Field(category = "main", group = "visual")
   public boolean renderRarityColor = true;
   @DynamicConfig.Field(category = "main", group = "visual")
   public boolean renderBeam = true;
   @DynamicConfig.Field(category = "main", group = "visual")
   public boolean renderDroplightBeam = false;
   @DynamicConfig.Field(category = "main", group = "visual")
   public boolean animateDroplightBeam = false;
   @DynamicConfig.Field(category = "main", group = "visual", min = 0.0, max = 5.0)
   public float droplightBeamAnimationSpeed = 1.0F;
   @DynamicConfig.Field(category = "main", group = "visual")
   public List<String> beamGradientModifiers = List.of("-h50", "+s35", "-v50");
   @DynamicConfig.Field(category = "main", group = "visual")
   public boolean smoothBeamSize = true;
   @DynamicConfig.Field(category = "main", group = "visual", min = 0.0, max = 100.0)
   public float smoothDuration = 3.0F;
   @DynamicConfig.Field(category = "main", group = "visual", min = 0.0, max = 5.0)
   public float beamRadius = 0.55F;
   @DynamicConfig.Field(category = "main", group = "visual", min = 0.0, max = 5.0)
   public float minBeamRadius = 0.0F;
   @DynamicConfig.Field(category = "main", group = "visual", min = 0.0, max = 24.0)
   public float beamHeight = 3.0F;
   @DynamicConfig.Field(category = "main", group = "visual", min = 0.0, max = 24.0)
   public float minBeamHeight = 0.0F;
   @DynamicConfig.Field(category = "main", group = "visual", min = -30.0, max = 30.0)
   public float beamYOffset = 0.0F;
   @DynamicConfig.Field(category = "main", group = "visual")
   public boolean commonShorterBeam = true;
   @DynamicConfig.Field(category = "main", group = "visual", min = 0.0, max = 1.0)
   public float beamAlpha = 0.75F;
   @DynamicConfig.Field(category = "main", group = "visual", min = 0.0, max = 1.0)
   public float minBeamAlpha = 0.0F;
   @DynamicConfig.Field(category = "main", group = "visual")
   public boolean solidBeam = true;
   @DynamicConfig.Field(category = "main", group = "visual")
   public boolean whiteCenter = true;
   @DynamicConfig.Field(category = "main", group = "visual")
   public boolean glowingBeam = true;
   @DynamicConfig.Field(category = "main", group = "visual", min = 0.0, max = 128.0)
   public float renderDistance = 48.0F;
   @DynamicConfig.Field(category = "main", group = "visual", min = 0.0, max = 128.0)
   public float changeDistance = 48.0F;
   @DynamicConfig.Field(category = "main", group = "visual", min = 0.0, max = 128.0)
   public float changeOffset = 40.0F;
   @DynamicConfig.Field(category = "main", group = "visual")
   public BeamOpacityOnApproach beamOpacityOnApproach = BeamOpacityOnApproach.FADE_IN;
   @DynamicConfig.Field(category = "main", group = "visual")
   public BeamSizeOnApproach beamSizeOnApproach = BeamSizeOnApproach.DISABLED;
   @DynamicConfig.Field(category = "main", group = "visual")
   public boolean requireOnGround = true;
   @DynamicConfig.Field(category = "groundGlow", group = "visual")
   public boolean glowEffect = true;
   @DynamicConfig.Field(category = "groundGlow", group = "visual")
   public LootBeamShaders.CustomShader glowCustomShader = LootBeamShaders.CustomShader.NONE;
   @DynamicConfig.Field(category = "groundGlow", group = "visual")
   public boolean useGlowGradient = false;
   @DynamicConfig.Field(category = "groundGlow", group = "visual")
   public List<String> glowGradientModifiers = List.of("-h50", "+s35", "-v50");
   @DynamicConfig.Field(category = "groundGlow", group = "visual", min = 0.0, max = 100.0)
   public float glowGradientStart = 0.0F;
   @DynamicConfig.Field(category = "groundGlow", group = "visual", min = 0.0, max = 100.0)
   public float glowGradientEnd = 100.0F;
   @DynamicConfig.Field(category = "groundGlow", group = "visual")
   public boolean smoothGlowEffectRadius = false;
   @DynamicConfig.Field(category = "groundGlow", group = "visual")
   public boolean smoothGlowEffectAlpha = true;
   @DynamicConfig.Field(category = "groundGlow", group = "visual")
   public GlowEffectManager.GlowEffectTexture glowEffectTexture = GlowEffectManager.GLOW_TEXTURE;
   @DynamicConfig.Field(category = "groundGlow", group = "visual", min = 1.0E-5, max = 3.0)
   public float glowEffectRadius = 0.5F;
   @DynamicConfig.Field(category = "groundGlow", group = "visual", min = 0.0, max = 1.0)
   public float glowEffectAlpha = 0.8F;
   @DynamicConfig.Field(category = "groundGlow", group = "visual")
   public boolean pulseGlow = true;
   @DynamicConfig.Field(category = "groundGlow", group = "visual", min = 0.0, max = 10.0)
   public float pulseGlowSpeed = 1.0F;
   @DynamicConfig.Field(category = "groundGlow", group = "visual", min = 0.0, max = 1.0)
   public float pulseGlowMinAlpha = 0.8F;
   @DynamicConfig.Field(category = "groundGlow", group = "visual", min = 0.0, max = 1.0)
   public float pulseGlowMaxAlpha = 1.0F;
   @DynamicConfig.Field(category = "groundGlow", group = "visual", min = 0.0, max = 3.0)
   public float pulseGlowMinRadius = 0.5F;
   @DynamicConfig.Field(category = "groundGlow", group = "visual", min = 0.0, max = 3.0)
   public float pulseGlowMaxRadius = 0.625F;
   @DynamicConfig.Field(category = "groundGlow", group = "visual")
   public boolean rotateGlow = false;
   @DynamicConfig.Field(category = "groundGlow", group = "visual")
   public boolean glowRotateClockwise = true;
   @DynamicConfig.Field(category = "groundGlow", group = "visual", min = 0.0, max = 50.0)
   public float glowRotationSpeed = 1.0F;
   @DynamicConfig.Field(category = "particles", group = "visual")
   public boolean particles = true;
   @DynamicConfig.Field(category = "particles", group = "visual")
   public ParticleManager.ParticleTexture particleTexture = ParticleManager.GLOW_TEXTURE;
   @DynamicConfig.Field(category = "particles", group = "visual")
   public LootBeamShaders.Shader particleColorMode = LootBeamShaders.Shader.PARTICLE_OVERLAY;
   @DynamicConfig.Field(category = "particles", group = "visual")
   public boolean particleInheritsColor = true;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 0.0, max = 20.0)
   public int tickPerParticleSpriteUpdate = 1;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 1.0, max = 20.0)
   public float particleCount = 5.0F;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 1.0, max = 100.0)
   public int particleLifetime = 15;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 1.0E-5, max = 10.0)
   public float particleSize = 0.25F;
   @DynamicConfig.Field(category = "particles", group = "visual")
   public boolean particleRandomSize = true;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 1.0E-5, max = 10.0)
   public float particleRadius = 0.1F;
   @DynamicConfig.Field(category = "particles", group = "visual", min = -35.0, max = 35.0)
   public float particleYOffset = 0.0F;
   @DynamicConfig.Field(category = "particles", group = "visual")
   public boolean particleRandomY = true;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 1.0E-5, max = 10.0)
   public float particleSpeed = 0.2F;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 0.0, max = 10.0)
   public float particleSpeedX = 0.2F;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 0.0, max = 10.0)
   public float particleSpeedY = 0.01F;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 0.0, max = 10.0)
   public float particleSpeedZ = 0.2F;
   @DynamicConfig.Field(category = "particles", group = "visual")
   public boolean particleUseConstantVerticalSpeed = false;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 0.0, max = 1.0)
   public float randomnessIntensity = 0.05F;
   @DynamicConfig.Field(category = "particles", group = "visual")
   public boolean particleRareOnly = true;
   @DynamicConfig.Field(category = "particles", min = 0.0, max = 1.0)
   public float particleDirectionX = 0.0F;
   @DynamicConfig.Field(category = "particles", min = 0.0, max = 1.0)
   public float particleDirectionY = 1.0F;
   @DynamicConfig.Field(category = "particles", min = 0.0, max = 1.0)
   public float particleDirectionZ = 0.0F;
   @DynamicConfig.Field(category = "particles", group = "visual")
   public boolean spinAroundBeam = true;
   @DynamicConfig.Field(category = "particles", group = "visual")
   public boolean trails = true;
   @DynamicConfig.Field(category = "particles", group = "visual")
   public boolean trailParticlesInvisible = true;
   @DynamicConfig.Field(category = "particles", group = "visual")
   public boolean trailUseScale = true;
   @DynamicConfig.Field(category = "particles", group = "visual")
   public boolean trailScaleHeightEqualsBeamHeight = true;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 1.0E-5, max = 24.0)
   public float trailScaleHeight = 3.0F;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 0.0, max = 1.0)
   public float trailChance = 0.4F;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 1.0E-5, max = 10.0)
   public float trailWidth = 0.2F;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 1.0, max = 200.0)
   public int trailLength = 30;
   @DynamicConfig.Field(category = "particles", group = "visual", min = 1.0, max = 200.0)
   public int trailFrequency = 1;
   @DynamicConfig.Field(category = "items", group = "visual")
   public boolean itemsGlow = false;
   @DynamicConfig.Field(category = "items")
   public boolean allItems = true;
   @DynamicConfig.Field(category = "items")
   public boolean onlyEquipment = false;
   @DynamicConfig.Field(category = "items")
   public boolean onlyRare = false;
   @DynamicConfig.Field(category = "items")
   public List<String> whitelist = new ArrayList<>();
   @DynamicConfig.Field(category = "items")
   public List<String> blacklist = new ArrayList<>();
   @DynamicConfig.Field(category = "items")
   public List<String> colorOverrides = new ArrayList<>();
   @DynamicConfig.Field(category = "nametags", group = "visual")
   public boolean advancedTooltips = true;
   @DynamicConfig.Field(category = "nametags", group = "visual")
   public boolean worldspaceTooltips = true;
   @DynamicConfig.Field(category = "nametags", group = "visual")
   public boolean itemFrameTooltips = true;
   @DynamicConfig.Field(category = "nametags", group = "visual")
   public boolean renderTooltipsInThirdPersonView = true;
   @DynamicConfig.Field(category = "nametags", group = "visual")
   public boolean borders = true;
   @DynamicConfig.Field(category = "nametags", group = "visual")
   public boolean renderNametags = true;
   @DynamicConfig.Field(category = "nametags", group = "visual")
   public boolean renderNametagsOnlook = true;
   @DynamicConfig.Field(category = "nametags", group = "visual")
   public boolean renderItemRarity = true;
   @DynamicConfig.Field(category = "nametags", group = "visual")
   public boolean renderItemRarityInTooltip = true;
   @DynamicConfig.Field(category = "nametags", group = "visual")
   public boolean renderStackcount = true;
   @DynamicConfig.Field(category = "nametags", min = 0.0, max = 5.0)
   public float nametagLookSensitivity = 0.018F;
   @DynamicConfig.Field(category = "nametags", group = "visual", min = 0.0, max = 1.0)
   public float nametagTextAlpha = 1.0F;
   @DynamicConfig.Field(category = "nametags", group = "visual", min = 0.0, max = 1.0)
   public float nametagBackgroundAlpha = 0.5F;
   @DynamicConfig.Field(category = "nametags", group = "visual", min = -10.0, max = 10.0)
   public float nametagScale = 1.0F;
   @DynamicConfig.Field(category = "nametags", group = "visual", min = -30.0, max = 30.0)
   public float nametagYOffset = 0.75F;
   @DynamicConfig.Field(category = "nametags")
   public boolean whiteRarities = false;
   @DynamicConfig.Field(category = "nametags")
   public boolean vanillaRarities = false;
   @DynamicConfig.Field(category = "nametags", group = "visual")
   public boolean screenTooltipsRequireCrouch = true;
   @DynamicConfig.Field(category = "nametags", group = "visual")
   public boolean combineNameAndRarity = false;
   @DynamicConfig.Field(category = "nametags")
   public List<String> customRarities = new ArrayList<>();
   @DynamicConfig.Field(category = "nametags")
   public List<String> alwaysDrawRaritiesOn = List.of("#minecraft:music_discs");
   @DynamicConfig.Field(category = "presets")
   public String selectedPreset = "";

   public Configuration() {
      this.migrateDistanceDefaults();
   }

   public boolean migrateDistanceDefaults() {
      if (!(this.renderDistance <= 24.0F || this.changeDistance < this.renderDistance || this.changeOffset < 8.0F)) {
         return false;
      }

      this.renderDistance = Math.max(this.renderDistance, 48.0F);
      this.changeDistance = this.renderDistance;
      this.changeOffset = Math.max(this.renderDistance - 8.0F, 0.0F);
      return true;
   }

   public static class Categories {
      @DynamicConfig.Category(name = "Loot Beams", key = "LootBeams", root = true)
      public static final String MAIN = "main";
      @DynamicConfig.Category(name = "Ground glow", key = "GroundGlow")
      public static final String GROUND_GLOW = "groundGlow";
      @DynamicConfig.Category(name = "Particles", key = "Particles")
      public static final String PARTICLES = "particles";
      @DynamicConfig.Category(name = "Items", key = "Items")
      public static final String ITEMS = "items";
      @DynamicConfig.Category(name = "Nametags", key = "Nametags")
      public static final String NAMETAGS = "nametags";
      @DynamicConfig.Category(name = "Presets", key = "Presets", display = false)
      public static final String PRESETS = "presets";

      public Categories() {
      }
   }

   public static class Groups {
      public static final String VISUAL = "visual";

      public Groups() {
      }
   }
}
