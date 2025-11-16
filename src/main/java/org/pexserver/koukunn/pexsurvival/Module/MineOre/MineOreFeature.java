package org.pexserver.koukunn.pexsurvival.Module.MineOre;

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
 * 鉱石を一撃でまとめて破壊する機能（鉱脈破壊）
 */
public class MineOreFeature implements Feature {

    private boolean enabled = false;
    private static final Set<Material> ORES = new HashSet<>(Arrays.asList(
            Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE, Material.DIAMOND_ORE,
            Material.EMERALD_ORE, Material.LAPIS_ORE, Material.REDSTONE_ORE, Material.NETHER_QUARTZ_ORE,
            Material.NETHER_GOLD_ORE, Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.COPPER_ORE
    ));

    private static final Set<Material> PICKAXES = new HashSet<>();
    static {
        PICKAXES.addAll(Arrays.asList(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE   
        ));
    }

    @Override public String getFeatureName() { return "mineore"; }
    @Override public String getDescription() { return "鉱脈を一撃で破壊します（同種の鉱石をまとめて破壊）"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void enable() { if (enabled) return; enabled = true; Bukkit.getLogger().info("[PEX] MineOreFeature enabled"); }
    @Override public void disable() { if (!enabled) return; enabled = false; Bukkit.getLogger().info("[PEX] MineOreFeature disabled"); }
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
        var hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) return;
        boolean isPick = false;
        try {
            isPick = org.bukkit.Tag.ITEMS_PICKAXES.isTagged(hand.getType());
        } catch (Throwable ignored) {
            isPick = PICKAXES.contains(hand.getType());
        }
        if (!isPick) return;
        Material t = b.getType();
        if (!ORES.contains(t)) return;

        List<Block> vein = new ArrayList<>();
        Deque<Block> stack = new ArrayDeque<>();
        Set<Block> seen = new HashSet<>();
        stack.push(b);
        seen.add(b);
        int limit = 300;

        while (!stack.isEmpty() && vein.size() < limit) {
            Block cur = stack.pop();
            vein.add(cur);
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                Block nb = cur.getRelative(dx, dy, dz);
                if (nb == null) continue;
                if (seen.contains(nb)) continue;
                if (nb.getType() == t) {
                    seen.add(nb);
                    stack.push(nb);
                }
            }
        }

        vein.removeIf(x -> x.equals(b));
        if (vein.isEmpty()) return;

        var pl = JavaPlugin.getPlugin(org.pexserver.koukunn.pexsurvival.Loader.class);
        final int perTick = 8;

        new BukkitRunnable() {
            final Iterator<Block> it = vein.iterator();

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
                        if (br.getWorld() != null) br.getWorld().playSound(br.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
                    } catch (Exception ignored) {}
                    i++;
                }
            }
        }.runTaskTimer(pl, 1L, 1L);
    }
}
