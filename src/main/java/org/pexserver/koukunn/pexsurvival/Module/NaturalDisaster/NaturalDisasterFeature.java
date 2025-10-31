package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster;

import org.pexserver.koukunn.pexsurvival.Core.Feature.Feature;
import org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters.DisasterRegistry;
import org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters.Disaster;
import org.pexserver.koukunn.pexsurvival.Core.Config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Player;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class NaturalDisasterFeature implements Feature {

    private boolean enabled = false;
    private final Random random = new Random();
    private BukkitRunnable disasterTask;
    private Plugin plugin;
    
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
    
    // 災害の継続時間（ティック）：設定ファイルから読み込む
    private int minDurationTicks = 600;  // デフォルト30秒
    private int maxDurationTicks = 1200; // デフォルト60秒
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
        
        // プラグインインスタンスを取得
        plugin = Bukkit.getPluginManager().getPlugin("pexsurvival");
        if (plugin == null) {
            Bukkit.getLogger().severe("プラグインインスタンスが見つかりません！");
            return;
        }
        
        // DisasterRegistryを初期化
        DisasterRegistry.initialize(plugin);
        
        // 設定を読み込む
        loadConfig();
        
        enabled = true;
        startDisasterTask();
        Bukkit.getLogger().info("自然災害機能が有効になりました (持続時間: " + (minDurationTicks/20) + "-" + (maxDurationTicks/20) + "秒)");
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
        disasterTask.runTaskTimer(plugin, 0, DISASTER_INTERVAL);
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
            int duration = random.nextInt(maxDurationTicks - minDurationTicks + 1) + minDurationTicks;
            
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
            int maxDuration = maxDurations.getOrDefault(worldName, maxDurationTicks);
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
     * 設定ファイルから災害の持続時間を読み込む
     */
    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "PEXConfig/naturaldisaster.json");
        
        // 設定ファイルが存在しない場合はデフォルト設定で作成
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }
        
        // 設定ファイルを読み込む
        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            DisasterConfig config = gson.fromJson(reader, DisasterConfig.class);
            
            if (config != null) {
                // 秒をティックに変換（1秒 = 20ティック）
                minDurationTicks = config.getDefaultMinSeconds() * 20;
                maxDurationTicks = config.getDefaultMaxSeconds() * 20;
                
                Bukkit.getLogger().info("災害設定を読み込みました: " + config.getDefaultMinSeconds() + "-" + config.getDefaultMaxSeconds() + "秒");
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("災害設定の読み込みに失敗しました。デフォルト設定を使用します: " + e.getMessage());
        }
    }
    
    /**
     * デフォルト設定ファイルを作成
     */
    private void createDefaultConfig(File configFile) {
        try {
            // 親ディレクトリを作成
            configFile.getParentFile().mkdirs();
            
            // デフォルト設定を作成
            DisasterConfig defaultConfig = new DisasterConfig();
            defaultConfig.setDefaultMinSeconds(30);
            defaultConfig.setDefaultMaxSeconds(60);
            
            // JSONに書き込む
            try (FileWriter writer = new FileWriter(configFile)) {
                Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                gson.toJson(defaultConfig, writer);
                Bukkit.getLogger().info("デフォルト災害設定ファイルを作成しました: " + configFile.getPath());
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("デフォルト設定ファイルの作成に失敗しました: " + e.getMessage());
        }
    }
    
    /**
     * プラグインインスタンスを取得（他のクラスからアクセス可能）
     */
    public Plugin getPlugin() {
        return plugin;
    }
}

