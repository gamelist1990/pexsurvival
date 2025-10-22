package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster;

import org.pexserver.koukunn.pexsurvival.Core.Feature.Feature;
import org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters.DisasterRegistry;
import org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters.Disaster;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class NaturalDisasterFeature implements Feature {

    private boolean enabled = false;
    private final Random random = new Random();
    private BukkitRunnable disasterTask;
    
    // ワールドごとのボスバー管理
    private final Map<String, BossBar> bossBars = new HashMap<>();
    private final Map<String, Disaster> currentDisasters = new HashMap<>();
    private final Map<String, Integer> remainingTimes = new HashMap<>();
    private final Map<String, Integer> maxDurations = new HashMap<>();
    
    // ワールドごとの災害の中心位置を管理
    private final Map<String, org.bukkit.Location> disasterCenters = new HashMap<>();
    // ワールドごとのターゲットプレイヤー(UUID)を管理（災害がこのプレイヤーを追尾する）
    private final Map<String, UUID> disasterTargets = new HashMap<>();
    
    // ワールドごとの火災カウンター（5秒ごとに火を広げる）
    private final Map<String, Integer> fireCounters = new HashMap<>();
    
    // 災害の継続時間（ティック）：2分～4分 = 2400～4800ティック
    private static final int MIN_DISASTER_DURATION = 2400; // 2分
    private static final int MAX_DISASTER_DURATION = 4800; // 4分
    private static final int DISASTER_INTERVAL = 20; // 1秒ごと（20ティック）
    private static final int FIRE_SPREAD_INTERVAL = 100; // 5秒ごと（100ティック）

    @Override
    public String getFeatureName() {
        return "naturaldisaster";
    }

    @Override
    public String getDescription() {
        return "自然災害が定期的に発生します（雷、地震、台風、竜巻、火災など）";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        if (enabled) return;
        
        enabled = true;
        startDisasterTask();
        Bukkit.getLogger().info("自然災害機能が有効になりました");
    }

    @Override
    public void disable() {
        if (!enabled) return;
        
        enabled = false;
        stopDisasterTask();
        
        // ボスバーをすべてクリア
        for (BossBar bossBar : bossBars.values()) {
            bossBar.removeAll();
        }
        bossBars.clear();
        currentDisasters.clear();
        remainingTimes.clear();
        maxDurations.clear();
        disasterCenters.clear();
        fireCounters.clear();
        
        Bukkit.getLogger().info("自然災害機能が無効になりました");
    }

    @Override
    public void reload() {
        disable();
        enable();
    }

    /**
     * 災害タスクを開始
     */
    private void startDisasterTask() {
        disasterTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    processWorld(world);
                }
            }
        };
        
        // 1秒（20ティック）ごとに実行
        disasterTask.runTaskTimer(Bukkit.getPluginManager().getPlugin("pexsurvival"), 0, DISASTER_INTERVAL);
    }

    /**
     * 災害タスクを停止
     */
    private void stopDisasterTask() {
        if (disasterTask != null) {
            disasterTask.cancel();
            disasterTask = null;
        }
    }

    /**
     * ワールドの災害処理
     */
    private void processWorld(World world) {
        String worldName = world.getName();
        
        // 新しい災害が必要か判定
        if (!currentDisasters.containsKey(worldName)) {
            // プレイヤーが存在しない場合はスキップ
            if (world.getPlayers().isEmpty()) {
                return;
            }
            
            // 新しい災害を開始
            Disaster disaster = DisasterRegistry.getRandomDisaster(random);
            int duration = random.nextInt(MAX_DISASTER_DURATION - MIN_DISASTER_DURATION + 1) + MIN_DISASTER_DURATION;
            
            currentDisasters.put(worldName, disaster);
            remainingTimes.put(worldName, duration);
            maxDurations.put(worldName, duration);
            
            // ターゲットプレイヤーをランダムに選択し、災害はこのプレイヤーを追尾する
            Player targetPlayer = world.getPlayers().get(random.nextInt(world.getPlayers().size()));
            disasterTargets.put(worldName, targetPlayer.getUniqueId());
            org.bukkit.Location centerLocation = targetPlayer.getLocation().clone();
            disasterCenters.put(worldName, centerLocation);
            
            // ボスバーを作成
            BossBar bossBar = Bukkit.createBossBar(
                    "【" + disaster.getName() + "】残り時間: " + (duration / 20) + "秒",
                    BarColor.RED,
                    BarStyle.SOLID
            );
            
            // ワールドのすべてのプレイヤーをボスバーに追加
            for (Player player : world.getPlayers()) {
                bossBar.addPlayer(player);
            }
            
            bossBars.put(worldName, bossBar);
            fireCounters.put(worldName, 0); // 火災カウンター初期化
        }
        
        int remaining = remainingTimes.get(worldName);
        Disaster currentDisaster = currentDisasters.get(worldName);
        org.bukkit.Location centerLocation = disasterCenters.get(worldName);

        // ターゲットプレイヤーが存在する場合は常にその現在位置を中心にする（災害がプレイヤーを追尾する）
        UUID targetUuid = disasterTargets.get(worldName);
        if (targetUuid != null) {
            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null && target.isOnline() && target.getWorld().equals(world)) {
                centerLocation = target.getLocation().clone();
                disasterCenters.put(worldName, centerLocation);
            } else {
                // ターゲットが見つからなければ新しいターゲットを選ぶ（オンラインプレイヤーからランダム）
                if (!world.getPlayers().isEmpty()) {
                    Player newTarget = world.getPlayers().get(random.nextInt(world.getPlayers().size()));
                    disasterTargets.put(worldName, newTarget.getUniqueId());
                    centerLocation = newTarget.getLocation().clone();
                    disasterCenters.put(worldName, centerLocation);
                }
            }
        }
        
        // 火災の場合は5秒ごとに発動
        if (currentDisaster.getName().equals("火災")) {
            int fireCounter = fireCounters.getOrDefault(worldName, 0);
            if (fireCounter >= FIRE_SPREAD_INTERVAL) {
                currentDisaster.execute(world, centerLocation, random);
                fireCounters.put(worldName, 0);
            } else {
                fireCounters.put(worldName, fireCounter + DISASTER_INTERVAL);
            }
        } else {
            // 他の災害は毎秒実行
            currentDisaster.execute(world, centerLocation, random);
        }
        
        // 残り時間を減らす
        remaining -= DISASTER_INTERVAL;
        remainingTimes.put(worldName, remaining);
        
        // ボスバーを更新
        BossBar bossBar = bossBars.get(worldName);
        if (bossBar != null) {
            int maxDuration = maxDurations.getOrDefault(worldName, MAX_DISASTER_DURATION);
            double progress = Math.max(0.0, (double) remaining / (double) maxDuration);
            
            bossBar.setProgress(progress);
            bossBar.setTitle("【" + currentDisaster.getName() + "】残り時間: " + (remaining / 20) + "秒");
        }
        
        // 災害終了判定
        if (remaining <= 0) {
            if (bossBar != null) {
                bossBar.removeAll();
            }
            bossBars.remove(worldName);
            currentDisasters.remove(worldName);
            remainingTimes.remove(worldName);
            maxDurations.remove(worldName);
            disasterCenters.remove(worldName);
            disasterTargets.remove(worldName);
            fireCounters.remove(worldName);
        }
    }
    
    /**
     * プレイヤーの位置から最低50ブロック離れた場所を計算
     */
    // (以前はプレイヤーから離れた位置に災害を生成していましたが)
    // 現在は災害はターゲットプレイヤーを追尾する設計のため、このメソッドは不要です。
}

