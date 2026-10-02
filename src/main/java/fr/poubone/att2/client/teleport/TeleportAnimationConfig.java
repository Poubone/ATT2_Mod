package fr.poubone.att2.client.teleport;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class TeleportAnimationConfig {
    private static final String FILE_NAME = "att2_teleport.properties";
    private static final String EFFECT_ENABLED_KEY = "effectEnabled";
    private static final String PLAYER_FREEZE_ENABLED_KEY = "playerFreezeEnabled";
    private static final String CROSS_DIMENSION_TRAVEL_ENABLED_KEY = "crossDimensionTravelEnabled";
    private static final String FALLBACK_CHUNK_FADE_ENABLED_KEY = "fallbackChunkFadeEnabled";
    private static final String CUSTOM_SOUNDS_ENABLED_KEY = "customSoundsEnabled";
    private static final String MINECRAFT_SOUND_VOLUME_KEY = "minecraftSoundVolume";
    private static final String CUSTOM_SOUND_VOLUME_KEY = "customSoundVolume";
    private static final String ZOOM_OUT_STAGE_KEY_PREFIX = "zoomOutStage";
    private static final String ZOOM_IN_STAGE_KEY_PREFIX = "zoomInStage";
    private static final String NETHER_ZOOM_OUT_STAGE_KEY_PREFIX = "netherZoomOutStage";
    private static final String NETHER_ZOOM_IN_STAGE_KEY_PREFIX = "netherZoomInStage";
    private static final String END_ZOOM_OUT_STAGE_KEY_PREFIX = "endZoomOutStage";
    private static final String END_ZOOM_IN_STAGE_KEY_PREFIX = "endZoomInStage";
    private static final String ZOOM_OUT_STAGE_TICKS_KEY_PREFIX = "zoomOutStageTicks";
    private static final String ZOOM_IN_STAGE_TICKS_KEY_PREFIX = "zoomInStageTicks";
    private static final String ZOOM_STAGE_GLIDE_HEIGHT_KEY = "zoomStageGlideHeight";
    private static final String ZOOM_STAGE_GLIDE_TICKS_KEY = "zoomStageGlideTicks";
    private static final String BODY_CAMERA_HEIGHT_KEY = "bodyCameraHeight";
    private static final String BODY_GLIDE_HEIGHT_KEY = "bodyGlideHeight";
    private static final String BODY_GLIDE_TICKS_KEY = "bodyGlideTicks";
    private static final String LOCAL_PLAYER_HIDE_TICKS_KEY = "localPlayerHideTicks";
    private static final String SAVE_COMMENT = """
            ATT2 teleport animation settings (Grand Teleport transition around the map's Teleportation Arrays).
            Sounds: minecraftSound* accept any sound event id, e.g. vanilla ids or the map resource pack's own
            sounds (minecraft:swooshing, minecraft:timewarp1, minecraft:teleportation, minecraft:magicspell, minecraft:wind).
            customSoundsEnabled=true switches to the att2:teleport.* events: provide the .ogg files through a resource pack
            (assets/att2/sounds.json + assets/att2/sounds/teleport/*.ogg, see README).""";
    private static final String SOUND_CAMERA_OUT_KEY = "minecraftSoundCameraOut";
    private static final String SOUND_CAMERA_IN_KEY = "minecraftSoundCameraIn";
    private static final String SOUND_STEP_KEY = "minecraftSoundStep";
    private static final String SOUND_TRAVEL_KEY = "minecraftSoundTravel";
    private static final String DEFAULT_SOUND_CAMERA = "minecraft:ui.toast.out";
    private static final String DEFAULT_SOUND_STEP = "minecraft:block.respawn_anchor.charge";
    private static final String DEFAULT_SOUND_TRAVEL = "minecraft:block.portal.trigger";

    private static final String DEFAULT_CONFIG_PROPERTIES = """
            effectEnabled=true
            playerFreezeEnabled=true
            crossDimensionTravelEnabled=false
            fallbackChunkFadeEnabled=true
            customSoundsEnabled=false
            minecraftSoundVolume=0.5
            customSoundVolume=0.5
            zoomOutStage1=20
            zoomOutStage2=40
            zoomOutStage3=60
            zoomInStage1=20
            zoomInStage2=40
            zoomInStage3=60
            netherZoomOutStage1=20
            netherZoomOutStage2=40
            netherZoomOutStage3=60
            netherZoomInStage1=20
            netherZoomInStage2=40
            netherZoomInStage3=60
            endZoomOutStage1=20
            endZoomOutStage2=40
            endZoomOutStage3=60
            endZoomInStage1=20
            endZoomInStage2=40
            endZoomInStage3=60
            zoomOutStageTicks1=13
            zoomOutStageTicks2=13
            zoomOutStageTicks3=13
            zoomInStageTicks1=13
            zoomInStageTicks2=13
            zoomInStageTicks3=13
            zoomStageGlideHeight=0.5
            zoomStageGlideTicks=13
            bodyCameraHeight=6.0
            bodyGlideHeight=0.5
            bodyGlideTicks=10
            localPlayerHideTicks=2
            """;

    private static final double[] DEFAULT_STAGE_HEIGHTS = {20.0D, 40.0D, 60.0D};
    private static final double MIN_STAGE_HEIGHT = 8.0D;
    private static final double MAX_STAGE_HEIGHT = 512.0D;
    private static final double MIN_STAGE_GAP = 1.0D;
    private static final int[] DEFAULT_STAGE_TICKS = {13, 13, 13};
    private static final int MIN_STAGE_TICKS = 1;
    private static final int MAX_STAGE_TICKS = 200;
    private static final double DEFAULT_ZOOM_STAGE_GLIDE_HEIGHT = 0.5D;
    private static final double MIN_ZOOM_STAGE_GLIDE_HEIGHT = 0.1D;
    private static final double MAX_ZOOM_STAGE_GLIDE_HEIGHT = 5.0D;
    private static final int DEFAULT_ZOOM_STAGE_GLIDE_TICKS = 13;
    private static final double DEFAULT_BODY_CAMERA_HEIGHT = 6.0D;
    private static final double MIN_BODY_CAMERA_HEIGHT = 0.1D;
    private static final double MAX_BODY_CAMERA_HEIGHT = 10.0D;
    private static final double DEFAULT_BODY_GLIDE_HEIGHT = 0.5D;
    private static final double MIN_BODY_GLIDE_HEIGHT = 0.1D;
    private static final double MAX_BODY_GLIDE_HEIGHT = 5.0D;
    private static final int DEFAULT_BODY_GLIDE_TICKS = 10;
    private static final int MIN_LOCAL_PLAYER_HIDE_TICKS = 0;
    private static final int MAX_LOCAL_PLAYER_HIDE_TICKS = 20;
    private static final int DEFAULT_LOCAL_PLAYER_HIDE_TICKS = 2;
    private static final double DEFAULT_MINECRAFT_SOUND_VOLUME = 0.5D;
    private static final double DEFAULT_CUSTOM_SOUND_VOLUME = 0.5D;
    private static final double MIN_SOUND_VOLUME = 0.1D;
    private static final double MAX_SOUND_VOLUME = 1.0D;

    private static Path configPath;
    private static boolean effectEnabled = true;
    private static boolean playerFreezeEnabled = true;
    private static boolean crossDimensionTravelEnabled;
    private static boolean fallbackChunkFadeEnabled = true;
    private static boolean customSoundsEnabled;
    private static double minecraftSoundVolume = DEFAULT_MINECRAFT_SOUND_VOLUME;
    private static double customSoundVolume = DEFAULT_CUSTOM_SOUND_VOLUME;
    private static double[] zoomOutStageHeights = DEFAULT_STAGE_HEIGHTS.clone();
    private static double[] zoomInStageHeights = DEFAULT_STAGE_HEIGHTS.clone();
    private static double[] netherZoomOutStageHeights = DEFAULT_STAGE_HEIGHTS.clone();
    private static double[] netherZoomInStageHeights = DEFAULT_STAGE_HEIGHTS.clone();
    private static double[] endZoomOutStageHeights = DEFAULT_STAGE_HEIGHTS.clone();
    private static double[] endZoomInStageHeights = DEFAULT_STAGE_HEIGHTS.clone();
    private static int[] zoomOutStageTicks = DEFAULT_STAGE_TICKS.clone();
    private static int[] zoomInStageTicks = DEFAULT_STAGE_TICKS.clone();
    private static double zoomStageGlideHeight = DEFAULT_ZOOM_STAGE_GLIDE_HEIGHT;
    private static int zoomStageGlideTicks = DEFAULT_ZOOM_STAGE_GLIDE_TICKS;
    private static double bodyCameraHeight = DEFAULT_BODY_CAMERA_HEIGHT;
    private static double bodyGlideHeight = DEFAULT_BODY_GLIDE_HEIGHT;
    private static int bodyGlideTicks = DEFAULT_BODY_GLIDE_TICKS;
    private static int localPlayerHideTicks = DEFAULT_LOCAL_PLAYER_HIDE_TICKS;
    private static String soundCameraOut = DEFAULT_SOUND_CAMERA;
    private static String soundCameraIn = DEFAULT_SOUND_CAMERA;
    private static String soundStep = DEFAULT_SOUND_STEP;
    private static String soundTravel = DEFAULT_SOUND_TRAVEL;

    private TeleportAnimationConfig() {
    }

    public static void load() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        resetToDefaults();

        if (!Files.exists(configPath)) {
            save();
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
            applyConfigProperties(properties);
        } catch (IOException ignored) {
            resetToDefaults();
        }
    }

    public static boolean save() {
        if (configPath == null) {
            configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        }

        Properties properties = new Properties();
        properties.setProperty(EFFECT_ENABLED_KEY, Boolean.toString(effectEnabled));
        properties.setProperty(PLAYER_FREEZE_ENABLED_KEY, Boolean.toString(playerFreezeEnabled));
        properties.setProperty(CROSS_DIMENSION_TRAVEL_ENABLED_KEY, Boolean.toString(crossDimensionTravelEnabled));
        properties.setProperty(FALLBACK_CHUNK_FADE_ENABLED_KEY, Boolean.toString(fallbackChunkFadeEnabled));
        properties.setProperty(CUSTOM_SOUNDS_ENABLED_KEY, Boolean.toString(customSoundsEnabled));
        properties.setProperty(MINECRAFT_SOUND_VOLUME_KEY, Double.toString(minecraftSoundVolume));
        properties.setProperty(CUSTOM_SOUND_VOLUME_KEY, Double.toString(customSoundVolume));
        writeStageHeights(properties, ZOOM_OUT_STAGE_KEY_PREFIX, zoomOutStageHeights);
        writeStageHeights(properties, ZOOM_IN_STAGE_KEY_PREFIX, zoomInStageHeights);
        writeStageHeights(properties, NETHER_ZOOM_OUT_STAGE_KEY_PREFIX, netherZoomOutStageHeights);
        writeStageHeights(properties, NETHER_ZOOM_IN_STAGE_KEY_PREFIX, netherZoomInStageHeights);
        writeStageHeights(properties, END_ZOOM_OUT_STAGE_KEY_PREFIX, endZoomOutStageHeights);
        writeStageHeights(properties, END_ZOOM_IN_STAGE_KEY_PREFIX, endZoomInStageHeights);
        writeStageTicks(properties, ZOOM_OUT_STAGE_TICKS_KEY_PREFIX, zoomOutStageTicks);
        writeStageTicks(properties, ZOOM_IN_STAGE_TICKS_KEY_PREFIX, zoomInStageTicks);
        properties.setProperty(ZOOM_STAGE_GLIDE_HEIGHT_KEY, Double.toString(zoomStageGlideHeight));
        properties.setProperty(ZOOM_STAGE_GLIDE_TICKS_KEY, Integer.toString(zoomStageGlideTicks));
        properties.setProperty(BODY_CAMERA_HEIGHT_KEY, Double.toString(bodyCameraHeight));
        properties.setProperty(BODY_GLIDE_HEIGHT_KEY, Double.toString(bodyGlideHeight));
        properties.setProperty(BODY_GLIDE_TICKS_KEY, Integer.toString(bodyGlideTicks));
        properties.setProperty(LOCAL_PLAYER_HIDE_TICKS_KEY, Integer.toString(localPlayerHideTicks));
        properties.setProperty(SOUND_CAMERA_OUT_KEY, soundCameraOut);
        properties.setProperty(SOUND_CAMERA_IN_KEY, soundCameraIn);
        properties.setProperty(SOUND_STEP_KEY, soundStep);
        properties.setProperty(SOUND_TRAVEL_KEY, soundTravel);

        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream output = Files.newOutputStream(configPath)) {
                properties.store(output, SAVE_COMMENT);
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public static boolean isEffectEnabled() {
        return effectEnabled;
    }

    public static boolean setEffectEnabled(boolean enabled) {
        effectEnabled = enabled;
        return save();
    }

    public static boolean isPlayerFreezeEnabled() {
        return playerFreezeEnabled;
    }

    public static boolean setPlayerFreezeEnabled(boolean enabled) {
        playerFreezeEnabled = enabled;
        return save();
    }

    public static boolean isCrossDimensionTravelEnabled() {
        return crossDimensionTravelEnabled;
    }

    public static boolean isFallbackChunkFadeEnabled() {
        return fallbackChunkFadeEnabled;
    }

    public static boolean isCustomSoundsEnabled() {
        return customSoundsEnabled;
    }

    public static double getMinecraftSoundVolume() {
        return minecraftSoundVolume;
    }

    public static double getCustomSoundVolume() {
        return customSoundVolume;
    }

    public static double[] getZoomOutStageHeights() {
        return getZoomOutStageHeights(ZoomDimension.OVERWORLD);
    }

    public static double[] getZoomOutStageHeights(ZoomDimension dimension) {
        return switch (sanitizeZoomDimension(dimension)) {
            case NETHER -> netherZoomOutStageHeights.clone();
            case END -> endZoomOutStageHeights.clone();
            default -> zoomOutStageHeights.clone();
        };
    }

    public static double[] getZoomInStageHeights() {
        return getZoomInStageHeights(ZoomDimension.OVERWORLD);
    }

    public static double[] getZoomInStageHeights(ZoomDimension dimension) {
        return switch (sanitizeZoomDimension(dimension)) {
            case NETHER -> netherZoomInStageHeights.clone();
            case END -> endZoomInStageHeights.clone();
            default -> zoomInStageHeights.clone();
        };
    }

    public static int[] getZoomOutStageTicks() {
        return zoomOutStageTicks.clone();
    }

    public static int[] getZoomInStageTicks() {
        return zoomInStageTicks.clone();
    }

    public static double getZoomStageGlideHeight() {
        return zoomStageGlideHeight;
    }

    public static int getZoomStageGlideTicks() {
        return zoomStageGlideTicks;
    }

    public static double getBodyCameraHeight() {
        return bodyCameraHeight;
    }

    public static double getBodyGlideHeight() {
        return bodyGlideHeight;
    }

    public static int getBodyGlideTicks() {
        return bodyGlideTicks;
    }

    /**
     * Sound played when the camera leaves / re-enters the player ("Minecraft sounds" mode).
     * Any sound event id works, including the map's resource pack sounds (e.g. {@code minecraft:swooshing},
     * {@code minecraft:timewarp1}, {@code minecraft:teleportation}, {@code minecraft:magicspell}).
     */
    public static SoundEvent getCameraSound(boolean entering) {
        return soundEvent(entering ? soundCameraIn : soundCameraOut, DEFAULT_SOUND_CAMERA);
    }

    public static SoundEvent getStepSound() {
        return soundEvent(soundStep, DEFAULT_SOUND_STEP);
    }

    public static SoundEvent getTravelSound() {
        return soundEvent(soundTravel, DEFAULT_SOUND_TRAVEL);
    }

    private static SoundEvent soundEvent(String id, String fallback) {
        Identifier identifier = Identifier.tryParse(id == null ? "" : id.trim());
        if (identifier == null) {
            identifier = Identifier.parse(fallback);
        }
        return SoundEvent.createVariableRangeEvent(identifier);
    }

    public static int getLocalPlayerHideTicks() {
        return localPlayerHideTicks;
    }

    private static void applyConfigProperties(Properties properties) {
        effectEnabled = readBoolean(properties, EFFECT_ENABLED_KEY, effectEnabled);
        playerFreezeEnabled = readBoolean(properties, PLAYER_FREEZE_ENABLED_KEY, playerFreezeEnabled);
        crossDimensionTravelEnabled = readBoolean(properties, CROSS_DIMENSION_TRAVEL_ENABLED_KEY, crossDimensionTravelEnabled);
        fallbackChunkFadeEnabled = readBoolean(properties, FALLBACK_CHUNK_FADE_ENABLED_KEY, fallbackChunkFadeEnabled);
        customSoundsEnabled = readBoolean(properties, CUSTOM_SOUNDS_ENABLED_KEY, customSoundsEnabled);
        minecraftSoundVolume = sanitizeSoundVolume(readDouble(properties, MINECRAFT_SOUND_VOLUME_KEY, minecraftSoundVolume));
        customSoundVolume = sanitizeSoundVolume(readDouble(properties, CUSTOM_SOUND_VOLUME_KEY, customSoundVolume));
        zoomOutStageHeights = readStageHeights(properties, ZOOM_OUT_STAGE_KEY_PREFIX, DEFAULT_STAGE_HEIGHTS);
        zoomInStageHeights = readStageHeights(properties, ZOOM_IN_STAGE_KEY_PREFIX, DEFAULT_STAGE_HEIGHTS);
        netherZoomOutStageHeights = readStageHeights(properties, NETHER_ZOOM_OUT_STAGE_KEY_PREFIX, zoomOutStageHeights);
        netherZoomInStageHeights = readStageHeights(properties, NETHER_ZOOM_IN_STAGE_KEY_PREFIX, zoomInStageHeights);
        endZoomOutStageHeights = readStageHeights(properties, END_ZOOM_OUT_STAGE_KEY_PREFIX, zoomOutStageHeights);
        endZoomInStageHeights = readStageHeights(properties, END_ZOOM_IN_STAGE_KEY_PREFIX, zoomInStageHeights);
        zoomOutStageTicks = readStageTicks(properties, ZOOM_OUT_STAGE_TICKS_KEY_PREFIX, DEFAULT_STAGE_TICKS);
        zoomInStageTicks = readStageTicks(properties, ZOOM_IN_STAGE_TICKS_KEY_PREFIX, DEFAULT_STAGE_TICKS);
        zoomStageGlideHeight = sanitizeZoomStageGlideHeight(readDouble(properties, ZOOM_STAGE_GLIDE_HEIGHT_KEY, zoomStageGlideHeight));
        zoomStageGlideTicks = sanitizeStageTicksValue(readInt(properties, ZOOM_STAGE_GLIDE_TICKS_KEY, zoomStageGlideTicks));
        bodyCameraHeight = sanitizeBodyCameraHeight(readDouble(properties, BODY_CAMERA_HEIGHT_KEY, bodyCameraHeight));
        bodyGlideHeight = sanitizeBodyGlideHeight(readDouble(properties, BODY_GLIDE_HEIGHT_KEY, bodyGlideHeight));
        bodyGlideTicks = sanitizeStageTicksValue(readInt(properties, BODY_GLIDE_TICKS_KEY, bodyGlideTicks));
        localPlayerHideTicks = sanitizeLocalPlayerHideTicks(readInt(properties, LOCAL_PLAYER_HIDE_TICKS_KEY, localPlayerHideTicks));
        soundCameraOut = properties.getProperty(SOUND_CAMERA_OUT_KEY, soundCameraOut).trim();
        soundCameraIn = properties.getProperty(SOUND_CAMERA_IN_KEY, soundCameraIn).trim();
        soundStep = properties.getProperty(SOUND_STEP_KEY, soundStep).trim();
        soundTravel = properties.getProperty(SOUND_TRAVEL_KEY, soundTravel).trim();
    }

    private static void resetToDefaults() {
        applyConfigProperties(createDefaultProperties());
    }

    private static Properties createDefaultProperties() {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(DEFAULT_CONFIG_PROPERTIES));
        } catch (IOException ignored) {
        }
        return properties;
    }

    private static double[] readStageHeights(Properties properties, String prefix, double[] defaults) {
        double[] values = defaults.clone();
        for (int i = 0; i < values.length; i++) {
            values[i] = readDouble(properties, prefix + (i + 1), values[i]);
        }
        return sanitizeStageHeights(values);
    }

    private static int[] readStageTicks(Properties properties, String prefix, int[] defaults) {
        int[] values = defaults.clone();
        for (int i = 0; i < values.length; i++) {
            values[i] = sanitizeStageTicksValue(readInt(properties, prefix + (i + 1), values[i]));
        }
        return sanitizeStageTicks(values);
    }

    private static double[] sanitizeStageHeights(double[] values) {
        double[] source = values == null || values.length < 3 ? DEFAULT_STAGE_HEIGHTS : values;
        double[] sanitized = new double[3];
        sanitized[0] = clamp(roundStageHeight(source[0]), MIN_STAGE_HEIGHT, MAX_STAGE_HEIGHT - MIN_STAGE_GAP * 2.0D);
        sanitized[1] = clamp(roundStageHeight(source[1]), sanitized[0] + MIN_STAGE_GAP, MAX_STAGE_HEIGHT - MIN_STAGE_GAP);
        sanitized[2] = clamp(roundStageHeight(source[2]), sanitized[1] + MIN_STAGE_GAP, MAX_STAGE_HEIGHT);
        return sanitized;
    }

    private static int[] sanitizeStageTicks(int[] values) {
        int[] source = values == null || values.length < 3 ? DEFAULT_STAGE_TICKS : values;
        int[] sanitized = new int[3];
        for (int i = 0; i < sanitized.length; i++) {
            sanitized[i] = sanitizeStageTicksValue(source[i]);
        }
        return sanitized;
    }

    private static double sanitizeZoomStageGlideHeight(double value) {
        return Math.round(clamp(value, MIN_ZOOM_STAGE_GLIDE_HEIGHT, MAX_ZOOM_STAGE_GLIDE_HEIGHT) * 10.0D) / 10.0D;
    }

    private static double sanitizeBodyCameraHeight(double value) {
        return Math.round(clamp(value, MIN_BODY_CAMERA_HEIGHT, MAX_BODY_CAMERA_HEIGHT) * 10.0D) / 10.0D;
    }

    private static double sanitizeBodyGlideHeight(double value) {
        return Math.round(clamp(value, MIN_BODY_GLIDE_HEIGHT, MAX_BODY_GLIDE_HEIGHT) * 10.0D) / 10.0D;
    }

    private static int sanitizeStageTicksValue(int value) {
        return clamp(value, MIN_STAGE_TICKS, MAX_STAGE_TICKS);
    }

    private static int sanitizeLocalPlayerHideTicks(int value) {
        return clamp(value, MIN_LOCAL_PLAYER_HIDE_TICKS, MAX_LOCAL_PLAYER_HIDE_TICKS);
    }

    private static double sanitizeSoundVolume(double value) {
        return Math.round(clamp(value, MIN_SOUND_VOLUME, MAX_SOUND_VOLUME) * 10.0D) / 10.0D;
    }

    private static boolean readBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(value);
    }

    private static double readDouble(Properties properties, String key, double fallback) {
        try {
            return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int readInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double roundStageHeight(double value) {
        return Math.rint(value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ZoomDimension sanitizeZoomDimension(ZoomDimension dimension) {
        return dimension == null ? ZoomDimension.OVERWORLD : dimension;
    }

    private static void writeStageHeights(Properties properties, String prefix, double[] values) {
        double[] sanitized = sanitizeStageHeights(values);
        for (int i = 0; i < sanitized.length; i++) {
            properties.setProperty(prefix + (i + 1), Integer.toString((int) sanitized[i]));
        }
    }

    private static void writeStageTicks(Properties properties, String prefix, int[] values) {
        int[] sanitized = sanitizeStageTicks(values);
        for (int i = 0; i < sanitized.length; i++) {
            properties.setProperty(prefix + (i + 1), Integer.toString(sanitized[i]));
        }
    }

    public enum ZoomDimension {
        OVERWORLD,
        NETHER,
        END;

        public static ZoomDimension fromLevel(ResourceKey<Level> dimension) {
            if (Level.NETHER.equals(dimension)) {
                return NETHER;
            }
            if (Level.END.equals(dimension)) {
                return END;
            }
            return OVERWORLD;
        }
    }
}
