package com.minkang.ultimate.backpack.pager;

import com.minkang.ultimate.backpack.BackpackPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandInterceptor implements Listener {
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

        // Base command match (allow aliases from plugin.yml)
        String base = parts[0];
        if (!(base.equalsIgnoreCase("가방") || base.equalsIgnoreCase("backpack") || base.equalsIgnoreCase("bag") || base.equalsIgnoreCase("백팩"))){
            return;
        }

        // Let the original command (/가방) handle help and admin cmds
        if (parts.length == 1) return;

        String sub = parts[1];
        Player p = e.getPlayer();

        // /가방 열기 <페이지>
        if (sub.equalsIgnoreCase("열기") && parts.length >= 3){
            int page;
            try { page = Integer.parseInt(parts[2]); }
            catch (NumberFormatException ex) { return; } // not a number -> let original command handle
            e.setCancelled(true);
            String title = plugin.getConfig().getString("backpack.title-format", "&6[개인가방] &7({page})");
            store.openPage(p, Math.max(2, page), title);
            return;
        }

        // /가방 다음
        if (sub.equalsIgnoreCase("다음")){
            e.setCancelled(true);
            String title = plugin.getConfig().getString("backpack.title-format", "&6[개인가방] &7({page})");
            store.nextPage(p, title);
            return;
        }

        // /가방 이전
        if (sub.equalsIgnoreCase("이전")){
            e.setCancelled(true);
            String title = plugin.getConfig().getString("backpack.title-format", "&6[개인가방] &7({page})");
            store.prevPage(p, title);
            return;
        }

        // Any other subcommand -> do NOT consume; keep original behavior
    }
}