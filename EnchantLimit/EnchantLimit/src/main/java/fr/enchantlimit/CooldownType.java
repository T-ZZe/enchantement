package fr.enchantlimit;

import org.bukkit.Material;

/** Actions pour lesquelles on peut configurer un cooldown. */
public enum CooldownType {
    MACE("Masse", "MACE", "Frapper avec une masse"),
    SPEAR("Lance", "DIAMOND_SPEAR", "Frapper avec une lance"),
    PEARL("Perle de l'Ender", "ENDER_PEARL", "Lancer une perle"),
    WIND_CHARGE("Wind Charge", "WIND_CHARGE", "Lancer une Wind Charge"),
    TRIDENT("Trident", "TRIDENT", "Lancer ou utiliser un trident (riptide)"),
    FIREWORK("Feu d'artifice", "FIREWORK_ROCKET", "Utiliser un feu d'artifice (élytres)"),
    END_CRYSTAL("Cristal de l'End", "END_CRYSTAL", "Placer un cristal"),
    RESPAWN_ANCHOR("Respawn Anchor", "RESPAWN_ANCHOR", "Placer une Respawn Anchor"),
    TNT_MINECART("Minecart TNT", "TNT_MINECART", "Placer un minecart TNT"),
    GAPPLE("Pomme d'or enchantée", "ENCHANTED_GOLDEN_APPLE", "Manger une pomme d'or enchantée");

    /** Paliers (secondes) : 0 = aucun cooldown. Clic gauche = on descend dans la liste. */
    public static final int[] STEPS = {0, 45, 30, 20, 15, 10, 7, 5, 3, 2};

    public final String label;
    public final Material icon;
    public final String description;

    CooldownType(String label, String iconName, String description) {
        this.label = label;
        Material m = Material.matchMaterial(iconName);
        this.icon = m == null ? Material.STICK : m;
        this.description = description;
    }
}
