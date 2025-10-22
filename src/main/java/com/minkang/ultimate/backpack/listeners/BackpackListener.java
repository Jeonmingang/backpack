
package com.minkang.ultimate.backpack.listeners;

import com.minkang.ultimate.backpack.BackpackPlugin;
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

import java.util.Objects;
import java.util.Set;

public class BackpackListener implements Listener {

    private static final int MAX_MAIN_SIZE = 54;

    private final BackpackPlugin plugin;
    public BackpackListener(BackpackPlugin plugin){ this.plugin = plugin; }

    private String cc(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }

    private boolean isTicket(ItemStack it){
        if (it == null || it.getType() == Material.AIR) return false;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String tag = pdc.get(plugin.getKeyTicket(), PersistentDataType.STRING);
        if (tag != null) return true;

        // Fallback: allow matching by configured PDC keys (optional)
        FileConfiguration cfg = plugin.getConfig();
        for (String k : cfg.getStringList("ticket.pdc-keys")){
            if (k == null || k.trim().isEmpty()) continue;
            String keyL = k.toLowerCase();
            for (NamespacedKey nk : pdc.getKeys()){
                String full = (nk.getNamespace()+":"+nk.getKey()).toLowerCase();
                if (full.contains(keyL) || nk.getKey().equalsIgnoreCase(k) || nk.getNamespace().equalsIgnoreCase(k)){
                    return true;
                }
            }
        }
        return false;
    }

    private void consumeOne(ItemStack it){
        if (it == null) return;
        if (it.getAmount() > 1) it.setAmount(it.getAmount()-1);
        else it.setType(Material.AIR);
    }

    @EventHandler(ignoreCancelled = true)
    public void onUseTicketInHand(PlayerInteractEvent e){
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        // Prevent offhand double-fire creating multiple pages
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player p = e.getPlayer();
        ItemStack inHand = p.getInventory().getItemInMainHand();
        if (!isTicket(inHand)) return;

        int current = plugin.getStorage().getCurrentSize(p.getUniqueId());
        // If sneaking and main bag is already 54, create/open ONLY page 2 with 9 slots.
        if (p.isSneaking() && current >= MAX_MAIN_SIZE){
            consumeOne(inHand);
            int sizeP2 = plugin.getPagerStore().getPageSize(p.getUniqueId(), 2, 0);
            if (sizeP2 <= 0) {
                plugin.getPagerStore().setPageSize(p.getUniqueId(), 2, 9);
                p.sendMessage(cc("&a2페이지가 &e9칸&a으로 생성되었습니다."));
            } else {
                p.sendMessage(cc("&a2페이지를 엽니다."));
            }
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            plugin.getPagerStore().openPage(p, 2, title);
            e.setCancelled(true);
            return;
        }

        // Non-sneak: expand MAIN bag to the next allowed size up to 54
        String tag = null;
        ItemMeta meta = inHand.getItemMeta();
        if (meta != null){
            tag = meta.getPersistentDataContainer().get(plugin.getKeyTicket(), PersistentDataType.STRING);
        }

        Integer target = null;
        if (tag != null && tag.startsWith("size:")){
            try { target = Integer.parseInt(tag.substring(5)); } catch (NumberFormatException ignored) {}
            if (target != null) target = plugin.getStorage().nearestAllowed(target);
        } else {
            target = plugin.getStorage().nextSize(current);
        }

        if (target == null || target <= current){
            p.sendMessage(cc("&c더 이상 확장할 수 없습니다. (현재: &e"+current+"&c칸)"));
            if (current >= MAX_MAIN_SIZE) p.sendMessage(cc("&7쉬프트+우클릭으로 &f2페이지 9칸&7을 여세요."));
            e.setCancelled(true);
            return;
        }

        plugin.getStorage().setCurrentSize(p.getUniqueId(), target);
        consumeOne(inHand);
        p.sendMessage(cc("&a메인가방 크기: &e"+current+" &7→ &e"+target));
        e.setCancelled(true);
    }
}
