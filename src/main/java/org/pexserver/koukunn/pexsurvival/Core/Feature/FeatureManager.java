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
    private final Map<String, Feature> features = new HashMap<>();

    public FeatureManager(Plugin plugin) {
        this.plugin = plugin;
        this.pluginManager = plugin.getServer().getPluginManager();
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
        
        // 機能を有効化
        feature.enable();
        
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
        
        return feature.isEnabled();
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
