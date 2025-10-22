package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 災害レジストリ - すべての災害を管理
 */
public class DisasterRegistry {
    
    private static final List<Disaster> disasters = new ArrayList<>();
    
    static {
        // ここに全ての災害を登録
        disasters.add(new LightningDisaster());
        disasters.add(new EarthquakeDisaster());
        disasters.add(new FireDisaster());
    disasters.add(new SandstormDisaster());
    disasters.add(new RandomBlockDisaster());
    disasters.add(new SinkholeDisaster());
        disasters.add(new MobPanicDisaster());
    }
    
    /**
     * ランダムに災害を選択
     */
    public static Disaster getRandomDisaster(Random random) {
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
}
