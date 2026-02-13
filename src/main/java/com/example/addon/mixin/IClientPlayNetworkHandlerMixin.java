package com.example.addon.mixin;

import net.minecraft.network.packet.Packet;

public interface IClientPlayNetworkHandlerMixin {
    void addon$sendQuietPacket(Packet<?> packet);
}