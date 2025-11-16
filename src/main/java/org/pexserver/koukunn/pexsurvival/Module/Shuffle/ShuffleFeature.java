package org.pexserver.koukunn.pexsurvival.Module.Shuffle;

import org.pexserver.koukunn.pexsurvival.Core.Feature.Feature;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import java.util.*;

/**
 * プレイヤー位置を定期的にランダムにシャッフルする機能
 * 1〜3分のランダム間隔で、オンラインプレイヤー全員の位置をシャッフルします。
 * ボスバーで残り時間を表示します。プレイヤーが1人しかいない場合は何もしません。
 */
public class ShuffleFeature implements Feature {

    private boolean enabled = false;
    private BossBar bossBar = null;
    private BukkitTask task = null;
    private int countdown = 0;
    private int currentInterval = 60;
    private final Random random = new Random();

    private org.pexserver.koukunn.pexsurvival.Loader plugin() {
        return JavaPlugin.getPlugin(org.pexserver.koukunn.pexsurvival.Loader.class);
    }

    @Override
    public String getFeatureName() {
        return "shuffle";
    }

    @Override
    public String getDescription() {
        return "プレイヤー全員の位置を定期的にランダムで入れ替えます（1〜3分毎）";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        if (enabled) return;
        enabled = true;
        Bukkit.getLogger().info("[PEX] ShuffleFeature enabled");
        startTask();
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;
        Bukkit.getLogger().info("[PEX] ShuffleFeature disabled");
        stopTask();
    }

    @Override
    public void reload() {
        // 再起動相当の処理
        stopTask();
        if (enabled) startTask();
    }

    private void startTask() {
        var pl = plugin();
        if (pl == null) return;

        // ボスバーを生成
        bossBar = pl.getServer().createBossBar("シャッフル準備中", BarColor.BLUE, BarStyle.SOLID);
        bossBar.setVisible(false);

        resetCountdown();

        task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!enabled) return;

                Collection<? extends Player> online = Bukkit.getServer().getOnlinePlayers();
                if (online.size() <= 1) {
                    if (bossBar != null) bossBar.setVisible(false);
                    return;
                }

                // 常に最新のプレイヤーにボスバーを表示
                bossBar.removeAll();
                for (Player p : online) bossBar.addPlayer(p);
                bossBar.setVisible(true);

                bossBar.setTitle("シャッフルまで: " + countdown + "秒");
                double prog = Math.max(0.0, Math.min(1.0, (double) countdown / (double) currentInterval));
                bossBar.setProgress(prog);

                countdown -= 1;
                if (countdown <= 0) {
                    // 実行
                    try {
                        shufflePlayers();
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("ShuffleFeature: シャッフル中に例外: " + e.getMessage());
                    }
                    // 次の周期をセット
                    resetCountdown();
                }
            }
        }.runTaskTimer(pl, 20L, 20L);
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null;
        }
    }

    private void resetCountdown() {
        // 1分(60s)〜3分(180s) のランダム
        currentInterval = 60 + random.nextInt(121); // 0..120 -> 60..180
        countdown = currentInterval;
    }

    private void shufflePlayers() {
        List<Player> players = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
        if (players.size() <= 1) return;

        List<Location> locs = new ArrayList<>();
        for (Player p : players) locs.add(p.getLocation().clone());

        Collections.shuffle(locs, random);

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Location dest = locs.get(i);
            try {
                p.teleport(dest);
            } catch (Exception ignored) {
            }
        }

        Bukkit.getLogger().info("[PEX] プレイヤー位置をシャッフルしました（" + players.size() + " 人）");
    }
}
