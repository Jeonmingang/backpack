
package com.minkang.ultimate.backpack.pager;

import com.minkang.ultimate.backpack.BackpackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.List;

public class PagerListener implements Listener {
    private final BackpackPlugin plugin;
    private final PageStore store;

    public PagerListener(BackpackPlugin plugin, PageStore store){
        this.plugin = plugin;
        this.store = store;
    }

    private String c(String s){ return ChatColor.translateAlternateColorCodes('&', s); }

    private boolean isBackpackTitle(String title){
        if (title == null) return false;
        String raw = ChatColor.stripColor(title);
        if (raw == null) return false;
        return raw.contains("가방") || raw.contains("Backpack") || raw.contains("Page ");
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        if (!isBackpackTitle(e.getView().getTitle())) return;
        Inventory top = e.getView().getTopInventory();
        store.savePage(e.getPlayer().getUniqueId(), store.getOpenPage((org.bukkit.entity.Player) e.getPlayer()), top.getContents());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof org.bukkit.entity.Player)) return;
        if (!isBackpackTitle(e.getView().getTitle())) return;

        // Q/F 네비게이션
        if (e.getClick() == ClickType.DROP){ // Q
            e.setCancelled(true);
            store.nextPage((org.bukkit.entity.Player) e.getWhoClicked(), plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})"));
            return;
        }
        if (e.getClick() == ClickType.SWAP_OFFHAND){ // F
            e.setCancelled(true);
            store.prevPage((org.bukkit.entity.Player) e.getWhoClicked(), plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})"));
            return;
        }

        // 강화서 차단
        ItemStack moving = e.getCursor();
        if (moving == null || moving.getType()==Material.AIR) moving = e.getCurrentItem();
        if (moving != null && moving.getType()!=Material.AIR){
            if (isEnchantScroll(moving)){
                e.setCancelled(true);
                e.getWhoClicked().sendMessage(c("&c강화서는 가방에 넣을 수 없습니다."));
            }
        }
    }

    private boolean containsAny(String text, List<String> needles){
        if (text == null) return false;
        String lower = ChatColor.stripColor(text).toLowerCase();
        for (String n : needles){
            if (lower.contains(n.toLowerCase())) return true;
        }
        return false;
    }

    private boolean pdcContains(ItemMeta meta, List<String> keys){
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (NamespacedKey key : pdc.getKeys()){
            String full = key.getNamespace()+":"+key.getKey();
            for (String s : keys){
                if (full.toLowerCase().contains(s.toLowerCase())) return true;
            }
        }
        return false;
    }

    private boolean isEnchantScroll(ItemStack it){
        if (it == null) return false;
        ItemMeta meta = it.getItemMeta();
        java.util.List<String> words = plugin.getConfig().getStringList("scroll-block.list");
        java.util.List<String> pdcKeys = plugin.getConfig().getStringList("scroll-block.pdc");
        if (meta != null){
            if (containsAny(meta.getDisplayName(), words)) return true;
            if (meta.hasLore()){
                for (String l : meta.getLore()){
                    if (containsAny(l, words)) return true;
                }
            }
            if (pdcContains(meta, pdcKeys)) return true;
            String loc = meta.getLocalizedName();
            if (containsAny(loc, words)) return true;
        }
        if (it.getType() == Material.PAPER && meta != null && containsAny(meta.getDisplayName(), java.util.Arrays.asList("강화","ENCHANT"))){
            return true;
        }
        return false;
    }

    // 확장권: 54칸 이후는 페이지 확장 (웅크리고 우클릭)
    @EventHandler
    public void onUseTicket(PlayerInteractEvent e){
        if (e.getItem()==null || e.getItem().getType()==Material.AIR) return;
        ItemStack hand = e.getItem();
        ItemMeta meta = hand.getItemMeta();
        if (meta==null) return;

        boolean looksLikeTicket = false;
        if (meta.hasLore()){
            for (String l : meta.getLore()){
                String s = ChatColor.stripColor(l);
                if (s != null && (s.contains("가방 확장") || s.contains("size:") || s.contains("확장권"))) { looksLikeTicket = true; break; }
            }
        }
        if (!looksLikeTicket) return;
        if (!e.getPlayer().isSneaking()) return;

        e.setCancelled(true);
        // 2페이지부터 9→18→…→54 확장. 2페이지가 54되면 3페이지 9부터 반복
        int page = 2;
        while (store.getPageSize(e.getPlayer().getUniqueId(), page) == 54){
            page++;
        }
        int curSize = store.getPageSize(e.getPlayer().getUniqueId(), page);
        int next = nextSize(curSize);
        store.setPageSize(e.getPlayer().getUniqueId(), page, next);
        hand.setAmount(Math.max(0, hand.getAmount()-1));
        e.getPlayer().sendMessage(c("&a확장권 사용됨: &7페이지 &e"+page+" &7크기 → &e"+next));
    }

    private int nextSize(int current){
        int[] allowed = new int[]{9,18,27,36,45,54};
        for (int s : allowed){
            if (s > current) return s;
        }
        return 54;
    }
}
