package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;

import java.util.Random;

/**
 * 雷の災害
 */
public class LightningDisaster implements Disaster {
    
    @Override
    public String getName() {
        return "雷";
    }
    
    @Override
    public void execute(World world, Location center, Random random) {
        // 中心位置から半径100ブロック以内にランダムに雷を落とす
        for (int i = 0; i < 5; i++) {
            double x = center.getX() + (random.nextDouble() - 0.5) * 100;
            double z = center.getZ() + (random.nextDouble() - 0.5) * 100;
            Location loc = new Location(world, x, world.getHighestBlockYAt((int)x, (int)z) + 1, z);
            world.strikeLightning(loc);
            world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        }
    }
}
