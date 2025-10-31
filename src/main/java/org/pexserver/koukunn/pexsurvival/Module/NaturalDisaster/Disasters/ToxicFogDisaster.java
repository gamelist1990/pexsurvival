package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * 毒霧災害
 * 範囲内のプレイヤーに毒エフェクトを付与し、視界を妨げるパーティクルを表示
 */
public class ToxicFogDisaster implements Disaster {

    private static final double FOG_RADIUS = 25.0; // 霧の半径
    private static final int POISON_DURATION = 60; // 毒の持続時間（3秒）
    private static final int POISON_AMPLIFIER = 0; // 毒のレベル（1 = レベル1）
    
    @Override
    public String getName() {
        return "毒霧";
    }

    @Override
    public void execute(World world, Location center, Random random) {
        // 毒霧のパーティクルを表示（緑色の煙）
        displayFogParticles(world, center, random);
        
        // 範囲内のプレイヤーに毒エフェクトを付与
        applyPoisonToPlayers(world, center);
        
        // 時々、不気味な音を鳴らす
        if (random.nextInt(5) == 0) {
            world.playSound(center, Sound.ENTITY_WITCH_AMBIENT, 0.3f, 0.7f);
        }
    }
    
    /**
     * 毒霧のパーティクルを表示
     */
    private void displayFogParticles(World world, Location center, Random random) {
        // 複数のパーティクルを円形に配置
        int particleCount = 50;
        
        for (int i = 0; i < particleCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = random.nextDouble() * FOG_RADIUS;
            double height = random.nextDouble() * 3.0; // 地面から3ブロックまでの高さ
            
            double x = center.getX() + Math.cos(angle) * radius;
            double y = center.getY() + height;
            double z = center.getZ() + Math.sin(angle) * radius;
            
            Location particleLoc = new Location(world, x, y, z);
            
            try {
                // 緑色の煙パーティクル（REDSTONE パーティクルを使用して色を指定）
                Particle.DustOptions dustOptions = new Particle.DustOptions(
                    Color.fromRGB(50, 205, 50), // ライムグリーン
                    2.0f // サイズ
                );
                world.spawnParticle(Particle.DUST, particleLoc, 1, 0.1, 0.1, 0.1, 0, dustOptions);
                
                // 追加の煙エフェクト
                if (random.nextInt(3) == 0) {
                    world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            } catch (Exception e) {
                // パーティクル互換性のフォールバック
                try {
                    world.spawnParticle(Particle.SMOKE, particleLoc, 3, 0.3, 0.3, 0.3, 0.01);
                } catch (Exception ignored) {
                }
            }
        }
    }
    
    /**
     * 範囲内のプレイヤーに毒エフェクトを付与
     */
    private void applyPoisonToPlayers(World world, Location center) {
        for (Player player : world.getPlayers()) {
            double distanceSq = player.getLocation().distanceSquared(center);
            
            // 範囲内のプレイヤーに毒を付与
            if (distanceSq <= FOG_RADIUS * FOG_RADIUS) {
                // 既存の毒エフェクトがある場合は上書きしない（重複を避ける）
                PotionEffect existingPoison = player.getPotionEffect(PotionEffectType.POISON);
                if (existingPoison == null || existingPoison.getDuration() < 20) {
                    player.addPotionEffect(new PotionEffect(
                        PotionEffectType.POISON, 
                        POISON_DURATION, 
                        POISON_AMPLIFIER,
                        false, // アンビエント
                        true,  // パーティクル表示
                        true   // アイコン表示
                    ));
                }
                
                // 視界妨害エフェクト（盲目を短時間付与）
                if (existingPoison == null) {
                    player.addPotionEffect(new PotionEffect(
                        PotionEffectType.BLINDNESS,
                        40, // 2秒
                        0,
                        false,
                        false,
                        true
                    ));
                }
            }
        }
    }
}
