package com.minkang.ultimate.backpack.listeners;

import com.minkang.ultimate.backpack.BackpackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BackpackListener implements Listener {

    private final BackpackPlugin plugin;

    public BackpackListener(BackpackPlugin plugin) {
        this.plugin = plugin;
    }

    private static String cc(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    private static String strip(String s) {
        return ChatColor.stripColor(s == null ? "" : s);
    }

    /** 가방 아이템 판단: config의 starter-item.display-name 과 동일한 이름이면 가방으로 간주 */
    private boolean isBagItem(ItemStack it) {
        if (it == null || it.getType() == Material.AIR) return false;
        ItemMeta m = it.getItemMeta();
        if (m == null || !m.hasDisplayName()) return false;
        String display = strip(m.getDisplayName());
        String cfgName = strip(cc(plugin.getConfig().getString("starter-item.display-name", "가방")));
        return !display.isEmpty() && display.equalsIgnoreCase(cfgName);
    }

    
    @EventHandler
    public void onUse(PlayerInteractEvent e){
        Action a = e.getAction();
        if (!(a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK)) return;
        if (e.getHand() != EquipmentSlot.HAND) return; // 메인핸드만 처리
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

        if (target == null) { p.sendMessage(cc("&c더 이상 확장할 수 없습니다.")); e.setCancelled(true); return; }
        if (target <= current) { p.sendMessage(cc("&c이미 해당 크기 이상입니다. 현재: &e" + current)); e.setCancelled(true); return; }

        plugin.getStorage().setCurrentSize(p.getUniqueId(), target);
        // 1장 소모
        it.setAmount(it.getAmount() - 1);
        p.sendMessage(cc("&a가방 크기가 &e" + current + " &7→ &e" + target + " &a로 확장되었습니다."));
        e.setCancelled(true);
    }
    
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        String plain = strip(title);
        if (plain == null || !plain.startsWith("[개인가방]")) return;

        // 위험 동작 차단
        ClickType ct = e.getClick();
        if (ct == ClickType.SWAP_OFFHAND || ct == ClickType.DROP || ct == ClickType.CONTROL_DROP || ct == ClickType.MIDDLE || ct == ClickType.DOUBLE_CLICK) {
            e.setCancelled(true);
            return;
        }

        Inventory top = e.getView().getTopInventory();
        Inventory bottom = e.getView().getBottomInventory();

        // 가방 아이템을 가방 내부로 넣는 상황만 차단
        if (e.getClickedInventory() == top) {
            ItemStack cursor = e.getCursor();
            if (cursor != null && isBagItem(cursor)) {
                e.setCancelled(true);
                return;
            }
        } else if (e.getClickedInventory() == bottom) {
            if (e.isShiftClick()) {
                ItemStack current = e.getCurrentItem();
                if (current != null && isBagItem(current)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        String plain = strip(title);
        if (plain == null || !plain.startsWith("[개인가방]")) return;

        ItemStack cursor = e.getOldCursor();
        if (cursor != null && isBagItem(cursor)) {
            e.setCancelled(true);
        }
    }


    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        String title = e.getView().getTitle();
        String plain = strip(title);
        if (plain == null || !plain.startsWith("[개인가방]")) return;

        Inventory inv = e.getInventory();
        ItemStack[] contents = inv.getContents();
        // 가방 아이템 자체는 저장하지 않기
        if (contents != null) {
            for (int i = 0; i < contents.length; i++) {
                ItemStack it = contents[i];
                if (it != null && isBagItem(it)) contents[i] = null;
            }
        }

        // sanitize 후 저장
        try {
            com.minkang.ultimate.backpack.storage.PersonalStorage storage = plugin.getStorage();
            if (storage.isOpen(p.getUniqueId())) {
                storage.saveAndClose(p);
            } else {
                // fallback: title-based check
                storage.saveContents(p.getUniqueId(), contents);
            }
        } catch (Throwable t) {
            // swallow to prevent dupes/loss from event errors, log only
            plugin.getLogger().warning("[UltimateBackpack] 저장 중 오류: " + t.getMessage());
        }
    }

}
