package fr.poubone.att2.client.sync;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

/** Long-distance client raycast for pings, independent from normal interaction reach. */
public final class PingTargeting {
    /** Far enough to cover rendered terrain while avoiding an unbounded entity query. */
    private static final double MAX_DISTANCE = 1_024.0;

    private PingTargeting() {
    }

    public static Target lookAt(Minecraft client) {
        if (client.player == null || client.level == null) {
            return null;
        }
        Vec3 start = client.player.getEyePosition();
        Vec3 direction = client.player.getViewVector(1.0F).normalize();
        Vec3 end = start.add(direction.scale(MAX_DISTANCE));

        BlockHitResult blockHit = client.level.clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player));
        double nearest = blockHit.getType() == HitResult.Type.MISS
                ? MAX_DISTANCE * MAX_DISTANCE
                : start.distanceToSqr(blockHit.getLocation());
        Target best = blockHit.getType() == HitResult.Type.MISS ? null : new Target(
                blockHit.getLocation(),
                client.level.getBlockState(blockHit.getBlockPos()).getBlock().getName().getString(), null, ItemStack.EMPTY);

        AABB scan = client.player.getBoundingBox().expandTowards(direction.scale(MAX_DISTANCE)).inflate(1.0);
        for (Entity entity : client.level.getEntities(client.player, scan,
                candidate -> candidate.isAlive() && (candidate.isPickable() || candidate instanceof ItemEntity))) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(0.3).clip(start, end);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(hit.get());
            if (distance >= nearest) {
                continue;
            }
            nearest = distance;
            String label = entity instanceof ItemEntity item
                    ? item.getItem().getHoverName().getString()
                    : entity.getDisplayName().getString();
            best = new Target(entity.position().add(0, entity.getBbHeight(), 0), label, entity.getUUID(),
                    entity instanceof ItemEntity item ? item.getItem().copy() : ItemStack.EMPTY);
        }
        return best;
    }

    public record Target(Vec3 pos, String label, UUID entityId, ItemStack item) {
    }
}
