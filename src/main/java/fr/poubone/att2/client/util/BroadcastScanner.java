package fr.poubone.att2.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class BroadcastScanner {
	private static final Map<UUID, Long> seen = new HashMap<>();
	private static final long LIFESPAN_MS = 2000;

	public static void tick(Minecraft client) {
		if (client.level == null || client.player == null) {
			return;
		}

		long now = System.currentTimeMillis();

		for (Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof ArmorStand stand)) {
				continue;
			}
			if (stand.getCustomName() == null) {
				continue;
			}

			String name = stand.getCustomName().getString();
			if (!name.equals("[BROADCAST_TAG]")) {
				continue;
			}

			UUID id = stand.getUUID();
			if (seen.containsKey(id)) {
				continue;
			}

			ItemStack stack = stand.getMainHandItem();
			if (!stack.isEmpty()) {
				ChatItemUtils.sendItemInChat(stack);
				seen.put(id, now);
			}
		}

		Iterator<Map.Entry<UUID, Long>> it = seen.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Long> entry = it.next();
			if (now - entry.getValue() > LIFESPAN_MS) {
				it.remove();
			}
		}
	}
}
