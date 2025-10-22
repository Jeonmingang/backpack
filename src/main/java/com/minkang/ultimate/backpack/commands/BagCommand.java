package com.minkang.ultimate.backpack.commands;

import com.minkang.ultimate.backpack.BackpackPlugin;
import com.minkang.ultimate.backpack.pager.PageStore;
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

import java.util.*;

public class BagCommand implements CommandExecutor {

    private final BackpackPlugin plugin;
    public BagCommand(BackpackPlugin plugin){ this.plugin = plugin; }

    private static String c(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }

    private void help(CommandSender s) {
        // 기본 도움말
        s.sendMessage(c("&e/가방 &7- 1페이지(메인가방) 열기"));
        s.sendMessage(c("&e/가방 열기 &f<페이지> &7- 2~n 페이지 열기"));
        s.sendMessage(c("&e/가방 다음 &7/ &e/가방 이전 &7- 페이지 이동 (GUI에서도 Q/F)"));
        s.sendMessage(c("&e/가방 정보 [닉] &7- 가방 크기 확인"));
        // 관리자
        s.sendMessage(c("&e/가방 지급아이템 <닉> [수량] &7- 가방 아이템 지급"));
        s.sendMessage(c("&e/가방 지급확장권 <닉> [수량] [크기] &7- 확장권 지급(크기 생략 시 다음 단계)"));
        s.sendMessage(c("&e/가방 크기 <닉> <페이지> <9/18/27/36/45/54> &7- 페이지 크기 설정(1=메인)"));
        s.sendMessage(c("&e/가방 리로드 &7- 설정 리로드"));
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
            // 메인 가방 오픈 (페이지 인자 없으면 1페이지)
            if (args.length == 1){
                plugin.getStorage().open((Player)sender);
            } else {
                int page;
                try { page = Integer.parseInt(args[1]); } catch (NumberFormatException ex) { sender.sendMessage(c("&c페이지는 숫자여야 합니다.")); return true; }
                if (page <= 1){
                    plugin.getStorage().open((Player)sender);
                } else {
                    String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
                    plugin.getPagerStore().openPage((Player)sender, Math.max(2, page), title);
                }
            }
            return true;
        }

