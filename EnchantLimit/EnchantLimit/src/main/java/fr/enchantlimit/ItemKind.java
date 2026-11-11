package fr.enchantlimit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

/** Objets dont on peut limiter la quantite portee. */
public enum ItemKind {
    WIND_CHARGE("Wind Charge", Material.WIND_CHARGE),
    ENCHANTED_GAPPLE("Pomme d'or enchantée", Material.ENCHANTED_GOLDEN_APPLE),
    TOTEM("Totem d'immortalité", Material.TOTEM_OF_UNDYING),
    STRENGTH_II("Potion de Force II", Material.POTION);

    /** Paliers : -1 = illimite. Clic gauche = on descend dans la liste. */
    public static final int[] STEPS = {-1, 16, 8, 6, 4, 3, 2, 1, 0};

    public final String label;
    public final Material icon;

    ItemKind(String label, Material icon) {
        this.label = label;
        this.icon = icon;
    }

    public boolean matches(ItemStack it) {
        if (it == null || it.getType().isAir()) return false;
        Material m = it.getType();
        switch (this) {
            case WIND_CHARGE:
                return m == Material.WIND_CHARGE;
            case ENCHANTED_GAPPLE:
                return m == Material.ENCHANTED_GOLDEN_APPLE;
            case TOTEM:
                return m == Material.TOTEM_OF_UNDYING;
            case STRENGTH_II:
                if (m != Material.POTION && m != Material.SPLASH_POTION && m != Material.LINGERING_POTION) {
                    return false;
                }
                if (!(it.getItemMeta() instanceof PotionMeta pm)) return false;
                if (pm.getBasePotionType() == PotionType.STRONG_STRENGTH) return true;
                for (PotionEffect ef : pm.getCustomEffects()) {
                    if (ef.getType().equals(PotionEffectType.STRENGTH) && ef.getAmplifier() >= 1) return true;
                }
                return false;
            default:
                return false;
        }
    }
}
