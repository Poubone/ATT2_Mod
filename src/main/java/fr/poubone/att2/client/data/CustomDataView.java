package fr.poubone.att2.client.data;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.mixin.CustomDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;

/**
 * An item's custom_data for reading. With HUD config {@code lightItemChecks} on it is the item's own tag, which the
 * per-frame checks on every dropped item would otherwise copy each time; callers must never change it.
 */
public final class CustomDataView {
    private CustomDataView() {
    }

    public static CompoundTag read(CustomData data) {
        return HUDConfig.get().lightItemChecks ? ((CustomDataAccessor) (Object) data).att2$tag() : data.copyTag();
    }
}
