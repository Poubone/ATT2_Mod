package com.lootbeams.extensions;

import com.lootbeams.helpers.StringHelper;
import com.lootbeams.mixin.TextureAtlasAccessor;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

public abstract class AnimatedTexture {
   private static final String COLORED_POSTFIX = "_colored";
   private static final Map<Class<?>, Map<String, AnimatedTexture>> TEXTURE_CACHE = new HashMap<>();
   public Identifier id;
   public String path;
   private boolean isSpriteSplitted = false;
   private int frameCount = 0;

   public AnimatedTexture(Identifier id) {
      this.id = id;
      this.path = id.toString();
   }

   public abstract TextureAtlas getSpriteAtlasTexture();

   private boolean isMissing(TextureAtlasSprite sprite) {
      return sprite == null || sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation());
   }

   public int getFrameCount() {
      return this.frameCount;
   }

   public boolean isColored() {
      return this.path.endsWith("_colored");
   }

   private void processSplittedSprite() {
      TextureAtlas atlas = this.getSpriteAtlasTexture();
      if (atlas == null) {
         return;
      }
      List<TextureAtlasSprite> spriteList = ((TextureAtlasAccessor) atlas).lootbeams$getTexturesByName()
            .values()
            .stream()
            .filter(sprite -> sprite.contents().name().toString().matches(Pattern.quote(this.id.toString()) + "_\\d+"))
            .toList();
      if (!spriteList.isEmpty()) {
         this.isSpriteSplitted = true;
         this.frameCount = spriteList.size();
      }
   }

   public TextureAtlasSprite getSprite() {
      TextureAtlas atlas = this.getSpriteAtlasTexture();
      if (atlas == null) {
         return null;
      }
      TextureAtlasSprite sprite = atlas.getSprite(this.id);
      return this.isMissing(sprite) ? this.getSprite(0) : sprite;
   }

   public TextureAtlasSprite getSprite(int frameIndex) {
      TextureAtlas atlas = this.getSpriteAtlasTexture();
      if (atlas == null) {
         return null;
      }
      TextureAtlasSprite sprite = atlas.getSprite(Identifier.parse(this.id.toString() + "_" + frameIndex));
      if (!this.isMissing(sprite) && !this.isSplitted()) {
         this.processSplittedSprite();
      }
      return sprite;
   }

   public Identifier getAtlasId() {
      TextureAtlas atlas = this.getSpriteAtlasTexture();
      return atlas == null ? this.id : atlas.location();
   }

   public boolean isSplitted() {
      return this.isSpriteSplitted;
   }

   @Override
   public String toString() {
      return this.path;
   }

   public String getDisplayName() {
      String displayName = this.path;
      if (displayName.endsWith("_colored")) {
         displayName = displayName.substring(0, displayName.length() - "_colored".length());
      }
      return StringHelper.capitalize(String.join(" ", displayName.replace(this.id.getNamespace() + ":", "").split("_")));
   }

   public static <T extends AnimatedTexture> Map<String, AnimatedTexture> getTextureCacheMap(Class<T> type) {
      if (!TEXTURE_CACHE.containsKey(type)) {
         TEXTURE_CACHE.put(type, new HashMap<>());
      }
      return TEXTURE_CACHE.get(type);
   }

   public static String toResourcePath(String displayName, String namespace) {
      return namespace + ":" + displayName.trim().toLowerCase().replace(" ", "_");
   }

   public static <T extends AnimatedTexture> T of(Identifier id, Class<T> type) {
      String resourcePath = id.toString();
      return of(resourcePath, "", type);
   }

   public static <T extends AnimatedTexture> T of(String path, String namespace, Class<T> type) {
      String resourcePath = path;
      if (!path.contains(namespace)) {
         resourcePath = toResourcePath(path, namespace);
      }

      Map<String, AnimatedTexture> MAP = getTextureCacheMap(type);
      if (MAP.containsKey(resourcePath)) {
         return type.cast(MAP.get(resourcePath));
      }
      try {
         Constructor<T> constructor = type.getConstructor(Identifier.class);
         T instance = constructor.newInstance(Identifier.parse(resourcePath));
         MAP.put(resourcePath, instance);
         return instance;
      } catch (Exception var7) {
         throw new RuntimeException("Failed to instantiate texture", var7);
      }
   }

   public static <T extends AnimatedTexture> List<T> getAnimatedTextures(TextureAtlas atlasTexture, Class<T> type) {
      List<T> animatedTextures = new ArrayList<>();
      if (atlasTexture == null) {
         return animatedTextures;
      }
      for (TextureAtlasSprite sprite : ((TextureAtlasAccessor) atlasTexture).lootbeams$getTexturesByName().values()) {
         if (sprite.contents().name().getNamespace().equals("lootbeams")) {
            animatedTextures.add(of(sprite.contents().name(), type));
         }
      }
      animatedTextures.sort(Comparator.comparing(AnimatedTexture::getDisplayName));
      return animatedTextures;
   }
}
