package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.NameProtect;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.util.*;

public class SnowNameTags extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<SettingColor> nameColor = sgRender.add(((ColorSetting.Builder)((ColorSetting.Builder)(new ColorSetting.Builder())
        .name("name-color"))
        .description("The color of the nametag names."))
        .defaultValue(new SettingColor())
        .build()
    );

    private final Color RED = new Color(255, 25, 25);
    private final Color GREEN = new Color(25, 255, 25);
    private final Color AMBER = new Color(255, 105, 25);
    private final Color background = new Color(50, 50, 50, 255);

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("Scale")
        .defaultValue(1F)
        .build()
    );

    private final List<Entity> entityList = new ArrayList<Entity>();
    private final Setting<Boolean> culling = sgGeneral.add(new BoolSetting.Builder()
        .name("Culling")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> maxCullCount = sgGeneral.add(new IntSetting.Builder()
        .name("Max Cull Count")
        .defaultValue(20)
        .build()
    );

    private final Setting<Boolean> displayHealth = sgPlayers.add(((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)(new BoolSetting.Builder()).name("health")).description("Shows the player's health.")).defaultValue(true)).build());;

    private final Vector3d pos;


    public SnowNameTags() {
        super(AddonTemplate.CATEGORY, "Snow Name Tags", "clarity nametag fr");
        this.pos = new Vector3d();
    }

    private int getRenderCount() {
        int count = (Boolean)this.culling.get() ? (Integer)this.maxCullCount.get() : this.entityList.size();
        count = MathHelper.clamp(count, 0, this.entityList.size());
        return count;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        this.entityList.clear();
        Vec3d cameraPos = this.mc.gameRenderer.getCamera().getPos();

        assert this.mc.world !=
            null;
        for (Entity entity : this.mc.world.getEntities())
            if (entity instanceof PlayerEntity && entity != mc.player) {
                this.entityList.add(entity);
            }

        this.entityList.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(cameraPos)));
    }

    private void drawBg(double x, double y, double width, double height) {
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x - (double)1.0F, y - (double)1.0F, width + (double)2.0F, height + (double)2.0F, (Color)background);
        Renderer2D.COLOR.quad(x, y, width, height, new Color(30, 30, 30, 255));
        Renderer2D.COLOR.quad(x, y - 2, width, 2, new Color(177, 205, 255, 255));
        Renderer2D.COLOR.render((MatrixStack)null);

    }

    private void renderNametagPlayer(Render2DEvent event, PlayerEntity player, boolean shadow) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(this.pos);
        Color nameColor = PlayerUtils.getPlayerColor(player, (Color) this.nameColor.get());
        String name = player == this.mc.player ? ((NameProtect) Modules.get().get(NameProtect.class)).getName(player.getName().getString()) : player.getName().getString();
        float absorption = player.getAbsorptionAmount();
        int health = Math.round(player.getHealth() + absorption);
        double healthPercentage = (double) health / (player.getMaxHealth() + absorption);
        String healthText = " " + health;
        Color healthColor = healthPercentage <= 0.333 ? this.RED : (healthPercentage <= 0.666 ? this.AMBER : this.GREEN);
        double nameWidth = text.getWidth(name, shadow);
        double healthWidth = text.getWidth(healthText, shadow);
        double width = nameWidth + ((Boolean) this.displayHealth.get() ? healthWidth : 0);
        double widthHalf = width / 2.0;
        double height = text.getHeight(shadow);
        text.beginBig();
        double hX = -widthHalf;
        double hY = -height;
        double heightDown = text.getHeight(shadow);
        drawBg(-widthHalf, -heightDown, width, heightDown);
        hX = text.render(name, hX, hY, nameColor, shadow);
        if ((Boolean) this.displayHealth.get()) {
            text.render(healthText, hX, hY, healthColor, shadow);
        }
        text.end();
        NametagUtils.end();
    }


    private double getHeight(Entity entity) {
        double height = (double)entity.getEyeHeight(entity.getPose());
        if (entity.getType() != EntityType.ITEM && entity.getType() != EntityType.ITEM_FRAME) {
            height += (double)0.5F;
        } else {
            height += 0.2;
        }

        return height;
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        int count = this.getRenderCount();
        boolean shadow = (Boolean) Config.get().customFont.get();
        for(int i = count - 1; i > -1; --i) {
            Entity entity = (Entity)this.entityList.get(i);
            Utils.set(this.pos, entity, (double)event.tickDelta);
            this.pos.add((double)0.0F, this.getHeight(entity), (double)0.0F);
            EntityType<?> type = entity.getType();
            if (!NametagUtils.to2D(this.pos, (Double)this.scale.get())) continue;
            if (type == EntityType.PLAYER) {
                this.renderNametagPlayer(event, (PlayerEntity)entity, shadow);
            }
        }
    }

}
