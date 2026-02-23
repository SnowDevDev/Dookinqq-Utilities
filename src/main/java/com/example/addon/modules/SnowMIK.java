package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.*;

public class SnowMIK extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgTeleport = settings.createGroup("Teleport");
    private final SettingGroup sgAttack = settings.createGroup("Attack");

    public enum Mode { Vanilla, Paper }
    public enum TargetPriority { Closest, LowestHealth, HighestHealth }

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing").defaultValue(true).build());

    private final Setting<Integer> attackDelay = sgGeneral.add(new IntSetting.Builder()
        .name("delay").defaultValue(0).min(0).sliderMax(20).build());

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(
        new EntityTypeListSetting.Builder()
            .name("entities")
            .defaultValue(EntityType.PLAYER)
            .build());

    private final Setting<Boolean> attackFriends = sgGeneral.add(
        new BoolSetting.Builder().name("attack-friends").defaultValue(false).build());

    private final Setting<TargetPriority> priority = sgTargeting.add(
        new EnumSetting.Builder<TargetPriority>()
            .name("priority")
            .defaultValue(TargetPriority.Closest)
            .build());

    private final Setting<Mode> mode = sgTeleport.add(
        new EnumSetting.Builder<Mode>()
            .name("mode")
            .defaultValue(Mode.Paper)
            .build());

    private final Setting<Double> rangeVanilla = sgTeleport.add(
        new DoubleSetting.Builder()
            .name("range-vanilla")
            .defaultValue(22.0)
            .min(1)
            .sliderMax(22)
            .visible(() -> mode.get() == Mode.Vanilla)
            .build());

    private final Setting<Double> rangePaper = sgTeleport.add(
        new DoubleSetting.Builder()
            .name("range-paper")
            .defaultValue(49.0)
            .min(1)
            .sliderMax(99)
            .visible(() -> mode.get() == Mode.Paper)
            .build());

    private final Setting<Boolean> multiAttack = sgAttack.add(
        new BoolSetting.Builder()
            .name("multi")
            .defaultValue(false)
            .build());

    private final Setting<Integer> attackCount = sgAttack.add(
        new IntSetting.Builder()
            .name("count")
            .defaultValue(2)
            .min(1)
            .sliderMax(4)
            .visible(multiAttack::get)
            .build());

    private double maxRange;
    private int tickCounter;
    private boolean sendingPackets;

    public SnowMIK() {
        super(AddonTemplate.CATEGORY, "Dookinqq MIK",
            "Safe optimized teleport melee aura.");
    }

    @Override
    public void onActivate() {
        updateRange();
        tickCounter = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null)
            return;

        updateRange();

        tickCounter++;
        if (tickCounter < attackDelay.get()) return;
        tickCounter = 0;

        attack();
    }

    private void updateRange() {
        maxRange = mode.get() == Mode.Vanilla
            ? rangeVanilla.get()
            : rangePaper.get();
    }

    private void attack() {
        Entity target = findTarget();
        if (target == null) return;

        Vec3d start = new Vec3d(
    mc.player.getX(),
    mc.player.getY(),
    mc.player.getZ()
);
        Vec3d hit = target.getBoundingBox().getCenter();

        if (start.distanceTo(hit) > maxRange) return;
        if (invalid(hit)) return;

        performAttack(start, hit, target);
    }

    private Entity findTarget() {
        Entity best = null;
        double bestScore = Double.MAX_VALUE;

        for (Entity e : mc.world.getEntities()) {
            if (!valid(e)) continue;

            double distSq = mc.player.squaredDistanceTo(e);
            double score = switch (priority.get()) {
                case Closest -> distSq;
                case LowestHealth -> e instanceof LivingEntity l
                    ? l.getHealth() + Math.sqrt(distSq)
                    : distSq;
                case HighestHealth -> e instanceof LivingEntity l
                    ? -l.getHealth() + Math.sqrt(distSq)
                    : distSq;
            };

            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }

        return best;
    }

    private boolean valid(Entity e) {
        if (e == null || e == mc.player) return false;
        if (!entities.get().contains(e.getType())) return false;
        if (!e.isAlive() || !e.isAttackable()) return false;

        if (!attackFriends.get() && e instanceof PlayerEntity p
            && Friends.get().isFriend(p)) return false;

        return mc.player.distanceTo(e) <= maxRange;
    }

    private void performAttack(Vec3d start, Vec3d hit, Entity target) {
        if (sendingPackets) return;
        sendingPackets = true;

        boolean attacked = false;

        try {
            int attacks = multiAttack.get() ? attackCount.get() : 1;

            for (int i = 0; i < attacks; i++) {
                if (invalid(hit)) break;

                sendMove(hit);

                if (swing.get()) {
                    mc.getNetworkHandler().sendPacket(
                        new HandSwingC2SPacket(Hand.MAIN_HAND));
                    mc.player.swingHand(Hand.MAIN_HAND);
                }

                mc.getNetworkHandler().sendPacket(
                    PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));

                attacked = true;

                sendMove(start);
            }

            if (attacked) {
                sendMove(start.add(0, 0.01, 0));
            }

        } finally {
            sendingPackets = false;
        }
    }

    private void sendMove(Vec3d pos) {
        PlayerMoveC2SPacket packet =
            new PlayerMoveC2SPacket.PositionAndOnGround(
                pos.x, pos.y, pos.z, false, false);

        ((IPlayerMoveC2SPacket) packet).meteor$setTag(1337);
        mc.player.networkHandler.sendPacket(packet);
        mc.player.setPosition(pos);
    }

    private boolean invalid(Vec3d pos) {
        if (mc.player == null || mc.world == null) return true;

        int minY = mc.world.getBottomY();
        int maxY = mc.world.getTopYInclusive();
        if (pos.y < minY || pos.y > maxY) return true;

        BlockPos bp = BlockPos.ofFloored(pos);
        int cx = bp.getX() >> 4;
        int cz = bp.getZ() >> 4;

        if (mc.world.getChunkManager().getWorldChunk(cx, cz) == null)
            return true;

        Box box = mc.player.getBoundingBox().offset(
            pos.x - mc.player.getX(),
            pos.y - mc.player.getY(),
            pos.z - mc.player.getZ());

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = MathHelper.floor(box.minX); x <= MathHelper.floor(box.maxX); x++) {
            for (int y = MathHelper.floor(box.minY); y <= MathHelper.floor(box.maxY); y++) {
                for (int z = MathHelper.floor(box.minZ); z <= MathHelper.floor(box.maxZ); z++) {

                    mutable.set(x, y, z);
                    BlockState state = mc.world.getBlockState(mutable);

                    if (state.isOf(Blocks.LAVA)
                        || state.isOf(Blocks.FIRE)
                        || state.isOf(Blocks.SOUL_FIRE)
                        || state.isOf(Blocks.MAGMA_BLOCK)
                        || state.isOf(Blocks.CAMPFIRE)
                        || state.isOf(Blocks.SWEET_BERRY_BUSH)
                        || state.isOf(Blocks.POWDER_SNOW)
                        || !state.getCollisionShape(mc.world, mutable).isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return !mc.world.getOtherEntities(mc.player, box).isEmpty();
    }
}