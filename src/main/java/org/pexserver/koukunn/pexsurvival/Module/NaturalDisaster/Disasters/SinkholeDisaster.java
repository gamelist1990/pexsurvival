package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 陥没災害
 * プレイヤーがほとんど動かずに3秒経つと、その下にランダムな3x3の穴を掘る
 */
public class SinkholeDisaster implements Disaster {

    // 監視中のプレイヤーを管理して二重開始を防止
    private final Set<UUID> watching = ConcurrentHashMap.newKeySet();

    @Override
    public String getName() {
        return "陥没";
    }

    @Override
    public void execute(World world, Location center, Random random) {
        // 各プレイヤーについて静止判定を行う（負荷に応じて間引き可能）
        for (Player player : world.getPlayers()) {
            UUID id = player.getUniqueId();
            if (watching.contains(id)) continue; // 既に監視中

            // 位置のスナップショット
            Location startLoc = player.getLocation().clone();
            watching.add(id);

            // 3秒（60ティック）間、ほとんど動いていないか確認するタスク
            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    try {
                        ticks++;
                        // プレイヤーがログアウトまたはワールドが違う場合は監視中止
                        if (!player.isOnline() || !player.getWorld().equals(world)) {
                            watching.remove(id);
                            this.cancel();
                            return;
                        }

                        // 動いた距離が小さいか判定（例: 0.5 ブロック以内）
                        if (player.getLocation().distanceSquared(startLoc) > 0.5 * 0.5) {
                            // 動いた -> 監視中止
                            watching.remove(id);
                            this.cancel();
                            return;
                        }

                        if (ticks >= 60) { // 3秒到達
                            // 陥没を生成
                            createSinkholeUnderPlayer(player, world, random);
                            watching.remove(id);
                            this.cancel();
                            return;
                        }
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("Sinkhole monitor error: " + e.getMessage());
                        watching.remove(id);
                        this.cancel();
                    }
                }
            }.runTaskTimer(Bukkit.getPluginManager().getPlugin("pexsurvival"), 0L, 1L);
        }
    }

    private void createSinkholeUnderPlayer(Player player, World world, Random random) {
        Location pl = player.getLocation();
        int centerX = pl.getBlockX();
        int centerZ = pl.getBlockZ();
        int surfaceY = world.getHighestBlockYAt(centerX, centerZ);

        // 基本深さ3～6
        int depth = 3 + random.nextInt(4);

        // 中央を少し深くするなどランダム性を加える
        int centerExtra = random.nextInt(2); // 0 or 1

        // 5x5 エリアを下方向に穴にする（-2..2）
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                // 中心からの距離に応じて深さを調整して自然にする
                int ring = Math.max(Math.abs(dx), Math.abs(dz));
                int localDepth = depth - ring + (ring == 0 ? centerExtra : 0);
                // 外周は浅めにする
                if (ring >= 2) localDepth = Math.max(1, localDepth - 1);
                localDepth = Math.max(2, localDepth);

                int startY = Math.min(pl.getBlockY(), surfaceY);
                for (int y = startY; y > startY - localDepth; y--) {
                    Block b = world.getBlockAt(centerX + dx, y, centerZ + dz);
                    if (isUnbreakable(b)) continue;
                    b.setType(Material.AIR);
                }
            }
        }

        // エフェクト
        Location effectLoc = new Location(world, centerX + 0.5, pl.getBlockY(), centerZ + 0.5);
        world.spawnParticle(Particle.CLOUD, effectLoc, 30, 1.0, 1.0, 1.0, 0.05);
        world.playSound(effectLoc, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 1.0f);

        // 少しプレイヤーを落とす/ノックバックして陥没に落ちやすくする
        try {
            player.setVelocity(player.getVelocity().add(new org.bukkit.util.Vector(0, -0.4, 0)));
        } catch (Exception ignored) {}
    }

    private boolean isUnbreakable(Block b) {
        Material m = b.getType();
        // 岩盤やエンドポータル系、破壊したくない主要なブロックは除外
        return m == Material.BEDROCK || m == Material.END_PORTAL || m == Material.END_PORTAL_FRAME ||
                m == Material.OBSIDIAN || m == Material.CRYING_OBSIDIAN;
    }
}
