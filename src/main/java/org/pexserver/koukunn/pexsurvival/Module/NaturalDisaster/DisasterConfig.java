package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster;

import java.util.HashMap;
import java.util.Map;

/**
 * 自然災害の設定クラス
 * PEXConfig/naturaldisaster.json から読み込まれる
 */
public class DisasterConfig {
    
    private int default_min_seconds = 30;
    private int default_max_seconds = 60;
    private Map<String, DisasterDuration> per_disaster = new HashMap<>();
    
    public int getDefaultMinSeconds() {
        return default_min_seconds;
    }
    
    public void setDefaultMinSeconds(int default_min_seconds) {
        this.default_min_seconds = default_min_seconds;
    }
    
    public int getDefaultMaxSeconds() {
        return default_max_seconds;
    }
    
    public void setDefaultMaxSeconds(int default_max_seconds) {
        this.default_max_seconds = default_max_seconds;
    }
    
    public Map<String, DisasterDuration> getPerDisaster() {
        return per_disaster;
    }
    
    public void setPerDisaster(Map<String, DisasterDuration> per_disaster) {
        this.per_disaster = per_disaster;
    }
    
    /**
     * 災害ごとの持続時間設定
     */
    public static class DisasterDuration {
        private int min_seconds;
        private int max_seconds;
        
        public int getMinSeconds() {
            return min_seconds;
        }
        
        public void setMinSeconds(int min_seconds) {
            this.min_seconds = min_seconds;
        }
        
        public int getMaxSeconds() {
            return max_seconds;
        }
        
        public void setMaxSeconds(int max_seconds) {
            this.max_seconds = max_seconds;
        }
    }
}
