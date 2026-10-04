package fr.poubone.att2.client.screen;

/** Keeps category buttons between the title and footer, including at a 320 x 180 GUI size. */
final class HUDConfigLayout {
    private HUDConfigLayout() {}

    static int tabTop(int height, int count) {
        return Math.min(36, height - 28 - 4 - ((count - 1) * tabStep(height, count) + 20));
    }

    static int tabStep(int height, int count) {
        return Math.max(20, Math.min(24, (height - 28 - 4 - 28 - 20) / Math.max(1, count - 1)));
    }
}
