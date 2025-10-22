package org.pexserver.koukunn.pexsurvival.Module.NoJump;

import org.pexserver.koukunn.pexsurvival.Core.Feature.Feature;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

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
        Bukkit.getLogger().info("[PEX] NoJumpFeature enabled");
    }

    @Override
    public void disable() {
        this.enabled = false;
        Bukkit.getLogger().info("[PEX] NoJumpFeature disabled");
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
        if (!enabled)
            return;

        Player player = event.getPlayer();
        // クリエイティブとスペクテイターは対象外
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        // ジャンプ（フライトトグル）を禁止
        if (event.isFlying()) {
            event.setCancelled(true);
        }
    }

    /**
     * PlayerMoveEvent を使ってジャンプの開始を検出し、上向き移動をキャンセルしてジャンプを抑止します
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!enabled)
            return;

        double fromY = event.getFrom().getY();
        double toY = event.getTo().getY();
        Player player = event.getPlayer();

        // クリエイティブとスペクテイターは対象外
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        if (toY > fromY + 0.01) {
            double vy = player.getVelocity().getY();
            boolean upwardVelocity = vy > 0.05;
            boolean wasOnGround = player.isOnGround() || isStandingOnSolid(player);

            if (upwardVelocity && wasOnGround) {
                event.setTo(event.getFrom());
                Vector vel = player.getVelocity();
                vel.setY(0);
                player.setVelocity(vel);
            }
        }
    }

    private boolean isStandingOnSolid(org.bukkit.entity.Player player) {
        var feet = player.getLocation();
        double[] offs = { -0.35, 0.0, 0.35 };
        for (double ox : offs) {
            for (double oz : offs) {
                var sample = feet.clone().add(ox, -0.1, oz).getBlock();
                try {
                    if (sample.getType().isSolid())
                        return true;
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
        return false;
    }
}
