package fr.poubone.att2.client.data;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Reads the same modifiers {@code strength/effect.mcfunction} uses:
 * {@code minecraft:attack_damage} ×100 and {@code minecraft:attack_speed} ×1000.
 */
public final class HeldWeaponStats {
    private static final Identifier DAMAGE = Identifier.withDefaultNamespace("attack_damage");
    private static final Identifier SPEED = Identifier.withDefaultNamespace("attack_speed");

    private HeldWeaponStats() {}

    public static StatEffectText.WeaponStats of(Player player) {
        if (player == null) {
            return null;
        }
        AttributeModifier damage = modifier(player.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE);
        AttributeModifier speed = modifier(player.getAttribute(Attributes.ATTACK_SPEED), SPEED);
        if (damage == null || speed == null) {
            return null;
        }
        return new StatEffectText.WeaponStats(scaled(damage.amount(), 100), scaled(speed.amount(), 1000));
    }

    private static AttributeModifier modifier(AttributeInstance instance, Identifier id) {
        return instance == null ? null : instance.getModifier(id);
    }

    private static int scaled(double value, int scale) {
        return (int) (value * scale);
    }
}
