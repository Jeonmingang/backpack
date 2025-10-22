
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

    private boolean isAlias(String cmd){
        cmd = cmd.toLowerCase();
        return cmd.equals("가방") || cmd.equals("백팩") || cmd.equals("backpack") || cmd.equals("bag");
    }
    private Integer parseTargetPage(String[] parts){
        // patterns:
        // /가방 열기 3 /가방 open 3 /가방 page 3 /가방 페이지 3 /가방 3
        for (int i=1;i<parts.length;i++){
            String t = parts[i].toLowerCase();
            if (t.equals("열기") || t.equals("open") || t.equals("page") || t.equals("페이지")) continue;
            try { return Integer.parseInt(t); } catch(NumberFormatException ignored){}
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreprocess(PlayerCommandPreprocessEvent e){
        // Normalize whitespace
        String raw = e.getMessage().replaceAll("\\s+"," ").trim();
        if (!raw.startsWith("/")) return;
        String noSlash = raw.substring(1);
        String[] parts = noSlash.split(" ");
        if (parts.length<1) return;
        if (!isAlias(parts[0])) return;

        Integer page = parseTargetPage(parts);
        if (page == null) return;
        if (page <= 1) return;

        Player p = e.getPlayer();
        int size = compat.getPageSize(p.getUniqueId(), page, 0);
        if (size > 0){
            // Page exists; additionally require previous page to be 54
            int prev = compat.getPageSize(p.getUniqueId(), page-1, 9);
            if (prev < 54){
                e.setCancelled(true);
                p.sendMessage(cc("&c페이지 "+page+"을(를) 열 수 없습니다. &7이전 페이지("+ (page-1) +")를 54칸까지 확장하세요."));
            }
            return;
        }
        // page not created -> block
        e.setCancelled(true);
        int prev = compat.getPageSize(p.getUniqueId(), page-1, 9);
        if (prev < 54){
            p.sendMessage(cc("&c페이지 "+page+"은(는) 아직 잠겨 있습니다."));
            p.sendMessage(cc("&7먼저 페이지 "+(page-1)+"을 54칸으로 만든 뒤, &f티켓 쉬프트+우클릭&7으로 다음 페이지를 생성하세요."));
        } else {
            p.sendMessage(cc("&7먼저 &f티켓 쉬프트+우클릭&7으로 페이지 "+page+"을 9칸으로 생성하세요."));
        }
    }
}
