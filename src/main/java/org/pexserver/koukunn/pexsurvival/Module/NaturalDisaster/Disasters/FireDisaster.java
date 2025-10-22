package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Random;

/**
 * 火災の災害
 */
public class FireDisaster implements Disaster {
    
    @Override
    public String getName() {
        return "火災";
    }
    
    @Override
    public void execute(World world, Location center, Random random) {
        int radius = 50;
        int fireCount = 0;
        int maxFires = 8;
        
        // 中心位置の周辺でランダムに複数の木を探して着火
        for (int attempt = 0; attempt < 50 && fireCount < maxFires; attempt++) {
            int randomX = center.getBlockX() + random.nextInt(radius * 2 + 1) - radius;
            int randomZ = center.getBlockZ() + random.nextInt(radius * 2 + 1) - radius;
            
            // 地形の高さを取得
            int groundY = world.getHighestBlockYAt(randomX, randomZ);
            Location blockLoc = new Location(world, randomX, groundY, randomZ);
            Block block = world.getBlockAt(blockLoc);
            
            // 木のブロックか確認
            if (isWoodBlock(block.getType())) {
                Block fireBlock = world.getBlockAt(blockLoc.getBlockX(), blockLoc.getBlockY() + 1, blockLoc.getBlockZ());
                if (fireBlock.getType() == Material.AIR) {
                    fireBlock.setType(Material.FIRE);
                    world.playSound(blockLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
                    fireCount++;
                }
            }
        }
        
        // 木が見つからない場合は中心の地面に複数着火
        if (fireCount == 0) {
            for (int i = 0; i < 4; i++) {
                Location fireLoc = center.clone().add(
                        (random.nextDouble() - 0.5) * 100,
                        0,
                        (random.nextDouble() - 0.5) * 100
                );
                
                int fireY = world.getHighestBlockYAt(fireLoc.getBlockX(), fireLoc.getBlockZ());
                Block groundBlock = world.getBlockAt(fireLoc.getBlockX(), fireY, fireLoc.getBlockZ());
                if (groundBlock.getType() != Material.FIRE) {
                    groundBlock.setType(Material.FIRE);
                    world.playSound(new Location(world, fireLoc.getBlockX(), fireY, fireLoc.getBlockZ()), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
                }
            }
        }
    }
    
    /**
     * 木のブロックか判定
     */
    private boolean isWoodBlock(Material material) {
        return material.name().contains("LOG") || 
               material.name().contains("WOOD") ||
               material == Material.OAK_LOG ||
               material == Material.BIRCH_LOG ||
               material == Material.SPRUCE_LOG ||
               material == Material.JUNGLE_LOG ||
               material == Material.ACACIA_LOG ||
               material == Material.DARK_OAK_LOG ||
               material == Material.MANGROVE_LOG ||
               material == Material.CHERRY_LOG ||
               material == Material.PALE_OAK_LOG;
    }
}
