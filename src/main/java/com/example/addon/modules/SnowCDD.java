package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Set;

public class SnowCDD extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> chatFeedback = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-feedback")
        .description("Displays findings in chat.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> displayCoords = sgGeneral.add(new BoolSetting.Builder()
        .name("display-coords")
        .description("Display disturbance coordinates in chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> fpDistance = sgGeneral.add(new IntSetting.Builder()
        .name("false-positive-distance")
        .description("Ignore disturbance if regular air exists in this radius.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 10)
        .build()
    );

    private final Setting<Boolean> removeOutsideRenderDistance = sgRender.add(new BoolSetting.Builder()
        .name("remove-outside-render-distance")
        .description("Remove cached disturbances/chunks outside render distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> renderDistance = sgRender.add(new IntSetting.Builder()
        .name("render-distance-chunks")
        .description("Chunks around player to scan/render.")
        .defaultValue(32)
        .min(6)
        .sliderRange(6, 256)
        .build()
    );

    private final Setting<Boolean> tracers = sgRender.add(new BoolSetting.Builder()
        .name("tracers")
        .description("Draw tracers to disturbances.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> nearestTracerOnly = sgRender.add(new BoolSetting.Builder()
        .name("nearest-tracer-only")
        .description("Only draw tracer to nearest disturbance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How boxes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("disturbance-side-color")
        .description("Fill color.")
        .defaultValue(new SettingColor(255, 0, 130, 55))
        .visible(() -> shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("disturbance-line-color")
        .description("Line color.")
        .defaultValue(new SettingColor(255, 0, 130, 200))
        .visible(() -> shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both || tracers.get())
        .build()
    );

    private final Set<ChunkPos> scannedChunks = new HashSet<>();
    private final Set<BlockPos> disturbanceLocations = new HashSet<>();
    private BlockPos nearestDisturbance;

    public SnowCDD() {
        super(AddonTemplate.CATEGORY, "Dookinqq CDD", "Detects suspicious regular-air pockets next to cave air.");
    }

    @Override
    public void onActivate() {
        clearData();
    }

    @Override
    public void onDeactivate() {
        clearData();
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen || event.screen instanceof LevelLoadingScreen) clearData();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        clearData();
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;

        scanLoadedArea();

        if (nearestTracerOnly.get()) {
            nearestDisturbance = findNearestDisturbance();
        } else {
            nearestDisturbance = null;
        }

        if (removeOutsideRenderDistance.get()) {
            pruneOutsideRange();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null) return;
        if (sideColor.get().a <= 5 && lineColor.get().a <= 5) return;

        if (nearestTracerOnly.get()) {
            if (nearestDisturbance == null) return;
            if (!isWithinRenderDistance(nearestDisturbance)) return;
            renderDisturbance(nearestDisturbance, event, true);
            return;
        }

        for (BlockPos pos : disturbanceLocations) {
            if (!isWithinRenderDistance(pos)) continue;
            renderDisturbance(pos, event, false);
        }
    }

    private void scanLoadedArea() {
        int radius = renderDistance.get();
        int playerChunkX = mc.player.getChunkPos().x;
        int playerChunkZ = mc.player.getChunkPos().z;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;

                if (!mc.world.isChunkLoaded(cx, cz)) continue;

                ChunkPos pos = new ChunkPos(cx, cz);
                if (scannedChunks.contains(pos)) continue;

                WorldChunk chunk = mc.world.getChunk(cx, cz);
                if (chunk == null || chunk.isEmpty()) continue;

                processChunk(chunk);
                scannedChunks.add(pos);
            }
        }
    }

    private void processChunk(WorldChunk chunk) {
        int minY = mc.world.getBottomY();
        int maxY = mc.world.getRegistryKey() == World.NETHER ? 126 : 180;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos cavePos = new BlockPos(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);
                    BlockState state = chunk.getBlockState(cavePos);

                    if (state.getBlock() != Blocks.CAVE_AIR) continue;

                    for (Direction dir : Direction.values()) {
                        BlockPos airPos = cavePos.offset(dir);

                        if (!isCandidateDisturbance(airPos, dir)) continue;
                        if (isFalsePositive(airPos)) continue;
                        disturbanceFound(airPos);
                    }
                }
            }
        }
    }

    private boolean isCandidateDisturbance(BlockPos airPos, Direction facingFromCave) {
        if (mc.world.getBlockState(airPos).getBlock() != Blocks.AIR) return false;

        BlockPos past = airPos.offset(facingFromCave);
        if (mc.world.getBlockState(past).getBlock() == Blocks.AIR) return false;

        if (facingFromCave.getAxis() == Direction.Axis.X) {
            return isNotAir(airPos.up()) && isNotAir(airPos.down()) && isNotAir(airPos.north()) && isNotAir(airPos.south());
        }

        if (facingFromCave.getAxis() == Direction.Axis.Z) {
            return isNotAir(airPos.up()) && isNotAir(airPos.down()) && isNotAir(airPos.east()) && isNotAir(airPos.west());
        }

        return isNotAir(airPos.east()) && isNotAir(airPos.west()) && isNotAir(airPos.north()) && isNotAir(airPos.south());
    }

    private boolean isNotAir(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() != Blocks.AIR;
    }

    private boolean isFalsePositive(BlockPos disturbance) {
        int r = fpDistance.get();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos p = disturbance.add(x, y, z);
                    if (p.equals(disturbance)) continue;
                    if (mc.world.getBlockState(p).getBlock() == Blocks.AIR) return true;
                }
            }
        }

        return false;
    }

    private void disturbanceFound(BlockPos disturbance) {
        if (!disturbanceLocations.add(disturbance)) return;

        if (!chatFeedback.get()) return;

        if (displayCoords.get()) {
            ChatUtils.sendMsg(Text.of("Disturbance in cave air found: " + disturbance));
        } else {
            ChatUtils.sendMsg(Text.of("Disturbance in cave air found!"));
        }
    }

    private BlockPos findNearestDisturbance() {
        BlockPos nearest = null;
        double nearestSq = Double.MAX_VALUE;

        for (BlockPos pos : disturbanceLocations) {
            double dx = pos.getX() - mc.player.getX();
            double dz = pos.getZ() - mc.player.getZ();
            double sq = dx * dx + dz * dz;

            if (sq < nearestSq) {
                nearestSq = sq;
                nearest = pos;
            }
        }

        return nearest;
    }

    private boolean isWithinRenderDistance(BlockPos pos) {
        BlockPos playerPos = new BlockPos(mc.player.getBlockX(), pos.getY(), mc.player.getBlockZ());
        return playerPos.isWithinDistance(pos, renderDistance.get() * 16.0);
    }

    private void renderDisturbance(BlockPos pos, Render3DEvent event, boolean forceTracer) {
        Box box = new Box(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), new Vec3d(pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1));

        if (tracers.get() && Math.abs(box.minX - RenderUtils.center.x) <= renderDistance.get() * 16.0 && Math.abs(box.minZ - RenderUtils.center.z) <= renderDistance.get() * 16.0) {
            if (!nearestTracerOnly.get() || forceTracer) {
                event.renderer.line(
                    RenderUtils.center.x,
                    RenderUtils.center.y,
                    RenderUtils.center.z,
                    box.minX + 0.5,
                    box.minY + ((box.maxY - box.minY) / 2.0),
                    box.minZ + 0.5,
                    lineColor.get()
                );
            }
        }

        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sideColor.get(), new Color(0, 0, 0, 0), shapeMode.get(), 0);
    }

    private void pruneOutsideRange() {
        int radius = renderDistance.get();
        int pcx = mc.player.getChunkPos().x;
        int pcz = mc.player.getChunkPos().z;

        scannedChunks.removeIf(cp -> Math.abs(cp.x - pcx) > radius || Math.abs(cp.z - pcz) > radius);
        disturbanceLocations.removeIf(pos -> {
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            return Math.abs(cx - pcx) > radius || Math.abs(cz - pcz) > radius;
        });
    }

    private void clearData() {
        scannedChunks.clear();
        disturbanceLocations.clear();
        nearestDisturbance = null;
    }
}




