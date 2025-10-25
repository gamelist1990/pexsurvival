package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mobパニック災害
 * プレイヤーの付近30ブロック以内に1秒ごとにMobをスポーンさせ、最終的に合計50体になるようにする
 */
public class MobPanicDisaster implements Disaster {

    // 同一ワールドで同時に複数のスポーンランナブルが動作しないようにするガード
    private static final Set<java.util.UUID> activeWorlds = ConcurrentHashMap.newKeySet();

    @Override
    public String getName() {
        return "Mobパニック";
    }

    @Override
    public void execute(World world, Location center, Random random) {
        // 同一ワールドですでにスポーン処理が動作している場合は二重実行を防ぐ
        java.util.UUID worldId = world.getUID();
        if (!activeWorlds.add(worldId)) {
            // 既に実行中
            return;
        }

        // 災害中心周辺（半径30）に合計50体を1秒ごとにスポーン
        new BukkitRunnable() {
            int spawned = 0;

            @Override
            public void run() {
                try {
                    if (spawned >= 10) {
                        this.cancel();
                        return;
                    }

                    // ランダムなスポーン地点（中心から半径30以内）
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = random.nextDouble() * 30.0;
                    double dx = Math.cos(angle) * radius;
                    double dz = Math.sin(angle) * radius;
                    double dy = (random.nextDouble() - 0.5) * 10; // 少し上下ランダム

                    Location spawnLoc = center.clone().add(dx, dy, dz);
                    int groundY = spawnLoc.getWorld().getHighestBlockYAt(spawnLoc.getBlockX(), spawnLoc.getBlockZ());
                    spawnLoc.setY(groundY + 1);

                    EntityType type = random.nextBoolean() ? EntityType.SKELETON : EntityType.ZOMBIE;

                    world.spawnParticle(Particle.SMOKE, spawnLoc, 20, 0.5, 0.5, 0.5, 0.02);
                    world.playSound(spawnLoc, Sound.ENTITY_ZOMBIE_AMBIENT, 0.8f, 1.0f);

                    LivingEntity mob = (LivingEntity) world.spawnEntity(spawnLoc, type);
                    mob.setCustomName("処刑人");
                    mob.setGlowing(true);
                    mob.setCustomNameVisible(false);

                    // スポーン直後に近くのプレイヤーをターゲットに設定し、継続的に追尾させる
                    Player nearest = findNearestPlayer(spawnLoc, world, 30.0);
                    if (nearest != null && mob instanceof Creature) {
                        ((Creature) mob).setTarget(nearest);
                    }

                    // 各Mobごとに短い間隔で再ターゲットを行い、リアルタイム追尾を維持する
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try {
                                if (mob == null || mob.isDead()) {
                                    this.cancel();
                                    return;
                                }

                                Player p = findNearestPlayer(mob.getLocation(), world, 40.0);
                                if (p != null && mob instanceof Creature) {
                                    ((Creature) mob).setTarget(p);
                                }
                            } catch (Exception e) {
                                Bukkit.getLogger().warning("Mob retarget task error: " + e.getMessage());
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(Bukkit.getPluginManager().getPlugin("pexsurvival"), 0L, 10L); // 10ティックごと

                    spawned++;
                } catch (Exception e) {
                    // 何か問題が起きてもタスクを止めないがログに残す
                    Bukkit.getLogger().warning("MobPanicDisaster encountered an error: " + e.getMessage());
                }
            }

            @Override
            public void cancel() {
                super.cancel();
                // 終了時にワールドガードを解除する
                activeWorlds.remove(worldId);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("pexsurvival"), 0L, 20L);
    }

    /**
     * 指定位置から最も近いプレイヤーを取得（範囲制限）
     */
    private Player findNearestPlayer(Location loc, World world, double maxDistance) {
        Player nearest = null;
        double bestSq = maxDistance * maxDistance;
        for (Player p : world.getPlayers()) {
            double distSq = p.getLocation().distanceSquared(loc);
            if (distSq <= bestSq) {
                bestSq = distSq;
                nearest = p;
            }
        }
        return nearest;
    }
}
