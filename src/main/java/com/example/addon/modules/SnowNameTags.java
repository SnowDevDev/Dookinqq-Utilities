package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SnowNameTags extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scaling");
    private final SettingGroup sgVisual = settings.createGroup("Visual");

    // IMPORTANT: used by the mixin
    public final Setting<Boolean> hideVanilla = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-vanilla")
        .description("Hides Minecraft's default nameplates while this module is active.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> animated = sgGeneral.add(new BoolSetting.Builder().name("animated").defaultValue(true).build());
    private final Setting<Boolean> glow = sgGeneral.add(new BoolSetting.Builder().name("glow").defaultValue(true).build());
    private final Setting<Boolean> healthBar = sgGeneral.add(new BoolSetting.Builder().name("health-bar").defaultValue(true).build());
    private final Setting<Boolean> distance = sgGeneral.add(new BoolSetting.Builder().name("distance").defaultValue(true).build());
    private final Setting<Boolean> ping = sgGeneral.add(new BoolSetting.Builder().name("ping").defaultValue(false).build());

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder().name("scale").defaultValue(1.0).min(0.5).sliderMax(3).build());
    private final Setting<Boolean> distanceScale = sgScale.add(new BoolSetting.Builder().name("distance-scale").defaultValue(true).build());
    private final Setting<Double> minScale = sgScale.add(new DoubleSetting.Builder().name("min-scale").defaultValue(0.6).min(0.1).sliderMax(1).build());
    private final Setting<Double> maxScaleDist = sgScale.add(new DoubleSetting.Builder().name("max-distance").defaultValue(50).min(1).sliderMax(200).build());

    private final Setting<SettingColor> nameColor = sgVisual.add(new ColorSetting.Builder()
        .name("name-color")
        .defaultValue(new SettingColor(140, 200, 255, 255))
        .build()
    );

    private final Setting<Double> glowAlpha = sgVisual.add(new DoubleSetting.Builder()
        .name("glow-alpha").defaultValue(0.25).min(0).sliderMax(1.0)
        .visible(glow::get).build()
    );

    private final Setting<Double> glowSize = sgVisual.add(new DoubleSetting.Builder()
        .name("glow-size").defaultValue(6).min(0).sliderMax(16)
        .visible(glow::get).build()
    );

    private final List<PlayerEntity> players = new ArrayList<>();
    private final Vector3d pos = new Vector3d();

    public SnowNameTags() {
        super(AddonTemplate.CATEGORY, "Dookinqq Name Tags", "Custom nametags with glow + bars.");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.world == null || mc.player == null) return;

        players.clear();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && !p.isRemoved()) players.add(p);
        }
        players.sort(Comparator.comparingDouble(p -> -mc.player.distanceTo(p)));

        TextRenderer text = TextRenderer.get();
        double time = System.currentTimeMillis() / 500.0;

        for (PlayerEntity player : players) {
            double x = MathHelper.lerp(event.tickDelta, player.lastRenderX, player.getX());
            double y = MathHelper.lerp(event.tickDelta, player.lastRenderY, player.getY());
            double z = MathHelper.lerp(event.tickDelta, player.lastRenderZ, player.getZ());

            pos.set(x, y + player.getEyeHeight(player.getPose()) + 0.6, z);

            double dist = mc.player.distanceTo(player);
            double finalScale = scale.get();

            if (distanceScale.get()) {
                double factor = 1 - MathHelper.clamp(dist / maxScaleDist.get(), 0, 1);
                finalScale *= (minScale.get() + (1 - minScale.get()) * factor);
            }

            if (!NametagUtils.to2D(pos, finalScale)) continue;

            NametagUtils.begin(pos, event.drawContext);
            text.beginBig();

            String name = player.getName().getString();
            if (Friends.get().isFriend(player)) name = "§b[F] §r" + name;

            double width = text.getWidth(name, true);
            double height = text.getHeight(true);
            double drawX = -width / 2;
            double drawY = -height;

            SettingColor sc = nameColor.get();
            Color base = new Color(sc.r, sc.g, sc.b, sc.a);

            if (animated.get()) {
                double pulse = (Math.sin(time + player.getId() * 0.7) * 0.5 + 0.5);
                base = new Color(base.r, base.g, base.b, (int) (180 + 75 * pulse));
            }

            if (glow.get()) {
                double gs = glowSize.get();
                int ga = (int) (255 * glowAlpha.get());
                Color glowC = new Color(base.r, base.g, base.b, ga);

                Renderer2D.COLOR.begin();
                Renderer2D.COLOR.quad(drawX - gs, drawY - gs, width + gs * 2, height + gs * 2, glowC);
                Renderer2D.COLOR.render();

                text.render(name, drawX + 1, drawY, new Color(base.r, base.g, base.b, 90), true);
                text.render(name, drawX - 1, drawY, new Color(base.r, base.g, base.b, 90), true);
                text.render(name, drawX, drawY + 1, new Color(base.r, base.g, base.b, 90), true);
                text.render(name, drawX, drawY - 1, new Color(base.r, base.g, base.b, 90), true);
            }

            text.render(name, drawX, drawY, base, true);

            if (healthBar.get()) {
                float hp = player.getHealth();
                float max = Math.max(1f, player.getMaxHealth());
                float pct = MathHelper.clamp(hp / max, 0f, 1f);

                double barY = drawY + height + 2;
                double barW = width;

                int rr = (int) ((1 - pct) * 255);
                int gg = (int) (pct * 255);

                Renderer2D.COLOR.begin();
                Renderer2D.COLOR.quad(drawX, barY, barW, 2, new Color(20, 20, 20, 180));
                Renderer2D.COLOR.quad(drawX, barY, barW * pct, 2, new Color(rr, gg, 0, 220));
                Renderer2D.COLOR.render();
            }

            double offset = drawX + width;

            if (distance.get()) {
                String d = String.format(" %.0fm", dist);
                text.render(d, offset, drawY, base, true);
                offset += text.getWidth(d, true);
            }

            if (ping.get() && mc.getNetworkHandler() != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
                if (entry != null) {
                    String pingText = " " + entry.getLatency() + "ms";
                    text.render(pingText, offset, drawY, base, true);
                }
            }

            text.end();
            NametagUtils.end(event.drawContext);
        }
    }
}
