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

public class BackpackListener implements Listener {

    private final BackpackPlugin plugin;
    public BackpackListener(BackpackPlugin plugin){ this.plugin = plugin; }

    private String strip(String s){ return ChatColor.stripColor(s==null?"":s); }
    private String cc(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }

    // ---- 확장권 사용 ----
    @EventHandler
    public void onUse(PlayerInteractEvent e){
        Action a = e.getAction();
        if (!(a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK)) return;
        if (e.getHand() == null || e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return; // 메인핸드만
        Player p = e.getPlayer();
        ItemStack it = e.getItem();
        if (it == null || it.getType() == Material.AIR) return;
        ItemMeta m = it.getItemMeta();
        if (m == null) return;
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        String tag = pdc.get(plugin.getKeyTicket(), PersistentDataType.STRING);
        if (tag == null) return; // 확장권 아님

        int current = plugin.getStorage().getCurrentSize(p.getUniqueId());
        Integer target = null;
        if (tag.startsWith("size:")) {
            try { target = Integer.parseInt(tag.substring(5)); } catch (NumberFormatException ignored) {}
            if (target != null) target = plugin.getStorage().nearestAllowed(target);
        } else {
            target = plugin.getStorage().nextSize(current);
        }

        // 쉬프트+우클릭: 메인가방이 54칸이면 2페이지 9칸을 생성/오픈
        if (p.isSneaking() && current >= 54) {
            // 소모
            if (it.getAmount() > 0) it.setAmount(it.getAmount()-1);
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            // 기본 9칸으로 초기화
            plugin.getPagerStore().setPageSize(p.getUniqueId(), 2, 9);
            plugin.getPagerStore().openPage(p, 2, title);
            p.sendMessage(cc("&a2페이지가 &e9칸&a으로 생성되었습니다."));
            e.setCancelled(true);
            return;
        }

        if (target == null) { p.sendMessage(cc("&c더 이상 확장할 수 없습니다.")); e.setCancelled(true); return; }
        if (target <= current) { p.sendMessage(cc("&c이미 해당 크기 이상입니다. 현재: &e" + current + "&7 / 최대 54칸. &f쉬프트+우클릭으로 2페이지를 여세요.")); e.setCancelled(true); return; }

        plugin.getStorage().setCurrentSize(p.getUniqueId(), target);
        // 1장 소모
        if (it.getAmount() > 0) it.setAmount(it.getAmount() - 1);
        p.sendMessage(cc("&a가방 크기가 &e" + current + " &7→ &e" + target + " &a로 확장되었습니다."));
        e.setCancelled(true);

    // ---- 강화서(스크롤) 차단 ----
    private boolean containsAny(String text, java.util.List<String> needles){
        if (text == null) return false;
        String s = strip(text).toLowerCase();
        for (String n : needles){
            if (s.contains(n.toLowerCase())) return true;
        }
        return false;
    }

    
    private boolean isTicket(ItemStack it){
        if (it == null || it.getType() == Material.AIR) return false;
        ItemMeta m = it.getItemMeta();
        if (m == null) return false;
        String tag = m.getPersistentDataContainer().get(plugin.getKeyTicket(), PersistentDataType.STRING);
        return tag != null;
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
                if (meta.hasLore()){
                    for (String ln : meta.getLore()){
                        if (containsAny(ln, words)) return true;
                    }
                }
            }
            if (!pdcKeys.isEmpty()){
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                for (NamespacedKey k : pdc.getKeys()){
                    String full = (k.getNamespace()+":"+k.getKey()).toLowerCase();
                    for (String key : pdcKeys){
                        String t = key.toLowerCase();
                        if (full.contains(t) || k.getKey().equalsIgnoreCase(key) || k.getNamespace().equalsIgnoreCase(key)){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isBackpackTop(Inventory inv, String title){
        if (inv == null) return false;
        String plain = strip(title);
        return plain != null && plain.startsWith("[개인가방]");
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        Inventory top = e.getView().getTopInventory();
        Inventory bottom = e.getView().getBottomInventory();
        boolean isBag = isBackpackTop(top, title);

        if (!isBag) return;

        // 플레이어 인벤 → 가방(top)으로 이동하려는 아이템 차단 (커서/쉬프트클릭)
        if (e.getClickedInventory() == bottom){
            if (e.isShiftClick()){
                ItemStack clicked = e.getCurrentItem();
                if (isBlockedScroll(clicked)){
                    e.setCancelled(true);
                    p.sendMessage(cc("&c해당 아이템은 가방에 넣을 수 없습니다."));
                    return;
                }
            } else {
                ItemStack cursor = e.getCursor();
                if (cursor != null && isBlockedScroll(cursor) && e.getClick() != ClickType.SWAP_OFFHAND){
                    e.setCancelled(true);
                    p.sendMessage(cc("&c해당 아이템은 가방에 넣을 수 없습니다."));
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e){
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        Inventory top = e.getView().getTopInventory();
        if (!isBackpackTop(top, title)) return;

        ItemStack cursor = e.getOldCursor();
        if (cursor == null || cursor.getType() == Material.AIR) return;
        if (!isBlockedScroll(cursor)) return;

        int topSize = top.getSize();
        for (int rawSlot : e.getRawSlots()){
            if (rawSlot < topSize){
                e.setCancelled(true);
                p.sendMessage(cc("&c해당 아이템은 가방에 넣을 수 없습니다."));
                return;
            }
        }
    }
}