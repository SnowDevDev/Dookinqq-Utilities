// src/main/java/com/example/addon/modules/SnowSpearKill.java
package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class SnowSpearKill extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTarget = settings.createGroup("Targeting");
    private final SettingGroup sgAim = settings.createGroup("Aim");
    private final SettingGroup sgBlink = settings.createGroup("Blink");
    private final SettingGroup sgLunge = settings.createGroup("Lunge");

    public enum Mode { Lunge, Blink }
    public enum TargetPriority { Closest, LowestHealth, Crosshair, HighestHealth }

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode").defaultValue(Mode.Lunge).build());

    private final Setting<Set<EntityType<?>>> entities = sgTarget.add(
        new EntityTypeListSetting.Builder()
            .name("entities").onlyAttackable().defaultValue(EntityType.PLAYER).build());

    private final Setting<Boolean> attackFriends = sgTarget.add(new BoolSetting.Builder()
        .name("attack-friends").defaultValue(false).build());

    private final Setting<Double> maxRange = sgTarget.add(new DoubleSetting.Builder()
        .name("max-range").defaultValue(128).min(1).sliderMax(512).build());

    private final Setting<Boolean> requireLos = sgTarget.add(new BoolSetting.Builder()
        .name("require-los").defaultValue(false).build());

    private final Setting<TargetPriority> priority = sgTarget.add(new EnumSetting.Builder<TargetPriority>()
        .name("priority").defaultValue(TargetPriority.Closest).build());

    private final Setting<Boolean> aimSmoothing = sgAim.add(new BoolSetting.Builder()
        .name("aim-smoothing").defaultValue(true).build());

    private final Setting<Double> smoothFactor = sgAim.add(new DoubleSetting.Builder()
        .name("smooth-factor").defaultValue(0.35).min(0).sliderMax(0.95)
        .visible(aimSmoothing::get).build());

    private final Setting<Boolean> prediction = sgAim.add(new BoolSetting.Builder()
        .name("prediction").defaultValue(true).build());

    private final Setting<Double> predictionFactor = sgAim.add(new DoubleSetting.Builder()
        .name("prediction-factor").defaultValue(0.85).min(0).sliderMax(2.0)
        .visible(prediction::get).build());

    private final Setting<Double> maxLead = sgAim.add(new DoubleSetting.Builder()
        .name("max-lead").defaultValue(2.6).min(0).sliderMax(8.0)
        .visible(prediction::get).build());

    private final Setting<Double> aimConfidence = sgAim.add(new DoubleSetting.Builder()
        .name("aim-confidence").description("Higher = stricter aim requirement before acting.")
        .defaultValue(0.35).min(0).sliderMax(1.0).build());

    private final Setting<Double> hitboxExpand = sgAim.add(new DoubleSetting.Builder()
        .name("hitbox-expand").defaultValue(0.25).min(0).sliderMax(1.5).build());

    private final Setting<Integer> maxBuffered = sgBlink.add(new IntSetting.Builder()
        .name("max-buffered").defaultValue(240).min(20).sliderMax(800)
        .visible(() -> mode.get() == Mode.Blink).build());

    private final Setting<Integer> flushAtTicks = sgBlink.add(new IntSetting.Builder()
        .name("flush-at-ticks").defaultValue(18).min(1).sliderMax(60)
        .visible(() -> mode.get() == Mode.Blink).build());

    private final Setting<Double> lungeStrength = sgLunge.add(new DoubleSetting.Builder()
        .name("lunge-strength").defaultValue(5.0).min(0).sliderMax(12)
        .visible(() -> mode.get() == Mode.Lunge).build());

    private final Setting<Boolean> clampVertical = sgLunge.add(new BoolSetting.Builder()
        .name("clamp-vertical").defaultValue(true)
        .visible(() -> mode.get() == Mode.Lunge).build());

    private final Setting<Double> maxVerticalFrac = sgLunge.add(new DoubleSetting.Builder()
        .name("max-vertical-frac").defaultValue(0.4).min(0.05).sliderMax(1.0)
        .visible(() -> mode.get() == Mode.Lunge && clampVertical.get()).build());

    @SuppressWarnings("unused")
    private final List<PlayerMoveC2SPacket> buffer = new ArrayList<>();
    private Entity target;
    private int chargeTicks;
    private boolean wasUsing;
    private float smoothYaw, smoothPitch;
    private boolean aimInit;

    public SnowSpearKill() {
        super(AddonTemplate.CATEGORY, "Dookinqq Spear Kill", "Accurate spear/trident kill with safer blink + better aim.");
    }

    @Override
    public void onActivate() {
        buffer.clear();
        target = null;
        chargeTicks = 0;
        wasUsing = false;
        aimInit = false;
    }

    @Override
    public void onDeactivate() {
        flushBuffer();
        buffer.clear();
        target = null;
        chargeTicks = 0;
        wasUsing = false;
        aimInit = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre e) {
        if (mc.player == null || mc.world == null) return;

        target = findTarget();
        boolean using = mc.player.isUsingItem() && isSpearLike(mc.player.getMainHandStack());

        if (using) chargeTicks++;
        else chargeTicks = 0;

        if (target != null && using) {
            rotateToTarget(target);
            if (mode.get() == Mode.Lunge) tryLunge(target);
        }

        if (mode.get() == Mode.Blink) {
            if (using && !wasUsing) buffer.clear();
            if (!using && wasUsing) flushBuffer();
            if (using && chargeTicks >= flushAtTicks.get()) flushBuffer();
        }

        wasUsing = using;
    }

    @EventHandler
    private void onSend(PacketEvent.Send e) {
        if (mode.get() != Mode.Blink) return;
        if (mc.player == null) return;

        if (e.packet instanceof PlayerMoveC2SPacket p) {
            if (mc.player.isUsingItem() && isSpearLike(mc.player.getMainHandStack())) {
                e.cancel();
                buffer.add(p);
                if (buffer.size() > maxBuffered.get()) buffer.remove(0);
            }
        }
    }

    private void flushBuffer() {
        if (mc.getNetworkHandler() == null) return;
        for (PlayerMoveC2SPacket p : buffer) mc.getNetworkHandler().sendPacket(p);
        buffer.clear();
    }

    private void tryLunge(Entity t) {
        if (mc.player == null) return;
        if (!isAimConfident(t)) return;

        Vec3d dir = buildAimVector(t);
        double speed = lungeStrength.get();

        if (clampVertical.get()) {
            double yAbs = Math.abs(dir.y);
            double maxY = maxVerticalFrac.get();
            if (yAbs > maxY) {
                double sign = Math.signum(dir.y);
                dir = new Vec3d(dir.x, sign * maxY, dir.z).normalize();
            }
        }

        mc.player.setSprinting(true);
        mc.player.setVelocity(dir.multiply(speed));
    }

    private boolean isAimConfident(Entity t) {
        if (mc.player == null) return false;

        Vec3d look = mc.player.getRotationVec(1.0f).normalize();
        Vec3d to = getAimPos(t).subtract(mc.player.getEyePos()).normalize();

        double dot = look.dotProduct(to);
        double conf = (dot + 1.0) * 0.5;
        return conf >= aimConfidence.get();
    }

    private Vec3d buildAimVector(Entity t) {
        if (mc.player == null) return new Vec3d(0, 0, 1);
        Vec3d from = mc.player.getEyePos();
        Vec3d to = getAimPos(t);
        return to.subtract(from).normalize();
    }

    private Vec3d getAimPos(Entity t) {
        Box b = t.getBoundingBox().expand(hitboxExpand.get());
        Vec3d center = b.getCenter();

        if (!prediction.get()) return center;
        if (!(t instanceof LivingEntity le)) return center;

        Vec3d vel = le.getVelocity();
        double dist = mc.player != null ? mc.player.distanceTo(t) : 0;

        double lead = Math.min(maxLead.get(), dist * 0.02) * predictionFactor.get();
        return center.add(vel.x * lead, vel.y * lead * 0.5, vel.z * lead);
    }

    private void rotateToTarget(Entity t) {
        if (mc.player == null) return;

        Vec3d eye = mc.player.getEyePos();
        Vec3d aim = getAimPos(t);

        double dx = aim.x - eye.x;
        double dy = aim.y - eye.y;
        double dz = aim.z - eye.z;

        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (MathHelper.atan2(dz, dx) * 57.295776f) - 90f;
        float pitch = (float) (-(MathHelper.atan2(dy, distXZ) * 57.295776f));

        if (!aimSmoothing.get()) {
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
            return;
        }

        if (!aimInit) {
            smoothYaw = mc.player.getYaw();
            smoothPitch = mc.player.getPitch();
            aimInit = true;
        }

        float tLerp = (float) (1.0 - smoothFactor.get());
        smoothYaw = lerpAngle(smoothYaw, yaw, tLerp);
        smoothPitch = MathHelper.lerp(tLerp, smoothPitch, pitch);

        mc.player.setYaw(smoothYaw);
        mc.player.setPitch(smoothPitch);
    }

    private float lerpAngle(float a, float b, float t) {
        float d = MathHelper.wrapDegrees(b - a);
        return a + d * t;
    }

    private Entity findTarget() {
        if (mc.player == null || mc.world == null) return null;

        List<Entity> list = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (!isValidListTarget(e)) continue;
            if (!attackFriends.get() && e instanceof PlayerEntity pe && Friends.get().isFriend(pe)) continue;
            if (!isValidTarget(e)) continue;
            list.add(e);
        }

        if (list.isEmpty()) return null;

        return switch (priority.get()) {
            case Closest -> list.stream().min(Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.player))).orElse(null);
            case LowestHealth -> list.stream().min(Comparator.comparingDouble(this::healthOf)).orElse(null);
            case HighestHealth -> list.stream().max(Comparator.comparingDouble(this::healthOf)).orElse(null);
            case Crosshair -> list.stream().min(Comparator.comparingDouble(this::crosshairScore)).orElse(null);
        };
    }

    private double healthOf(Entity e) {
        if (e instanceof LivingEntity le) return le.getHealth();
        return Double.MAX_VALUE;
    }

    private double crosshairScore(Entity e) {
        if (mc.player == null) return Double.MAX_VALUE;
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d to = e.getBoundingBox().getCenter().subtract(mc.player.getEyePos()).normalize();
        return 1.0 - look.dotProduct(to);
    }

    private boolean isValidListTarget(Entity entity) {
        return entities.get().contains(entity.getType())
            && entity.isAlive()
            && entity.isAttackable()
            && !entity.isInvulnerable()
            && entity != mc.player;
    }

    private boolean isValidTarget(Entity entity) {
        if (!isValidListTarget(entity)) return false;
        if (mc.player.distanceTo(entity) > maxRange.get()) return false;
        if (requireLos.get() && !canSeeTarget(entity)) return false;
        return true;
    }

    private boolean canSeeTarget(Entity entity) {
        if (mc.player == null || mc.world == null) return false;

        Vec3d from = mc.player.getEyePos();
        Vec3d to = entity.getBoundingBox().getCenter();
        var hit = mc.world.raycast(new RaycastContext(from, to,
            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));

        if (hit.getType() == HitResult.Type.MISS) return true;
        return from.distanceTo(hit.getPos()) >= from.distanceTo(to) - 0.35;
    }

    private boolean isSpearLike(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String name = stack.getItem().toString().toLowerCase();
        return name.contains("spear") || name.contains("trident");
    }
}
