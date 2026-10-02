package fr.poubone.att2.client.discord;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Maps the local player's dimension and XZ position to a Discord Rich Presence art-asset key.
 * Keys must be uploaded on the Discord Developer Portal (Rich Presence → Art Assets).
 */
public final class DiscordRegion {
    public static final String FALLBACK_ASSET = "logo-brillant";

    public record Info(String assetKey, String nameKey) {
    }

    private record Anchor(String assetKey, String nameKey, double x, double z) {
    }

    /**
     * Overworld towns / realms. Discord keys are lowercase a-z and hyphens only
     * (same rule as the portal).
     */
    private static final Anchor[] OVERWORLD = {
            new Anchor("hill-valley", "discord.region.hill_valley", 788, 908),
            new Anchor("ouranos", "discord.region.ouranos", 7046, 6972),
            new Anchor("nojelanth", "discord.region.nojelanth", -7371, -4531),
            new Anchor("ryliath", "discord.region.ryliath", -5005, -5077),
            new Anchor("meleim", "discord.region.meleim", -3845, -5808),
            new Anchor("eolorion", "discord.region.eolorion", -5248, -6202),
            new Anchor("elcheol", "discord.region.elcheol", -5607, -6382),
            new Anchor("kortaek", "discord.region.kortaek", -5533, -4678),
            new Anchor("kert", "discord.region.kert", -5523, -5033),
            new Anchor("asunark", "discord.region.asunark", -3661, -4977),
            new Anchor("owsastr", "discord.region.owsastr", -4661, -4546),
            new Anchor("soquai", "discord.region.soquai", -4790, -5686)
    };

    private static final Info SYLBERLAND = new Info("sylberland", "discord.region.sylberland");
    private static final Info ANGBAND = new Info("angband", "discord.region.angband");
    private static final Info BILLGART = new Info("billgart", "discord.region.billgart");

    /** Beyond this XZ distance (blocks), use the generic Sylberländ key instead of a town. */
    private static final double MAX_TOWN_DISTANCE_SQ = 1_800.0 * 1_800.0;

    private DiscordRegion() {
    }

    public static Info of(LocalPlayer player) {
        if (player == null || player.level() == null) return SYLBERLAND;
        String id = String.valueOf(player.level().dimension());
        if (id.contains("the_nether") || id.endsWith(":nether")) return ANGBAND;
        if (id.contains("the_end") || id.endsWith(":end")) return BILLGART;

        Vec3 pos = player.position();
        Anchor nearest = null;
        double best = Double.MAX_VALUE;
        for (Anchor anchor : OVERWORLD) {
            double dx = pos.x - anchor.x;
            double dz = pos.z - anchor.z;
            double d = dx * dx + dz * dz;
            if (d < best) {
                best = d;
                nearest = anchor;
            }
        }
        if (nearest == null || best > MAX_TOWN_DISTANCE_SQ) return SYLBERLAND;
        return new Info(nearest.assetKey, nearest.nameKey);
    }
}
