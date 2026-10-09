package fr.poubone.att2.client.renderer;

import com.lootbeams.compat.iris.IrisCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.mixin.OutlineBufferSourceAccessor;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * HUD config {@code directItemOutlines}: a glowing dropped item's outline vertices are written straight into the
 * outline buffer, the three values the outline keeps (position, outline colour, texture position), as vanilla's
 * outline generator writes them. The usual path builds full item vertices and converts each quad to the outline
 * format, which with Sodium costs a format lookup and a conversion per quad.
 */
public final class ItemOutlines {
    private static final boolean IRIS = IrisCompat.isIrisLoaded();
    private static final Vector3f position = new Vector3f();

    private ItemOutlines() {
    }

    /** Draws the outline of one item submit; false leaves it to the usual path. */
    public static boolean draw(OutlineBufferSource source, ItemDisplayContext context, PoseStack.Pose pose, List<BakedQuad> quads,
                               RenderType renderType) {
        if (context != ItemDisplayContext.GROUND || !HUDConfig.get().directItemOutlines || IRIS && IrisCompat.isShaderPackInUse()) {
            return false;
        }
        RenderType outlineType = renderType.isOutline() ? renderType : renderType.outline().orElse(null);
        if (outlineType == null) {
            return false; // the usual path reports this
        }
        OutlineBufferSourceAccessor outline = (OutlineBufferSourceAccessor) source;
        VertexConsumer buffer = outline.att2$buffers().getBuffer(outlineType);
        int color = outline.att2$color();
        Matrix4f matrix = pose.pose();
        for (int i = 0, n = quads.size(); i < n; i++) {
            BakedQuad quad = quads.get(i);
            for (int corner = 0; corner < 4; corner++) {
                matrix.transformPosition(quad.position(corner), position);
                long uv = quad.packedUV(corner);
                buffer.addVertex(position.x(), position.y(), position.z());
                buffer.setColor(color);
                buffer.setUv(UVPair.unpackU(uv), UVPair.unpackV(uv));
            }
        }
        return true;
    }
}
