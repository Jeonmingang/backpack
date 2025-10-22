package com.minkang.ultimate.backpack.listeners;

import com.minkang.ultimate.backpack.BackpackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class BackpackListener implements Listener {

    private final BackpackPlugin plugin;
    public BackpackListener(BackpackPlugin plugin){ this.plugin = plugin; }

    private String strip(String s){ return ChatColor.stripColor(s==null?"":s); }
    private String cc(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }

    // 확장권 사용 (우클릭)
    @EventHandler
    public void onUse(PlayerInteractEvent e){
        Action a = e.getAction();
        if (!(a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK)) return;
        if (e.getHand() == null || e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;

        Player p = e.getPlayer();
        ItemStack it = e.getItem();
        if (it == null || it.getType() == Material.AIR) return;

        ItemMeta m = it.getItemMeta();
        if (m == null) return;
        String tag = m.getPersistentDataContainer().get(plugin.getKeyTicket(), PersistentDataType.STRING);
        if (tag == null) return;

        int current = plugin.getStorage().getCurrentSize(p.getUniqueId());
        Integer target;
        if (tag.startsWith("size:")) {
            try { target = Integer.parseInt(tag.substring(5)); } catch (NumberFormatException ex) { target = null; }
            if (target != null) target = plugin.getStorage().nearestAllowed(target);
        } else {
            target = plugin.getStorage().nextSize(current);
        }
        if (target == null || target <= current) { e.setCancelled(true); p.sendMessage(cc("&c더 이상 확장할 수 없습니다.")); return; }

        plugin.getStorage().setCurrentSize(p.getUniqueId(), target);
        it.setAmount(it.getAmount()-1);
        p.sendMessage(cc("&a가방 크기: &e" + current + " &7→ &e" + target));
        e.setCancelled(true);
    }

    // -------- 스크롤(강화서) 차단 공통 로직 --------
    private boolean containsAny(String txt, java.util.List<String> needles){
        if (txt == null) return false;
        String s = strip(txt).toLowerCase();
        for (String n : needles) if (s.contains(n.toLowerCase())) return true;
        return false;
    }
    private boolean isBlockedScroll(ItemStack it){
        if (it == null || it.getType() == Material.AIR) return false;
        FileConfiguration cfg = plugin.getConfig();
        java.util.List<String> words = cfg.getStringList("scroll-block.list");
        java.util.List<String> pdcKeys = cfg.getStringList("scroll-block.pdc");
        ItemMeta meta = it.getItemMeta();
        if (meta != null){
            if (!words.isEmpty()){
                if (meta.hasDisplayName() && containsAny(meta.getDisplayName(), words)) return true;
                if (meta.hasLore()) for (String ln : meta.getLore()) if (containsAny(ln, words)) return true;
            }
            if (!pdcKeys.isEmpty()){
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                for (NamespacedKey k : pdc.getKeys()){
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
    private boolean isBackpackTop(Inventory top, String title){
        String plain = strip(title);
        return top != null && plain != null && plain.startsWith("[개인가방]");
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player)e.getWhoClicked();
        if (!isBackpackTop(e.getView().getTopInventory(), e.getView().getTitle())) return;

        // 플레이어 인벤 → 가방으로 이동 시 차단
        if (e.getClickedInventory() == e.getView().getBottomInventory()){
            if (e.isShiftClick()){
                if (isBlockedScroll(e.getCurrentItem())) { e.setCancelled(true); p.sendMessage(cc("&c이 아이템은 가방에 넣을 수 없습니다.")); }
            } else if (e.getClick() != ClickType.SWAP_OFFHAND) {
                if (isBlockedScroll(e.getCursor())) { e.setCancelled(true); p.sendMessage(cc("&c이 아이템은 가방에 넣을 수 없습니다.")); }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e){
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player)e.getWhoClicked();
        Inventory top = e.getView().getTopInventory();
        if (!isBackpackTop(top, e.getView().getTitle())) return;

        ItemStack cursor = e.getOldCursor();
        if (cursor == null || cursor.getType() == Material.AIR) return;
        if (!isBlockedScroll(cursor)) return;

        int topSize = top.getSize();
        for (int raw : e.getRawSlots()){
            if (raw < topSize){ e.setCancelled(true); p.sendMessage(cc("&c이 아이템은 가방에 넣을 수 없습니다.")); return; }
        }
    }
}
