package fr.poubone.att2.client.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ModLanguageManager {
    public static final String FALLBACK = "fr";

    /** Native labels used by the language cycle button (always in their own script). */
    public static final Map<String, String> NATIVE_NAMES = new LinkedHashMap<>();

    static {
        NATIVE_NAMES.put("fr", "Français");
        NATIVE_NAMES.put("en", "English");
        NATIVE_NAMES.put("zh", "中文");
        NATIVE_NAMES.put("ja", "日本語");
        NATIVE_NAMES.put("ko", "한국어");
        NATIVE_NAMES.put("ar", "العربية");
        NATIVE_NAMES.put("ru", "Русский");
        NATIVE_NAMES.put("es", "Español");
        NATIVE_NAMES.put("de", "Deutsch");
        NATIVE_NAMES.put("hi", "हिन्दी");
        NATIVE_NAMES.put("pt", "Português");
    }

    public static final List<String> CODES = List.copyOf(NATIVE_NAMES.keySet());

    private static final Map<String, String> translations = new LinkedHashMap<>();
    private static String currentLanguage = FALLBACK;

    public static void loadLanguage(Minecraft client, String langCode) {
        currentLanguage = normalize(langCode);
        translations.clear();
        loadFile(client, FALLBACK);
        if (!FALLBACK.equals(currentLanguage)) {
            loadFile(client, currentLanguage);
        }
    }

    public static String normalize(String langCode) {
        if (langCode != null && NATIVE_NAMES.containsKey(langCode)) {
            return langCode;
        }
        return FALLBACK;
    }

    public static boolean isRightToLeft() {
        return "ar".equals(currentLanguage);
    }

    public static String nativeName(String langCode) {
        return NATIVE_NAMES.getOrDefault(normalize(langCode), NATIVE_NAMES.get(FALLBACK));
    }

    private static void loadFile(Minecraft client, String langCode) {
        Identifier langFile = Identifier.fromNamespaceAndPath("att2", "lang_mod/" + langCode + ".json");
        Optional<Resource> resource = client.getResourceManager().getResource(langFile);
        if (resource.isEmpty()) {
            System.err.println("[ATT2] Missing language file: " + langFile);
            return;
        }
        try (InputStream stream = resource.get().open()) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                translations.put(entry.getKey(), entry.getValue().getAsString());
            }
        } catch (IOException e) {
            System.err.println("[ATT2] Failed to load language file: " + e.getMessage());
        }
    }

    public static String getCurrentLanguage() {
        return currentLanguage;
    }

    public static MutableComponent get(String key) {
        return Component.literal(getString(key));
    }

    public static String getString(String key) {
        return translations.getOrDefault(key, "\u00a7c?" + key);
    }

    /** Mod language for shared inventory/shop labels; Minecraft fallback before resources are loaded. */
    public static MutableComponent shared(String key) {
        return translations.containsKey(key) ? get(key) : Component.translatable(key);
    }

    public static MutableComponent shared(String key, String placeholder, Object value) {
        Object text = value instanceof Component component ? component.getString() : value;
        return translations.containsKey(key) ? Component.literal(format(key, placeholder, text))
                : Component.translatable(key, value);
    }

    /** Replaces {placeholders} in a translated string. */
    public static String format(String key, Object... pairs) {
        String value = getString(key);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            value = value.replace("{" + pairs[i] + "}", String.valueOf(pairs[i + 1]));
        }
        return value;
    }
}
