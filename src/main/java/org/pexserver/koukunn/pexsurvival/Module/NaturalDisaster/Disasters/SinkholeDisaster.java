package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * 陥没災害
 * プレイヤーがほとんど動かずに3秒経つと、その下にランダムな3x3の穴を掘る
 */
public class SinkholeDisaster implements Disaster {

    // 監視中のプレイヤー管理は不要になったため削除
    // 歩いた位置を記録して、3秒後に順次壊すためのキュー
    private final ConcurrentMap<String, Long> scheduledBlocks = new ConcurrentHashMap<>();
    private static int animationIdCounter = 1;
    private volatile boolean processorRunning = false;

    @Override
    public String getName() {
        return "陥没";
    }

    @Override
    public void execute(World world, Location center, Random random) {
        // 各プレイヤーが歩いた位置を記録し、3秒後に順次壊していく
        for (Player player : world.getPlayers()) {
            // プレイヤーの現在位置の地面ブロックとその周囲(3x3)を予定に登録
            recordVisitedBlocks(player);
        }

        // バックグラウンドプロセッサを起動（1回のみ）
        startProcessorIfNeeded(world, random);
    }

    private void recordVisitedBlocks(Player player) {
        Location loc = player.getLocation();
        org.bukkit.World world = player.getWorld();
        int baseX = loc.getBlockX();
        int baseY = loc.getBlockY() - 1; // 足元の下のブロックを狙う
        int baseZ = loc.getBlockZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;
                int y = baseY;
                Block b = world.getBlockAt(x, y, z);
                if (b == null) continue;
                if (b.isEmpty() || isUnbreakable(b) || b.isLiquid()) continue;
                String key = blockKey(world.getName(), x, y, z);
                scheduledBlocks.putIfAbsent(key, System.currentTimeMillis());
            }
        }
    }

    private String blockKey(String worldName, int x, int y, int z) {
        return worldName + ":" + x + "," + y + "," + z;
    }

    private void startProcessorIfNeeded(World world, Random random) {
        if (processorRunning) return;
        processorRunning = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    for (Map.Entry<String, Long> e : scheduledBlocks.entrySet()) {
                        if (now - e.getValue() >= 3000) { // 3秒経過
                            String key = e.getKey();
                            scheduledBlocks.remove(key);
                            // parse key
                            String[] parts = key.split(":" , 2);
                            if (parts.length < 2) continue;
                            String wname = parts[0];
                            String[] coords = parts[1].split(",");
                            if (coords.length != 3) continue;
                            int x = Integer.parseInt(coords[0]);
                            int y = Integer.parseInt(coords[1]);
                            int z = Integer.parseInt(coords[2]);
                            World w = Bukkit.getWorld(wname);
                            if (w == null) continue;
                            Block block = w.getBlockAt(x, y, z);
                            if (block == null) continue;
                            if (block.isEmpty() || isUnbreakable(block) || block.isLiquid()) continue;
                            // アニメーションを再生してブロックを壊す
                            playMiningAnimation(w, block, random);
                        }
                    }
                } catch (Exception ex) {
                    Bukkit.getLogger().warning("Sinkhole processor error: " + ex.getMessage());
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("pexsurvival"), 0L, 10L);
    }

    // createSinkholeUnderPlayer removed: new walking-based sinkhole logic is used instead

    private boolean isUnbreakable(Block b) {
        Material m = b.getType();
        // 岩盤やエンドポータル系、破壊したくない主要なブロックは除外
        return m == Material.BEDROCK || m == Material.END_PORTAL || m == Material.END_PORTAL_FRAME ||
                m == Material.OBSIDIAN || m == Material.CRYING_OBSIDIAN;
    }

    // 以下は EarthquakeDisaster と同等のブロック破壊アニメーション用メソッド
    private void playMiningAnimation(World world, Block block, Random random) {
        new BukkitRunnable() {
            int stage = 0;
            final Location blockLoc = block.getLocation();
            final int animationId = getNextAnimationId();
            final int maxStage = 9;

            @Override
            public void run() {
                try {
                    Block current = blockLoc.getBlock();
                    if (current == null || current.getType() == Material.AIR) {
                        for (Player player : getNearbyPlayers(blockLoc, 48)) {
                            sendBlockDestructionPacket(player, animationId, blockLoc, -1);
                        }
                        this.cancel();
                        return;
                    }

                    int displayStage = Math.min(stage, maxStage);

                    for (Player player : getNearbyPlayers(blockLoc, 64)) {
                        sendBlockDestructionPacket(player, animationId, blockLoc, displayStage);
                    }

                    world.playSound(blockLoc, Sound.BLOCK_STONE_HIT, 1.0f, 0.5f + (stage * 0.03f));

                    if (stage < maxStage) {
                        stage++;
                    } else {
                        current.setType(Material.AIR);

                        for (Player player : getNearbyPlayers(blockLoc, 64)) {
                            sendBlockDestructionPacket(player, animationId, blockLoc, -1);
                        }
                        world.playSound(blockLoc, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
                        this.cancel();
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().warning("playMiningAnimation error: " + e.getMessage());
                    this.cancel();
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("pexsurvival"), 0, 2);
    }

    private void sendBlockDestructionPacket(Player player, int entityId, Location blockLoc, int stage) {
        final boolean DEBUG = false;

        try {
            float progress;
            if (stage < 0) {
                progress = 0.0f;
            } else {
                progress = Math.min(Math.max(stage, 0), 9) / 9.0f;
            }

            if (DEBUG) {
                Bukkit.getLogger().info(String.format("[SinkholeDisaster] sendBlockDestructionPacket -> player=%s, entityId=%d, block=%s,%d,%s, stage=%d, progress=%.3f",
                        player.getName(), entityId, blockLoc.getWorld().getName(), blockLoc.getBlockX(), blockLoc.getBlockZ(), stage, progress));
            }

            player.sendBlockDamage(blockLoc, progress, entityId);
        } catch (NoSuchMethodError e) {
            Bukkit.getLogger().warning("sendBlockDamage API not available: " + e.getMessage());
            if (DEBUG) Bukkit.getLogger().warning("[SinkholeDisaster] NoSuchMethodError in sendBlockDestructionPacket for player=" + player.getName());
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to call sendBlockDamage: " + e.getMessage());
            if (DEBUG) {
                Bukkit.getLogger().warning("[SinkholeDisaster] Exception while sending block damage to player=" + player.getName());
                e.printStackTrace();
            }
        }
    }

    private java.util.List<Player> getNearbyPlayers(Location loc, double radius) {
        java.util.List<Player> list = new java.util.ArrayList<>();
        World w = loc.getWorld();
        double r2 = radius * radius;
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= r2) list.add(p);
        }
        return list;
    }

    private synchronized int getNextAnimationId() {
        return animationIdCounter++;
    }
}
