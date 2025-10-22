package com.minkang.ultimate.backpack.pager;

import com.minkang.ultimate.backpack.BackpackPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PagerListener implements Listener {

    private final BackpackPlugin plugin;
    private final PageStore store;

    public PagerListener(BackpackPlugin plugin, PageStore store){
        this.plugin = plugin;
        this.store = store;
    }

    private boolean isBackpackTitle(String title){
        if (title == null) return false;
        String fmt = plugin.getConfig().getString("backpack.title-format", "&6[개인가방] &7({page})");
        // quick check – compare without colors and replace {page}
        String plainFmt = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', fmt));
        String plainTitle = org.bukkit.ChatColor.stripColor(title);
        String probe = plainFmt.replace("{page}", "");
        return plainTitle.contains(probe.trim());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        if (!isBackpackTitle(e.getView().getTitle())) return;
        Inventory top = e.getView().getTopInventory();
        store.savePage(e.getPlayer().getUniqueId(), store.getOpenPage((org.bukkit.entity.Player)e.getPlayer()), top.getContents());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof org.bukkit.entity.Player)) return;
        if (!isBackpackTitle(e.getView().getTitle())) return;

        // Q/F navigation
        if (e.getClick() == ClickType.DROP){ // Q
            e.setCancelled(true);
            String title = plugin.getConfig().getString("backpack.title-format", "&6[개인가방] &7({page})");
            store.nextPage((org.bukkit.entity.Player)e.getWhoClicked(), title);
            return;
        }
        if (e.getClick() == ClickType.SWAP_OFFHAND){ // F
            e.setCancelled(true);
            String title = plugin.getConfig().getString("backpack.title-format", "&6[개인가방] &7({page})");
            store.prevPage((org.bukkit.entity.Player)e.getWhoClicked(), title);
            return;
        }
    }

    @EventHandler
    public void onOpenWithBagItem(PlayerInteractEvent e){
        // Right-click with a designated "bag item" opens page 1
        org.bukkit.event.block.Action a = e.getAction();
        if (a != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && a != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        ItemStack it = e.getItem();
        if (it == null || it.getType().isAir()) return;

        boolean open = false;
        if (it.hasItemMeta()){
            PersistentDataContainer pdc = it.getItemMeta().getPersistentDataContainer();
            if (plugin.getKeyBagFlag() != null && pdc.has(plugin.getKeyBagFlag(), PersistentDataType.BYTE)){
                open = true; // new PDC-based item
            } else {
                // Legacy compatibility: match display-name from config
                String cfg = plugin.getConfig().getString("starter-item.display-name", "&6가방");
                String display = it.getItemMeta().hasDisplayName() ? it.getItemMeta().getDisplayName() : "";
                String a = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', cfg));
                String b = org.bukkit.ChatColor.stripColor(display);
                if (!a.isEmpty() && a.equalsIgnoreCase(b)) {
                    open = true;
                }
            }
        }
        if (open){
            e.setCancelled(true);
            plugin.getStorage().open(e.getPlayer());
        }
    }
}