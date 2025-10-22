
package com.minkang.ultimate.backpack.listeners;

import com.minkang.ultimate.backpack.util.Compat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandGuardListener implements Listener {
    private final JavaPlugin plugin;
    private final Compat compat;
    public CommandGuardListener(JavaPlugin plugin){
        this.plugin = plugin;
        this.compat = new Compat(plugin);
    }

    private String cc(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e){
        String msg = e.getMessage().trim().toLowerCase();
        // patterns: /가방 열기 3  | /bag open 3 | /backpack open 3
        if (!(msg.startsWith("/가방 ") || msg.startsWith("/bag ") || msg.startsWith("/backpack ") || msg.startsWith("/백팩 "))) return;
        if (!(msg.contains(" 열기 ") || msg.contains(" open "))) return;

        String[] parts = e.getMessage().trim().split("\\s+");
        if (parts.length < 3) return;
        int page;
        try { page = Integer.parseInt(parts[2]); } catch (NumberFormatException ex){ return; }
        if (page <= 1) return; // allow main

        Player p = e.getPlayer();

        // Rule: page N can be opened only if page N exists (size > 0)
        int size = compat.getPageSize(p.getUniqueId(), page, 0);
        if (size <= 0){
            // Additionally enforce sequential rule: previous page must be 54
            int prev = compat.getPageSize(p.getUniqueId(), page-1, 9);
            if (prev < 54){
                e.setCancelled(true);
                p.sendMessage(cc("&c페이지 "+page+"은(는) 아직 잠겨 있습니다."));
                p.sendMessage(cc("&7먼저 페이지 "+(page-1)+"을 54칸까지 확장한 뒤, &f티켓 쉬프트+우클릭&7으로 다음 페이지를 생성하세요."));
            }
        }
    }
}
