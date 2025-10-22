
package com.minkang.ultimate.backpack.pager;

import com.minkang.ultimate.backpack.BackpackPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PagerListener implements Listener {
    private static final int MAX_MAIN_SIZE = 54;

    private final BackpackPlugin plugin;
    private final PageStore store;

    public PagerListener(BackpackPlugin plugin, PageStore store){
        this.plugin = plugin;
        this.store = store;
    }

    private boolean isBackpackTitle(String title){
        String fmt = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
        String plainFmt = org.bukkit.ChatColor.stripColor(fmt);
        String plainTitle = org.bukkit.ChatColor.stripColor(title);
        String probe = plainFmt.replace("{page}", "").replace("{{page}}","");
        return plainTitle != null && probe != null && plainTitle.contains(probe.trim());
    }

    private boolean isTicket(ItemStack it){
        if (it == null) return false;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String tag = pdc.get(plugin.getKeyTicket(), PersistentDataType.STRING);
        if (tag != null) return true;
        for (String k : plugin.getConfig().getStringList("ticket.pdc-keys")){
            if (k == null || k.isEmpty()) continue;
            String keyL = k.toLowerCase();
            for (org.bukkit.NamespacedKey nk : pdc.getKeys()){
                String full = (nk.getNamespace()+":"+nk.getKey()).toLowerCase();
                if (full.contains(keyL) || nk.getKey().equalsIgnoreCase(k) || nk.getNamespace().equalsIgnoreCase(k)){
                    return true;
                }
            }
        }
        return false;
    }

    private int nextAllowed(int now){
        int[] allowed = new int[]{9,18,27,36,45,54};
        for (int a : allowed){
            if (a > now) return a;
        }
        return now;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPagerTicket(InventoryClickEvent e){
        Inventory top = e.getView().getTopInventory();
        if (top == null) return;
        if (!isBackpackTitle(e.getView().getTitle())) return;

        Player p = (Player)e.getWhoClicked();
        int open = Math.max(2, store.getOpenPage(p)); // default 2+ views considered pager
        if (open < 2) return;

        // Use only when clicking TOP with a ticket on cursor
        if (e.getClickedInventory() != top) return;
        ClickType ct = e.getClick();
        if (!(ct == ClickType.RIGHT || ct == ClickType.SHIFT_RIGHT)) return;
        ItemStack cursor = e.getCursor();
        if (!isTicket(cursor)) return;

        e.setCancelled(true);

        int pageNow = store.getOpenPage(p);
        if (pageNow < 2) pageNow = 2;
        int sizeNow = store.getPageSize(p.getUniqueId(), pageNow, 9);

        if (ct == ClickType.SHIFT_RIGHT){
            // Only create NEXT page when current page has reached 54
            if (sizeNow >= 54){
                int nextPage = Math.max(2, pageNow + 1);
                store.setPageSize(p.getUniqueId(), nextPage, 9);
                String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
                store.openPage(p, nextPage, title);
                if (cursor.getAmount() > 0) cursor.setAmount(cursor.getAmount()-1);
                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&a페이지 &e"+nextPage+"&a가 &e9칸&a으로 생성되었습니다."));
            } else {
                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c먼저 현재 페이지를 54칸까지 확장하세요. (티켓 &f우클릭&c)"));
            }
            return;
        }

        // RIGHT click: grow current page by 9, capped at 54
        if (sizeNow >= 54){
            p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c이미 최대 크기(54칸)입니다. &7(&f쉬프트+우클릭&7=다음 페이지 생성)"));
            return;
        }
        int target = nextAllowed(sizeNow);
        store.setPageSize(p.getUniqueId(), pageNow, target);
        String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
        store.openPage(p, pageNow, title);
        if (cursor.getAmount() > 0) cursor.setAmount(cursor.getAmount()-1);
        p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&a페이지 크기: &e"+sizeNow+" &7→ &e"+target));
    }
}
