package com.minkang.ultimate.backpack.listeners;

import com.minkang.ultimate.backpack.BackpackPlugin;
import com.minkang.ultimate.backpack.storage.PersonalStorage;
import com.minkang.ultimate.backpack.util.ItemUtil;
import com.minkang.ultimate.backpack.util.ItemSanitizer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BackpackListener implements Listener {
    private final BackpackPlugin plugin;
    public BackpackListener(BackpackPlugin plugin) { this.plugin = plugin; }
    private String c(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    private boolean isBagItem(ItemStack it) {
        if (ItemUtil.hasTag(it, plugin.getKeyBag(), "1")) return true;
        FileConfiguration cfg = plugin.getConfig();
        if (cfg.getBoolean("starter-item.match-by-name", false)) {
            String dn = (it != null && it.hasItemMeta() && it.getItemMeta().hasDisplayName()) ? it.getItemMeta().getDisplayName() : null;
            String target = c(cfg.getString("starter-item.display-name", "&6가방"));
            if (dn != null && dn.equals(target)) return true;
        }
        return false;
    }
    private boolean isTicket(ItemStack it) {
        if (ItemUtil.hasTag(it, plugin.getKeyTicket(), "1")) return true;
        if (it == null || !it.hasItemMeta()) return false;
        String dn = it.getItemMeta().getDisplayName();
        String t = c(plugin.getConfig().getString("ticket.display-name", "&d가방 확장권"));
        return dn != null && dn.equals(t);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("starter-item.give-on-first-join", false)) return;
        PersonalStorage ps = plugin.getStorage();
        if (ps.isStarterGiven(p.getUniqueId())) return;
        String matStr = cfg.getString("starter-item.material", "CHEST");
        Material mat; try { mat = Material.valueOf(matStr); } catch (IllegalArgumentException ex) { mat = Material.CHEST; }
        String name = cfg.getString("starter-item.display-name", "&6가방");
        List<String> lore = cfg.getStringList("starter-item.lore");
        ItemStack it = ItemUtil.buildTaggedItem(mat, name, lore, plugin.getKeyBag(), "1");
        p.getInventory().addItem(it);
        ps.markStarterGiven(p.getUniqueId());
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Action a = e.getAction();
        boolean right = (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK);
        if (!right) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) return;

        if (isTicket(hand)) {
            PersonalStorage ps = plugin.getStorage();
            int cur = ps.getCurrentSize(p.getUniqueId());
            Integer next = ps.nextSize(cur);
            if (next == null) { p.sendMessage(c("&c이미 최대 크기입니다.")); return; }
            ps.setCurrentSize(p.getUniqueId(), next);
            p.sendMessage(c("&a가방 크기가 &e" + cur + " &7→ &e" + next + " &a로 확장되었습니다!"));
            int amt = hand.getAmount();
            if (amt <= 1) p.getInventory().setItemInMainHand(null);
            else { hand.setAmount(amt - 1); p.getInventory().setItemInMainHand(hand); }
            return;
        }

        if (isBagItem(hand)) {
            if (plugin.getConfig().getStringList("backpack.blocked-worlds").contains(p.getWorld().getName())) {
                p.sendMessage(c("&c이 월드에서는 가방을 열 수 없습니다."));
                return;
            }
            plugin.getStorage().open(p);
            try {
                String s = plugin.getConfig().getString("backpack.open-sound", "BLOCK_CHEST_OPEN");
                Sound sound = Sound.valueOf(s);
                p.playSound(p.getLocation(), sound, 0.8f, 1.0f);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @EventHandler
public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player)) return;
    Player p = (Player) e.getWhoClicked();
    String title = e.getView().getTitle();
    String plain = ChatColor.stripColor(title);
    if (plain == null || !plain.startsWith("[개인가방]")) return;

    // 위험한 동작만 차단
    if (e.getClick() == ClickType.SWAP_OFFHAND || 
        e.getClick() == ClickType.DROP || e.getClick() == ClickType.CONTROL_DROP || 
        e.getClick() == ClickType.MIDDLE || e.getClick() == ClickType.DOUBLE_CLICK) {
        e.setCancelled(true);
        return;
    }

    Inventory top = e.getView().getTopInventory();
    Inventory bottom = e.getView().getBottomInventory();

    // 가방 아이템이 가방 내부로 들어가는 것을 차단
    if (e.getClickedInventory() == top) {
        ItemStack cursor = e.getCursor();
        if (cursor != null && isBagItem(cursor)) {
            e.setCancelled(true);
            p.sendMessage(c("&c가방 안에 가방을 넣을 수 없습니다."));
            return;
        }
    } else if (e.getClickedInventory() == bottom) {
        if (e.isShiftClick()) {
            ItemStack current = e.getCurrentItem();
            if (current != null && isBagItem(current)) {
                e.setCancelled(true);
                p.sendMessage(c("&c가방 안에 가방을 넣을 수 없습니다."));
                return;
            }
        }
    }
}

public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getConfig().getBoolean("backpack.prevent-drop-while-open", true)) return;
        if (!plugin.getStorage().isOpen(p.getUniqueId())) return;
        ItemStack dropped = e.getItemDrop().getItemStack();
        if (isBagItem(dropped)) {
            e.setCancelled(true);
            p.sendMessage(c("&c가방을 연 상태에서는 가방 아이템을 버릴 수 없습니다."));
        }
    }
}


@EventHandler
public void onDrag(InventoryDragEvent e) {
    if (!(e.getWhoClicked() instanceof Player)) return;
    Player p = (Player) e.getWhoClicked();
    String title = e.getView().getTitle();
    String plain = ChatColor.stripColor(title);
    if (plain == null || !plain.startsWith("[개인가방]")) return;

    ItemStack cursor = e.getOldCursor();
    if (cursor != null && isBagItem(cursor)) {
        e.setCancelled(true);
        p.sendMessage(c("&c가방 안에 가방을 넣을 수 없습니다."));
    }
}
