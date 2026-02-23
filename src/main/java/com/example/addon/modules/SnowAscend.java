package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;


public class SnowAscend extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMovement = settings.createGroup("packet-movement");
    private final SettingGroup sgClient = settings.createGroup("packet-client");
    private final SettingGroup sgBypass = settings.createGroup("packet-bypass");

    public SnowAscend() {
        super(AddonTemplate.CATEGORY, "Dookinqq Ascend", "car car go");
    }

    private enum Mode {
        CREATIVE,
        VECTOR,
        PACKET
    }

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("mode").description("Flight mode").defaultValue(Mode.CREATIVE).build());

    private final Setting<Double> flySpeed = sgGeneral.add(new DoubleSetting.Builder().name("flyspeed").description("the speed you fly").defaultValue(1.0d).range(0.0d, 5.0d).build());
    private final Setting<Double> TpUp = sgGeneral.add(new DoubleSetting.Builder().name("TpUp").description("amount of block you get teleported up on activate").defaultValue(0.2).range(0, 3).build());
    private final Setting<Integer> goDownTime = sgGeneral.add(new IntSetting.Builder().name("goDownTime").description("time(ticks) to go down").defaultValue(60).range(1,800).build());
    private final Setting<Boolean> antiKick = sgGeneral.add(new BoolSetting.Builder().name("anti kick").description("time(ticks) to go down").defaultValue(true).build());
    private final Setting<Boolean> creativeflight = sgGeneral.add(new BoolSetting.Builder().name("creativeflight").description("switches the flight type").defaultValue(true).build());
    private final Setting<Boolean> vectorflight = sgGeneral.add(new BoolSetting.Builder().name("vectorflight").description("switches the flight type").defaultValue(false).build());

    int toggle =0;
    int MAX_SPEED = 2147483647;
    double FALL_SPEED = -0.04;
    double acceleration = 0.1;

    private int nowDownTime;

    private final MinecraftClient client = MinecraftClient.getInstance();

    // Packet-mode state
    private final HashSet<PlayerMoveC2SPacket> packets = new HashSet<>();
    private final Setting<Double> horizontalSpeed = sgMovement.add(new DoubleSetting.Builder()
        .name("horizontal-speed")
        .description("Horizontal speed in blocks per second.")
        .defaultValue(0.501)
        .min(0.0)
        .max(20.0)
        .sliderMin(0.0)
        .sliderMax(20.0)
        .build()
    );

    private final Setting<Double> verticalSpeed = sgMovement.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Vertical speed in blocks per second.")
        .defaultValue(0.501)
        .min(0.0)
        .max(20.0)
        .sliderMin(0.0)
        .sliderMax(20.0)
        .build()
    );

    private final Setting<Boolean> sendTeleport = sgMovement.add(new BoolSetting.Builder()
        .name("teleport")
        .description("Sends teleport packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> setYaw = sgClient.add(new BoolSetting.Builder()
        .name("set-yaw")
        .description("Sets yaw client side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> setMove = sgClient.add(new BoolSetting.Builder()
        .name("set-move")
        .description("Sets movement client side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> setPos = sgClient.add(new BoolSetting.Builder()
        .name("set-pos")
        .description("Sets position client side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> setID = sgClient.add(new BoolSetting.Builder()
        .name("set-id")
        .description("Updates teleport id when a position packet is received.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiKickPacket = sgBypass.add(new BoolSetting.Builder()
        .name("anti-kick")
        .description("Moves down occasionally to prevent kicks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> downDelay = sgBypass.add(new IntSetting.Builder()
        .name("down-delay")
        .description("How often you move down when not flying upwards. (ticks)")
        .defaultValue(4)
        .sliderMin(1)
        .sliderMax(30)
        .min(1)
        .max(30)
        .build()
    );

    private final Setting<Integer> downDelayFlying = sgBypass.add(new IntSetting.Builder()
        .name("flying-down-delay")
        .description("How often you move down when flying upwards. (ticks)")
        .defaultValue(10)
        .sliderMin(1)
        .sliderMax(30)
        .min(1)
        .max(30)
        .build()
    );

    private final Setting<Boolean> invalidPacket = sgBypass.add(new BoolSetting.Builder()
        .name("invalid-packet")
        .description("Sends invalid movement packets.")
        .defaultValue(true)
        .build()
    );

    private int flightCounter = 0;
    private int teleportID = 0;

    @Override
    public void onActivate() {
        if (mode.get() == Mode.CREATIVE || creativeflight.get()) {
            if (client.player != null) {
                client.player.getAbilities().setFlySpeed(flySpeed.get().floatValue());
                client.player.getAbilities().flying = true;
                if (client.player.isOnGround()) {
                    client.player.setPosition(client.player.getX(), client.player.getY() + TpUp.get(), client.player.getZ());
                }
            }
            nowDownTime = 0;
        }

        if (mode.get() == Mode.PACKET) {
            packets.clear();
            flightCounter = 0;
            teleportID = 0;
        }
    }

    // NAPICU POUZIVA METEOR FUNKCI ON TICK
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mode.get() == Mode.CREATIVE || creativeflight.get()) {
            if (client.player != null && client.player.isOnGround()) {
                // removed erroneous call to this.toggle();
            }

            if (antiKick.get() && client.player != null) {
                if (nowDownTime == 1) {
                    client.player.setPosition(client.player.getX(), client.player.getY() + 0.2, client.player.getZ());
                }
                if (nowDownTime >= goDownTime.get()) {
                    client.player.setPosition(client.player.getX(), client.player.getY() - 0.2, client.player.getZ());
                    nowDownTime = 0;
                }

                nowDownTime += 1;
            }
        }

        if (client.player != null && this.isActive() && mode.get() == Mode.VECTOR || vectorflight.get()) {
            boolean jumpPressed = client.options.jumpKey.isPressed();
            boolean forwardPressed = client.options.forwardKey.isPressed();
            boolean leftPressed = client.options.leftKey.isPressed();
            boolean rightPressed = client.options.rightKey.isPressed();
            boolean backPressed = client.options.backKey.isPressed();

            Entity object = client.player;
            if (client.player.hasVehicle()) {
                object = client.player.getVehicle();
            }

            Vec3d velocity = object.getVelocity();
            Vec3d newVelocity = new Vec3d(velocity.x, -FALL_SPEED, velocity.z);
            if (jumpPressed) {
                if (forwardPressed) {
                    newVelocity = client.player.getRotationVector().multiply(acceleration);
                }
                if (leftPressed && !client.player.hasVehicle()) {
                    newVelocity = client.player.getRotationVector().multiply(acceleration).rotateY(3.1415927F / 2);
                    newVelocity = new Vec3d(newVelocity.x, 0, velocity.z);
                }
                if (rightPressed && !client.player.hasVehicle()) {
                    newVelocity = client.player.getRotationVector().multiply(acceleration).rotateY(-3.1415927F / 2);
                    newVelocity = new Vec3d(newVelocity.x, 0, velocity.z);
                }
                if (backPressed) {
                    newVelocity = client.player.getRotationVector().negate().multiply(acceleration);
                }

                newVelocity = new Vec3d(newVelocity.x, (toggle == 0 && newVelocity.y > FALL_SPEED) ? FALL_SPEED : newVelocity.y, newVelocity.z);
                object.setVelocity(newVelocity);

                if (forwardPressed || leftPressed || rightPressed || backPressed) {
                    if (acceleration < MAX_SPEED) {
                        acceleration += 0.1;
                    }
                } else if (acceleration > 0.2) {
                    acceleration -= 0.2;
                }
            }

            if (toggle == 0 || newVelocity.y <= -0.04) {
                toggle = 40;
            }
            toggle--;
        }
    }

    @EventHandler
    public void onSendMovementPackets(SendMovementPacketsEvent.Pre event) {
        if (mode.get() != Mode.PACKET || client.player == null) return;

        client.player.setVelocity(0.0, 0.0, 0.0);
        double speed = 0.0;
        boolean checkCollisionBoxes = checkHitBoxes();

        boolean movingForward = client.options.forwardKey.isPressed() && !client.options.backKey.isPressed();
        boolean movingSideways = client.options.leftKey.isPressed() != client.options.rightKey.isPressed();
        boolean jumpPressed = client.options.jumpKey.isPressed();
        boolean sneakPressed = client.options.sneakKey.isPressed();

        if (jumpPressed && (checkCollisionBoxes || !(movingForward || movingSideways))) {
            if (antiKickPacket.get() && !checkCollisionBoxes) {
                speed = resetCounter(downDelayFlying.get()) ? -0.032 : verticalSpeed.get() / 20.0;
            } else {
                speed = verticalSpeed.get() / 20.0;
            }
        } else {
            if (sneakPressed) {
                speed = verticalSpeed.get() / -20.0;
            } else if (!checkCollisionBoxes) {
                if (resetCounter(downDelay.get())) {
                    speed = antiKickPacket.get() ? -0.04 : 0.0;
                } else {
                    speed = 0.0;
                }
            } else {
                speed = 0.0;
            }
        }

        Vec3d horizontal = PlayerUtils.getHorizontalVelocity(horizontalSpeed.get());

        client.player.setVelocity(horizontal.x, speed, horizontal.z);
        sendPackets(client.player.getVelocity().x, client.player.getVelocity().y, client.player.getVelocity().z, sendTeleport.get());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (mode.get() != Mode.PACKET || client.player == null) return;
        if (setMove.get() && flightCounter != 0) {
            event.movement = new Vec3d(client.player.getVelocity().x, client.player.getVelocity().y, client.player.getVelocity().z);
        }
    }

    @EventHandler
    public void onPacketSent(PacketEvent.Send event) {
        if (mode.get() != Mode.PACKET) return;
        if (event.packet instanceof PlayerMoveC2SPacket && !packets.remove((PlayerMoveC2SPacket) event.packet)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mode.get() != Mode.PACKET || client.player == null || client.world == null) return;
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            PlayerPositionLookS2CPacket packet = (PlayerPositionLookS2CPacket) event.packet;
            if (setID.get()) teleportID = packet.teleportId();
        }
    }

    private boolean checkHitBoxes() {
        return client.player != null && client.world != null && !client.world.getBlockCollisions(client.player, client.player.getBoundingBox().stretch(-0.0625, -0.0625, -0.0625)).iterator().hasNext();
    }

    private boolean resetCounter(int counter) {
        if (++flightCounter >= counter) {
            flightCounter = 0;
            return true;
        }
        return false;
    }

    private void sendPackets(double x, double y, double z, boolean teleport) {
        Vec3d vec = new Vec3d(x, y, z);
        Vec3d position = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ()).add(vec);
        Vec3d oob = outOfBoundsVec(vec, position);
        packetSender(new PlayerMoveC2SPacket.PositionAndOnGround(position.x, position.y, position.z, client.player.isOnGround(), client.player.horizontalCollision));
        if (invalidPacket.get()) packetSender(new PlayerMoveC2SPacket.PositionAndOnGround(oob.x, oob.y, oob.z, client.player.isOnGround(), client.player.horizontalCollision));
        if (setPos.get()) client.player.setPos(position.x, position.y, position.z);
        teleportPacket(position, teleport);
    }

    private void teleportPacket(Vec3d pos, boolean shouldTeleport) {
        if (shouldTeleport) client.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(++teleportID));
    }

    private Vec3d outOfBoundsVec(Vec3d offset, Vec3d position) {
        return position.add(0.0, 1500.0, 0.0);
    }

    private void packetSender(PlayerMoveC2SPacket packet) {
        packets.add(packet);
        client.player.networkHandler.sendPacket(packet);
    }

    @Override
    public void onDeactivate() {
        if (client.player != null && (mode.get() == Mode.CREATIVE || creativeflight.get())) {
            client.player.getAbilities().flying = false;
        }
    }

}




