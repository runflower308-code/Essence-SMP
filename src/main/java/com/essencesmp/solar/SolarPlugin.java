package com.essencesmp.solar;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public final class SolarPlugin extends JavaPlugin implements Listener {
    private NamespacedKey itemTypeKey;
    private NamespacedKey targetTierKey;
    private final Map<UUID, Map<String, Integer>> tiers = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    @Override public void onEnable() {
        itemTypeKey = new NamespacedKey(this, "essence_item");
        targetTierKey = new NamespacedKey(this, "target_tier");
        loadTiers();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("solar")).setExecutor(this::solarCommand);
        Objects.requireNonNull(getCommand("essence")).setExecutor(this::essenceCommand);
        Bukkit.addRecipe(upgraderRecipe(2));
        Bukkit.addRecipe(upgraderRecipe(3));
        getLogger().info("EssenceSMP Solar enabled.");
    }

    @Override public void onDisable() { saveTiers(); }

    private ShapedRecipe upgraderRecipe(int tier) {
        ItemStack result = upgraderItem(tier);
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "tier_" + tier + "_upgrader"), result);
        recipe.shape("DGD", "GNG", "DGD");
        if (tier == 2) {
            recipe.setIngredient('D', Material.DIAMOND);
            recipe.setIngredient('G', Material.GOLD_INGOT);
            recipe.setIngredient('N', Material.DIAMOND_BLOCK);
        } else {
            recipe.setIngredient('D', Material.DIAMOND_BLOCK);
            recipe.setIngredient('G', Material.NETHERITE_INGOT);
            recipe.setIngredient('N', Material.NETHER_STAR);
        }
        return recipe;
    }

    private ItemStack upgraderItem(int tier) {
        ItemStack item = new ItemStack(tier == 2 ? Material.DIAMOND : Material.NETHERITE_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Essence Upgrader " + (tier == 2 ? "II" : "III"));
        meta.setLore(List.of(ChatColor.GRAY + "One-use upgrade item", ChatColor.YELLOW + "Right-click to upgrade your current Essence", ChatColor.DARK_GRAY + "Target tier: " + tier));
        meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, "upgrader");
        meta.getPersistentDataContainer().set(targetTierKey, PersistentDataType.INTEGER, tier);
        item.setItemMeta(meta);
        return item;
    }

    private boolean solarCommand(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (args.length == 0) {
            player.sendMessage(ChatColor.GOLD + "Solar abilities: /solar flare (Tier 1), /solar judgment (Tier 2), /solar eclipse (Tier 3)");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "flare" -> castFlare(player);
            case "judgment", "judgement" -> {
                if (tier(player, "solar") < 2) player.sendMessage(ChatColor.RED + "Solar Judgment requires Solar Tier 2.");
                else castJudgment(player);
            }
            case "eclipse" -> {
                if (tier(player, "solar") < 3) player.sendMessage(ChatColor.RED + "Solar Eclipse requires Solar Tier 3.");
                else castEclipse(player);
            }
            default -> player.sendMessage(ChatColor.RED + "Use /solar <flare|judgment|eclipse>.");
        }
        return true;
    }

    private boolean essenceCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player p) p.sendMessage(ChatColor.GOLD + "Solar Essence: Tier " + tier(p, "solar") + ". Progress is saved separately per Essence.");
            else sender.sendMessage("Use /essence giveupgrader <player|self> <tier2|tier3>");
            return true;
        }
        if (args[0].equalsIgnoreCase("giveupgrader")) {
            if (!sender.hasPermission("essencesmp.admin")) { sender.sendMessage(ChatColor.RED + "You need permission essencesmp.admin."); return true; }
            if (args.length < 3) { sender.sendMessage("Usage: /essence giveupgrader <player|self> <tier2|tier3>"); return true; }
            Player target = args[1].equalsIgnoreCase("self") && sender instanceof Player p ? p : Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
            int targetTier = args[2].equalsIgnoreCase("tier2") ? 2 : args[2].equalsIgnoreCase("tier3") ? 3 : 0;
            if (targetTier == 0) { sender.sendMessage(ChatColor.RED + "Choose tier2 or tier3."); return true; }
            target.getInventory().addItem(upgraderItem(targetTier));
            sender.sendMessage(ChatColor.GREEN + "Gave a Tier " + targetTier + " upgrader to " + target.getName() + ".");
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
        return true;
    }

    @EventHandler public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        String type = meta.getPersistentDataContainer().get(itemTypeKey, PersistentDataType.STRING);
        if ("upgrader".equals(type)) {
            event.setCancelled(true);
            Integer targetTier = meta.getPersistentDataContainer().get(targetTierKey, PersistentDataType.INTEGER);
            if (targetTier == null) return;
            Player player = event.getPlayer();
            int current = tier(player, "solar");
            if (targetTier != current + 1) {
                player.sendMessage(ChatColor.RED + "This upgrader is for Tier " + targetTier + ". Your Solar Essence is Tier " + current + ". You must upgrade one tier at a time.");
                return;
            }
            setTier(player, "solar", targetTier);
            consumeOne(player, item);
            player.sendMessage(ChatColor.GOLD + "Solar Essence upgraded to Tier " + targetTier + "!");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
            return;
        }
        if (item.getType() == Material.BLAZE_ROD) { event.setCancelled(true); castFlare(event.getPlayer()); }
        else if (item.getType() == Material.STICK && tier(event.getPlayer(), "solar") >= 2) { event.setCancelled(true); castJudgment(event.getPlayer()); }
        else if (item.getType() == Material.NETHER_STAR && tier(event.getPlayer(), "solar") >= 3) { event.setCancelled(true); castEclipse(event.getPlayer()); }
    }

    private void castFlare(Player p) {
        if (!ready(p, "flare", 6000)) return;
        Location center = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(2.2));
        p.getWorld().spawnParticle(Particle.FLAME, center, 35, 0.6, 0.6, 0.6, 0.03);
        p.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
        p.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.3f);
        for (Entity entity : p.getWorld().getNearbyEntities(center, 2.2, 2.2, 2.2)) {
            if (entity instanceof LivingEntity target && target != p && !isFriendly(p, target)) {
                target.damage(5.0, p);
                Vector knock = target.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.55).setY(0.25);
                target.setVelocity(knock);
            }
        }
    }

    private void castJudgment(Player p) {
        if (!ready(p, "judgment", 14000)) return;
        Location point = p.getEyeLocation().clone();
        Vector direction = point.getDirection().normalize();
        Set<UUID> hit = new HashSet<>();
        for (int i = 0; i < 22; i++) {
            point.add(direction.clone().multiply(0.7));
            p.getWorld().spawnParticle(Particle.END_ROD, point, 4, 0.12, 0.12, 0.12, 0.01);
            p.getWorld().spawnParticle(Particle.FLAME, point, 2, 0.1, 0.1, 0.1, 0.01);
            for (Entity e : p.getWorld().getNearbyEntities(point, 0.75, 0.75, 0.75)) {
                if (e instanceof LivingEntity target && target != p && !hit.contains(target.getUniqueId()) && !isFriendly(p, target)) {
                    hit.add(target.getUniqueId());
                    target.damage(7.0, p);
                    target.setFireTicks(30);
                }
            }
        }
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.6f);
    }

    private void castEclipse(Player p) {
        if (!ready(p, "eclipse", 60000)) return;
        Location center = p.getLocation().clone().add(0, 10, 0);
        p.getWorld().spawnParticle(Particle.FLAME, center, 120, 2.5, 0.8, 2.5, 0.02);
        p.getWorld().spawnParticle(Particle.LAVA, center, 60, 2, 0.5, 2, 0.02);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.5f);
        for (int tick = 0; tick < 6; tick++) {
            final int pulse = tick;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!p.isOnline() || !p.isValid()) return;
                Location pulseCenter = p.getLocation().clone();
                pulseCenter.getWorld().spawnParticle(Particle.FLAME, pulseCenter, 55, 4, 1.5, 4, 0.04);
                for (Entity e : pulseCenter.getWorld().getNearbyEntities(pulseCenter, 4, 3, 4)) {
                    if (e instanceof LivingEntity target && target != p && !isFriendly(p, target)) target.damage(2.0, p);
                }
            }, tick * 10L);
        }
    }

    private boolean ready(Player p, String ability, long cooldownMs) {
        long now = System.currentTimeMillis();
        long until = cooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).getOrDefault(ability, 0L);
        if (now < until) { p.sendMessage(ChatColor.RED + ability + " cooldown: " + Math.max(1, (until - now + 999) / 1000) + "s."); return false; }
        cooldowns.get(p.getUniqueId()).put(ability, now + cooldownMs);
        return true;
    }

    private boolean isFriendly(Player caster, LivingEntity target) {
        if (target instanceof Player other) {
            if (caster.equals(other)) return true;
            if (caster.getWorld().getPVP() == false) return true;
        }
        return false;
    }

    private int tier(Player p, String essence) {
        return tiers.getOrDefault(p.getUniqueId(), Collections.emptyMap()).getOrDefault(essence.toLowerCase(Locale.ROOT), 1);
    }
    private void setTier(Player p, String essence, int value) {
        tiers.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(essence.toLowerCase(Locale.ROOT), value);
        saveTiers();
    }
    private void consumeOne(Player player, ItemStack held) {
        if (held.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else held.setAmount(held.getAmount() - 1);
    }
    private void loadTiers() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        var config = getConfig();
        if (!config.isConfigurationSection("tiers")) return;
        for (String uuid : Objects.requireNonNull(config.getConfigurationSection("tiers")).getKeys(false)) {
            try {
                UUID id = UUID.fromString(uuid);
                var section = config.getConfigurationSection("tiers." + uuid);
                if (section == null) continue;
                Map<String, Integer> map = new HashMap<>();
                for (String essence : section.getKeys(false)) map.put(essence, section.getInt(essence, 1));
                tiers.put(id, map);
            } catch (IllegalArgumentException ignored) {}
        }
    }
    private void saveTiers() {
        var config = getConfig();
        config.set("tiers", null);
        for (var entry : tiers.entrySet()) for (var essence : entry.getValue().entrySet())
            config.set("tiers." + entry.getKey() + "." + essence.getKey(), essence.getValue());
        saveConfig();
    }
}
