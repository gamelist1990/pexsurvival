package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;

import java.util.Random;

/**
 * 砂嵐の災害
 * プレイヤー付近にランダムで上空から砂が降ってくる
 */
public class SandstormDisaster implements Disaster {

    @Override
    public String getName() {
        return "砂嵐";
    }

    @Override
    public void execute(World world, Location center, Random random) {
        try {
            // ワールド内の各プレイヤーの付近にランダムで砂を降らせる
            for (Player player : world.getPlayers()) {
                // プレイヤーごとに発生確率（80%） — より頻繁に発生
                if (random.nextDouble() > 0.8) continue;

                // 降らす砂の個数を増やす（5～12個）
                int count = 5 + random.nextInt(8); // 5～12個
                for (int i = 0; i < count; i++) {
                    // 範囲を広げる（±12ブロック）
                    double offsetX = (random.nextDouble() - 0.5) * 24.0; // ±12
                    double offsetZ = (random.nextDouble() - 0.5) * 24.0; // ±12

                    double x = player.getLocation().getX() + offsetX;
                    double z = player.getLocation().getZ() + offsetZ;

                    // 地表の上空に出現（高さ: 地表 + 25～40）
                    int groundY = world.getHighestBlockYAt((int) x, (int) z);
                    int spawnY = groundY + 25 + random.nextInt(16);

                    Location spawnLoc = new Location(world, x, spawnY, z);

                    // FallingBlock を生成
                    FallingBlock fb = world.spawnFallingBlock(spawnLoc, Material.SAND.createBlockData());
                    fb.setDropItem(false);
                    fb.setHurtEntities(true);

                    // FallingBlock を生成のみ行う（着地時のブロック生成は FallingBlock の標準挙動に任せる）

                    // パーティクルと効果音（FALLING_DUST を優先して穏やかに表示）
                    Location pLoc = player.getLocation().add(0, 1, 0);
                    try {
                        world.spawnParticle(Particle.valueOf("FALLING_DUST"), pLoc, 10, 0.2, 0.2, 0.2, Material.SAND.createBlockData());
                    } catch (IllegalArgumentException | NoSuchMethodError ex) {
                        world.spawnParticle(Particle.CLOUD, pLoc, 10, 0.2, 0.2, 0.2, 0.01);
                    }
                    world.playSound(player.getLocation(), Sound.BLOCK_SAND_BREAK, 0.6f, 1.0f);
                }
            }
        } catch (Exception e) {
            // 何か問題があればログに残す
            org.bukkit.Bukkit.getLogger().warning("SandstormDisaster error: " + e.getMessage());
        }
    }
}
