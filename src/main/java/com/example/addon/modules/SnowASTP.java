package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class SnowASTP extends Module {
  public SnowASTP() {
    super(AddonTemplate.CATEGORY, "Dookinqq ASTP", "Cancels out s2c teleportation packets.");
  }

  @EventHandler(priority = EventPriority.HIGHEST + 1)
  private void onReceivePacket(PacketEvent.Receive event) {
    if (!(event.packet instanceof PlayerPositionLookS2CPacket pkt)) return;
    event.cancel();

    mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(pkt.teleportId()));
  }

}



