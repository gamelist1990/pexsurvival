package org.pexserver.koukunn.pexsurvival.Module.NaturalDisaster.Disasters;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * 地震の災害
 */
public class EarthquakeDisaster implements Disaster {

    private static int animationIdCounter = 1;

    @Override
    public String getName() {
        return "地震";
    }

    @Override
    public void execute(World world, Location center, Random random) {
        for (Player player : world.getNearbyPlayers(center, 100)) {
            Location loc = player.getLocation();

            for (int i = 0; i < 10; i++) {
                double offsetY = (random.nextDouble() - 0.5) * 0.5;
                Vector knockback = new Vector(
                        (random.nextDouble() - 0.5) * 0.3,
                        offsetY,
                        (random.nextDouble() - 0.5) * 0.3
                );
                player.setVelocity(player.getVelocity().add(knockback));
            }

            loc.getWorld().spawnParticle(Particle.SMOKE, loc, 50, 5, 5, 5);
            world.playSound(loc, Sound.BLOCK_GRAVEL_HIT, 1.0f, 0.5f);

            breakBlocksNearPlayer(player, world, random);
        }
    }

    private void breakBlocksNearPlayer(Player player, World world, Random random) {
        int radius = 4 + random.nextInt(3);
        int count = 3 + random.nextInt(5);

        int attempts = 0;
        int created = 0;
        while (created < count && attempts < count * 12) {
            attempts++;

            double angle = random.nextDouble() * Math.PI * 2;
            double r = random.nextDouble() * radius;
            int dx = (int) Math.round(Math.cos(angle) * r);
            int dz = (int) Math.round(Math.sin(angle) * r);

            int tx = player.getLocation().getBlockX() + dx;
            int tz = player.getLocation().getBlockZ() + dz;

            if (tx == player.getLocation().getBlockX() && tz == player.getLocation().getBlockZ()) continue;

            int groundY = world.getHighestBlockYAt(tx, tz);
            Location blockLoc = new Location(world, tx, groundY, tz);
            Block block = blockLoc.getBlock();

            if (block == null) continue;
            if (block.isEmpty() || isLiquid(block) || isUnbreakable(block)) continue;

            playMiningAnimation(world, block, random);
            created++;
        }
    }

    private boolean isLiquid(Block block) {
        return block.isLiquid();
    }

    private boolean isUnbreakable(Block block) {
        Material material = block.getType();
        return material == Material.BEDROCK ||
               material == Material.OBSIDIAN ||
               material == Material.CRYING_OBSIDIAN;
    }

    private void playMiningAnimation(World world, Block block, Random random) {
        new BukkitRunnable() {
            int stage = 0;
            final Location blockLoc = block.getLocation();
            final int animationId = getNextAnimationId();
            final int maxStage = 9;

            @Override
            public void run() {
                try {
                    Block current = blockLoc.getBlock();
                    if (current == null || current.getType() == Material.AIR) {
                        for (Player player : getNearbyPlayers(blockLoc, 48)) {
                            sendBlockDestructionPacket(player, animationId, blockLoc, -1);
                        }
                        this.cancel();
                        return;
                    }

                    int displayStage = Math.min(stage, maxStage);

                    for (Player player : getNearbyPlayers(blockLoc, 64)) {
                        sendBlockDestructionPacket(player, animationId, blockLoc, displayStage);
                    }

                    world.playSound(blockLoc, Sound.BLOCK_STONE_HIT, 1.0f, 0.5f + (stage * 0.03f));

                    if (stage < maxStage) {
                        stage++;
                    } else {
                        current.setType(Material.AIR);

                        for (Player player : getNearbyPlayers(blockLoc, 64)) {
                            sendBlockDestructionPacket(player, animationId, blockLoc, -1);
                        }
                        world.playSound(blockLoc, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
                        this.cancel();
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().warning("playMiningAnimation error: " + e.getMessage());
                    this.cancel();
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("pexsurvival"), 0, 2);
    }

    private void sendBlockDestructionPacket(Player player, int entityId, Location blockLoc, int stage) {
        final boolean DEBUG = false;

        try {
            float progress;
            if (stage < 0) {
                progress = 0.0f;
            } else {
                progress = Math.min(Math.max(stage, 0), 9) / 9.0f;
            }

            if (DEBUG) {
                Bukkit.getLogger().info(String.format("[EarthquakeDisaster] sendBlockDestructionPacket -> player=%s, entityId=%d, block=%s,%d,%s, stage=%d, progress=%.3f",
                        player.getName(), entityId, blockLoc.getWorld().getName(), blockLoc.getBlockX(), blockLoc.getBlockZ(), stage, progress));
            }

            player.sendBlockDamage(blockLoc, progress, entityId);
        } catch (NoSuchMethodError e) {
            Bukkit.getLogger().warning("sendBlockDamage API not available: " + e.getMessage());
            if (DEBUG) Bukkit.getLogger().warning("[EarthquakeDisaster] NoSuchMethodError in sendBlockDestructionPacket for player=" + player.getName());
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to call sendBlockDamage: " + e.getMessage());
            if (DEBUG) {
                Bukkit.getLogger().warning("[EarthquakeDisaster] Exception while sending block damage to player=" + player.getName());
                e.printStackTrace();
            }
        }
    }

    private java.util.List<Player> getNearbyPlayers(Location loc, double radius) {
        java.util.List<Player> list = new java.util.ArrayList<>();
        World w = loc.getWorld();
        double r2 = radius * radius;
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= r2) list.add(p);
        }
        return list;
    }

    private synchronized int getNextAnimationId() {
        return animationIdCounter++;
    }
}
