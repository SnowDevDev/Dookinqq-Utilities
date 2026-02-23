// src/main/java/com/example/addon/modules/SnowRIK.java
package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AntiHunger;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.*;

public class SnowRIK extends Module {
    private final SettingGroup sgGeneral    = settings.getDefaultGroup();
    private final SettingGroup sgTargeting  = settings.createGroup("Targeting");
    private final SettingGroup sgTeleport   = settings.createGroup("Teleport");
    private final SettingGroup sgProjectile = settings.createGroup("Projectile");
    private final SettingGroup sgAccuracy   = settings.createGroup("Accuracy");
    private final SettingGroup sgLegacy     = settings.createGroup("Legacy");

    public enum Mode { Projectile, Melee }
    public enum TargetPriority { Closest, LowestHealth, Crosshair, HighestHealth }

    private final Setting<Mode> activeMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode").description("Projectile or Melee attack mode.")
        .defaultValue(Mode.Projectile).build());

    private final Setting<Integer> interactAmount = sgGeneral.add(new IntSetting.Builder()
        .name("interact-amount").description("Number of interact packets to send.")
        .defaultValue(1).min(1).sliderMax(15).build());

    private final Setting<Set<EntityType<?>>> entities = sgTargeting.add(
        new EntityTypeListSetting.Builder()
            .name("entities").description("Entity types to target.")
            .onlyAttackable().defaultValue(EntityType.PLAYER).build());

    private final Setting<Boolean> attackFriends = sgTargeting.add(new BoolSetting.Builder()
        .name("attack-friends").defaultValue(false).build());

    private final Setting<Double> maxDistance = sgTargeting.add(new DoubleSetting.Builder()
        .name("max-distance").description("Maximum targeting distance.")
        .defaultValue(60.0).min(5.0).sliderMax(120.0).build());

    private final Setting<Boolean> requireLineOfSight = sgTargeting.add(new BoolSetting.Builder()
        .name("require-line-of-sight").description("Only target entities you can see.")
        .defaultValue(false).build());

    private final Setting<TargetPriority> targetPriority = sgTargeting.add(
        new EnumSetting.Builder<TargetPriority>()
            .name("target-priority").description("How to rank targets.")
            .defaultValue(TargetPriority.Closest).build());

    private final Setting<Integer> spamPackets = sgTeleport.add(new IntSetting.Builder()
        .name("spam-packets").description("Position packets before teleporting.")
        .defaultValue(8).min(0).sliderMax(20).build());

    private final Setting<Double> teleportHeight = sgTeleport.add(new DoubleSetting.Builder()
        .name("teleport-height").description("Height to teleport above targets.")
        .defaultValue(60.0).min(5.0).sliderMax(130.0).build());

    private final Setting<Double> targetOffset = sgTeleport.add(new DoubleSetting.Builder()
        .name("above-target-offset").description("Vertical offset from target top.")
        .defaultValue(0.01).min(0.0).sliderMax(10.0).build());

    private final Setting<Double> offsetHorizontal = sgTeleport.add(new DoubleSetting.Builder()
        .name("horizontal-offset").description("Horizontal randomization.")
        .defaultValue(0.05).min(0.001).sliderMax(0.99).build());

    private final Setting<Double> offsetY = sgTeleport.add(new DoubleSetting.Builder()
        .name("y-offset").description("Vertical randomization.")
        .defaultValue(0.01).min(0.001).sliderMax(0.99).build());

    private final ItemListSetting projectileItems = sgProjectile.add(new ItemListSetting.Builder()
        .name("projectile-items").description("Projectile items to fire.")
        .defaultValue(
            Items.ENDER_PEARL, Items.SPLASH_POTION, Items.LINGERING_POTION,
            Items.EXPERIENCE_BOTTLE, Items.SNOWBALL, Items.EGG,
            Items.WIND_CHARGE, Items.BOW, Items.TRIDENT)
        .filter(item -> item == Items.ENDER_PEARL || item == Items.SPLASH_POTION ||
            item == Items.LINGERING_POTION || item == Items.EXPERIENCE_BOTTLE ||
            item == Items.SNOWBALL || item == Items.EGG || item == Items.WIND_CHARGE ||
            item == Items.BOW || item == Items.TRIDENT)
        .build());

    private final Setting<Boolean> bowMachinegun = sgProjectile.add(new BoolSetting.Builder()
        .name("bow-machinegun").description("Automatically fire arrows while holding bow.")
        .defaultValue(true).build());

    private final Setting<Integer> chargeTicks = sgProjectile.add(new IntSetting.Builder()
        .name("charge-ticks").description("Ticks to charge bow before firing.")
        .defaultValue(4).min(4).sliderRange(4, 60)
        .visible(bowMachinegun::get).build());

    private final Setting<Integer> pathCheckSteps = sgAccuracy.add(new IntSetting.Builder()
        .name("path-check-steps").description("Samples along path for obstacle detection.")
        .defaultValue(12).min(4).sliderMax(32).build());

    private final Setting<Boolean> validateReturn = sgAccuracy.add(new BoolSetting.Builder()
        .name("validate-return-position").description("Verify return position is safe.")
        .defaultValue(true).build());

    private final Setting<Boolean> legacyMode = sgLegacy.add(new BoolSetting.Builder()
        .name("legacy-mode")
        .description("Old jitter-based mode that consumes hunger. May work on older servers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> legacyMultiplier = sgLegacy.add(new IntSetting.Builder()
        .name("multiplier")
        .description("Higher values increase success chance at the cost of more hunger.")
        .defaultValue(90)
        .min(1)
        .sliderRange(1, 300)
        .visible(legacyMode::get)
        .build()
    );

    @SuppressWarnings("unused")
    private boolean isChargingBow = false;
    private int bowChargeTimer = 0;
    private boolean wasHoldingBow = false;

    private Entity target = null;
    private boolean executingInteract = false;
    private boolean paction = false;

    public SnowRIK() {
        super(AddonTemplate.CATEGORY, "Dookinqq RIK", "Unified ranged instakill with projectile + teleport + legacy mode.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        target = findTarget();

        if (!bowMachinegun.get() || mc.player == null || mc.interactionManager == null) return;

        boolean holdingBow = mc.player.getMainHandStack().getItem() instanceof BowItem
            || mc.player.getOffHandStack().getItem() instanceof BowItem;

        if (!holdingBow) {
            isChargingBow = false;
            bowChargeTimer = 0;
            wasHoldingBow = false;
            return;
        }

        if (holdingBow && !wasHoldingBow) bowChargeTimer = 0;

        if (isChargingBow) {
            bowChargeTimer++;
            if (bowChargeTimer >= chargeTicks.get()) {
                fireArrowTick();
                bowChargeTimer = 0;
            }
        } else bowChargeTimer = 0;

        wasHoldingBow = holdingBow;
    }

    private void fireArrowTick() {
        if (mc.getNetworkHandler() == null || mc.player == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
            BlockPos.ORIGIN,
            Direction.DOWN,
            mc.player.getInventory().getSelectedSlot()
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        if (event.packet instanceof PlayerInteractItemC2SPacket packet) {
            ItemStack stack = packet.getHand() == Hand.MAIN_HAND ? mc.player.getMainHandStack() : mc.player.getOffHandStack();
            Item item = stack.getItem();

            if (isValidProjectile(item)) {
                if (executingInteract) return;
                executingInteract = true;
                event.cancel();

                try {
                    if (legacyMode.get()) sendLegacyPackets();
                    else interact(packet.getHand());
                } finally {
                    executingInteract = false;
                }
                return;
            }

            if (item instanceof BowItem && projectileItems.get().contains(Items.BOW)) isChargingBow = true;
        }

        if (event.packet instanceof PlayerActionC2SPacket packet && !paction) {
            if (packet.getAction() != PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) return;

            Item a = mc.player.getMainHandStack().getItem();
            Item b = mc.player.getOffHandStack().getItem();
            if (!isAction(a) && !isAction(b)) return;

            isChargingBow = false;
            event.cancel();

            if (legacyMode.get()) sendLegacyPackets();
            else action(packet.getAction());
        }
    }

    private void sendLegacyPackets() {
        if (mc.player == null) return;

        boolean antihungerWasEnabled = false;
        AntiHunger ah = Modules.get().get(AntiHunger.class);
        if (ah != null && ah.isActive()) {
            ah.toggle();
            antihungerWasEnabled = true;
        }

        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

        for (int i = 0; i < legacyMultiplier.get(); i++) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() - 0.000000001, mc.player.getZ(), true, mc.player.horizontalCollision
            ));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() + 0.000000001, mc.player.getZ(), false, mc.player.horizontalCollision
            ));
        }

        if (!mc.options.sprintKey.isPressed() || !mc.options.getSprintToggled().getValue()) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }

        if (antihungerWasEnabled && ah != null) ah.toggle();
    }

    private Entity findTarget() {
        if (mc.player == null || mc.world == null) return null;

        List<Entity> candidates = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (!isValidListTarget(e)) continue;
            if (!attackFriends.get() && e instanceof PlayerEntity pe && Friends.get().isFriend(pe)) continue;
            if (!isValidTarget(e)) continue;
            candidates.add(e);
        }

        if (candidates.isEmpty()) return null;

        return switch (targetPriority.get()) {
            case Closest -> candidates.stream().min(Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.player))).orElse(null);
            case LowestHealth -> candidates.stream().min(Comparator.comparingDouble(this::healthOf)).orElse(null);
            case HighestHealth -> candidates.stream().max(Comparator.comparingDouble(this::healthOf)).orElse(null);
            case Crosshair -> candidates.stream().min(Comparator.comparingDouble(this::crosshairScore)).orElse(null);
        };
    }

    private double healthOf(Entity e) {
        if (e instanceof net.minecraft.entity.LivingEntity le) return le.getHealth();
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
        if (mc.player.distanceTo(entity) > maxDistance.get()) return false;
        if (requireLineOfSight.get() && !canSeeTarget(entity)) return false;
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

    public void interact(Hand hand) {
        if (target == null || !isValidTarget(target) || mc.player == null) return;

        Entity mover = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;
        Vec3d home = new Vec3d(mover.getX(), mover.getY(), mover.getZ());

        Box tb = target.getBoundingBox();
        Vec3d top = new Vec3d((tb.minX + tb.maxX) / 2.0, tb.maxY, (tb.minZ + tb.maxZ) / 2.0);

        Vec3d aboveHome = home.add(0, teleportHeight.get(), 0);
        Vec3d aboveTarget = top.add(0, teleportHeight.get(), 0);
        Vec3d shotPos = top.add(0, targetOffset.get(), 0);
        Vec3d homeOffset = getOffset(home);

        if (invalid(aboveHome) || invalid(aboveTarget) || invalid(shotPos) || (validateReturn.get() && invalid(homeOffset))) return;
        if (!hasClearPath(aboveHome, aboveTarget) || !hasClearPath(aboveTarget, shotPos)) return;

        for (int i = 0; i < spamPackets.get(); i++) {
            if (mover == mc.player) mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
            else mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mover));
        }

        moveTo(mover, aboveHome);
        moveTo(mover, aboveTarget);
        moveTo(mover, shotPos);

        float yaw = mc.player.getYaw();
        for (int i = 0; i < interactAmount.get(); i++) {
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(hand, 0, yaw, 90.0f));
        }

        moveTo(mover, aboveTarget);
        moveTo(mover, aboveHome);
        moveTo(mover, home);
        moveTo(mover, homeOffset);
    }

    public void action(PlayerActionC2SPacket.Action action) {
        if (target == null || !isValidTarget(target) || mc.player == null) return;

        Entity mover = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;
        Vec3d home = new Vec3d(mover.getX(), mover.getY(), mover.getZ());

        Box tb = target.getBoundingBox();
        Vec3d top = new Vec3d((tb.minX + tb.maxX) / 2.0, tb.maxY, (tb.minZ + tb.maxZ) / 2.0);

        Vec3d aboveHome = home.add(0, teleportHeight.get(), 0);
        Vec3d aboveTarget = top.add(0, teleportHeight.get(), 0);
        Vec3d shotPos = top.add(0, targetOffset.get(), 0);
        Vec3d homeOffset = getOffset(home);

        if (invalid(aboveHome) || invalid(aboveTarget) || invalid(shotPos) || (validateReturn.get() && invalid(homeOffset))) return;
        if (!hasClearPath(aboveHome, aboveTarget) || !hasClearPath(aboveTarget, shotPos)) return;

        for (int i = 0; i < spamPackets.get(); i++) {
            if (mover == mc.player) mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
            else mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mover));
        }

        moveTo(mover, aboveHome);
        moveTo(mover, aboveTarget);
        moveTo(mover, shotPos);

        paction = true;
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(action, BlockPos.ORIGIN, Direction.DOWN, 0));
        paction = false;

        moveTo(mover, aboveTarget);
        moveTo(mover, aboveHome);
        moveTo(mover, home);
        moveTo(mover, homeOffset);
    }

    private void moveTo(Entity entity, Vec3d pos) {
        if (mc.player == null) return;

        if (entity == mc.player) {
            PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, false, false);
            ((IPlayerMoveC2SPacket) packet).meteor$setTag(1337);
            mc.player.networkHandler.sendPacket(packet);
        } else {
            mc.player.networkHandler.sendPacket(new VehicleMoveC2SPacket(pos, entity.getYaw(), entity.getPitch(), false));
        }

        entity.setPosition(pos);
    }

    private boolean invalid(Vec3d pos) {
        if (mc.world == null || mc.player == null) return true;

        BlockPos bp = BlockPos.ofFloored(pos);
        if (mc.world.getChunk(bp) == null) return true;

        Entity entity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;

        Box targetBox = entity.getBoundingBox().offset(
            pos.x - entity.getX(),
            pos.y - entity.getY(),
            pos.z - entity.getZ()
        );

        for (BlockPos checkPos : BlockPos.iterate(
            BlockPos.ofFloored(targetBox.minX, targetBox.minY, targetBox.minZ),
            BlockPos.ofFloored(targetBox.maxX, targetBox.maxY, targetBox.maxZ)
        )) {
            BlockState state = mc.world.getBlockState(checkPos);
            if (state.isOf(Blocks.LAVA) || !state.getCollisionShape(mc.world, checkPos).isEmpty()) return true;
        }

        for (Entity e : mc.world.getOtherEntities(entity, targetBox)) {
            if (e.isCollidable(entity)) return true;
        }

        return false;
    }

    private boolean hasClearPath(Vec3d start, Vec3d end) {
        if (invalid(start) || invalid(end)) return false;

        int steps = Math.max(10, Math.min(200, (int) (start.distanceTo(end) * pathCheckSteps.get())));
        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            Vec3d sample = start.lerp(end, t);
            if (invalid(sample)) return false;
        }
        return true;
    }

    private Vec3d getOffset(Vec3d base) {
        double dx = offsetHorizontal.get();
        double dy = offsetY.get();

        Vec3d[] offsets = new Vec3d[] {
            base.add( dx, dy,  0),
            base.add(-dx, dy,  0),
            base.add( 0, dy,  dx),
            base.add( 0, dy, -dx),
            base.add( dx, dy,  dx),
            base.add(-dx, dy, -dx),
            base.add(-dx, dy,  dx),
            base.add( dx, dy, -dx)
        };

        List<Vec3d> list = Arrays.asList(offsets);
        Collections.shuffle(list);

        for (Vec3d p : list) if (!invalid(p)) return p;

        Vec3d fallback = base.add(0, dy, 0);
        return invalid(fallback) ? base : fallback;
    }

    private boolean isValidProjectile(Item item) {
        return (item instanceof EnderPearlItem && projectileItems.get().contains(Items.ENDER_PEARL)) ||
            (item instanceof SplashPotionItem && projectileItems.get().contains(Items.SPLASH_POTION)) ||
            (item instanceof LingeringPotionItem && projectileItems.get().contains(Items.LINGERING_POTION)) ||
            (item instanceof ExperienceBottleItem && projectileItems.get().contains(Items.EXPERIENCE_BOTTLE)) ||
            (item instanceof SnowballItem && projectileItems.get().contains(Items.SNOWBALL)) ||
            (item instanceof WindChargeItem && projectileItems.get().contains(Items.WIND_CHARGE)) ||
            (item instanceof EggItem && projectileItems.get().contains(Items.EGG));
    }

    private boolean isAction(Item item) {
        return (item instanceof BowItem && projectileItems.get().contains(Items.BOW)) ||
            (item instanceof TridentItem && projectileItems.get().contains(Items.TRIDENT));
    }
}
