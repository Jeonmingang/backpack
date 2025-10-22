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
        String fmt = plugin.getConfig().getString("pager.title", plugin.getConfig().getString("backpack.title-format", "&6가방 &7(Page {page})"));
        String plainFmt = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', fmt));
        String plainTitle = org.bukkit.ChatColor.stripColor(title);
        String probe = plainFmt.replace("{page}", "");
        return plainTitle.contains(probe.trim());
    }

    // ---- scroll-block helpers (same as BackpackListener) ----
    private boolean containsAny(String txt, java.util.List<String> needles){
        if (txt == null) return false;
        String s = org.bukkit.ChatColor.stripColor(txt).toLowerCase();
        for (String n : needles) if (s.contains(n.toLowerCase())) return true;
        return false;
    }
    private boolean isBlockedScroll(org.bukkit.inventory.ItemStack it){
        if (it == null || it.getType().isAir()) return false;
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        java.util.List<String> words = cfg.getStringList("scroll-block.list");
        java.util.List<String> pdcKeys = cfg.getStringList("scroll-block.pdc");
        org.bukkit.inventory.meta.ItemMeta meta = it.getItemMeta();
        if (meta != null){
            if (!words.isEmpty()){
                if (meta.hasDisplayName() && containsAny(meta.getDisplayName(), words)) return true;
                if (meta.hasLore()) for (String ln : meta.getLore()) if (containsAny(ln, words)) return true;
            }
            if (!pdcKeys.isEmpty()){
                org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                for (org.bukkit.NamespacedKey k : pdc.getKeys()){
                    String full = (k.getNamespace()+":"+k.getKey()).toLowerCase();
                    for (String want : pdcKeys){
                        String w = want.toLowerCase();
                        if (full.contains(w) || k.getKey().equalsIgnoreCase(w) || k.getNamespace().equalsIgnoreCase(w)) return true;
                    }
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        int page = store.getOpenPage((org.bukkit.entity.Player)e.getPlayer());
        if (page < 2 && !isBackpackTitle(e.getView().getTitle())) return;
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

        // Q/F navigation
        if (e.getClick() == ClickType.DROP){
            e.setCancelled(true);
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            store.nextPage(p, title);
            return;
        }
        if (e.getClick() == ClickType.SWAP_OFFHAND){
            e.setCancelled(true);
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            store.prevPage(p, title);
            return;
        }

        // Scroll-block when moving from player inv -> top (pager)
        if (e.getClickedInventory() == e.getView().getBottomInventory()){
            if (e.isShiftClick()){
                if (isBlockedScroll(e.getCurrentItem())) { e.setCancelled(true); p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c이 아이템은 가방에 넣을 수 없습니다.")); }
            } else if (e.getClick() != ClickType.SWAP_OFFHAND) {
                if (isBlockedScroll(e.getCursor())) { e.setCancelled(true); p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c이 아이템은 가방에 넣을 수 없습니다.")); }
            }
        }
    }

    @EventHandler
    public void onOpenWithBagItem(PlayerInteractEvent e){
        org.bukkit.event.block.Action action = e.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        ItemStack it = e.getItem();
        if (it == null || it.getType().isAir()) return;

        boolean open = false;
        if (it.hasItemMeta()){
            PersistentDataContainer pdc = it.getItemMeta().getPersistentDataContainer();
            if (plugin.getKeyBagFlag() != null && pdc.has(plugin.getKeyBagFlag(), PersistentDataType.BYTE)){
                open = true;
            } else {
                String cfg = plugin.getConfig().getString("starter-item.display-name", "&6가방");
                String display = it.getItemMeta().hasDisplayName() ? it.getItemMeta().getDisplayName() : "";
                String cfgName = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', cfg));
                String dispName = org.bukkit.ChatColor.stripColor(display);
                if (!cfgName.isEmpty() && cfgName.equalsIgnoreCase(dispName)) open = true;
            }
        }
        if (open){
            e.setCancelled(true);
            plugin.getStorage().open(e.getPlayer());
        }
    }
}