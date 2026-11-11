package fr.enchantlimit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Applique les cooldowns configures dans le menu. */
public class CooldownListener implements Listener {

    private final LimitManager limits;
    private final Map<UUID, EnumMap<CooldownType, Long>> expiry = new HashMap<>();

    public CooldownListener(LimitManager limits) {
        this.limits = limits;
    }

    private boolean bypass(Player p) {
        return p.hasPermission("enchantlimit.bypass");
    }

    /** Secondes restantes (0 = libre). */
    private long remaining(Player p, CooldownType t) {
        if (limits.getCooldown(t) <= 0 || bypass(p)) return 0;
        EnumMap<CooldownType, Long> m = expiry.get(p.getUniqueId());
        if (m == null) return 0;
        Long end = m.get(t);
        if (end == null) return 0;
        long ms = end - System.currentTimeMillis();
        return ms <= 0 ? 0 : (ms + 999) / 1000;
    }

    private void register(Player p, CooldownType t, Material item) {
        int sec = limits.getCooldown(t);
        if (sec <= 0 || bypass(p)) return;
        expiry.computeIfAbsent(p.getUniqueId(), k -> new EnumMap<>(CooldownType.class))
                .put(t, System.currentTimeMillis() + sec * 1000L);
        if (item != null) p.setCooldown(item, sec * 20); // affichage du cooldown sur l'item
    }

    private boolean blocked(Player p, CooldownType t) {
        long r = remaining(p, t);
        if (r > 0) {
            p.sendActionBar(Component.text(t.label + " : encore " + r + " s", NamedTextColor.RED));
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------ clic droit

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack it = e.getItem();
        if (it == null) return;
        Player p = e.getPlayer();
        Material m = it.getType();
        Block b = e.getClickedBlock();

        CooldownType t = null;
        boolean register = false; // true = le cooldown demarre ici (sinon : au lancement reel)

        if (m == Material.ENDER_PEARL) {
            t = CooldownType.PEARL;
        } else if (m == Material.WIND_CHARGE) {
            t = CooldownType.WIND_CHARGE;
        } else if (m == Material.TRIDENT) {
            t = CooldownType.TRIDENT;
        } else if (m == Material.ENCHANTED_GOLDEN_APPLE) {
            t = CooldownType.GAPPLE;
        } else if (m == Material.FIREWORK_ROCKET) {
            if (a == Action.RIGHT_CLICK_BLOCK || p.isGliding()) {
                t = CooldownType.FIREWORK;
                register = true;
            }
        } else if (m == Material.END_CRYSTAL) {
            if (b != null && (b.getType() == Material.OBSIDIAN || b.getType() == Material.BEDROCK)) {
                t = CooldownType.END_CRYSTAL;
                register = true;
            }
        } else if (m == Material.TNT_MINECART) {
            if (b != null && Tag.RAILS.isTagged(b.getType())) {
                t = CooldownType.TNT_MINECART;
                register = true;
            }
        }
        if (t == null) return;

        if (blocked(p, t)) {
            e.setCancelled(true);
        } else if (register) {
            register(p, t, m);
        }
    }

    // ------------------------------------------------------------ projectiles

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player p)) return;
        EntityType et = e.getEntityType();
        CooldownType t = null;
        Material m = null;
        if (et == EntityType.ENDER_PEARL) {
            t = CooldownType.PEARL;
            m = Material.ENDER_PEARL;
        } else if (et == EntityType.WIND_CHARGE) {
            t = CooldownType.WIND_CHARGE;
            m = Material.WIND_CHARGE;
        } else if (et == EntityType.TRIDENT) {
            t = CooldownType.TRIDENT;
            m = Material.TRIDENT;
        }
        if (t == null) return;

        if (blocked(p, t)) {
            e.setCancelled(true);
        } else {
            register(p, t, m);
        }
    }

    /** Trident utilise en riptide (pas de projectile). */
    @EventHandler
    public void onRiptide(PlayerRiptideEvent e) {
        register(e.getPlayer(), CooldownType.TRIDENT, Material.TRIDENT);
    }

    // ------------------------------------------------------------ attaques

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        Material m = p.getInventory().getItemInMainHand().getType();
        CooldownType t = null;
        if (m == Material.MACE) t = CooldownType.MACE;
        else if (m.name().endsWith("_SPEAR")) t = CooldownType.SPEAR;
        if (t == null) return;

        if (blocked(p, t)) {
            e.setCancelled(true);
        } else {
            register(p, t, m);
        }
    }

    // ------------------------------------------------------------ pomme d'or / blocs

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        if (e.getItem().getType() != Material.ENCHANTED_GOLDEN_APPLE) return;
        Player p = e.getPlayer();
        if (blocked(p, CooldownType.GAPPLE)) {
            e.setCancelled(true);
        } else {
            register(p, CooldownType.GAPPLE, Material.ENCHANTED_GOLDEN_APPLE);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() != Material.RESPAWN_ANCHOR) return;
        Player p = e.getPlayer();
        if (blocked(p, CooldownType.RESPAWN_ANCHOR)) {
            e.setCancelled(true);
        } else {
            register(p, CooldownType.RESPAWN_ANCHOR, Material.RESPAWN_ANCHOR);
        }
    }
}
