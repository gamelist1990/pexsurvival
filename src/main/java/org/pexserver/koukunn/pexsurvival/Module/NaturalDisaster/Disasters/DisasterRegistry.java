package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters;

import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 災害レジストリ - すべての災害を管理
 */
public class DisasterRegistry {
    
    private static final List<Disaster> disasters = new ArrayList<>();
    private static boolean initialized = false;
    
    /**
     * 災害を初期化（プラグインインスタンスが必要）
     */
    public static void initialize(Plugin plugin) {
        if (initialized) {
            return;
        }
        
        // ここに全ての災害を登録
        disasters.add(new LightningDisaster());
        disasters.add(new EarthquakeDisaster());
        disasters.add(new FireDisaster());
        disasters.add(new SandstormDisaster());
        disasters.add(new RandomBlockDisaster());
        disasters.add(new SinkholeDisaster());
        disasters.add(new MobPanicDisaster(plugin));
        disasters.add(new ToxicFogDisaster());
        
        initialized = true;
    }
    
    /**
     * ランダムに災害を選択
     */
    public static Disaster getRandomDisaster(Random random) {
        if (disasters.isEmpty()) {
            throw new IllegalStateException("DisasterRegistry is not initialized! Call initialize(Plugin) first.");
        }
        return disasters.get(random.nextInt(disasters.size()));
    }
    
    /**
     * 登録されている全ての災害を取得
     */
    public static List<Disaster> getAllDisasters() {
        return new ArrayList<>(disasters);
    }
    
    /**
     * 災害を登録
     */
    public static void registerDisaster(Disaster disaster) {
        disasters.add(disaster);
    }
    
    /**
     * すべての災害をクリア（テストやリロード時に使用）
     */
    public static void clear() {
        disasters.clear();
        initialized = false;
    }
}
