package fr.enchantlimit;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Stocke toutes les limites configurees dans les menus. */
public class LimitManager {

    public static final int NO_PIECE_LIMIT = 4;

    private final EnchantLimitPlugin plugin;
    private final File file;
    private final Map<Material, Map<String, Integer>> limits = new EnumMap<>(Material.class);
    private final Map<Tier, Integer> pieceLimits = new EnumMap<>(Tier.class);
    private final Map<ItemKind, Integer> itemLimits = new EnumMap<>(ItemKind.class);
    private final Map<CooldownType, Integer> cooldowns = new EnumMap<>(CooldownType.class);

    public LimitManager(EnchantLimitPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "limits.yml");
    }

    public void load() {
        limits.clear();
        pieceLimits.clear();
        itemLimits.clear();
        cooldowns.clear();
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            ConfigurationSection sec = cfg.getConfigurationSection(key);
            if (sec == null) continue;
            switch (key) {
                case "_pieces" -> {
                    for (String t : sec.getKeys(false)) {
                        try {
                            pieceLimits.put(Tier.valueOf(t), sec.getInt(t));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                case "_items" -> {
                    for (String t : sec.getKeys(false)) {
                        try {
                            itemLimits.put(ItemKind.valueOf(t), sec.getInt(t));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                case "_cooldowns" -> {
                    for (String t : sec.getKeys(false)) {
                        try {
                            cooldowns.put(CooldownType.valueOf(t), sec.getInt(t));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                default -> {
                    Material mat = Material.matchMaterial(key);
                    if (mat == null) continue;
                    Map<String, Integer> map = new HashMap<>();
                    for (String ench : sec.getKeys(false)) {
                        map.put(ench, sec.getInt(ench));
                    }
                    if (!map.isEmpty()) limits.put(mat, map);
                }
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (var e : limits.entrySet()) {
            for (var l : e.getValue().entrySet()) {
                cfg.set(e.getKey().name() + "." + l.getKey(), l.getValue());
            }
        }
        for (var e : pieceLimits.entrySet()) cfg.set("_pieces." + e.getKey().name(), e.getValue());
        for (var e : itemLimits.entrySet()) cfg.set("_items." + e.getKey().name(), e.getValue());
        for (var e : cooldowns.entrySet()) cfg.set("_cooldowns." + e.getKey().name(), e.getValue());
        try {
            plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Impossible de sauvegarder limits.yml : " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------- niveaux

    public Integer getLimit(Material mat, Enchantment ench) {
        Map<String, Integer> m = limits.get(mat);
        return m == null ? null : m.get(ench.getKey().getKey());
    }

    public int getEffective(Material mat, Enchantment ench) {
        Integer l = getLimit(mat, ench);
        return l == null ? ench.getMaxLevel() : l;
    }

    public void setLimit(Material mat, Enchantment ench, int level) {
        if (level >= ench.getMaxLevel()) {
            Map<String, Integer> m = limits.get(mat);
            if (m != null) {
                m.remove(ench.getKey().getKey());
                if (m.isEmpty()) limits.remove(mat);
            }
        } else {
            limits.computeIfAbsent(mat, k -> new HashMap<>()).put(ench.getKey().getKey(), Math.max(0, level));
        }
        save();
    }

    public void reset(Material mat) {
        limits.remove(mat);
        save();
    }

    public int countRestrictions(Material mat) {
        Map<String, Integer> m = limits.get(mat);
        return m == null ? 0 : m.size();
    }

    public boolean clamp(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        Map<String, Integer> m = limits.get(item.getType());
        if (m == null || m.isEmpty()) return false;
        boolean changed = false;
        for (var e : new HashMap<>(item.getEnchantments()).entrySet()) {
            Integer lim = m.get(e.getKey().getKey().getKey());
            if (lim != null && e.getValue() > lim) {
                item.removeEnchantment(e.getKey());
                if (lim > 0) item.addUnsafeEnchantment(e.getKey(), lim);
                changed = true;
            }
        }
        return changed;
    }

    // ---------------------------------------------------------- pieces portees

    /** Nombre max de pieces de cette famille portees en meme temps (4 = illimite). */
    public int getPieceLimit(Tier tier) {
        return pieceLimits.getOrDefault(tier, NO_PIECE_LIMIT);
    }

    public void setPieceLimit(Tier tier, int value) {
        if (value >= NO_PIECE_LIMIT) pieceLimits.remove(tier);
        else pieceLimits.put(tier, Math.max(0, value));
        save();
    }

    // ---------------------------------------------------------- quantite d'objets

    /** Quantite max portee (-1 = illimite). */
    public int getItemLimit(ItemKind kind) {
        return itemLimits.getOrDefault(kind, -1);
    }

    public void setItemLimit(ItemKind kind, int value) {
        if (value < 0) itemLimits.remove(kind);
        else itemLimits.put(kind, value);
        save();
    }

    // ---------------------------------------------------------- cooldowns

    /** Cooldown en secondes (0 = aucun). */
    public int getCooldown(CooldownType type) {
        return cooldowns.getOrDefault(type, 0);
    }

    public void setCooldown(CooldownType type, int seconds) {
        if (seconds <= 0) cooldowns.remove(type);
        else cooldowns.put(type, seconds);
        save();
    }
}
