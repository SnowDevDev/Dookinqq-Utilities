package com.example.addon.events;

import net.minecraft.screen.slot.SlotActionType;
import meteordevelopment.meteorclient.events.Cancellable;

public class ClickWindowEvent extends Cancellable {
    private static final ClickWindowEvent INSTANCE = new ClickWindowEvent();

    public int windowId;
    public int slotId;
    public int mouseButtonClicked;
    public SlotActionType mode;

    public static ClickWindowEvent get(int windowId, int slotId, int mouseButtonClicked, SlotActionType mode) {
        INSTANCE.setCancelled(false);
        INSTANCE.windowId = windowId;
        INSTANCE.slotId = slotId;
        INSTANCE.mouseButtonClicked = mouseButtonClicked;
        INSTANCE.mode = mode;
        return INSTANCE;
    }
}