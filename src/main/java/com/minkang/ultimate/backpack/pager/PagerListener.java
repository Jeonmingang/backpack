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

    // Title helper kept only as fallback; primary detection uses store.getOpenPage
    private boolean isBackpackTitle(String title){
        if (title == null) return false;
        String fmt = plugin.getConfig().getString("pager.title", plugin.getConfig().getString("backpack.title-format", "&6가방 &7(Page {page})"));
        String plainFmt = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', fmt));
        String plainTitle = org.bukkit.ChatColor.stripColor(title);
        String probe = plainFmt.replace("{page}", "");
        return plainTitle.contains(probe.trim());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        int page = store.getOpenPage((org.bukkit.entity.Player)e.getPlayer());
        if (page < 2 && !isBackpackTitle(e.getView().getTitle())) return;
        // save only if it's a pager inventory
        if (page >= 2){
            Inventory top = e.getView().getTopInventory();
            store.savePage(e.getPlayer().getUniqueId(), page, top.getContents());
            store.setOpenPage((org.bukkit.entity.Player)e.getPlayer(), 0);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player)e.getWhoClicked();
        int page = store.getOpenPage(p);
        if (page < 2 && !isBackpackTitle(e.getView().getTitle())) return;

        if (e.getClick() == ClickType.DROP){ // Q
            e.setCancelled(true);
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            store.nextPage(p, title);
            return;
        }
        if (e.getClick() == ClickType.SWAP_OFFHAND){ // F
            e.setCancelled(true);
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            store.prevPage(p, title);
            return;
        }
    }

    @EventHandler
    public void onOpenWithBagItem(PlayerInteractEvent e){
        // Right-click with a designated "bag item" opens main bag
        org.bukkit.event.block.Action action = e.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

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
                String cfgName = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', cfg));
                String dispName = org.bukkit.ChatColor.stripColor(display);
                if (!cfgName.isEmpty() && cfgName.equalsIgnoreCase(dispName)) {
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