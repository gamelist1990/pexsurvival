package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * 視線先のブロックを完全にランダムなブロックに置き換える災害
 */
public class RandomBlockDisaster implements Disaster {

    // 置換対象とするブロックのリストは Material.values() から動的に作成する
    private static final Material[] CANDIDATES;

    static {
        List<Material> list = new ArrayList<>();
        for (Material m : Material.values()) {
            try {
                // ブロックでないものは除外
                boolean isBlock = false;
                try {
                    isBlock = m.isBlock();
                } catch (NoSuchMethodError e) {
                    // 古い API の場合は isSolid を代替利用
                    try {
                        isBlock = m.isSolid();
                    } catch (NoSuchMethodError ignored) {
                        // 最悪、AIR として扱う
                        isBlock = false;
                    }
                }
                if (!isBlock) continue;

                String name = m.name();
                // 除外リスト（破壊したくないもの、液体、エンド系、コンテナなど）
                if (name.contains("AIR") || name.contains("PORTAL") || name.contains("BEDROCK") ||
                        name.contains("CHEST") || name.contains("SHULKER") || name.contains("HOPPER") ||
                        name.contains("WATER") || name.contains("LAVA") || name.contains("BARRIER") ||
                        name.contains("END_GATEWAY") || name.contains("END_PORTAL_FRAME")) continue;

                list.add(m);
            } catch (Throwable t) {
                // 念のため落ちても続行
            }
        }
        CANDIDATES = list.toArray(new Material[0]);
    }

    @Override
    public String getName() {
        return "ランダムブロック変化";
    }

    @Override
    public void execute(World world, Location center, Random random) {
        try {
            for (Player player : world.getPlayers()) {
                // プレイヤー視線の最長距離（例: 50ブロック）
                Block target = player.getTargetBlockExact(50);
                if (target == null) continue;

                // 危険なブロックやワールド境界などは無視
                if (isUnchangeable(target)) continue;

                // 完全ランダムに候補から選ぶ
                Material newMat = CANDIDATES[random.nextInt(CANDIDATES.length)];

                // 置き換え前に穏やかなブロック粒子/音を出す
                Location loc = target.getLocation().add(0.5, 0.5, 0.5);
                try {
                    // FALLING_DUST を試みる（ブロック表現が自然）
                    world.spawnParticle(Particle.valueOf("FALLING_DUST"), loc, 6, 0.1, 0.1, 0.1, target.getBlockData());
                } catch (IllegalArgumentException | NoSuchMethodError e) {
                    // フォールバック: CLOUD を少なめに出す
                    world.spawnParticle(Particle.CLOUD, loc, 6, 0.1, 0.1, 0.1, 0.01);
                }
                world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.6f, 1.0f);

                // 実際にブロックを置き換える
                target.setType(newMat);
            }
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().warning("RandomBlockDisaster error: " + e.getMessage());
        }
    }

    private boolean isUnchangeable(Block block) {
        Material m = block.getType();
        // ベッドロック・エンダーポータル等は変更しない
        if (m == Material.BEDROCK || m == Material.END_PORTAL || m == Material.END_PORTAL_FRAME) return true;
        // チェストなどのコンテナは避ける
        if (m.name().contains("CHEST") || m.name().contains("SHULKER") || m.name().contains("HOPPER")) return true;
        return false;
    }
}
