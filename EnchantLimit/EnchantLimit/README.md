# EnchantLimit

Plugin Paper 26.3 : `/enchantement` (OP).

- **Items** / **Armures** : menus par matiere (fer, diamant, cuir...) puis par piece, puis les livres d'enchantement.
  - Clic gauche : baisse la limite (boucle 4 -> 3 -> 2 -> 1 -> 0 interdit -> 4)
  - Clic droit : monte, Shift + clic : vanilla
- **Limites de pieces** : nombre max de pieces d'une meme armure portees en meme temps (ex. 2 en netherite).
- Applique aux tables d'enchantement, enclumes, forges (diamant -> netherite) et inventaires.

## Limites d'objets et cooldowns

- **Limites d'objets** : quantite max portee de Wind Charge, pommes d'or enchantees, totems et potions de Force II.
- **Cooldowns** : masse, lance, perles, Wind Charge, trident, feux d'artifice, cristaux, Respawn Anchor,
  minecart TNT, pomme d'or enchantee. Paliers : aucun, 45, 30, 20, 15, 10, 7, 5, 3, 2 secondes.
- Permission `enchantlimit.bypass` : ignore les limites de pieces, d'objets et les cooldowns (aucun joueur par defaut).
