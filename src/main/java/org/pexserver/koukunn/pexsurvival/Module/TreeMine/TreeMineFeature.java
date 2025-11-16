package org.pexserver.koukunn.pexsurvival.Module.TreeMine;

import org.pexserver.koukunn.pexsurvival.Core.Feature.Feature;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.*;

/**
 * 木を一撃でまとめて破壊する機能
 */
public class TreeMineFeature implements Feature {

    private boolean enabled = false;
    private static final Set<Material> LOGS = new HashSet<>();
    static {
        for (Material m : Material.values()) {
            String n = m.name();
            if (n.endsWith("_LOG") || n.endsWith("_WOOD")) LOGS.add(m);
        }
    }
    private static final Set<Material> AXES = new HashSet<>();
    static {
        AXES.addAll(Arrays.asList(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
        ));
    }

    @Override public String getFeatureName() { return "treemine"; }
    @Override public String getDescription() { return "木を一撃で伐採します（接続するログをまとめて破壊）"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void enable() { if (enabled) return; enabled = true; Bukkit.getLogger().info("[PEX] TreeMineFeature enabled"); }
    @Override public void disable() { if (!enabled) return; enabled = false; Bukkit.getLogger().info("[PEX] TreeMineFeature disabled"); }
    @Override public void reload() {}

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        if (player == null) return;
        switch (player.getGameMode()) {
            case CREATIVE, SPECTATOR -> { return; }
            default -> {}
        }

        Block b = event.getBlock();
        // メインハンドが斧でない場合は処理しない
        var hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) return;
        boolean isAxe = false;
        try {
            isAxe = org.bukkit.Tag.ITEMS_AXES.isTagged(hand.getType());
        } catch (Throwable ignored) {
            isAxe = AXES.contains(hand.getType());
        }
        if (!isAxe) return;
        // 誤爆防止: スニーク(シフト)していない場合は動作しない
        if (!player.isSneaking()) {
            return;
        }
        if (!LOGS.contains(b.getType())) return;

        List<Block> toBreak = new ArrayList<>();
        Deque<Block> stack = new ArrayDeque<>();
        Set<Block> seen = new HashSet<>();
        stack.push(b);
        seen.add(b);
        int limit = 300; // 上限

        while (!stack.isEmpty() && toBreak.size() < limit) {
            Block cur = stack.pop();
            toBreak.add(cur);
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                Block nb = cur.getRelative(dx, dy, dz);
                if (nb == null) continue;
                if (seen.contains(nb)) continue;
                if (LOGS.contains(nb.getType())) {
                    seen.add(nb);
                    stack.push(nb);
                }
            }
        }

        toBreak.removeIf(x -> x.equals(b));

        if (toBreak.isEmpty()) return;

        var pl = JavaPlugin.getPlugin(org.pexserver.koukunn.pexsurvival.Loader.class);

        final int perTick = 6;

        new BukkitRunnable() {
            final Iterator<Block> it = toBreak.iterator();

            @Override
            public void run() {
                if (!enabled || !it.hasNext()) {
                    cancel();
                    return;
                }
                int i = 0;
                while (i < perTick && it.hasNext()) {
                    Block br = it.next();
                    try {
                        br.breakNaturally(player.getInventory().getItemInMainHand());
                        if (br.getWorld() != null) br.getWorld().playSound(br.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);
                    } catch (Exception ignored) {}
                    i++;
                }
            }
        }.runTaskTimer(pl, 1L, 1L);
    }
}
