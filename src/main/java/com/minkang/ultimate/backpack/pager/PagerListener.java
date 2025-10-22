package com.minkang.ultimate.backpack.pager;

import com.minkang.ultimate.backpack.BackpackPlugin;
import org.bukkit.entity.Player;
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
        if (title == null) return false;
        String fmt = plugin.getConfig().getString("pager.title", plugin.getConfig().getString("backpack.title-format", "&6가방 &7(Page {page})"));
        String plainFmt = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', fmt));
        String plainTitle = org.bukkit.ChatColor.stripColor(title);
        String probe = plainFmt.replace("{page}", "").replace("{{page}}","");
        return plainTitle != null && plainTitle.contains(probe.trim());
    }

    private boolean isTicket(ItemStack it){
        if (it == null || it.getType().isAir()) return false;
        ItemMeta m = it.getItemMeta();
        if (m == null) return false;
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        String tag = pdc.get(plugin.getKeyTicket(), PersistentDataType.STRING);
        return tag != null;
    }

    private int nextAllowed(int size){
        if (size >= 54) return 54;
        int step = ((size + 8) / 9) * 9;
        if (step < 9) step = 9;
        if (step == size) step += 9;
        if (step > 54) step = 54;
        return step;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player)e.getPlayer();
        int page = store.getOpenPage(p);
        if (page >= 2){
            Inventory top = e.getView().getTopInventory();
            store.savePage(p.getUniqueId(), page, top.getContents());
            store.setOpenPage(p, 0);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player)e.getWhoClicked();
        int openPage = store.getOpenPage(p);
        boolean isPagerView = openPage >= 2 || isBackpackTitle(e.getView().getTitle());
        if (!isPagerView) return;

        int current = plugin.getStorage().getCurrentSize(p.getUniqueId());

        // Q/F navigation (next/prev) gated by 54
        if (e.getClick() == ClickType.DROP){ // Q
            e.setCancelled(true);
            if (current < MAX_MAIN_SIZE){
                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c메인가방을 &e54&c칸까지 먼저 확장하세요."));
                return;
            }
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            store.nextPage(p, title);
            return;
        }
        if (e.getClick() == ClickType.SWAP_OFFHAND){ // F
            e.setCancelled(true);
            if (current < MAX_MAIN_SIZE){
                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c메인가방을 &e54&c칸까지 먼저 확장하세요."));
                return;
            }
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            store.prevPage(p, title);
            return;
        }

        // Ticket usage on TOP inventory (page GUI)
        if (e.getClickedInventory() == e.getView().getTopInventory()){
            ClickType ct = e.getClick();
            if ((ct == ClickType.RIGHT || ct == ClickType.SHIFT_RIGHT) && e.getCursor() != null && isTicket(e.getCursor())){
                e.setCancelled(true);
                int pageNow = store.getOpenPage(p);
                if (pageNow < 2){
                    // safety: determine page from title if possible; default to 2
                    pageNow = 2;
                }
                int sizeNow = store.getPageSize(p.getUniqueId(), pageNow, 9);

                if (ct == ClickType.SHIFT_RIGHT){
                    if (sizeNow >= 54){
                        int nextPage = Math.max(2, pageNow + 1);
                        store.setPageSize(p.getUniqueId(), nextPage, 9);
                        String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
                        store.openPage(p, nextPage, title);
                        if (e.getCursor().getAmount() > 0) e.getCursor().setAmount(e.getCursor().getAmount()-1);
                        p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&a" + nextPage + "페이지가 &e9칸&a으로 생성되었습니다."));
                    } else {
                        int target = nextAllowed(sizeNow);
                        if (target > sizeNow){
                            store.setPageSize(p.getUniqueId(), pageNow, target);
                            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
                            store.openPage(p, pageNow, title);
                            if (e.getCursor().getAmount() > 0) e.getCursor().setAmount(e.getCursor().getAmount()-1);
                            p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&a페이지 크기: &e" + sizeNow + " &7→ &e" + target));
                        } else {
                            p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c더 이상 확장할 수 없습니다."));
                        }
                    }
                } else { // RIGHT (no shift)
                    if (sizeNow >= 54){
                        p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c이미 최대 크기(54칸)입니다. &f쉬프트+우클릭으로 다음 페이지를 여세요."));
                    } else {
                        int target = nextAllowed(sizeNow);
                        store.setPageSize(p.getUniqueId(), pageNow, target);
                        String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
                        store.openPage(p, pageNow, title);
                        if (e.getCursor().getAmount() > 0) e.getCursor().setAmount(e.getCursor().getAmount()-1);
                        p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&a페이지 크기: &e" + sizeNow + " &7→ &e" + target));
                    }
                }
            }
        }
    }

    // Bag item RIGHT / SHIFT+RIGHT handled in BackpackListener; keep this method only if needed for feature parity
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
        if (!open) return;

        Player p = e.getPlayer();
        int current = plugin.getStorage().getCurrentSize(p.getUniqueId());

        if (p.isSneaking()){ // SHIFT + Right Click
            e.setCancelled(true);
            if (current < MAX_MAIN_SIZE){
                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',"&c메인가방을 &e54&c칸까지 먼저 확장하세요."));
                return;
            }
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            store.openPage(p, 2, title);
            return;
        } else {
            e.setCancelled(true);
            plugin.getStorage().open(p);
        }
    }
}
