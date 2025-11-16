package org.pexserver.koukunn.pexsurvival.Module.RandomBreak;

import org.pexserver.koukunn.pexsurvival.Core.Feature.Feature;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * ブロック破壊時に一定確率で特殊効果（爆発/ランダムブロック/範囲破壊）を発生させる機能
 */
public class ExplosiveBlockFeature implements Feature {

    private boolean enabled = false;
    private final Random random = new Random();

    @Override
    public String getFeatureName() { return "randombreak"; }

    @Override
    public String getDescription() { return "ブロック破壊時に確率で爆発・ランダムブロック・範囲破壊を発生させます"; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void enable() {
        if (enabled) return;
        enabled = true;
        Bukkit.getLogger().info("[PEX] ExplosiveBlockFeature enabled");
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;
        Bukkit.getLogger().info("[PEX] ExplosiveBlockFeature disabled");
    }

    @Override
    public void reload() {
        // no-op
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        if (player == null) return;
        switch (player.getGameMode()) {
            case CREATIVE, SPECTATOR -> { return; }
            default -> {}
        }

        // 発生確率（デフォルト10%）
        double chance = 0.10;
        if (random.nextDouble() >= chance) return;

        Block block = event.getBlock();
        var pl = JavaPlugin.getPlugin(org.pexserver.koukunn.pexsurvival.Loader.class);

        Bukkit.getScheduler().runTask(pl, () -> {
            try {
                int mode = random.nextInt(3); // 0=爆発,1=ランダム置換,2=範囲破壊
                Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                switch (mode) {
                    case 0 -> { // 小規模爆発
                        block.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 2.0f, false, true);
                    }
                    case 1 -> { // ランダムブロックに置換
                        Material[] pool = new Material[] {Material.STONE, Material.DIRT, Material.SAND, Material.COBBLESTONE, Material.GRAVEL, Material.OAK_LOG, Material.OAK_PLANKS};
                        Material m = pool[random.nextInt(pool.length)];
                        block.setType(m);
                    }
                    case 2 -> { // 範囲破壊（半径2）
                        int r = 2;
                        int bx = block.getX();
                        int by = block.getY();
                        int bz = block.getZ();
                        for (int x = bx - r; x <= bx + r; x++) {
                            for (int y = Math.max(0, by - r); y <= Math.min(block.getWorld().getMaxHeight(), by + r); y++) {
                                for (int z = bz - r; z <= bz + r; z++) {
                                    Block b = block.getWorld().getBlockAt(x, y, z);
                                    if (b.isEmpty()) continue;
                                    try { b.breakNaturally(); } catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("ExplosiveBlockFeature: エラー: " + e.getMessage());
            }
        });
    }
}
