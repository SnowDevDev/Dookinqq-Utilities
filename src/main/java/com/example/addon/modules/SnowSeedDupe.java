package com.example.addon.modules;

import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import com.example.addon.AddonTemplate;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public class SnowSeedDupe extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> whitelist = sgGeneral.add(new ItemListSetting.Builder()
        .name("whitelist")
        .description("Items allowed to move to offhand.")
        .defaultValue(Items.TOTEM_OF_UNDYING)
        .build()
    );

    private final Setting<Integer> cycleDelay = sgGeneral.add(new IntSetting.Builder()
        .name("cycle-delay")
        .description("Ticks between full cycles.")
        .defaultValue(10)
        .min(1)
        .sliderMax(200)
        .build()
    );

    private final Setting<Integer> waitAfterCommand = sgGeneral.add(new IntSetting.Builder()
        .name("wait-after-command")
        .description("Ticks to wait after sending command.")
        .defaultValue(10)
        .min(1)
        .sliderMax(40)
        .build()
    );

    private enum State {
        OFFHAND,
        COMMAND,
        WAITING,
        CLICK,
        DELAY
    }

    private State state = State.OFFHAND;
    private int timer = 0;

    public SnowSeedDupe() {
        super(AddonTemplate.CATEGORY, "Dookinqq Seed Dupe", "Repeats a controlled offhand + command + click cycle infinitely.");
    }

    @Override
    public void onActivate() {
        state = State.OFFHAND;
        timer = 0;
    }

    @Override
    public void onDeactivate() {
        state = State.OFFHAND;
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        switch (state) {
            case OFFHAND -> {
                moveWhitelistItemToOffhand();
                state = State.COMMAND;
            }

            case COMMAND -> {
                mc.player.networkHandler.sendChatCommand("seed");
                timer = 0;
                state = State.WAITING;
            }

            case WAITING -> {
                if (++timer >= waitAfterCommand.get()) {
                    timer = 0;
                    state = State.CLICK;
                }
            }

            case CLICK -> {
                clickOneSeed();
                timer = 0;
                state = State.DELAY;
            }

            case DELAY -> {
                if (++timer >= cycleDelay.get()) {
                    timer = 0;
                    state = State.OFFHAND; // loops forever
                }
            }
        }
    }

    private void moveWhitelistItemToOffhand() {
        for (Item item : whitelist.get()) {
            FindItemResult result = InvUtils.find(item);
            if (result.found()) {
                InvUtils.move().from(result.slot()).toOffhand();
                return;
            }
        }
    }

private void clickOneSeed() {
    FindItemResult result = InvUtils.find(Items.WHEAT_SEEDS);

    if (!result.found()) return;

    InvUtils.click().slot(result.slot());
}
}


