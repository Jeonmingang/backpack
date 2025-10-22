
package com.minkang.ultimate.backpack.pager;

import com.minkang.ultimate.backpack.BackpackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandInterceptor implements Listener {
    private final BackpackPlugin plugin;
    private final PageStore store;

    public CommandInterceptor(BackpackPlugin plugin, PageStore store){
        this.plugin = plugin; this.store = store;
    }

    private String c(String s){ return ChatColor.translateAlternateColorCodes('&', s); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCmd(PlayerCommandPreprocessEvent e){
        String msg = e.getMessage().trim();
        if (!msg.startsWith("/")) return;
        String raw = msg.substring(1);
        String lower = raw.toLowerCase();
        if (!lower.startsWith("가방")) return;

        String[] parts = raw.split("\\s+");
        Player p = e.getPlayer();

        // /가방 -> 우리 도움말로 대체
        if (parts.length == 1){
            e.setCancelled(true);
            help(p);
            return;
        }

        // /가방 열기 [페이지]
        if (parts.length >= 2 && parts[1].equalsIgnoreCase("열기")){
            e.setCancelled(true);
            int page = 1;
            if (parts.length >= 3){
                try { page = Integer.parseInt(parts[2]); } catch (Exception ignore) {}
            }
            if (page <= 1){
                p.performCommand("가방"); // 원본 1페이지
            } else {
                store.openPage(p, page, plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})"));
            }
            return;
        }

        // /가방 다음|이전
        if (parts.length == 2 && (parts[1].equalsIgnoreCase("다음") || parts[1].equalsIgnoreCase("이전"))){
            e.setCancelled(true);
            if (parts[1].equalsIgnoreCase("다음")) store.nextPage(p, plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})"));
            else store.prevPage(p, plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})"));
            return;
        }

        // /가방 설정 <닉> <페이지> <크기>
        if (parts.length >= 5 && parts[1].equalsIgnoreCase("설정")){
            if (!(p.isOp() || p.hasPermission("ultimate.backpack.admin"))){
                p.sendMessage(c("&c권한이 없습니다.")); e.setCancelled(true); return;
            }
            e.setCancelled(true);
            Player t = Bukkit.getPlayerExact(parts[2]);
            if (t == null){ p.sendMessage(c("&c해당 플레이어가 오프라인입니다.")); return; }
            int page, size;
            try{ page = Integer.parseInt(parts[3]); size = Integer.parseInt(parts[4]); }
            catch (Exception ex){ p.sendMessage(c("&c사용법: /가방 설정 <닉> <페이지> <9|18|27|36|45|54>")); return; }
            if (page <= 1){ p.sendMessage(c("&c1페이지는 기존 가방에서 설정하세요.")); return; }
            if (!(size==9||size==18||size==27||size==36||size==45||size==54)){
                p.sendMessage(c("&c허용 크기만 가능합니다.")); return;
            }
            store.setPageSize(t.getUniqueId(), page, size);
            p.sendMessage(c("&a설정됨: &f"+t.getName()+" &7P"+page+" → &e"+size));
            return;
        }
        // 그 외 서브커맨드는 원본으로 흘려보냄
    }

    private void help(Player p){
        p.sendMessage(c("&8&l┏━━━━━━━━━━━━━━━━━━━━┓"));
        p.sendMessage(c("&6&l  가방 도움말 &7(페이지 시스템 포함)"));
        p.sendMessage(c("&7  • &e/가방 &7- 1페이지 열기 (기존)"));
        p.sendMessage(c("&7  • &e/가방 열기 &f<페이지> &7- 해당 페이지 열기 (2페이지 이상)"));
        p.sendMessage(c("&7  • &e/가방 다음 &7/ &e/가방 이전 &7- Q/F 없이 명령으로 페이지 이동"));
        p.sendMessage(c("&7  • &eQ키&7(드랍) 다음 페이지  &8|  &eF키&7(보조손) 이전 페이지"));
        p.sendMessage(c("&7  • &e/가방 설정 &f<닉> <페이지> <9/18/27/36/45/54> &8- 관리자"));
        p.sendMessage(c("&8&l┗━━━━━━━━━━━━━━━━━━━━┛"));
    }
}
