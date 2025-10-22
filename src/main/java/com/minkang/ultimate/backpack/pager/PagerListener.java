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
    private boolean isTicket(org.bukkit.inventory.ItemStack it){
        if (it == null || it.getType().isAir()) return false;
        org.bukkit.inventory.meta.ItemMeta m = it.getItemMeta();
        if (m == null) return false;
        String tag = m.getPersistentDataContainer().get(plugin.getKeyTicket(), PersistentDataType.STRING);
        return tag != null;
    }
    
    private static final int MAX_MAIN_SIZE = 54;

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

        if (e.getClick() == ClickType.DROP) {
            e.setCancelled(true);
            int current = plugin.getStorage().getCurrentSize(p.getUniqueId());
            if (current < MAX_MAIN_SIZE){
                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c메인가방을 &e54&c칸까지 먼저 확장하세요."));
                return;
            }
            e.setCancelled(true);
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            store.nextPage(p, title);
            return;
        }
        if (e.getClick() == ClickType.SWAP_OFFHAND) {
            e.setCancelled(true);
            int current = plugin.getStorage().getCurrentSize(p.getUniqueId());
            if (current < MAX_MAIN_SIZE){
                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c메인가방을 &e54&c칸까지 먼저 확장하세요."));
                return;
            }
            e.setCancelled(true);
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            store.prevPage(p, title);
            return;
        }

        if (e.getClickedInventory() == e.getView().getBottomInventory()){
            if (e.isShiftClick()){
                if (isBlockedScroll(e.getCurrentItem())) { e.setCancelled(true); p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c이 아이템은 가방에 넣을 수 없습니다.")); }
            }
        // Ticket usage on top inventory (page GUI): RIGHT to expand size, SHIFT+RIGHT at 54 → open next page (9)
        if (e.getClickedInventory() == e.getView().getTopInventory()){
            org.bukkit.event.inventory.ClickType ct = e.getClick();
            org.bukkit.entity.Player pp = (org.bukkit.entity.Player)e.getWhoClicked();
            if ((ct == ClickType.RIGHT || ct == ClickType.SHIFT_RIGHT) && e.getCursor() != null && isTicket(e.getCursor())){
                e.setCancelled(true);
                int pageNow = store.getOpenPage(pp);
                if (pageNow < 2 && !isBackpackTitle(e.getView().getTitle())) return; // safety

                int sizeNow = store.getPageSize(pp.getUniqueId(), pageNow, 9);
                if (ct == ClickType.SHIFT_RIGHT){
                    if (sizeNow >= 54){
                        int nextPage = Math.max(2, pageNow+1);
                        store.setPageSize(pp.getUniqueId(), nextPage, 9);
                        String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
                        store.openPage(pp, nextPage, title);
                        if (e.getCursor().getAmount() > 0) e.getCursor().setAmount(e.getCursor().getAmount()-1);
                        pp.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&a" + nextPage + "페이지가 &e9칸&a으로 생성되었습니다."));
                    } else {
                        Integer target = plugin.getStorage().nearestAllowed(sizeNow+9);
                        if (target != null && target > sizeNow && target <= 54){
                            store.setPageSize(pp.getUniqueId(), pageNow, target);
                            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
                            store.openPage(pp, pageNow, title);
                            if (e.getCursor().getAmount() > 0) e.getCursor().setAmount(e.getCursor().getAmount()-1);
                            pp.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&a페이지 크기: &e" + sizeNow + " &7→ &e" + target));
                        } else {
                            pp.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c더 이상 확장할 수 없습니다."));
                        }
                    }
                } else {
                        // shift-right while not full: treat as normal increment
                        Integer target = plugin.getStorage().nearestAllowed(sizeNow+9);
                        if (target != null && target > sizeNow && target <= 54){
                            store.setPageSize(pp.getUniqueId(), pageNow, target);
                            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
                            store.openPage(pp, pageNow, title);
                            if (e.getCursor().getAmount() > 0) e.getCursor().setAmount(e.getCursor().getAmount()-1);
                            pp.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&a페이지 크기: &e" + sizeNow + " &7→ &e" + target));
                        }
                    }
                } else { // RIGHT (no shift)
                    if (sizeNow >= 54){
                        pp.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c이미 최대 크기(54칸)입니다. &f쉬프트+우클릭으로 다음 페이지를 여세요."));
                    } else {
                        Integer target = plugin.getStorage().nearestAllowed(sizeNow+9);
                        if (target == null || target <= sizeNow) target = Math.min(54, sizeNow + 9);
                        store.setPageSize(pp.getUniqueId(), pageNow, target);
                        String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
                        store.openPage(pp, pageNow, title);
                        if (e.getCursor().getAmount() > 0) e.getCursor().setAmount(e.getCursor().getAmount()-1);
                        pp.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&a페이지 크기: &e" + sizeNow + " &7→ &e" + target));
                    }
                }
                return;
            }
        }
 else if (e.getClick() != ClickType.SWAP_OFFHAND) {
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
            org.bukkit.entity.Player p2 = e.getPlayer();
            int current2 = plugin.getStorage().getCurrentSize(p2.getUniqueId());
            e.setCancelled(true);
            if (p2.isSneaking()){
                if (current2 < MAX_MAIN_SIZE){
                    p2.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c메인가방을 &e54&c칸까지 먼저 확장하세요."));
                } else {
                    String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
                    store.openPage(p2, 2, title);
                }
            } else {
                plugin.getStorage().open(p2);
            }
        }
    }
}