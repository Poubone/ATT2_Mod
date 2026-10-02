package fr.poubone.att2.client.util;

import net.minecraft.resources.Identifier;

import java.util.Map;

public class ModTextures {
    public static final Identifier XP_ORB = Identifier.withDefaultNamespace("textures/entity/experience_orb.png");
    public static final Identifier MANA_FRAME = Identifier.fromNamespaceAndPath("att2", "textures/cadre_vide.png");
    public static final Identifier DAR_SPRITE = Identifier.fromNamespaceAndPath("att2", "textures/dahal.png");
    public static final Identifier FX_BUBBLE = Identifier.fromNamespaceAndPath("att2", "textures/fx/bubble.png");
    public static final Identifier FX_SPARK = Identifier.fromNamespaceAndPath("att2", "textures/fx/spark.png");
    public static final Identifier FX_GLOW = Identifier.fromNamespaceAndPath("att2", "textures/fx/glow.png");
    public static final Identifier FX_XP_MOTE = Identifier.fromNamespaceAndPath("att2", "textures/fx/xp_mote.png");

    /** Icons for the 9 stats of the 1.1.1 remaster (DAR uses the animated DAR_SPRITE instead). */
    public static final Map<String, Identifier> STAT_ICONS = Map.of(
            "HAS", Identifier.withDefaultNamespace("textures/mob_effect/haste.png"),
            "HER", Identifier.withDefaultNamespace("textures/mob_effect/regeneration.png"),
            "HUN", Identifier.withDefaultNamespace("textures/mob_effect/saturation.png"),
            "LUC", Identifier.withDefaultNamespace("textures/mob_effect/luck.png"),
            "RES", Identifier.withDefaultNamespace("textures/mob_effect/resistance.png"),
            "SPD", Identifier.withDefaultNamespace("textures/mob_effect/speed.png"),
            "STR", Identifier.withDefaultNamespace("textures/mob_effect/strength.png"),
            "CRT", Identifier.withDefaultNamespace("textures/item/netherite_sword.png")
    );
}
