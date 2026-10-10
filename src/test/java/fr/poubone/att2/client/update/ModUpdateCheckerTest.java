package fr.poubone.att2.client.update;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModUpdateCheckerTest {
    private String body(String version, String url) {
        return """
                {"release":{"version":"%s","minecraftVersions":["1.21.11"],
                "minLoaderVersion":"0.18.4","pageUrl":"%s"}}
                """.formatted(version, url);
    }

    @Test
    void onlyNewerCompatibleStableVersionsAreProposed() throws Exception {
        String json = body("2.10.0", "https://guide-att2.com/mod/");
        assertNotNull(ModUpdateChecker.parseRelease(json, "2.9.0", "1.21.11", "0.18.4"));
        assertNull(ModUpdateChecker.parseRelease(json, "2.10.0", "1.21.11", "0.18.4"));
        assertNull(ModUpdateChecker.parseRelease(json, "3.0.0", "1.21.11", "0.18.4"));
        assertNull(ModUpdateChecker.parseRelease(json, "2.9.0", "1.20.1", "0.18.4"));
        assertNull(ModUpdateChecker.parseRelease(json, "2.9.0", "1.21.11", "0.18.3"));
        assertNull(ModUpdateChecker.parseRelease(body("3.0.0-beta.1", "https://guide-att2.com/mod/"),
                "2.9.0", "1.21.11", "0.18.4"));
        assertNull(ModUpdateChecker.parseRelease("{\"release\":null}", "2.9.0", "1.21.11", "0.18.4"));
    }

    @Test
    void rejectsUrlsOutsideTheHttpsGuideSite() throws Exception {
        for (String url : new String[]{"http://guide-att2.com/mod/", "https://evil.example/mod/",
                "https://user@guide-att2.com/mod/", "https://guide-att2.com:444/mod/"}) {
            assertNull(ModUpdateChecker.parseRelease(body("2.1.0", url), "2.0.0", "1.21.11", "0.18.4"));
        }
    }
}
