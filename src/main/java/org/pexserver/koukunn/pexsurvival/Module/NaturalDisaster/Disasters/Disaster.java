package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Random;

/**
 * 災害の基底インターフェース
 */
public interface Disaster {
    
    /**
     * 災害の名前を取得
     */
    String getName();
    
    /**
     * 災害を実行
     * @param world ワールド
     * @param center 災害の中心位置
     * @param random ランダムジェネレーター
     */
    void execute(World world, Location center, Random random);
}
