package fr.poubone.att2.client.data;

import fr.poubone.att2.client.hud.ChronotonDisplay;
import fr.poubone.att2.client.hud.HudFx;
import fr.poubone.att2.client.hud.WorldToHud;
import fr.poubone.att2.client.renderer.ItemRarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Spawns flying coin particles when a chronoton nugget is picked up. */
public final class ChronotonPickupTracker {
    private static final Map<Integer, Vec3> LAST_POS = new HashMap<>();

    private ChronotonPickupTracker() {
    }

    public static void reset() {
        LAST_POS.clear();
    }

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            LAST_POS.clear();
            return;
        }

        AABB box = player.getBoundingBox().inflate(18.0);
        Set<Integer> seen = new HashSet<>();
        for (ItemEntity entity : client.level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (!isChronoton(entity.getItem())) continue;
            seen.add(entity.getId());
            LAST_POS.put(entity.getId(), entity.position().add(0, 0.25, 0));
        }

        Iterator<Map.Entry<Integer, Vec3>> it = LAST_POS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Vec3> entry = it.next();
            if (seen.contains(entry.getKey())) continue;
            Vec3 pos = entry.getValue();
            it.remove();
            if (player.distanceToSqr(pos) > 9.0) continue;
            WorldToHud.project(pos).ifPresent(screen -> {
                int n = 3 + (int) (Math.random() * 3);
                for (int i = 0; i < n; i++) {
                    HudFx.flyingCoin(
                            screen[0] + (float) (Math.random() - 0.5) * 10,
                            screen[1] + (float) (Math.random() - 0.5) * 10,
                            ChronotonDisplay.iconX,
                            ChronotonDisplay.iconY
                    );
                }
            });
        }
    }

    private static boolean isChronoton(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.GOLD_NUGGET) || stack.is(Items.GOLD_INGOT)) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data == null || data.isEmpty()) return false;
            if (data.copyTag().getString("Coin").isPresent()) return true;
            ItemRarity rarity = ItemRarity.fromStack(stack);
            return rarity == ItemRarity.CUR;
        }
        return false;
    }
}
