package org.pexserver.koukunn.pexsurvival.Module.MobBoom;

import org.pexserver.koukunn.pexsurvival.Core.Feature.Feature;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class MobBoomFeature implements Feature {

    private boolean enabled = false;
    private final Random random = new Random();

    private final Map<Entity, Long> mobBoomSchedule = new HashMap<>();
    private final long WARNING_COOLDOWN = 500;
    private final Map<Entity, Long> lastWarningTime = new HashMap<>();
    // recentExplosionVictims removed because onPlayerDeath handler was deleted

    @Override
    public String getFeatureName() {
        return "mobboom";
    }

    @Override
    public String getDescription() {
        return "プレイヤーがモブに近づくと爆発が起きる";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        Bukkit.getLogger().info("[PEX] MobBoomFeature enabled");
    }

    @Override
    public void disable() {
        this.enabled = false;
        mobBoomSchedule.clear();
        lastWarningTime.clear();
        Bukkit.getLogger().info("[PEX] MobBoomFeature disabled");
    }

    @Override
    public void reload() {

    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!enabled)
            return;

        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
            return;
        Location playerLoc = player.getLocation();

        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (!(entity instanceof LivingEntity))
                continue;

            LivingEntity mob = (LivingEntity) entity;

            if (mob instanceof Villager || mob instanceof Enderman)
                continue;

            if (mobBoomSchedule.containsKey(mob))
                continue;

            double distance = playerLoc.distance(mob.getLocation());

            if (distance <= 2.0) {

                scheduleBoom(mob, player);
            }
        }
    }

    private void scheduleBoom(LivingEntity mob, Player player) {

        long delaySeconds = 1 + random.nextInt(3);
        long explosionTime = System.currentTimeMillis() + (delaySeconds * 1000);

        mobBoomSchedule.put(mob, explosionTime);

        playWarningSound(mob);

        scheduleExplosion(mob, delaySeconds, player);
    }

    private void playWarningSound(LivingEntity mob) {
        long now = System.currentTimeMillis();
        long lastWarning = lastWarningTime.getOrDefault(mob, 0L);

        if (now - lastWarning < WARNING_COOLDOWN) {
            return;
        }

        lastWarningTime.put(mob, now);

        Location mobLoc = mob.getLocation();

        mobLoc.getWorld().playSound(mobLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }

    private void scheduleExplosion(LivingEntity mob, long delaySeconds, Player cause) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (mob.isDead() || !mob.isValid()) {
                    mobBoomSchedule.remove(mob);
                    lastWarningTime.remove(mob);
                    cancel();
                    return;
                }

                explodeMob(mob, cause);
                mobBoomSchedule.remove(mob);
                lastWarningTime.remove(mob);
                cancel();
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("pexsurvival"), delaySeconds * 20L);
    }

    private void explodeMob(LivingEntity mob, Player cause) {
        Location mobLoc = mob.getLocation();

        mob.setVelocity(mob.getVelocity().setY(1.5));

        spawnExplosionParticles(mobLoc);

        mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);

        float explosionPower = 10.0f + random.nextFloat() * 2.0f;
        // explosion の setFire を true にして、サーバーに火をつける挙動を許可
        mobLoc.getWorld().createExplosion(mobLoc.getX(), mobLoc.getY(), mobLoc.getZ(), explosionPower, false, true);


        // 近くのプレイヤーに燃焼を与える（10秒）
        for (Player p : mobLoc.getWorld().getPlayers()) {
            double dsq = p.getLocation().distanceSquared(mobLoc);
            double rangeSq = (explosionPower * explosionPower * 4);
            if (dsq <= rangeSq) {
                p.setFireTicks(20 * 10);
            }
        }

        // Player インスタンスに対して remove() を呼ばない（CraftPlayer.remove は例外を投げるため）
        if (!mob.isDead() && mob.isValid() && !(mob instanceof Player)) {
            mob.remove();
        }
    }

    // onPlayerDeath handler removed: death message handling is disabled per request

    // ExplosionInfo removed because onPlayerDeath was deleted

    private void spawnExplosionParticles(Location center) {
        org.bukkit.World world = center.getWorld();

        if (world == null)
            return;

        world.spawnParticle(Particle.EXPLOSION, center, 3, 0.5, 0.5, 0.5, 0.1);

        world.spawnParticle(Particle.FLAME, center, 20, 1.0, 1.0, 1.0, 0.1);

        world.spawnParticle(Particle.SMOKE, center, 15, 0.8, 0.8, 0.8, 0.05);
    }
}