        if (sub.equalsIgnoreCase("정보")){
            Player t;
            if (args.length >= 2){
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
            String matStr = cfg.getString("bag-item.material", "PAPER");
            Material mat;
            try { mat = Material.valueOf(matStr); } catch (IllegalArgumentException ex) { mat = Material.PAPER; }
            String name = cfg.getString("bag-item.display-name", "&6가방 전용 아이템");
            List<String> lore = new ArrayList<>(cfg.getStringList("bag-item.lore"));
            ItemStack it = new ItemStack(mat);
            ItemMeta im = it.getItemMeta();
            if (im != null){
                im.setDisplayName(c(name));
                if (!lore.isEmpty()){
                    List<String> pretty = new ArrayList<>();
                    for (String ln : lore) pretty.add(c(ln));
                    im.setLore(pretty);
                }
                // Flag as bag item
                im.getPersistentDataContainer().set(plugin.getKeyBagFlag(), org.bukkit.persistence.PersistentDataType.BYTE, (byte)1);
                it.setItemMeta(im);
            }
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
            Integer forcedSize = null;
            if (args.length >= 4){
                try { forcedSize = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) {}
                if (forcedSize != null && !ALLOWED.contains(forcedSize)){
                    sender.sendMessage(c("&c크기는 9/18/27/36/45/54 중 하나여야 합니다.")); return true;
                }
            }
            FileConfiguration cfg = plugin.getConfig();
            String matStr = cfg.getString("ticket.material", "PAPER");
            Material mat;
            try { mat = Material.valueOf(matStr); } catch (IllegalArgumentException ex) { mat = Material.PAPER; }
            String name = cfg.getString("ticket.display-name", "&6가방 확장권");
            List<String> lore = new ArrayList<>(cfg.getStringList("ticket.lore"));
            // 중복 제거하며 extra-lore 병합
            List<String> extra = cfg.getStringList("ticket-extra-lore");
            LinkedHashSet<String> merged = new LinkedHashSet<>();
            for (String ln : lore) merged.add(c(ln));
            for (String ln : extra) merged.add(c(ln));
            ItemStack it = new ItemStack(mat);
            ItemMeta im = it.getItemMeta();
            if (im != null){
                im.setDisplayName(c(name));
                if (!merged.isEmpty()) im.setLore(new ArrayList<>(merged));
                String tag = (forcedSize == null) ? "next" : "size:" + forcedSize;
                im.getPersistentDataContainer().set(plugin.getKeyTicket(), org.bukkit.persistence.PersistentDataType.STRING, tag);
                it.setItemMeta(im);
            }
            it.setAmount(amount);
            t.getInventory().addItem(it);
            sender.sendMessage(c("&a확장권 지급: &7" + t.getName() + " &7(" + (forcedSize==null?"다음 단계":forcedSize) + ") x" + amount));
            return true;
        }

        if (sub.equalsIgnoreCase("아이템")){
            if (!(sender.isOp() || sender.hasPermission("ultimatebackpack.admin"))) { sender.sendMessage(c("&c권한이 없습니다.")); return true; }
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용 가능합니다."); return true; }
            Player p = (Player) sender;
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR){ sender.sendMessage(c("&c손에 든 아이템이 없습니다.")); return true; }
            ItemMeta meta = hand.getItemMeta();
            if (meta == null){ sender.sendMessage(c("&c이 아이템은 표시 이름을 설정할 수 없습니다.")); return true; }
            // 전용 가방 아이템 플래그 부여
            meta.getPersistentDataContainer().set(plugin.getKeyBagFlag(), org.bukkit.persistence.PersistentDataType.BYTE, (byte)1);
            // 이름/로어 정리
            String baseName = meta.hasDisplayName() ? meta.getDisplayName() : ItemUtil.prettifyMaterial(hand.getType());
            String finalName = c("&6" + ChatColor.stripColor(baseName) + " &7- 전용 가방 아이템");
            meta.setDisplayName(finalName);
            hand.setItemMeta(meta);
            sender.sendMessage(c("&a전용 가방 아이템으로 설정 완료."));
            return true;
        }

        if (sub.equalsIgnoreCase("크기")){
            if (!(sender.isOp() || sender.hasPermission("ultimatebackpack.admin"))) { sender.sendMessage(c("&c권한이 없습니다.")); return true; }
            if (args.length < 4){ sender.sendMessage(c("&c사용법: /가방 크기 <닉> <페이지> <9/18/27/36/45/54>  &7(페이지 1 = 메인가방)")); return true; }
            Player t = Bukkit.getPlayerExact(args[1]);
            if (t == null){ sender.sendMessage(c("&c해당 플레이어를 찾을 수 없습니다.")); return true; }
            int page;
            int size;
            try { page = Integer.parseInt(args[2]); } catch (NumberFormatException e){ sender.sendMessage(c("&c페이지는 숫자여야 합니다.")); return true; }
            try { size = Integer.parseInt(args[3]); } catch (NumberFormatException e){ sender.sendMessage(c("&c크기는 숫자여야 합니다.")); return true; }
            if (!ALLOWED.contains(size)){ sender.sendMessage(c("&c크기는 9/18/27/36/45/54 중 하나여야 합니다.")); return true; }
            if (page <= 1){
                plugin.getStorage().setCurrentSize(t.getUniqueId(), size);
            } else {
                plugin.getPagerStore().setPageSize(t.getUniqueId(), page, size);
            }
            sender.sendMessage(c("&a설정 완료: &f" + t.getName() + " &7페이지 &e" + page + " &7→ &e" + size));
            return true;
        }

        // 그 외
        help(sender);
        return true;
    }
}