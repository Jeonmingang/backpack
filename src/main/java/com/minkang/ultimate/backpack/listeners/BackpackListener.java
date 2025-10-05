package com.minkang.ultimate.backpack.listeners;




import com.minkang.ultimate.backpack.BackpackPlugin;

import com.minkang.ultimate.backpack.storage.PersonalStorage;

import com.minkang.ultimate.backpack.util.ItemSanitizer;

import com.minkang.ultimate.backpack.util.ItemUtil;

import java.util.List;


import org.bukkit.ChatColor;
import org.bukkit.ChatColor;

import org.bukkit.Material;

import org.bukkit.Sound;

import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.entity.Player;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventHandler;

import org.bukkit.event.Listener;
import org.bukkit.event.Listener;

import org.bukkit.event.block.Action;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.ClickType;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import org.bukkit.event.inventory.InventoryCloseEvent;

import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import org.bukkit.event.player.PlayerDropItemEvent;

import org.bukkit.event.player.PlayerInteractEvent;

import org.bukkit.event.player.PlayerJoinEvent;

import org.bukkit.inventory.EquipmentSlot;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.Inventory;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemStack;

import org.bukkit.inventory.ItemStack;



public class BackpackListener implements Listener {

boolean isBagItem(ItemStack it) {
        if (ItemUtil.hasTag(it, plugin.getKeyBag(), "1")) return true;
        FileConfiguration cfg = plugin.getConfig();
        if (cfg.getBoolean("starter-item.match-by-name", false)) {
            String dn = (it != null && it.hasItemMeta() && it.getItemMeta().hasDisplayName()) ? it.getItemMeta().getDisplayName() : null;
            String target = c(cfg.getString("starter-item.display-name", "&6가방"));
            if (dn != null && dn.equals(target)) return true;
        }
        

String c(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    


@EventHandler
public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player)) return;
    Player p = (Player) e.getWhoClicked();
    String title = e.getView().getTitle();
    String plain = ChatColor.stripColor(title);
    if (plain == null || !plain.startsWith("[개인가방]")) return;

    ClickType ct = e.getClick();
    if (ct == ClickType.SWAP_OFFHAND || ct == ClickType.DROP || ct == ClickType.CONTROL_DROP || ct == ClickType.MIDDLE || ct == ClickType.DOUBLE_CLICK) {
        e.setCancelled(true);
        return;
    }

    Inventory top = e.getView().getTopInventory();
    Inventory bottom = e.getView().getBottomInventory();

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
    String plain = ChatColor.stripColor(title);
    if (plain == null || !plain.startsWith("[개인가방]")) return;

    ItemStack cursor = e.getOldCursor();
    if (cursor != null && isBagItem(cursor)) {
        e.setCancelled(true);
    }
}

}
