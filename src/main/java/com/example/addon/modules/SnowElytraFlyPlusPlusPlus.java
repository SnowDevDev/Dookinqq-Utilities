package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.utils.Compat;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SnowElytraFlyPlusPlusPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRotation = settings.createGroup("Rotation");
    private final SettingGroup sgGlide = settings.createGroup("Glide");

    public enum Mode {
        BOUNCE,
        ROTATION,
        GLIDE
    }

    private enum GlideState {
        DIVE,
        CRUISE,
        CLIMB
    }

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Flight mode.")
        .defaultValue(Mode.ROTATION)
        .build()
    );

    private final Setting<Boolean> autoStart = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-start")
        .description("Automatically sends start-fall-flying while airborne.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> bouncePitch = sgGeneral.add(new BoolSetting.Builder()
        .name("bounce-pitch")
        .description("Forces pitch in bounce mode.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.BOUNCE)
        .build()
    );

    private final Setting<Integer> bouncePitchDeg = sgGeneral.add(new IntSetting.Builder()
        .name("bounce-pitch-deg")
        .description("Pitch used in bounce mode.")
        .defaultValue(75)
        .range(-89, 89)
        .sliderRange(-89, 89)
        .visible(() -> mode.get() == Mode.BOUNCE && bouncePitch.get())
        .build()
    );

    private final Setting<Boolean> lockPitch = sgRotation.add(new BoolSetting.Builder()
        .name("lock-pitch")
        .description("Locks pitch while rotating.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.ROTATION)
        .build()
    );

    private final Setting<Integer> lockedPitchDeg = sgRotation.add(new IntSetting.Builder()
        .name("locked-pitch-deg")
        .description("Pitch value used when lock-pitch is enabled.")
        .defaultValue(-3)
        .range(-89, 89)
        .sliderRange(-89, 89)
        .visible(() -> mode.get() == Mode.ROTATION && lockPitch.get())
        .build()
    );

    private final Setting<Boolean> hover = sgRotation.add(new BoolSetting.Builder()
        .name("hover")
        .description("Alternates yaw while idle in rotation mode.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.ROTATION)
        .build()
    );

    private final Setting<Integer> hoverSpeed = sgRotation.add(new IntSetting.Builder()
        .name("hover-speed")
        .description("Ticks between hover yaw flips.")
        .defaultValue(10)
        .range(2, 20)
        .sliderRange(2, 20)
        .visible(() -> mode.get() == Mode.ROTATION && hover.get())
        .build()
    );

    private final Setting<Boolean> rotationRocketSpam = sgRotation.add(new BoolSetting.Builder()
        .name("rotation-rocket-spam")
        .description("Automatically uses fireworks in rotation mode.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.ROTATION)
        .build()
    );

    private final Setting<Integer> rotationRocketDelay = sgRotation.add(new IntSetting.Builder()
        .name("rotation-rocket-delay")
        .description("Ticks between rocket uses in rotation mode.")
        .defaultValue(6)
        .range(0, 60)
        .sliderRange(0, 60)
        .visible(() -> mode.get() == Mode.ROTATION && rotationRocketSpam.get())
        .build()
    );

    private final Setting<Boolean> swapBack = sgRotation.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swap back to previous hotbar slot after using a rocket.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.ROTATION)
        .build()
    );

    private final Setting<Integer> targetY = sgGlide.add(new IntSetting.Builder()
        .name("target-y")
        .description("Target Y level for glide mode climbs.")
        .defaultValue(180)
        .range(60, 600)
        .sliderRange(60, 400)
        .visible(() -> mode.get() == Mode.GLIDE)
        .build()
    );

    private final Setting<Integer> vLow = sgGlide.add(new IntSetting.Builder()
        .name("min-speed")
        .description("Lower speed threshold for glide state machine.")
        .defaultValue(14)
        .range(6, 40)
        .sliderRange(6, 40)
        .visible(() -> mode.get() == Mode.GLIDE)
        .build()
    );

    private final Setting<Integer> vHigh = sgGlide.add(new IntSetting.Builder()
        .name("max-speed")
        .description("Upper speed threshold for glide state machine.")
        .defaultValue(27)
        .range(10, 60)
        .sliderRange(10, 60)
        .visible(() -> mode.get() == Mode.GLIDE)
        .build()
    );

    private final Setting<Integer> climbPitch = sgGlide.add(new IntSetting.Builder()
        .name("climb-pitch")
        .description("Pitch used while climbing in glide mode.")
        .defaultValue(40)
        .range(0, 60)
        .sliderRange(0, 60)
        .visible(() -> mode.get() == Mode.GLIDE)
        .build()
    );

    private final Setting<Integer> divePitch = sgGlide.add(new IntSetting.Builder()
        .name("dive-pitch")
        .description("Pitch used while diving in glide mode.")
        .defaultValue(38)
        .range(20, 60)
        .sliderRange(20, 60)
        .visible(() -> mode.get() == Mode.GLIDE)
        .build()
    );

    private final Setting<Integer> cruiseMin = sgGlide.add(new IntSetting.Builder()
        .name("cruise-min")
        .description("Minimum pitch used while cruising.")
        .defaultValue(4)
        .range(0, 20)
        .sliderRange(0, 20)
        .visible(() -> mode.get() == Mode.GLIDE)
        .build()
    );

    private final Setting<Integer> cruiseMax = sgGlide.add(new IntSetting.Builder()
        .name("cruise-max")
        .description("Maximum pitch used while cruising.")
        .defaultValue(12)
        .range(2, 25)
        .sliderRange(2, 25)
        .visible(() -> mode.get() == Mode.GLIDE)
        .build()
    );

    private final Setting<Boolean> allowRockets = sgGlide.add(new BoolSetting.Builder()
        .name("allow-rockets")
        .description("Use fireworks while climbing in glide mode.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.GLIDE)
        .build()
    );

    private final Setting<Integer> glideRocketDelay = sgGlide.add(new IntSetting.Builder()
        .name("glide-rocket-delay")
        .description("Ticks between rocket uses in glide mode.")
        .defaultValue(70)
        .range(0, 200)
        .sliderRange(0, 200)
        .visible(() -> mode.get() == Mode.GLIDE && allowRockets.get())
        .build()
    );

    private GlideState glideState = GlideState.CLIMB;
    private double speed;
    private final double[] speedSamples = new double[25];
    private int speedSampleIndex;
    private boolean speedBufferFilled;
    private double lastX;
    private double lastZ;
    private double cruisePhase;
    private int rocketTicks;
    private int hoverTicks;
    private boolean hoverFlip;
    private boolean climbingToTarget;
    private boolean warnedNoRockets;

    public SnowElytraFlyPlusPlusPlus() {
        super(AddonTemplate.CATEGORY, "Dookinqq Elytra Fly +++", "Advanced Elytra flight with rotation and auto-rocket support.");
    }

    @Override
    public void onActivate() {
        glideState = GlideState.CLIMB;
        speed = 0;
        speedSampleIndex = 0;
        speedBufferFilled = false;
        cruisePhase = 0;
        rocketTicks = 0;
        hoverTicks = 0;
        hoverFlip = false;
        climbingToTarget = false;
        warnedNoRockets = false;

        if (mc.player != null) {
            lastX = mc.player.getX();
            lastZ = mc.player.getZ();
        }
    }

    @Override
    public void onDeactivate() {
        rocketTicks = 0;
        hoverTicks = 0;
        hoverFlip = false;
        warnedNoRockets = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (!mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) return;

        if (autoStart.get() && !mc.player.isOnGround() && !mc.player.isGliding()) {
            sendFallFlying();
        }

        switch (mode.get()) {
            case BOUNCE -> handleBounceMode();
            case ROTATION -> handleRotationMode();
            case GLIDE -> handleGlideMode();
        }

        updateSpeedSample();
    }

    private void handleBounceMode() {
        if (bouncePitch.get()) {
            Rotations.rotate(mc.player.getYaw(), bouncePitchDeg.get());
        }

        if (!mc.player.isGliding()) {
            sendFallFlying();
        }
    }

    private void handleRotationMode() {
        if (!mc.player.isGliding()) return;

        Vec3d dir = getControlDirection();
        float targetYaw = mc.player.getYaw();
        float targetPitch = mc.player.getPitch();

        if (dir != null) {
            if (Math.abs(dir.y) > 0.5) {
                targetPitch = dir.y > 0 ? -89f : 89f;
            } else {
                targetYaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90f;
                if (lockPitch.get()) targetPitch = lockedPitchDeg.get().floatValue();
            }

            Rotations.rotate(targetYaw, targetPitch);
        } else if (hover.get()) {
            if (++hoverTicks >= hoverSpeed.get()) {
                hoverTicks = 0;
                hoverFlip = !hoverFlip;
            }

            float hoverYaw = mc.player.getYaw() + (hoverFlip ? 180f : 0f);
            float hoverPitch = lockPitch.get() ? lockedPitchDeg.get().floatValue() : -3f;
            Rotations.rotate(hoverYaw, hoverPitch);
        }

        if (rotationRocketSpam.get()) {
            rocketTicks++;
            if (rocketTicks >= rotationRocketDelay.get()) {
                if (useFirework()) {
                    rocketTicks = 0;
                    warnedNoRockets = false;
                } else if (!warnedNoRockets) {
                    warning("No fireworks found in offhand or hotbar.");
                    warnedNoRockets = true;
                }
            }
        }
    }

    private void handleGlideMode() {
        if (!mc.player.isGliding()) return;

        double playerY = mc.player.getY();

        if (!climbingToTarget && playerY < targetY.get() - 80) {
            climbingToTarget = true;
        }

        GlideState effectiveState;

        if (climbingToTarget) {
            effectiveState = GlideState.CLIMB;

            if (allowRockets.get() && ++rocketTicks >= glideRocketDelay.get()) {
                if (useFirework()) {
                    rocketTicks = 0;
                }
            }

            if (playerY >= targetY.get()) {
                climbingToTarget = false;
            }
        } else {
            int low = vLow.get();
            int high = Math.max(vHigh.get(), low + 2);

            switch (glideState) {
                case DIVE -> {
                    if (speed >= high) glideState = GlideState.CRUISE;
                }
                case CLIMB -> {
                    if (speed <= low) glideState = GlideState.DIVE;
                }
                case CRUISE -> {
                    if (speed <= low - 1) glideState = GlideState.DIVE;
                    else if (speed >= high + 2) glideState = GlideState.CLIMB;
                }
            }

            effectiveState = glideState;
        }

        float targetPitch;
        if (effectiveState == GlideState.DIVE) {
            targetPitch = clampPitch(divePitch.get());
        } else if (effectiveState == GlideState.CLIMB) {
            targetPitch = clampPitch(-climbPitch.get());
        } else {
            targetPitch = cruisePitch();
        }

        float smoothPitch = approach(mc.player.getPitch(), targetPitch, 10f);
        Rotations.rotate(mc.player.getYaw(), smoothPitch);
    }

    private void updateSpeedSample() {
        if (mc.player == null) return;

        double dx = mc.player.getX() - lastX;
        double dz = mc.player.getZ() - lastZ;
        double instantSpeed = Math.sqrt(dx * dx + dz * dz) * 20;

        speedSamples[speedSampleIndex] = instantSpeed;
        speedSampleIndex = (speedSampleIndex + 1) % speedSamples.length;
        if (speedSampleIndex == 0) speedBufferFilled = true;

        int count = speedBufferFilled ? speedSamples.length : speedSampleIndex;
        double sum = 0;
        for (int i = 0; i < count; i++) sum += speedSamples[i];
        speed = count > 0 ? sum / count : 0;

        lastX = mc.player.getX();
        lastZ = mc.player.getZ();
    }

    private Vec3d getControlDirection() {
        boolean forward = mc.options.forwardKey.isPressed();
        boolean back = mc.options.backKey.isPressed();
        boolean left = mc.options.leftKey.isPressed();
        boolean right = mc.options.rightKey.isPressed();
        boolean up = mc.options.jumpKey.isPressed();
        boolean down = mc.options.sneakKey.isPressed();

        if (!(forward || back || left || right || up || down)) return null;

        if (up && !down) return new Vec3d(0, 1, 0);
        if (down && !up) return new Vec3d(0, -1, 0);

        double forwardVal = (forward ? 1.0 : 0.0) - (back ? 1.0 : 0.0);
        double strafeVal = (right ? 1.0 : 0.0) - (left ? 1.0 : 0.0);
        if (forwardVal == 0.0 && strafeVal == 0.0) return null;

        double yawRad = Math.toRadians(mc.player.getYaw());
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        double rx = -Math.sin(yawRad + Math.PI / 2.0);
        double rz = Math.cos(yawRad + Math.PI / 2.0);

        double wx = fx * forwardVal + rx * strafeVal;
        double wz = fz * forwardVal + rz * strafeVal;

        Vec3d worldDir = new Vec3d(wx, 0, wz);
        if (worldDir.lengthSquared() == 0.0) return null;
        return worldDir.normalize();
    }

    private boolean useFirework() {
        if (mc.player == null || mc.interactionManager == null) return false;

        if (mc.player.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            return true;
        }

        int fireworkSlot = getFireworkHotbarSlot();
        if (fireworkSlot == -1) return false;

        int prevSlot = Compat.getSelectedSlot(mc.player.getInventory());
        if (fireworkSlot != prevSlot) InvUtils.swap(fireworkSlot, false);

        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);

        if (swapBack.get() && fireworkSlot != prevSlot) {
            InvUtils.swap(prevSlot, false);
        }

        return true;
    }

    private int getFireworkHotbarSlot() {
        if (mc.player == null) return -1;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) {
                return i;
            }
        }

        return -1;
    }

    private void sendFallFlying() {
        if (mc.getNetworkHandler() == null || mc.player == null) return;

        mc.getNetworkHandler().sendPacket(
            new ClientCommandC2SPacket(
                mc.player,
                ClientCommandC2SPacket.Mode.START_FALL_FLYING
            )
        );
    }

    private float cruisePitch() {
        double dt = 1.0 / 20.0;
        double period = 2.6;
        cruisePhase += dt / period;
        if (cruisePhase > 1.0) cruisePhase -= 1.0;

        double tri = 2.0 * Math.abs(2.0 * (cruisePhase - Math.floor(cruisePhase + 0.5))) - 1.0;
        tri = Math.copySign(tri * tri, tri);

        double min = cruiseMin.get();
        double max = Math.max(cruiseMax.get(), cruiseMin.get() + 1);
        double ampPitch = min + (max - min) * (0.5 * (tri + 1.0));

        return clampPitch((float) -ampPitch);
    }

    private float approach(float current, float target, float maxDelta) {
        float delta = MathHelper.clamp(target - current, -maxDelta, maxDelta);
        return current + delta;
    }

    private float clampPitch(float pitchDeg) {
        return MathHelper.clamp(pitchDeg, -89f, 89f);
    }
}

