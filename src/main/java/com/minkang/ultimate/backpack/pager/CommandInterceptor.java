package com.minkang.ultimate.backpack.pager;

import com.minkang.ultimate.backpack.BackpackPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandInterceptor implements Listener {

    private static final int MAX_MAIN_SIZE = 54;

    private final BackpackPlugin plugin;
    private final PageStore store;

    public CommandInterceptor(BackpackPlugin plugin, PageStore store){
        this.plugin = plugin;
        this.store = store;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCmd(PlayerCommandPreprocessEvent e){
        String msg = e.getMessage().trim();
        if (!msg.startsWith("/")) return;

        String[] parts = msg.substring(1).split("\\s+");
        if (parts.length == 0) return;

        String base = parts[0];
        if (!(base.equalsIgnoreCase("가방") || base.equalsIgnoreCase("backpack") || base.equalsIgnoreCase("bag") || base.equalsIgnoreCase("백팩"))) return;

        if (parts.length == 1) return; // executor handles

        String sub = parts[1];
        Player p = e.getPlayer();
        int current = plugin.getStorage().getCurrentSize(p.getUniqueId());

        if (sub.equalsIgnoreCase("열기") && parts.length >= 3){
            int page;
            try { page = Integer.parseInt(parts[2]); } catch (NumberFormatException ex) { return; }
            if (page <= 1) return;
            if (current < MAX_MAIN_SIZE){
                e.setCancelled(true);
                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c메인가방을 &e54&c칸까지 먼저 확장하세요."));
                return;
            }
            e.setCancelled(true);
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            store.openPage(p, Math.max(2, page), title);
            return;
        }

        if (sub.equalsIgnoreCase("다음")){
            if (current < MAX_MAIN_SIZE){
                e.setCancelled(true);
                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c메인가방을 &e54&c칸까지 먼저 확장하세요."));
                return;
            }
            e.setCancelled(true);
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            store.nextPage(p, title);
            return;
        }

        if (sub.equalsIgnoreCase("이전")){
            if (current < MAX_MAIN_SIZE){
                e.setCancelled(true);
                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c메인가방을 &e54&c칸까지 먼저 확장하세요."));
                return;
            }
            e.setCancelled(true);
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            store.prevPage(p, title);
        }
    }
}
