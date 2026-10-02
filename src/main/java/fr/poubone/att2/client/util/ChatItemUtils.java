package fr.poubone.att2.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;

public class ChatItemUtils {
	public static void sendItemInChat(ItemStack originalStack, String label) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || originalStack.isEmpty()) {
			return;
		}

		player.displayClientMessage(message(originalStack, label), false);
	}

	/** Keep the actual components: getString() loses custom colours and translated names. */
	public static Component message(ItemStack originalStack, String label) {
		ItemStack snapshot = originalStack.copy();
		Component item = snapshot.getDisplayName().copy()
			.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowItem(snapshot)));
		return label == null || label.isBlank() ? item : Component.literal(label + " ").append(item);
	}

	public static void sendItemInChat(ItemStack stack) {
		sendItemInChat(stack, "");
	}
}
