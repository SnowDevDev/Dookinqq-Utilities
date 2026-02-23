package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Vec3d;

public class SnowSpeed extends Module {

    public SnowSpeed() {
        super(AddonTemplate.CATEGORY, "Dookinqq Speed", "Accelerates your walking speed.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgGrim = settings.createGroup("Grim");
    private final SettingGroup sgNcp = settings.createGroup("NCP");

    public enum Mode {
        GrimStrafe,
        NcpStrafe
    }

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .defaultValue(Mode.GrimStrafe)
        .onChanged(m -> reset())
        .build()
    );

    // Grim
    private final Setting<Boolean> diagonal = sgGrim.add(new BoolSetting.Builder()
        .name("diagonal")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.GrimStrafe)
        .build()
    );

    private final Setting<Double> grimBoatBoost = sgGrim.add(new DoubleSetting.Builder()
        .name("boat-boost")
        .defaultValue(0.4)
        .min(0.0)
        .max(1.7)
        .sliderRange(0.0, 1.7)
        .visible(() -> mode.get() == Mode.GrimStrafe)
        .build()
    );

    // NCP
    private final Setting<Boolean> strict = sgNcp.add(new BoolSetting.Builder()
        .name("strict")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.NcpStrafe)
        .build()
    );

    private final Setting<Boolean> lowerJump = sgNcp.add(new BoolSetting.Builder()
        .name("lower-jump")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.NcpStrafe)
        .build()
    );

    private final Setting<Boolean> autoJump = sgNcp.add(new BoolSetting.Builder()
        .name("auto-jump")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.NcpStrafe)
        .build()
    );

    private final Setting<Double> timerBoost = sgNcp.add(new DoubleSetting.Builder()
        .name("timer-boost")
        .defaultValue(1.08)
        .min(1.0)
        .max(1.1)
        .sliderRange(1.0, 1.1)
        .visible(() -> mode.get() == Mode.NcpStrafe)
        .build()
    );

    private static final double NCP_BASE_SPEED = 0.2873;
    private static final double NCP_AIR_DECAY = 0.9937;

    private double ncpSpeed = NCP_BASE_SPEED;
    private double lastDistance = 0;

    private enum NCPPhase {
        Jump,
        JumpPost,
        SlowDown
    }

    private NCPPhase phase = NCPPhase.SlowDown;

    // ------------------------
    // Events
    // ------------------------

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (!shouldWork()) {
            reset();
            return;
        }

        if (mode.get() == Mode.NcpStrafe) {
            handleNcp(event);
        } else {
            handleGrim(event);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        double dx = mc.player.getX() - com.example.addon.utils.Compat.getPrevX(mc.player);
        double dz = mc.player.getZ() - com.example.addon.utils.Compat.getPrevZ(mc.player);
        lastDistance = Math.sqrt(dx * dx + dz * dz);

        if (mode.get() == Mode.NcpStrafe && shouldWork()) {
            mc.options.jumpKey.setPressed(false);
        }
    }

    // ------------------------
    // Handlers
    // ------------------------

    private void handleGrim(PlayerMoveEvent event) {
        if (!isMoving()) return;

        double boost = 0;

        if (grimBoatBoost.get() > 0) {
            for (BoatEntity boat : mc.world.getEntitiesByClass(
                    BoatEntity.class,
                    mc.player.getBoundingBox().expand(4),
                    e -> true
            )) {
                boost += grimBoatBoost.get();
            }
        }

        setSpeed(event, boost);
    }

    private void handleNcp(PlayerMoveEvent event) {
        boolean shouldJump = mc.options.jumpKey.isPressed() || (autoJump.get() && isMoving());

        if (mc.player.isOnGround() && shouldJump) {
            phase = NCPPhase.Jump;
        }

        switch (phase) {
            case Jump -> {
                if (mc.player.isOnGround()) {
                    mc.player.setVelocity(mc.player.getVelocity().x,
                            lowerJump.get() ? 0.4 : 0.42,
                            mc.player.getVelocity().z);
                    ncpSpeed = NCP_BASE_SPEED + 0.3;
                    phase = NCPPhase.JumpPost;
                } else phase = NCPPhase.SlowDown;
            }
            case JumpPost -> {
                ncpSpeed *= strict.get() ? 0.59 : 0.62;
                phase = NCPPhase.SlowDown;
            }
            case SlowDown -> ncpSpeed = lastDistance * NCP_AIR_DECAY;
        }

        if (mc.player.isOnGround() && !shouldJump) ncpSpeed = NCP_BASE_SPEED;

        ncpSpeed = Math.max(NCP_BASE_SPEED, Math.min(ncpSpeed, 1.0));
        setSpeed(event, isMoving() ? ncpSpeed : 0);
    }

    // ------------------------
    // Utils
    // ------------------------

    private boolean isMoving() {
        double forward = (mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0);
        double strafe = (mc.options.rightKey.isPressed() ? 1 : 0) - (mc.options.leftKey.isPressed() ? 1 : 0);
        return forward != 0 || strafe != 0;
    }

    private boolean shouldWork() {
        if (mc.player == null) return false;

        return !mc.player.getAbilities().flying
                && !mc.player.isGliding()
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava()
                && !mc.player.hasVehicle();
    }

    private void setSpeed(PlayerMoveEvent event, double speed) {
        if (!isMoving()) return;

        float yaw = mc.player.getYaw();
        double forward = (mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0);
        double strafe = (mc.options.rightKey.isPressed() ? 1 : 0) - (mc.options.leftKey.isPressed() ? 1 : 0);

        if (forward != 0) {
            if (strafe > 0) yaw += (forward > 0 ? -45 : 45);
            else if (strafe < 0) yaw += (forward > 0 ? 45 : -45);
            strafe = 0;
            forward = forward > 0 ? 1 : -1;
        }

        double rad = Math.toRadians(yaw + 90);
        double newX = forward * speed * Math.cos(rad) + strafe * speed * Math.sin(rad);
        double newZ = forward * speed * Math.sin(rad) - strafe * speed * Math.cos(rad);

        // Immutable movement fix
        Vec3d delta = new Vec3d(newX - event.movement.x, 0, newZ - event.movement.z);
        event.movement = event.movement.add(delta);
    }

    private void reset() {
        phase = NCPPhase.SlowDown;
        ncpSpeed = NCP_BASE_SPEED;
    }
}


