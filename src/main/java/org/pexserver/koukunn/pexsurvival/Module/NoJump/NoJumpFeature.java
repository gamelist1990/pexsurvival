package org.pexserver.koukunn.pexsurvival.Module.NoJump;

import org.pexserver.koukunn.pexsurvival.Core.Feature.Feature;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleFlightEvent;

/**
 * ノージャンプ機能
 * ワールド全員のジャンプを無効/有効に切り替えます
 */
public class NoJumpFeature implements Feature {

    private boolean enabled = false;

    @Override
    public String getFeatureName() {
        return "nojump";
    }

    @Override
    public String getDescription() {
        return "ワールド全員のジャンプを禁止";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public void reload() {
        // リロード処理（必要に応じて実装）
    }

    /**
     * ジャンプイベントのハンドル
     */
    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if (!enabled) {
            return;
        }

        // ジャンプ（フライトトグル）を禁止
        if (event.isFlying()) {
            event.setCancelled(true);
        }
    }
}

