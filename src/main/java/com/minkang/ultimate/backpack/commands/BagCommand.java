
package com.minkang.ultimate.backpack.commands;

import com.minkang.ultimate.backpack.BackpackPlugin;
import com.minkang.ultimate.backpack.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BagCommand implements CommandExecutor {
    private final BackpackPlugin plugin;
    public BagCommand(BackpackPlugin plugin){ this.plugin = plugin; }

    private static String c(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }

    private void help(CommandSender s){
        s.sendMessage(c("&e/가방 열기 &7- (허용 시) 개인가방 열기"));
        s.sendMessage(c("&e/가방 정보 [닉] &7- 가방 크기 확인"));
        s.sendMessage(c("&e/가방 지급아이템 <닉> [수량] &7- 가방 아이템 지급"));
        s.sendMessage(c("&e/가방 지급확장권 <닉> [수량] [크기] &7- 확장권 지급(우클릭 시 확장, 크기 생략 시 다음 단계)"));
        s.sendMessage(c("&e/가방 크기 <닉> <9|18|27|36|45|54> &7- 크기 설정"));
        s.sendMessage(c("&e/가방 설정 [닉] &7- 손에 든 아이템을 가방으로 지정"));
        s.sendMessage(c("&e/가방 리로드 &7- 설정 리로드"));
    
    sender.sendMessage(c("&8&l┏━━━━━━━━━━━━━━━━━━━━┓"));
    sender.sendMessage(c("&6&l  가방 도움말 &7(페이지 시스템)"));
    sender.sendMessage(c("&7  • &e/가방 &7- 1페이지 열기"));
    sender.sendMessage(c("&7  • &e/가방 열기 &f<페이지> &7- 2~n 페이지 열기"));
    sender.sendMessage(c("&7  • &e/가방 다음 &7/ &e/가방 이전 &7- 페이지 이동"));
    sender.sendMessage(c("&7  • &eQ키&7(드랍) 다음  &8|  &eF키&7(보조손) 이전"));
    sender.sendMessage(c("&7  • &e/가방 설정 &f<닉> <페이지> <9/18/27/36/45/54> &8- 관리자"));
    sender.sendMessage(c("&8&l┗━━━━━━━━━━━━━━━━━━━━┛"));
}


    private static final List<Integer> ALLOWED = Arrays.asList(9,18,27,36,45,54);

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0){ help(sender); return true; }

        String sub = args[0];

        if (sub.equalsIgnoreCase("리로드")){
            if (!(sender.isOp() || sender.hasPermission("ultimatebackpack.admin"))) { sender.sendMessage(c("&c권한이 없습니다.")); return true; }
            plugin.reloadConfig();
            sender.sendMessage(c("&a설정을 리로드했습니다."));
            return true;
        }

        if (sub.equalsIgnoreCase("열기")){
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용 가능합니다."); return true; }
            if (!plugin.getConfig().getBoolean("backpack.allow-command-open", true)) {
                sender.sendMessage(c("&c명령으로 열 수 없습니다. 가방 아이템을 사용하세요.")); return true;
            }
            plugin.getStorage().open((Player)sender);
            return true;
        }

        if (sub.equalsIgnoreCase("정보")){
            Player t;
            if (args.length >= 2){
                if (!(sender.isOp() || sender.hasPermission("ultimatebackpack.admin"))) { sender.sendMessage(c("&c권한이 없습니다.")); return true; }
                t = Bukkit.getPlayerExact(args[1]);
                if (t == null){ sender.sendMessage(c("&c해당 플레이어를 찾을 수 없습니다.")); return true; }
            } else {
                if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용 가능합니다."); return true; }
                t = (Player)sender;
            }
            int size = plugin.getStorage().getCurrentSize(t.getUniqueId());
            Integer next = plugin.getStorage().nextSize(size);
            sender.sendMessage(c("&a" + t.getName() + " &7가방 크기: &e" + size + " &7| 다음 단계: &e" + (next==null?"없음(최대)":next)));
            return true;
        }

        if (sub.equalsIgnoreCase("지급아이템")){
            if (!(sender.isOp() || sender.hasPermission("ultimatebackpack.admin"))) { sender.sendMessage(c("&c권한이 없습니다.")); return true; }
            if (args.length < 2){ sender.sendMessage(c("&c사용법: /가방 지급아이템 <닉> [수량]")); return true; }
            Player t = Bukkit.getPlayerExact(args[1]);
            if (t == null){ sender.sendMessage(c("&c해당 플레이어를 찾을 수 없습니다.")); return true; }
            int amount = 1;
            if (args.length >= 3){
                try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                if (amount < 1) amount = 1;
            }
            FileConfiguration cfg = plugin.getConfig();
            String matStr = cfg.getString("starter-item.material", "CHEST");
            Material mat;
            try { mat = Material.valueOf(matStr); } catch (IllegalArgumentException ex) { mat = Material.CHEST; }
            String name = cfg.getString("starter-item.display-name", "&6가방");
            List<String> lore = cfg.getStringList("starter-item.lore");
            ItemStack it = com.minkang.ultimate.backpack.util.ItemUtil.buildTaggedItem(mat, name, lore, plugin.getKeyBag(), "1");
            it.setAmount(amount);
            t.getInventory().addItem(it);
            sender.sendMessage(c("&a가방 아이템 지급: &7" + t.getName() + " x" + amount));
            return true;
        }

        if (sub.equalsIgnoreCase("지급확장권")){
            if (!(sender.isOp() || sender.hasPermission("ultimatebackpack.admin"))) { sender.sendMessage(c("&c권한이 없습니다.")); return true; }
            if (args.length < 2){ sender.sendMessage(c("&c사용법: /가방 지급확장권 <닉> [수량] [크기]")); return true; }
            Player t = Bukkit.getPlayerExact(args[1]);
            if (t == null){ sender.sendMessage(c("&c해당 플레이어를 찾을 수 없습니다.")); return true; }
            int amount = 1;
            if (args.length >= 3){
                try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                if (amount < 1) amount = 1;
            }
            // optional size
            String sizeTag = "next";
            if (args.length >= 4){
                try {
                    int req = Integer.parseInt(args[3]);
                    if (ALLOWED.contains(req)) sizeTag = "size:" + req;
                } catch (NumberFormatException ignored) {}
            }
            FileConfiguration cfg = plugin.getConfig();
            String matStr = cfg.getString("ticket.material", "PAPER");
            Material mat;
            try { mat = Material.valueOf(matStr); } catch (IllegalArgumentException ex) { mat = Material.PAPER; }
            String name = cfg.getString("ticket.display-name", "&d가방 확장권");
            List<String> lore = new ArrayList<>(cfg.getStringList("ticket.lore"));
            // 안내 한 줄 추가(선택)
            lore.add(c("&7지정 크기: &f" + (sizeTag.startsWith("size:") ? sizeTag.substring(5) : "다음 단계")));
            ItemStack it = com.minkang.ultimate.backpack.util.ItemUtil.buildTaggedItem(mat, name, lore, plugin.getKeyTicket(), sizeTag);
            it.setAmount(amount);
            t.getInventory().addItem(it);
            sender.sendMessage(c("&a확장권 지급: &7" + t.getName() + " x" + amount + " &8(" + (sizeTag.startsWith("size:")?sizeTag.substring(5):"다음 단계") + ")"));
            return true;
        }

        if (sub.equalsIgnoreCase("크기")){
            if (!(sender.isOp() || sender.hasPermission("ultimatebackpack.admin"))) { sender.sendMessage(c("&c권한이 없습니다.")); return true; }
            if (args.length < 3){ sender.sendMessage(c("&c사용법: /가방 크기 <닉> <9|18|27|36|45|54>")); return true; }
            Player t = Bukkit.getPlayerExact(args[1]);
            if (t == null){ sender.sendMessage(c("&c해당 플레이어를 찾을 수 없습니다.")); return true; }
            int req;
            try { req = Integer.parseInt(args[2]); } catch (NumberFormatException ex) { sender.sendMessage(c("&c숫자를 입력하세요.")); return true; }
            if (!ALLOWED.contains(req)){ sender.sendMessage(c("&c허용 크기만 설정 가능합니다: " + ALLOWED)); return true; }
            plugin.getStorage().setCurrentSize(t.getUniqueId(), req);
            sender.sendMessage(c("&a설정 완료: &7" + t.getName() + " → &e" + req));
            return true;
        }

        if (sub.equalsIgnoreCase("설정")){
            Player p;
            if (args.length >= 2){
                if (!(sender.isOp() || sender.hasPermission("ultimatebackpack.admin"))) { sender.sendMessage(c("&c권한이 없습니다.")); return true; }
                p = Bukkit.getPlayerExact(args[1]);
                if (p == null){ sender.sendMessage(c("&c해당 플레이어를 찾을 수 없습니다.")); return true; }
            } else {
                if (!(sender instanceof Player)){ sender.sendMessage("플레이어만 사용 가능합니다."); return true; }
                p = (Player)sender;
            }
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR){ sender.sendMessage(c("&c손에 아이템이 없습니다.")); return true; }
            ItemMeta meta = hand.getItemMeta();
            if (meta == null){ sender.sendMessage(c("&c이 아이템은 표시 이름을 설정할 수 없습니다.")); return true; }
            String nick = (args.length >= 2) ? args[1] : p.getName();
            String baseName = meta.hasDisplayName() ? meta.getDisplayName() : ItemUtil.prettifyMaterial(hand.getType());
            String finalName = c("&6" + baseName + " &7( &f" + nick + " &7)&f 의 가방");
            meta.setDisplayName(finalName);
            hand.setItemMeta(meta);
            sender.sendMessage(c("&a손에 든 아이템을 가방으로 지정했습니다: &f" + ChatColor.stripColor(finalName)));
            return true;
        }

        help(sender);
        return true;
    }
}
