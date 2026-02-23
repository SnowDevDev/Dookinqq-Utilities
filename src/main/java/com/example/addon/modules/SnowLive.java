package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Set;

/**
 * SnowLive - Auto totem dupe and offhand link module.
 *
 * Features:
 * - Automatic totem duplication via chat command
 * - Offhand linking with entity proximity triggers
 * - Intelligent slot selection (inventory vs hotbar preferences)
 * - Container pulling (pull from chests while inventory management)
 * - Cooldown and threshold systems
 * - Low-totem warnings
 * - Per-entity-type trigger configuration
 */
public class SnowLive extends Module {

    // ─── Setting Groups ───────────────────────────────────────────────────────
    private final SettingGroup sgGeneral  = settings.getDefaultGroup();
    private final SettingGroup sgOffhand  = settings.createGroup("Offhand Link");
    private final SettingGroup sgDupe     = settings.createGroup("Dupe Settings");
    private final SettingGroup sgNotify   = settings.createGroup("Notifications");

    // ─── General Dupe Settings ────────────────────────────────────────────────
    private final Setting<Boolean> alwaysDupe = sgGeneral.add(new BoolSetting.Builder()
        .name("always-dupe")
        .description("Dupe totems regardless of current count.")
        .defaultValue(false)
        .build());

    private final Setting<Integer> dupeWhen = sgGeneral.add(new IntSetting.Builder()
        .name("dupe-when")
        .description("Trigger a dupe when totem count falls at or below this value.")
        .defaultValue(10)
        .min(1)
        .max(64)
        .sliderMin(1)
        .sliderMax(64)
        .visible(() -> !alwaysDupe.get())
        .build());

    private final Setting<Integer> dupeAmount = sgGeneral.add(new IntSetting.Builder()
        .name("dupe-amount")
        .description("How many totems to request per dupe command.")
        .defaultValue(8)
        .min(1)
        .max(64)
        .sliderMin(1)
        .sliderMax(64)
        .build());

    private final Setting<Integer> dupeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("dupe-delay")
        .description("Ticks to wait before switching back to the previous hotbar slot.")
        .defaultValue(2)
        .min(0)
        .max(20)
        .sliderMin(0)
        .sliderMax(20)
        .build());

    private final Setting<Boolean> pullFromContainers = sgGeneral.add(new BoolSetting.Builder()
        .name("pull-from-containers")
        .description("Quick-move totems from open containers into inventory.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> switchBack = sgGeneral.add(new BoolSetting.Builder()
        .name("switch-back")
        .description("Return to the previous hotbar slot after issuing a dupe command.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> dupeEnabled = sgGeneral.add(new BoolSetting.Builder()
        .name("dupe-enabled")
        .description("Enable the auto-dupe logic. Disable to only use offhand link.")
        .defaultValue(true)
        .build());

    // ─── Dupe Logic Settings ──────────────────────────────────────────────────
    private final Setting<Boolean> clampToDeficit = sgDupe.add(new BoolSetting.Builder()
        .name("clamp-to-deficit")
        .description("Cap the dupe amount to exactly how many totems are missing.")
        .defaultValue(true)
        .visible(() -> !alwaysDupe.get())
        .build());

    private final Setting<Boolean> preferInventory = sgDupe.add(new BoolSetting.Builder()
        .name("prefer-inventory-over-hotbar")
        .description("When duping, switch to an inventory totem rather than burning a hotbar slot.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> hotbarOnly = sgDupe.add(new BoolSetting.Builder()
        .name("hotbar-only")
        .description("Only dupe if a totem is already in the hotbar (no slot-switch needed).")
        .defaultValue(false)
        .build());

    private final Setting<Integer> minDupeInterval = sgDupe.add(new IntSetting.Builder()
        .name("min-dupe-interval")
        .description("Minimum ticks between successive dupe commands (anti-spam).")
        .defaultValue(10)
        .min(0)
        .sliderMax(40)
        .build());

    // ─── Offhand Link Settings ────────────────────────────────────────────────
    private final Setting<Boolean> offhandLink = sgOffhand.add(new BoolSetting.Builder()
        .name("offhand-link")
        .description("Equip a totem to the offhand when trigger entities are nearby.")
        .defaultValue(true)
        .build());

    private final Setting<Double> offhandRadius = sgOffhand.add(new DoubleSetting.Builder()
        .name("radius")
        .description("Radius within which to activate offhand link.")
        .defaultValue(8.0)
        .min(1.0)
        .sliderRange(1.0, 64.0)
        .visible(offhandLink::get)
        .build());

    private final Setting<Set<EntityType<?>>> offhandEntities = sgOffhand.add(
        new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entity types that trigger the offhand equip.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .visible(offhandLink::get)
            .build());

    private final Setting<Boolean> includeFriends = sgOffhand.add(new BoolSetting.Builder()
        .name("include-friends")
        .description("Count friends as trigger entities.")
        .defaultValue(false)
        .visible(offhandLink::get)
        .build());

    private final Setting<Boolean> offhandAlways = sgOffhand.add(new BoolSetting.Builder()
        .name("always-equip-offhand")
        .description("Always keep a totem in the offhand, ignoring proximity checks.")
        .defaultValue(false)
        .visible(offhandLink::get)
        .build());

    private final Setting<Boolean> preferInventoryForOffhand = sgOffhand.add(new BoolSetting.Builder()
        .name("prefer-inventory-for-offhand")
        .description("Source the offhand totem from inventory slots before the hotbar.")
        .defaultValue(true)
        .visible(offhandLink::get)
        .build());

    private final Setting<Integer> offhandEquipCooldown = sgOffhand.add(new IntSetting.Builder()
        .name("equip-cooldown")
        .description("Ticks between offhand equip attempts.")
        .defaultValue(5)
        .min(1)
        .sliderMax(20)
        .visible(offhandLink::get)
        .build());

    // ─── Notification Settings ────────────────────────────────────────────────
    private final Setting<Boolean> warnLowTotems = sgNotify.add(new BoolSetting.Builder()
        .name("warn-low-totems")
        .description("Print a chat warning when totems fall below the threshold.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> warnThreshold = sgNotify.add(new IntSetting.Builder()
        .name("warn-threshold")
        .description("Totem count at which the low-totem warning fires.")
        .defaultValue(3)
        .min(1)
        .sliderMax(20)
        .visible(warnLowTotems::get)
        .build());

    private final Setting<Boolean> notifyDupe = sgNotify.add(new BoolSetting.Builder()
        .name("notify-on-dupe")
        .description("Print a chat message each time a dupe command is issued.")
        .defaultValue(false)
        .build());

    // ─── Runtime State ────────────────────────────────────────────────────────
    private int  switchBackTimer      = 0;
    private int  previousSlot         = -1;
    private int  dupeCooldown         = 0;
    private int  offhandEquipTimer    = 0;
    private boolean warnedThisCycle   = false;

    // ��── Constructor ───────────────────────────────────────────────────────────
    public SnowLive() {
        super(AddonTemplate.CATEGORY, "Dookinqq Live",
              "Auto totem dupe + offhand link with proximity triggers.");
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onActivate() {
        switchBackTimer = 0;
        previousSlot = -1;
        dupeCooldown = 0;
        offhandEquipTimer = 0;
        warnedThisCycle = false;
    }
}