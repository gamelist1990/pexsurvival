package org.pexserver.koukunn.pexsurvival.Core.Feature;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * 機能の登録・管理・制御を行うマネージャー
 */
public class FeatureManager {

    private final Plugin plugin;
    private final PluginManager pluginManager;
    private final org.pexserver.koukunn.pexsurvival.Core.Config.ConfigManager configManager;
    private final Map<String, Feature> features = new HashMap<>();

    public FeatureManager(Plugin plugin) {
        this(plugin, new org.pexserver.koukunn.pexsurvival.Core.Config.ConfigManager(plugin));
    }

    public FeatureManager(Plugin plugin, org.pexserver.koukunn.pexsurvival.Core.Config.ConfigManager configManager) {
        this.plugin = plugin;
        this.pluginManager = plugin.getServer().getPluginManager();
        this.configManager = configManager;
        // NOTE: do not clean or mutate config here — features are not yet registered.
    }

    /**
     * 機能を登録します
     * @param feature 登録する機能
     */
    public void registerFeature(Feature feature) {
        String name = feature.getFeatureName().toLowerCase();
        features.put(name, feature);
        
        // イベントリスナーとして登録
        pluginManager.registerEvents(feature, plugin);
        
        // コンフィグの優先ルール:
        // 1) features.json に "states" が存在する -> その中にキーがあればその値を採用
        //    その中にキーが無ければ "無効"
        // 2) "states" が存在しない -> feature.getDefaultEnabled() を採用
        try {
            var opt = configManager.loadConfig("features.json");
            if (opt.isPresent()) {
                var cfg = opt.get();
                Object o = cfg.get("states");
                if (o instanceof java.util.Map<?, ?>) {
                    var map = (java.util.Map<?, ?>) o;
                    if (map.containsKey(name)) {
                        Object v = map.get(name);
                        if (v instanceof Boolean && (Boolean) v) {
                            feature.enable();
                        } else {
                            feature.disable();
                        }
                    } else {
                        // states は存在するがキーがなければ無効とみなす
                        feature.disable();
                    }
                } else {
                    // states キー自体が無ければデフォルトに従う
                    if (feature.getDefaultEnabled()) feature.enable(); else feature.disable();
                }
            } else {
                // 設定ファイルが無ければデフォルト
                if (feature.getDefaultEnabled()) feature.enable(); else feature.disable();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("設定読み込み中に例外: " + e.getMessage());
            if (feature.getDefaultEnabled()) feature.enable(); else feature.disable();
        }
        
        plugin.getLogger().info("機能登録: " + feature.getFeatureName() + 
                              " (" + feature.getDescription() + ")");
    }

    /**
     * 複数の機能を一括登録
     * @param features 登録する機能の配列
     */
    public void registerAll(Feature... features) {
        for (Feature feature : features) {
            registerFeature(feature);
        }
    }

    /**
     * 機能を取得
     * @param name 機能名
     * @return 機能またはnull
     */
    public Feature getFeature(String name) {
        return features.get(name.toLowerCase());
    }

    /**
     * すべての機能を取得
     * @return 機能マップ
     */
    public Map<String, Feature> getFeatures() {
        return new HashMap<>(features);
    }

    /**
     * 機能を有効にします
     * @param name 機能名
     * @return 成功した場合true
     */
    public boolean enableFeature(String name) {
        Feature feature = getFeature(name);
        if (feature == null) {
            return false;
        }
        
        if (feature.isEnabled()) {
            return true;  // すでに有効
        }
        
        feature.enable();
        plugin.getLogger().info("機能有効化: " + feature.getFeatureName());
        saveFeatureState(name, true);
        return true;
    }

    /**
     * 機能を無効にします
     * @param name 機能名
     * @return 成功した場合true
     */
    public boolean disableFeature(String name) {
        Feature feature = getFeature(name);
        if (feature == null) {
            return false;
        }
        
        if (!feature.isEnabled()) {
            return true;  // すでに無効
        }
        
        feature.disable();
        plugin.getLogger().info("機能無効化: " + feature.getFeatureName());
        saveFeatureState(name, false);
        return true;
    }

    /**
     * 機能をトグル（有効/無効を切り替え）
     * @param name 機能名
     * @return トグル後の状態（有効な場合true）
     */
    public boolean toggleFeature(String name) {
        Feature feature = getFeature(name);
        if (feature == null) {
            return false;
        }
        
        if (feature.isEnabled()) {
            feature.disable();
            plugin.getLogger().info("機能無効化: " + feature.getFeatureName());
        } else {
            feature.enable();
            plugin.getLogger().info("機能有効化: " + feature.getFeatureName());
        }
        // トグル後の状態を保存
        saveFeatureState(name, feature.isEnabled());
        return feature.isEnabled();
    }

    /**
     * 保存された機能の有効/無効状態を読み込む
     */
    // 設定の事前読み込みは不要になったため削除

    // getSavedState は registerFeature の新しいロジックで不要になった

    private void saveFeatureState(String featureName, boolean enabled) {
        try {
            var cfg = configManager.loadConfig("features.json").orElseGet(() -> new org.pexserver.koukunn.pexsurvival.Core.Config.PEXConfig());
            Object o = cfg.get("states");
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            if (o instanceof java.util.Map<?, ?>) {
                for (java.util.Map.Entry<?, ?> e : ((java.util.Map<?, ?>) o).entrySet()) {
                    if (e.getKey() instanceof String) {
                        map.put((String) e.getKey(), e.getValue());
                    }
                }
            }
            map.put(featureName.toLowerCase(), enabled);
            cfg.put("states", map);
            configManager.saveConfig("features.json", cfg);
        } catch (Exception e) {
            plugin.getLogger().warning("設定の保存に失敗しました: " + e.getMessage());
        }
    }

    /**
     * 設定内に存在するが現在の登録リストにない（無効/古い）機能を削除して永続化する
     */
    private void cleanupInternalConfig() {
        try {
            var opt = configManager.loadConfig("features.json");
            if (opt.isEmpty()) return;
            var cfg = opt.get();
            Object o = cfg.get("states");
            if (!(o instanceof java.util.Map)) return;
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            if (o instanceof java.util.Map<?, ?>) {
                for (java.util.Map.Entry<?, ?> e : ((java.util.Map<?, ?>) o).entrySet()) {
                    if (e.getKey() instanceof String) {
                        map.put(((String) e.getKey()).toLowerCase(), e.getValue());
                    }
                }
            }
            boolean changed = false;
            java.util.List<String> keys = new java.util.ArrayList<>(map.keySet());
            for (String key : keys) {
                String lower = key.toLowerCase();
                if (!features.containsKey(lower)) {
                    map.remove(key);
                    changed = true;
                }
            }
            if (changed) {
                cfg.put("states", map);
                configManager.saveConfig("features.json", cfg);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("無効な機能の削除中にエラー: " + e.getMessage());
        }
    }

    /**
     * 外部から設定内の無効な機能を削除するための公開メソッド
     */
    public void cleanupConfig() {
        cleanupInternalConfig();
    }

    /**
     * 機能をリロードします
     * @param name 機能名
     * @return 成功した場合true
     */
    public boolean reloadFeature(String name) {
        Feature feature = getFeature(name);
        if (feature == null) {
            return false;
        }
        
        feature.reload();
        plugin.getLogger().info("機能リロード: " + feature.getFeatureName());
        return true;
    }

    /**
     * 機能が存在するかチェック
     * @param name 機能名
     * @return 存在する場合true
     */
    public boolean hasFeature(String name) {
        return features.containsKey(name.toLowerCase());
    }

    /**
     * 登録済みの全機能をリロード
     */
    public void reloadAll() {
        for (Feature feature : features.values()) {
            feature.reload();
            plugin.getLogger().info("機能リロード: " + feature.getFeatureName());
        }
    }

    /**
     * 登録済みの全機能を無効化
     */
    public void disableAll() {
        for (Feature feature : features.values()) {
            if (feature.isEnabled()) {
                feature.disable();
            }
        }
    }
}
