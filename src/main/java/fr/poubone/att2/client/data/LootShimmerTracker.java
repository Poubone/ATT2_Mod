package fr.poubone.att2.client.data;

import fr.poubone.att2.client.hud.WorldToHud;
import fr.poubone.att2.client.renderer.ItemRarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Plays a chime the first time a rare dropped item enters the player's view. */
public final class LootShimmerTracker {
    private static final Set<Integer> ANNOUNCED = new HashSet<>();
    private static int cooldown;

    private LootShimmerTracker() {
    }

    public static void reset() {
        ANNOUNCED.clear();
        cooldown = 0;
    }

    public static void tick(Minecraft client) {
        if (cooldown > 0) cooldown--;
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            ANNOUNCED.clear();
            return;
        }

        AABB box = player.getBoundingBox().inflate(24.0);
        Set<Integer> alive = new HashSet<>();
        for (ItemEntity entity : client.level.getEntitiesOfClass(ItemEntity.class, box)) {
            alive.add(entity.getId());
            ItemRarity rarity = ItemRarity.fromStack(entity.getItem());
            if (rarity == null || !rarity.shimmer) continue;
            if (ANNOUNCED.contains(entity.getId())) continue;
            if (!WorldToHud.isInView(entity.position().add(0, 0.35, 0), 24.0 * 24.0)) continue;
            ANNOUNCED.add(entity.getId());
            if (cooldown > 0) continue;
            cooldown = 8;
            client.level.playLocalSound(
                    entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS,
                    0.45f,
                    rarity.pitch,
                    false
            );
        }
        Iterator<Integer> it = ANNOUNCED.iterator();
        while (it.hasNext()) {
            if (!alive.contains(it.next())) it.remove();
        }
    }
}
