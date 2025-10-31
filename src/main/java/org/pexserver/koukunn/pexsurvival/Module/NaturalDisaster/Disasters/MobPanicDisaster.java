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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mobパニック災害
 * プレイヤーの付近30ブロック以内に1秒ごとにMobをスポーンさせ、最終的に合計50体になるようにする
 */
public class MobPanicDisaster implements Disaster {

    private final Plugin plugin;
    
    // 同一ワールドで同時に複数のスポーンランナブルが動作しないようにするガード
    private static final Set<java.util.UUID> activeWorlds = ConcurrentHashMap.newKeySet();
    
    // 各Mobの再ターゲットタスクを管理
    private static final Map<java.util.UUID, Set<BukkitTask>> mobTasks = new ConcurrentHashMap<>();
    
    public MobPanicDisaster(Plugin plugin) {
        this.plugin = plugin;
    }

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
                    // コメントとの整合性：合計50体に変更
                    if (spawned >= 50) {
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
                    BukkitTask retargetTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            try {
                                if (mob == null || mob.isDead()) {
                                    this.cancel();
                                    // タスクをリストから削除
                                    Set<BukkitTask> tasks = mobTasks.get(worldId);
                                    if (tasks != null) {
                                        tasks.remove(this);
                                    }
                                    return;
                                }

                                Player p = findNearestPlayer(mob.getLocation(), world, 40.0);
                                if (p != null && mob instanceof Creature) {
                                    ((Creature) mob).setTarget(p);
                                }
                            } catch (Exception e) {
                                Bukkit.getLogger().warning("Mob retarget task error: " + e.getMessage());
                                this.cancel();
                                // タスクをリストから削除
                                Set<BukkitTask> tasks = mobTasks.get(worldId);
                                if (tasks != null) {
                                    tasks.remove(this);
                                }
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 10L); // 10ティックごと
                    
                    // タスクを追跡
                    mobTasks.computeIfAbsent(worldId, k -> ConcurrentHashMap.newKeySet()).add(retargetTask);

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
                // すべての再ターゲットタスクをキャンセル
                cancelAllTasks(worldId);
            }
        }.runTaskTimer(plugin, 0L, 20L);
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
    
    /**
     * 指定ワールドのすべてのタスクをキャンセル
     */
    private static void cancelAllTasks(java.util.UUID worldId) {
        Set<BukkitTask> tasks = mobTasks.remove(worldId);
        if (tasks != null) {
            for (BukkitTask task : tasks) {
                if (task != null && !task.isCancelled()) {
                    task.cancel();
                }
            }
            tasks.clear();
        }
    }
    
    /**
     * すべてのワールドのタスクをキャンセル（プラグイン無効化時に使用）
     */
    public static void cancelAllTasksGlobal() {
        for (java.util.UUID worldId : new HashSet<>(mobTasks.keySet())) {
            cancelAllTasks(worldId);
        }
        activeWorlds.clear();
    }
}
