package fr.poubone.att2.client.data;

import java.util.Locale;
import java.util.Optional;

/**
 * Effect lines for the stat-upgrade tooltip, matched to Definitive Edition v1.0.0
 * ({@code gameplay/stat}).
 */
public final class StatEffectText {
    private static final String UNKNOWN = "screen.stat_upgrade.effect.unknown";
    private static final String HUN_HUNGER = "screen.stat_upgrade.effect.hun.hunger";
    private static final String HUN_NONE = "screen.stat_upgrade.effect.hun.none";
    private static final String HUN_NEUTRAL = "screen.stat_upgrade.effect.hun.neutral";
    private static final String HUN_SAT = "screen.stat_upgrade.effect.hun.sat";

    private StatEffectText() {}

    /** Weapon {@code attack_damage} ×100 and {@code attack_speed} ×1000, as the datapack stores them. */
    public record WeaponStats(int damageX100, int attackSpeedX1000) {}

    public record EffectView(String langKey, Object[] args) {}

    public static double hunSeconds(int timerTicks) {
        return timerTicks / 20.0;
    }

    public static EffectView current(String statKey, int tot) {
        return current(statKey, tot, null);
    }

    public static EffectView current(String statKey, int tot, WeaponStats weapon) {
        return switch (statKey) {
            case "STR" -> strength(tot, weapon);
            case "CRT" -> new EffectView("screen.stat_upgrade.effect.crt", new Object[]{tot});
            case "RES" -> resistance(tot);
            case "HAS" -> new EffectView("screen.stat_upgrade.effect.has", new Object[]{5 * tot});
            case "SPD" -> new EffectView("screen.stat_upgrade.effect.spd", new Object[]{10 * tot});
            case "HER" -> new EffectView(
                    "screen.stat_upgrade.effect.her",
                    new Object[]{String.format(Locale.ROOT, "%.1f", tot / 10.0)}
            );
            case "DAR" -> new EffectView("screen.stat_upgrade.effect.dar", new Object[]{dahalPerSecond(tot)});
            case "LUC" -> new EffectView(
                    "screen.stat_upgrade.effect.luc",
                    new Object[]{String.format(Locale.ROOT, "%.1f", 0.5 * tot)}
            );
            case "HUN" -> hunCurrent(tot);
            default -> new EffectView(UNKNOWN, new Object[]{statKey});
        };
    }

    public static Optional<EffectView> next(String statKey, int tot, int base) {
        return next(statKey, tot, base, null);
    }

    public static Optional<EffectView> next(String statKey, int tot, int base, WeaponStats weapon) {
        if (base >= StatCaps.maxBase(statKey)) {
            return Optional.empty();
        }
        return Optional.of(current(statKey, tot + 1, weapon));
    }

    /**
     * Integer damage from {@code strength/effect.mcfunction}.
     * {@code damageX100} is the {@code minecraft:attack_damage} modifier ×100,
     * {@code attackSpeedX1000} is the {@code minecraft:attack_speed} modifier ×1000.
     */
    public static int strengthDamage(int strTot, int damageX100, int attackSpeedX1000) {
        int dps = 1000;
        dps += attackSpeedX1000;
        dps *= 4;
        dps *= damageX100;
        dps /= 1000;
        int percent = strTot * strTot * 6 + 1100;
        dps *= percent;
        dps /= 100000;
        int damage = strTot * 2;
        damage -= damageX100 / 100;
        damage += dps;
        return damage;
    }

    /** Dahäl points per second ({@code DAHAL_TICK += OP_DAHAL} each tick, {@code DAHAL = DAHAL_TICK / 20}). */
    public static int dahalPerSecond(int darTot) {
        int x = darTot * 10 / 3;
        int z = (darTot * 100) % 3;
        int op = 15 * x + z;
        x = x * 10 + 100;
        op = op * x + 10000;
        return op / 10000;
    }

    private static EffectView strength(int tot, WeaponStats weapon) {
        if (weapon == null) {
            return new EffectView("screen.stat_upgrade.effect.str.weapon", new Object[]{});
        }
        return new EffectView(
                "screen.stat_upgrade.effect.str",
                new Object[]{strengthDamage(tot, weapon.damageX100(), weapon.attackSpeedX1000())}
        );
    }

    private static EffectView resistance(int tot) {
        if (tot <= 0) {
            return new EffectView("screen.stat_upgrade.effect.res.none", new Object[]{});
        }
        if (tot >= 17) {
            return new EffectView("screen.stat_upgrade.effect.res.immune", new Object[]{});
        }
        return new EffectView("screen.stat_upgrade.effect.res", new Object[]{2 * tot, 6 * tot});
    }

    private static EffectView hunCurrent(int tot) {
        if (tot <= -10) {
            return hunger(4, 1.5);
        }
        if (tot <= -8) {
            return hunger(3, 2.0);
        }
        if (tot <= -6) {
            return hunger(3, 2.5);
        }
        if (tot == -5) {
            return hunger(2, 2.75);
        }
        if (tot == -4) {
            return hunger(2, 3.25);
        }
        if (tot == -3) {
            return hunger(2, 4.0);
        }
        if (tot == -2) {
            return hunger(2, 5.0);
        }
        if (tot == -1) {
            return hunger(2, 6.25);
        }
        if (tot == 0) {
            return hunger(2, 7.5);
        }
        if (tot == 1) {
            return hunger(2, 8.75);
        }
        if (tot == 2) {
            return hunger(1, 10.0);
        }
        if (tot == 3) {
            return new EffectView(HUN_NONE, new Object[]{});
        }
        if (tot == 4) {
            return hunger(1, 12.5);
        }
        if (tot == 5) {
            return hunger(1, 15.0);
        }
        if (tot <= 7) {
            return new EffectView(HUN_NEUTRAL, new Object[]{});
        }
        if (tot == 8) {
            return sat(1, 125.0);
        }
        if (tot == 9) {
            return sat(1, 112.5);
        }
        if (tot == 10) {
            return sat(1, 100.0);
        }
        if (tot == 11) {
            return sat(1, 87.5);
        }
        if (tot == 12) {
            return sat(1, 75.0);
        }
        if (tot == 13) {
            return sat(1, 62.5);
        }
        if (tot == 14) {
            return sat(2, 56.0);
        }
        if (tot == 15) {
            return sat(2, 50.0);
        }
        return sat(2, 40.0);
    }

    private static EffectView hunger(int level, double seconds) {
        return new EffectView(HUN_HUNGER, new Object[]{level, seconds});
    }

    private static EffectView sat(int level, double seconds) {
        return new EffectView(HUN_SAT, new Object[]{level, seconds});
    }
}
