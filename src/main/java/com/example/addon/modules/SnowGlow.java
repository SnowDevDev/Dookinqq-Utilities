package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

/**
 * SnowGlow - Animated gradient entity highlighting with glow effects.
 */
public class SnowGlow extends Module {

    private final SettingGroup sgGeneral  = settings.getDefaultGroup();
    private final SettingGroup sgGradient = settings.createGroup("Gradient");
    private final SettingGroup sgOutline  = settings.createGroup("Outline");
    private final SettingGroup sgGlow     = settings.createGroup("Glow");
    private final SettingGroup sgFill     = settings.createGroup("Fill");

    private final Setting<Boolean> targetPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("target-players").defaultValue(true).build());
    private final Setting<Boolean> targetMobs = sgGeneral.add(new BoolSetting.Builder()
        .name("target-mobs").defaultValue(true).build());
    private final Setting<Boolean> targetAnimals = sgGeneral.add(new BoolSetting.Builder()
        .name("target-animals").defaultValue(false).build());
    private final Setting<Boolean> targetAllLiving = sgGeneral.add(new BoolSetting.Builder()
        .name("target-all-living").defaultValue(false).build());
    private final Setting<Boolean> renderHands = sgGeneral.add(new BoolSetting.Builder()
        .name("render-hands").defaultValue(true).build());

    private final Setting<Boolean> gradientEnabled = sgGradient.add(new BoolSetting.Builder()
        .name("gradient").defaultValue(true).build());
    private final Setting<SettingColor> colorStart = sgGradient.add(new ColorSetting.Builder()
        .name("color-start").defaultValue(new SettingColor(0, 128, 255, 255)).build());
    private final Setting<SettingColor> colorEnd = sgGradient.add(new ColorSetting.Builder()
        .name("color-end").defaultValue(new SettingColor(0, 64, 128, 255)).build());
    private final Setting<Double> gradientSpeed = sgGradient.add(new DoubleSetting.Builder()
        .name("gradient-speed").defaultValue(0.5).min(0.05).sliderMax(3.0).build());

    private final Setting<Double> outlineWidth = sgOutline.add(new DoubleSetting.Builder()
        .name("outline-width").defaultValue(1.5).min(0.5).sliderMax(5.0).build());
    private final Setting<Integer> outlineAlpha = sgOutline.add(new IntSetting.Builder()
        .name("outline-alpha").defaultValue(255).range(0, 255).sliderMax(255).build());

    private final Setting<Boolean> glowEnabled = sgGlow.add(new BoolSetting.Builder()
        .name("glow").defaultValue(true).build());
    private final Setting<Integer> glowLayers = sgGlow.add(new IntSetting.Builder()
        .name("glow-layers").defaultValue(6).range(1, 16).sliderMax(16)
        .visible(glowEnabled::get).build());
    private final Setting<Double> glowSpread = sgGlow.add(new DoubleSetting.Builder()
        .name("glow-spread").defaultValue(0.02).min(0.005).sliderMax(0.1)
        .visible(glowEnabled::get).build());
    private final Setting<Integer> glowAlphaStart = sgGlow.add(new IntSetting.Builder()
        .name("glow-alpha-start").defaultValue(120).range(0, 255).sliderMax(255)
        .visible(glowEnabled::get).build());
    private final Setting<Boolean> glowOnFill = sgGlow.add(new BoolSetting.Builder()
        .name("glow-on-fill").defaultValue(false).visible(glowEnabled::get).build());

    private final Setting<Boolean> fillEnabled = sgFill.add(new BoolSetting.Builder()
        .name("fill").defaultValue(true).build());
    private final Setting<Integer> fillAlpha = sgFill.add(new IntSetting.Builder()
        .name("fill-alpha").defaultValue(40).range(0, 255).sliderMax(255)
        .visible(fillEnabled::get).build());

    private double animPhase = 0.0;
    private long lastTime = System.currentTimeMillis();
    private final Color workColor = new Color();

    public SnowGlow() {
        super(AddonTemplate.CATEGORY, "Dookinqq Glow",
              "Animated gradient outline + fill on entities and hands.");
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

        long now = System.currentTimeMillis();
        long delta = now - lastTime;
        lastTime = now;

        if (delta > 0) {
            animPhase = (animPhase + (delta / 1000.0 * gradientSpeed.get())) % 1.0;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !shouldRender(entity)) continue;
            renderEntity(event, entity);
        }
    }

    private void renderEntity(Render3DEvent event, Entity entity) {
        Box box = getFullEntityBox(entity, event.tickDelta);
        Color base = getCurrentColor();

        if (fillEnabled.get()) {
            if (glowEnabled.get() && glowOnFill.get()) {
                renderInnerGlow(event, box, base);
            }
            event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                withAlpha(base, fillAlpha.get()), null, ShapeMode.Sides, 0);
        }

        if (glowEnabled.get()) {
            renderOutlineGlow(event, box, base);
        }

        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
            null, withAlpha(base, outlineAlpha.get()), ShapeMode.Lines, 0);

        if (renderHands.get() && entity instanceof PlayerEntity player) {
            renderHandOutlines(event, player, base);
        }
    }

    private void renderOutlineGlow(Render3DEvent event, Box box, Color base) {
        int layers = glowLayers.get();
        double spread = glowSpread.get();
        int alphaStart = glowAlphaStart.get();

        for (int i = 1; i <= layers; i++) {
            double expansion = spread * i;
            float ratio = 1f - ((float) i / layers);
            int alpha = (int) (alphaStart * ratio * ratio);

            if (alpha <= 0) continue;

            event.renderer.box(
                box.minX - expansion, box.minY - expansion, box.minZ - expansion,
                box.maxX + expansion, box.maxY + expansion, box.maxZ + expansion,
                null, withAlpha(base, alpha), ShapeMode.Lines, 0);
        }
    }

    private void renderInnerGlow(Render3DEvent event, Box box, Color base) {
        int layers = glowLayers.get();
        double spread = glowSpread.get();
        int alphaStart = glowAlphaStart.get();

        for (int i = 1; i <= layers; i++) {
            double shrink = spread * i;
            float ratio = 1f - ((float) i / layers);
            int alpha = (int) (alphaStart * ratio * ratio * 0.6);

            if (alpha <= 0) break;

            event.renderer.box(
                box.minX + shrink, box.minY + shrink, box.minZ + shrink,
                box.maxX - shrink, box.maxY - shrink, box.maxZ - shrink,
                withAlpha(base, alpha), null, ShapeMode.Sides, 0);
        }
    }

    private void renderHandOutlines(Render3DEvent event, PlayerEntity player, Color base) {
        double x = net.minecraft.util.math.MathHelper.lerp(event.tickDelta, player.lastRenderX, player.getX());
        double y = net.minecraft.util.math.MathHelper.lerp(event.tickDelta, player.lastRenderY, player.getY());
        double z = net.minecraft.util.math.MathHelper.lerp(event.tickDelta, player.lastRenderZ, player.getZ());

        float bodyYaw = player.getBodyYaw();
        double yawRad = Math.toRadians(bodyYaw);
        double sinY = Math.sin(yawRad);
        double cosY = Math.cos(yawRad);

        final double ARM_WIDTH = 0.25, ARM_DEPTH = 0.25, ARM_HEIGHT = 0.75;
        final double SIDE_OFFSET = 0.35, FRONT_OFFSET = 0.15, BASE_HEIGHT = 0.75;

        double[][] armOffsets = { { -SIDE_OFFSET, BASE_HEIGHT }, { SIDE_OFFSET, BASE_HEIGHT } };
        Color outlineColor = withAlpha(base, outlineAlpha.get());

        for (double[] offset : armOffsets) {
            double lateral = offset[0];
            double armX = x + (-lateral * cosY + FRONT_OFFSET * (-sinY));
            double armZ = z + (-lateral * (-sinY) + FRONT_OFFSET * cosY);
            Box armBox = new Box(armX - ARM_WIDTH, y + BASE_HEIGHT, armZ - ARM_DEPTH,
                                armX + ARM_WIDTH, y + BASE_HEIGHT + ARM_HEIGHT, armZ + ARM_DEPTH);

            if (fillEnabled.get()) {
                event.renderer.box(armBox.minX, armBox.minY, armBox.minZ,
                    armBox.maxX, armBox.maxY, armBox.maxZ,
                    withAlpha(base, fillAlpha.get()), null, ShapeMode.Sides, 0);
            }

            if (glowEnabled.get()) {
                renderOutlineGlow(event, armBox, base);
            }
            event.renderer.box(armBox.minX, armBox.minY, armBox.minZ,
                armBox.maxX, armBox.maxY, armBox.maxZ,
                null, outlineColor, ShapeMode.Lines, 0);
        }
    }

    /**
     * FIXED: Gets the FULL entity bounding box, not just torso.
     */
    private Box getFullEntityBox(Entity entity, float tickDelta) {
        if (entity == null) {
            return new Box(0, 0, 0, 0, 0, 0);
        }

        double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
        float halfWidth = entity.getWidth() / 2f;
        float height = entity.getHeight();
        float inset = 0.02f;

        // Use the full bounding box
        return new Box(
            x - halfWidth + inset, y + inset, z - halfWidth + inset,
            x + halfWidth - inset, y + height - inset, z + halfWidth - inset);
    }

    private Color getCurrentColor() {
        if (!gradientEnabled.get()) {
            SettingColor s = colorStart.get();
            return workColor.set(s.r, s.g, s.b, 255);
        }

        float t = (float) Math.abs(Math.sin(animPhase * Math.PI));
        SettingColor start = colorStart.get();
        SettingColor end = colorEnd.get();

        return workColor.set(
            lerpInt(start.r, end.r, t),
            lerpInt(start.g, end.g, t),
            lerpInt(start.b, end.b, t),
            255);
    }

    private Color withAlpha(Color base, int alpha) {
        return new Color(base.r, base.g, base.b, alpha);
    }

    private int lerpInt(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    private boolean shouldRender(Entity entity) {
        if (entity == null || !entity.isAlive()) return false;

        if (targetPlayers.get() && entity instanceof PlayerEntity) return true;
        if (targetMobs.get() && entity instanceof net.minecraft.entity.mob.MobEntity) return true;
        if (targetAnimals.get() && entity instanceof net.minecraft.entity.passive.AnimalEntity) return true;
        if (targetAllLiving.get() && entity instanceof LivingEntity) return true;

        return false;
    }
}