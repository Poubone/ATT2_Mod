package fr.poubone.att2.client.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** One background request per launch; UI and config are only accessed on the client thread. */
public final class ModUpdateChecker {
    private static boolean started;
    private static boolean shown;
    private static volatile Release pending;

    public record Release(String version, URI pageUrl) {}

    private ModUpdateChecker() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!(client.screen instanceof TitleScreen) || client.getOverlay() != null) return;
            HUDConfig config = HUDConfig.get();
            if (!config.checkModUpdates) return;
            if (!started) {
                started = true;
                FabricLoader loader = FabricLoader.getInstance();
                String installed = loader.getModContainer("att2").orElseThrow().getMetadata().getVersion().getFriendlyString();
                String minecraft = loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
                String fabric = loader.getModContainer("fabricloader").orElseThrow().getMetadata().getVersion().getFriendlyString();
                String baseUrl = config.modUpdateApiUrl;
                Thread.startVirtualThread(() -> check(baseUrl, installed, minecraft, fabric));
            }
            Release release = pending;
            if (shown || release == null) return;
            shown = true;
            if (release.version().equals(config.ignoredModUpdateVersion)) return;
            ModLanguageManager.loadLanguage(client, config.modLanguage);
            client.setScreen(new ModUpdateScreen(client.screen, release));
        });
    }

    private static void check(String baseUrl, String installed, String minecraft, String fabric) {
        if (baseUrl == null || baseUrl.isBlank()) return;
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build()) {
            URI endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/v1/mod-update?minecraft="
                    + URLEncoder.encode(minecraft, StandardCharsets.UTF_8) + "&loader="
                    + URLEncoder.encode(fabric, StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json").GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200 || response.body().length() > 16_384) return;
            pending = parseRelease(response.body(), installed, minecraft, fabric);
        } catch (Exception e) {
            // Updates must never prevent the game from starting, including offline launches.
            LoggerFactory.getLogger("att2-updates").debug("Mod update check unavailable", e);
        }
    }

    static Release parseRelease(String body, String installed, String minecraft, String fabric) throws Exception {
        JsonObject envelope = JsonParser.parseString(body).getAsJsonObject();
        if (!envelope.has("release") || envelope.get("release").isJsonNull()) return null;
        JsonObject release = envelope.getAsJsonObject("release");
        String version = release.get("version").getAsString();
        if (!version.matches("\\d+\\.\\d+\\.\\d+")) return null;
        if (Version.parse(version).compareTo(Version.parse(installed)) <= 0) return null;
        boolean compatible = false;
        for (var value : release.getAsJsonArray("minecraftVersions")) {
            if (minecraft.equals(value.getAsString())) compatible = true;
        }
        if (!compatible || Version.parse(fabric).compareTo(Version.parse(release.get("minLoaderVersion").getAsString())) < 0) return null;
        URI page = URI.create(release.get("pageUrl").getAsString());
        if (!"https".equals(page.getScheme()) || !"guide-att2.com".equals(page.getHost())
                || page.getUserInfo() != null || (page.getPort() != -1 && page.getPort() != 443)) return null;
        return new Release(version, page);
    }
}
