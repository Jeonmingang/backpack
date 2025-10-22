
package com.minkang.ultimate.backpack.listeners;

import com.minkang.ultimate.backpack.util.Compat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;

public class BackpackListener implements Listener {
    private static final int MAX_MAIN = 54;
    private final JavaPlugin plugin;
    private final Compat compat;

    public BackpackListener(JavaPlugin plugin){
        this.plugin = plugin;
        this.compat = new Compat(plugin);
    }
    private String cc(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }

    private boolean matchText(String hay, String needle){
        return ChatColor.stripColor(hay==null?"":hay).toLowerCase().contains(ChatColor.stripColor(needle==null?"":needle).toLowerCase());
    }
    private boolean isTicket(ItemStack it){
        if (it==null || it.getType()== Material.AIR) return false;
        ItemMeta meta = it.getItemMeta(); if (meta==null) return false;
        FileConfiguration cfg = plugin.getConfig();

        // 1) Name exact or contains keywords
        String expect = ChatColor.translateAlternateColorCodes('&', cfg.getString("ticket.name",""));
        if (meta.hasDisplayName()){
            String dn = meta.getDisplayName();
            if (expect.length()>0 && ChatColor.stripColor(dn).equalsIgnoreCase(ChatColor.stripColor(expect))) return true;
            // keywords fallback
            for (String kw : cfg.getStringList("ticket.name-keywords")){
                if (matchText(dn, ChatColor.translateAlternateColorCodes('&', kw))) return true;
            }
        }
        // 2) Lore contains
        if (meta.hasLore()){
            List<String> lore = meta.getLore();
            for (String need: cfg.getStringList("ticket.lore-contains")){
                String needle = ChatColor.translateAlternateColorCodes('&', need);
                for (String line: lore){
                    if (matchText(line, needle)) return true;
                }
            }
        }
        // 3) PDC keys
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (String raw : cfg.getStringList("ticket.pdc-keys")){
            if (raw==null || raw.trim().isEmpty()) continue;
            for (NamespacedKey nk: pdc.getKeys()){
                String full = nk.getNamespace()+":"+nk.getKey();
                if (full.equalsIgnoreCase(raw) || nk.getKey().equalsIgnoreCase(raw)) return true;
            }
        }
        try {
            NamespacedKey ticketKey = (NamespacedKey) plugin.getClass().getMethod("getKeyTicket").invoke(plugin);
            if (ticketKey != null && pdc.has(ticketKey, PersistentDataType.STRING)) return true;
        } catch(Throwable ignored){}
        return false;
    }
    private void consume(ItemStack it){ if (it.getAmount()>1) it.setAmount(it.getAmount()-1); else it.setType(Material.AIR); }

    @EventHandler(ignoreCancelled = true)
    public void onUse(PlayerInteractEvent e){
        Action a = e.getAction();
        if (a!=Action.RIGHT_CLICK_AIR && a!=Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand()!=EquipmentSlot.HAND) return; // offhand duplicate guard

        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isTicket(hand)) return;

        int current = compat.getCurrentSize(p.getUniqueId(), 9);

        if (p.isSneaking()){
            if (current < MAX_MAIN){
                p.sendMessage(cc("&c먼저 메인가방을 54칸까지 확장하세요."));
                e.setCancelled(true); return;
            }
            int p2 = compat.getPageSize(p.getUniqueId(), 2, 0);
            if (p2<=0){
                compat.setPageSize(p.getUniqueId(), 2, 9);
                p.sendMessage(cc("&a2페이지를 &e9칸&a으로 생성했습니다."));
            }else{
                p.sendMessage(cc("&a2페이지를 엽니다."));
            }
            String title = plugin.getConfig().getString("pager.title","&6가방 &7(Page {page})");
            compat.openPage(p, 2, title);
            consume(hand);
            e.setCancelled(true); return;
        }

        int target = compat.nextSize(current);
        if (target <= current){
            p.sendMessage(cc("&c더 이상 확장할 수 없습니다. (&e현재 "+current+"칸&c)"));
            if (current>=MAX_MAIN) p.sendMessage(cc("&7쉬프트+우클릭: &f2페이지 9칸 생성/오픈"));
            e.setCancelled(true); return;
        }
        compat.setCurrentSize(p.getUniqueId(), target);
        consume(hand);
        p.sendMessage(cc("&a메인가방 크기: &e"+current+" &7→ &e"+target));
        e.setCancelled(true);
    }
}
