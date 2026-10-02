package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.util.ModTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lightweight HUD particles: sparks, XP motes that home in, floating stat deltas, flying chronoton coins.
 */
public final class HudFx {
    private static final List<Particle> PARTICLES = new ArrayList<>();
    private static long lastPickupFxMs = 0L;

    private HudFx() {
    }

    public static void reset() {
        PARTICLES.clear();
        lastPickupFxMs = 0L;
    }

    public static void tick() {
        Iterator<Particle> it = PARTICLES.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.age++;
            if (p.age >= p.life) {
                it.remove();
                continue;
            }
            if (p.homing) {
                float t = p.age / (float) p.life;
                float ease = t * t * (3f - 2f * t);
                p.x = Mth.lerp(ease, p.sx, p.tx);
                p.y = Mth.lerp(ease, p.sy, p.ty);
                p.x += (float) Math.sin((p.age + p.seed) * 0.35f) * (1f - t) * 4f;
            } else {
                p.x += p.vx;
                p.y += p.vy;
                p.vy += p.gravity;
                p.vx *= 0.96f;
                p.vy *= 0.96f;
            }
        }
    }

    public static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        for (Particle p : PARTICLES) {
            float t = p.age / (float) p.life;
            int alpha = (int) ((1f - t) * p.alpha * 255f);
            if (alpha <= 4) continue;
            int color = (alpha << 24) | (p.rgb & 0xFFFFFF);

            if (p.kind == Kind.TEXT) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(p.x, p.y);
                float scale = 0.9f + (1f - t) * 0.25f;
                graphics.pose().scale(scale, scale);
                graphics.drawString(font, p.text, 0, 0, color, false);
                graphics.pose().popMatrix();
                continue;
            }

            if (p.kind == Kind.COIN) {
                float scale = 0.7f + (1f - t) * 0.35f;
                graphics.pose().pushMatrix();
                graphics.pose().translate(p.x, p.y);
                graphics.pose().scale(scale, scale);
                graphics.renderItem(COIN, -8, -8);
                graphics.pose().popMatrix();
                continue;
            }

            Identifier texture = switch (p.kind) {
                case BUBBLE -> ModTextures.FX_BUBBLE;
                case XP -> ModTextures.FX_XP_MOTE;
                default -> ModTextures.FX_SPARK;
            };
            int size = p.size;
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                    (int) p.x - size / 2, (int) p.y - size / 2,
                    0f, 0f, size, size, size, size, color);
        }
    }

    public static void spark(float x, float y, int rgb) {
        Particle p = new Particle(Kind.SPARK);
        p.x = x;
        p.y = y;
        p.vx = (float) ((Math.random() - 0.5) * 2.4);
        p.vy = (float) ((Math.random() - 0.5) * 2.4);
        p.life = 12 + (int) (Math.random() * 10);
        p.rgb = rgb;
        p.size = 6 + (int) (Math.random() * 5);
        PARTICLES.add(p);
    }

    public static void bubble(float x, float y) {
        Particle p = new Particle(Kind.BUBBLE);
        p.x = x;
        p.y = y;
        p.vx = (float) ((Math.random() - 0.5) * 0.4);
        p.vy = -0.35f - (float) Math.random() * 0.45f;
        p.life = 28 + (int) (Math.random() * 18);
        p.rgb = 0xB8F0FF;
        p.size = 4 + (int) (Math.random() * 5);
        PARTICLES.add(p);
    }

    public static void convergeXp(float fromX, float fromY, float toX, float toY) {
        Particle p = new Particle(Kind.XP);
        p.sx = fromX;
        p.sy = fromY;
        p.x = fromX;
        p.y = fromY;
        p.tx = toX;
        p.ty = toY;
        p.homing = true;
        p.life = 16 + (int) (Math.random() * 10);
        p.rgb = 0x4DFF4D;
        p.size = 6 + (int) (Math.random() * 4);
        p.seed = (float) (Math.random() * 40);
        PARTICLES.add(p);
    }

    public static void floatingText(float x, float y, String text, int rgb) {
        Particle p = new Particle(Kind.TEXT);
        p.x = x;
        p.y = y;
        p.vy = -0.55f;
        p.life = 28;
        p.rgb = rgb;
        p.text = text;
        p.alpha = 1f;
        PARTICLES.add(p);
    }

    public static void flyingCoin(float fromX, float fromY, float toX, float toY) {
        Particle p = new Particle(Kind.COIN);
        p.sx = fromX;
        p.sy = fromY;
        p.x = fromX;
        p.y = fromY;
        p.tx = toX;
        p.ty = toY;
        p.homing = true;
        p.life = 14 + (int) (Math.random() * 8);
        p.seed = (float) (Math.random() * 30);
        lastPickupFxMs = System.currentTimeMillis();
        PARTICLES.add(p);
    }

    public static boolean recentlySpawnedPickupFx() {
        return System.currentTimeMillis() - lastPickupFxMs < 400L;
    }

    public static void burstToward(float toX, float toY, int count, int rgb, boolean xp) {
        Minecraft client = Minecraft.getInstance();
        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();
        for (int i = 0; i < count; i++) {
            float angle = (float) (Math.random() * Math.PI * 2);
            float dist = 28 + (float) Math.random() * Math.min(sw, sh) * 0.28f;
            float fx = toX + Mth.cos(angle) * dist;
            float fy = toY + Mth.sin(angle) * dist;
            if (xp) {
                convergeXp(fx, fy, toX, toY);
            } else {
                spark(fx, fy, rgb);
            }
        }
    }

    private static final ItemStack COIN = new ItemStack(Items.GOLD_NUGGET);

    private enum Kind {SPARK, BUBBLE, XP, TEXT, COIN}

    private static final class Particle {
        final Kind kind;
        float x, y, vx, vy, sx, sy, tx, ty, seed;
        float gravity = 0f;
        float alpha = 1f;
        int rgb = 0xFFFFFF;
        int size = 8;
        int age, life = 20;
        boolean homing;
        String text = "";

        Particle(Kind kind) {
            this.kind = kind;
        }
    }
}
