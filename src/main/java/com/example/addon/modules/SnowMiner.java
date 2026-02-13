package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.mixin.IClientPlayNetworkHandlerMixin;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Direction;

import java.util.Set;

public class SnowMiner extends Module {

    private static final Set<Block> UNBREAKABLE = new ReferenceOpenHashSet<>(Set.of(
            Blocks.BEDROCK,
            Blocks.COMMAND_BLOCK,
            Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK,
            Blocks.END_PORTAL_FRAME,
            Blocks.BARRIER
    ));

    public SnowMiner() {
        super(AddonTemplate.CATEGORY, "Snow Miner", "Manipulates mining sequence to fix AutoMine breaks.");
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerActionC2SPacket packet)) return;

        // Cancel unbreakable blocks
        if (UNBREAKABLE.contains(mc.world.getBlockState(packet.getPos()).getBlock())) {
            event.cancel();
            return;
        }

        // Fix mining sequence (credit: Shoreline)
        if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
            sendQuietPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, packet.getPos(), Direction.UP));
            sendQuietPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, packet.getPos(), Direction.UP));
            sendQuietPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos(), Direction.UP));
            event.cancel();
        }
    }

    private void sendQuietPacket(final Packet<?> packet) {
        if (mc.getNetworkHandler() != null) {
            ((IClientPlayNetworkHandlerMixin) mc.getNetworkHandler()).addon$sendQuietPacket(packet);
        }
    }
}