package fr.enchantlimit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** Limite la quantite portee de certains objets (Wind Charge, gapples, totems, Force II). */
public class ItemLimitListener implements Listener {

    private final LimitManager limits;

    public ItemLimitListener(LimitManager limits) {
        this.limits = limits;
    }

    private boolean bypass(Player p) {
        return p.hasPermission("enchantlimit.bypass");
    }

    public int count(Player p, ItemKind kind) {
        int n = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (kind.matches(it)) n += it.getAmount();
        }
        return n;
    }

    /** Fait tomber ce qui depasse la limite (appele regulierement). */
    public void enforce(Player p) {
        if (bypass(p)) return;
        PlayerInventory inv = p.getInventory();
        for (ItemKind kind : ItemKind.values()) {
            int limit = limits.getItemLimit(kind);
            if (limit < 0) continue;
            int excess = count(p, kind) - limit;
            if (excess <= 0) continue;

            for (int i = inv.getSize() - 1; i >= 0 && excess > 0; i--) {
                ItemStack it = inv.getItem(i);
                if (!kind.matches(it)) continue;
                int take = Math.min(excess, it.getAmount());
                ItemStack drop = it.clone();
                drop.setAmount(take);
                if (take >= it.getAmount()) {
                    inv.setItem(i, null);
                } else {
                    it.setAmount(it.getAmount() - take);
                    inv.setItem(i, it);
                }
                Item dropped = p.getWorld().dropItem(p.getLocation(), drop);
                dropped.setPickupDelay(60);
                excess -= take;
            }
            p.sendMessage(Component.text("Tu ne peux porter que " + limit + " × "
                    + kind.label + " à la fois.", NamedTextColor.RED));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p) || bypass(p)) return;
        ItemStack st = e.getItem().getItemStack();
        for (ItemKind kind : ItemKind.values()) {
            if (!kind.matches(st)) continue;
            int limit = limits.getItemLimit(kind);
            if (limit < 0) return;

            int allowed = limit - count(p, kind);
            if (allowed >= st.getAmount()) return;

            e.setCancelled(true);
            if (allowed > 0) { // ramasse seulement ce qui rentre dans la limite
                ItemStack give = st.clone();
                give.setAmount(allowed);
                int left = 0;
                for (ItemStack l : p.getInventory().addItem(give).values()) left += l.getAmount();
                int given = allowed - left;
                if (given > 0) {
                    st.setAmount(st.getAmount() - given);
                    e.getItem().setItemStack(st);
                }
            }
            p.sendActionBar(Component.text("Limite atteinte : " + limit + " × " + kind.label,
                    NamedTextColor.RED));
            return;
        }
    }
}
