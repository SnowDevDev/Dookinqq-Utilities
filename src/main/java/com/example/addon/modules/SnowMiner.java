package com.example.addon.modules;

import com.example.addon.AddonTemplate;
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

    private boolean sendingQuiet = false;

    public SnowMiner() {
        super(AddonTemplate.CATEGORY, "Snow Miner", "Manipulates mining sequence to fix AutoMine breaks.");
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (sendingQuiet) return;

        if (!(event.packet instanceof PlayerActionC2SPacket packet)) return;

        if (UNBREAKABLE.contains(mc.world.getBlockState(packet.getPos()).getBlock())) {
            event.cancel();
            return;
        }

        if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
            sendingQuiet = true;

            mc.getNetworkHandler().sendPacket(
                new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, packet.getPos(), Direction.UP)
            );

            mc.getNetworkHandler().sendPacket(
                new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, packet.getPos(), Direction.UP)
            );

            mc.getNetworkHandler().sendPacket(
                new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos(), Direction.UP)
            );

            sendingQuiet = false;
            event.cancel();
        }
    }
}