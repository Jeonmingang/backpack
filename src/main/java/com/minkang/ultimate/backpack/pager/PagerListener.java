
package com.minkang.ultimate.backpack.pager;

import com.minkang.ultimate.backpack.util.Compat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class PagerListener implements Listener {
    private final JavaPlugin plugin;
    private final Compat compat;

    public PagerListener(JavaPlugin plugin){
        this.plugin = plugin;
        this.compat = new Compat(plugin);
    }
    // Overload for old code: new PagerListener(this, pageStore)
    public PagerListener(com.minkang.ultimate.backpack.BackpackPlugin plugin,
                         com.minkang.ultimate.backpack.pager.PageStore store){
        this((JavaPlugin) plugin);
    }

    private String cc(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }

    private boolean isTicket(ItemStack it){
        if (it==null) return false;
        ItemMeta m = it.getItemMeta(); if (m==null) return false;
        // Name exact/contains
        String expect = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("ticket.name",""));
        if (m.hasDisplayName()){
            String dn = m.getDisplayName();
            if (expect.length()>0 && ChatColor.stripColor(dn).equalsIgnoreCase(ChatColor.stripColor(expect))) return true;
            for (String kw: plugin.getConfig().getStringList("ticket.name-keywords")){
                String t = ChatColor.translateAlternateColorCodes('&', kw);
                if (ChatColor.stripColor(dn).toLowerCase().contains(ChatColor.stripColor(t).toLowerCase())) return true;
            }
        }
        if (m.hasLore()){
            for (String need: plugin.getConfig().getStringList("ticket.lore-contains")){
                String t = ChatColor.translateAlternateColorCodes('&', need);
                for (String line: m.getLore()){
                    if (ChatColor.stripColor(line).toLowerCase().contains(ChatColor.stripColor(t).toLowerCase())) return true;
                }
            }
        }
        for (org.bukkit.NamespacedKey nk : m.getPersistentDataContainer().getKeys()){
            for (String raw: plugin.getConfig().getStringList("ticket.pdc-keys")){
                if (raw == null || raw.isEmpty()) continue;
                String full = nk.getNamespace()+":"+nk.getKey();
                if (full.equalsIgnoreCase(raw) || nk.getKey().equalsIgnoreCase(raw)) return true;
            }
        }
        try {
            org.bukkit.NamespacedKey ticketKey = (org.bukkit.NamespacedKey) plugin.getClass().getMethod("getKeyTicket").invoke(plugin);
            if (ticketKey != null && m.getPersistentDataContainer().has(ticketKey, org.bukkit.persistence.PersistentDataType.STRING)) return true;
        } catch(Throwable ignored){}
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPagerClick(InventoryClickEvent e){
        Inventory top = e.getView().getTopInventory();
        if (top == null) return;
        if (e.getClickedInventory() != top) return;

        ClickType ct = e.getClick();
        if (!(ct == ClickType.RIGHT || ct == ClickType.SHIFT_RIGHT)) return;

        ItemStack cursor = e.getCursor();
        if (!isTicket(cursor)) return;

        e.setCancelled(true);

        Player p = (Player) e.getWhoClicked();
        int pageNow = Math.max(2, compat.getOpenPage(p));
        int sizeNow = compat.getPageSize(p.getUniqueId(), pageNow, 9);

        if (ct == ClickType.RIGHT){
            int target = Math.min(54, ((sizeNow/9)+1)*9);
            if (target <= sizeNow){
                p.sendMessage(cc("&c이미 최대 크기입니다. (&e"+sizeNow+"칸&c)"));
                return;
            }
            compat.setPageSize(p.getUniqueId(), pageNow, target);
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            compat.openPage(p, pageNow, title);
            if (cursor.getAmount()>0) cursor.setAmount(cursor.getAmount()-1);
            p.sendMessage(cc("&a페이지 크기: &e"+sizeNow+" &7→ &e"+target));
            return;
        }

        if (sizeNow < 54){
            p.sendMessage(cc("&c먼저 현재 페이지를 54칸까지 확장하세요."));
            return;
        }
        int nextPage = pageNow + 1;
        compat.setPageSize(p.getUniqueId(), nextPage, 9);
        String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
        compat.openPage(p, nextPage, title);
        if (cursor.getAmount()>0) cursor.setAmount(cursor.getAmount()-1);
        p.sendMessage(cc("&a페이지 &e"+nextPage+"&a가 &e9칸&a으로 생성되었습니다."));
    }
}
