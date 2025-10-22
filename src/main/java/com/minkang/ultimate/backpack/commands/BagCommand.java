package com.minkang.ultimate.backpack.commands;

import com.minkang.ultimate.backpack.BackpackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class BagCommand implements CommandExecutor {
    private org.bukkit.inventory.ItemStack makeBagItem(int amount){
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        String matStr = cfg.getString("bag-item.material", "CHEST");
        org.bukkit.Material mat;
        try { mat = org.bukkit.Material.valueOf(matStr); } catch (IllegalArgumentException ex) { mat = org.bukkit.Material.CHEST; }
        String name = cfg.getString("bag-item.display-name", "&6휴대용 가방");
        java.util.List<String> lore = cfg.getStringList("bag-item.lore");
        if (lore == null || lore.isEmpty()){
            lore = java.util.Arrays.asList(
                "&7우클릭: &f메인가방 열기",
                "&7쉬프트+우클릭: &f(메인 54칸 달성 시) 2페이지 열기",
                "&7확장권: &f가방 GUI에서 우클릭으로 확장, 쉬프트+우클릭으로 다음 페이지 생성"
            );
        }
        org.bukkit.inventory.ItemStack it = new org.bukkit.inventory.ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta im = it.getItemMeta();
        if (im != null){
            im.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));
            java.util.List<String> colored = new java.util.ArrayList<>();
            for (String ln : lore) colored.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', ln));
            im.setLore(colored);
            im.getPersistentDataContainer().set(plugin.getKeyBagFlag(), org.bukkit.persistence.PersistentDataType.BYTE, (byte)1);
            it.setItemMeta(im);
        }
        it.setAmount(Math.max(1, amount));
        return it;
    }
    

    private static final int MAX_MAIN_SIZE = 54;
    private static final List<Integer> ALLOWED = Arrays.asList(9,18,27,36,45,54);

    private final BackpackPlugin plugin;
    public BagCommand(BackpackPlugin plugin){ this.plugin = plugin; }

    private static String c(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }

    private void help(CommandSender s) {
        s.sendMessage(c("&e/가방 &7- 메인가방 열기"));
        s.sendMessage(c("&e/가방 열기 &f<페이지> &7- 2~n 페이지 열기 (&c메인가방 54칸 이후 가능&)"));
        s.sendMessage(c("&e/가방 다음 &7/ &e/가방 이전 &7- 페이지 이동 (GUI에서도 Q/F, &c54칸 이후&)"));
        s.sendMessage(c("&e/가방 정보 [닉] &7- 가방 크기 확인"));
        // 관리자
        s.sendMessage(c("&e/가방 지급확장권 <닉> <갯수> &7- 다음 단계로 확장되는 확장권 지급"));
        s.sendMessage(c("&e/가방 크기 <닉> <페이지> <9/18/27/36/45/54> &7- 페이지 크기 설정(1=메인)"));
        s.sendMessage(c("&e/가방 아이템 &7- 손 아이템을 전용 가방 아이템으로 설정(우클릭=메인, 쉬프트+우클릭=2페이지 / 54칸 이후)"));
        s.sendMessage(c("&e/가방 리로드 &7- 설정 리로드"));
    }

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
            Player p = (Player) sender;
            int current = plugin.getStorage().getCurrentSize(p.getUniqueId());
            if (args.length == 1){
                plugin.getStorage().open(p);
                return true;
            }
            int page;
            try { page = Integer.parseInt(args[1]); } catch (NumberFormatException ex) { sender.sendMessage(c("&c페이지는 숫자여야 합니다.")); return true; }
            if (page <= 1){
                plugin.getStorage().open(p);
                return true;
            }
            if (current < MAX_MAIN_SIZE){
                sender.sendMessage(c("&c메인가방을 &e54&c칸까지 먼저 확장하세요."));
                return true;
            }
            String title = plugin.getConfig().getString("pager.title", "&6가방 &7(Page {page})");
            plugin.getPagerStore().openPage(p, Math.max(2, page), title);
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

        if (sub.equalsIgnoreCase("지급확장권")){
            if (!(sender.isOp() || sender.hasPermission("ultimatebackpack.admin"))) { sender.sendMessage(c("&c권한이 없습니다.")); return true; }
            if (args.length < 3){ sender.sendMessage(c("&c사용법: /가방 지급확장권 <닉> <갯수>")); return true; }
            Player t = Bukkit.getPlayerExact(args[1]);
            if (t == null){ sender.sendMessage(c("&c해당 플레이어를 찾을 수 없습니다.")); return true; }
            int amount = 1;
            try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
            if (amount < 1) amount = 1;

            org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
            String matStr = cfg.getString("ticket.material", "PAPER");
            org.bukkit.Material mat;
            try { mat = org.bukkit.Material.valueOf(matStr); } catch (IllegalArgumentException ex) { mat = org.bukkit.Material.PAPER; }
            String name = cfg.getString("ticket.display-name", "&6가방 확장권");
            java.util.List<String> lore = new java.util.ArrayList<>(cfg.getStringList("ticket.lore"));
            java.util.List<String> extra = cfg.getStringList("ticket-extra-lore");
            java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>();
            for (String ln : lore) merged.add(c(ln));
            for (String ln : extra) merged.add(c(ln));

            org.bukkit.inventory.ItemStack it = new org.bukkit.inventory.ItemStack(mat);
            org.bukkit.inventory.meta.ItemMeta im = it.getItemMeta();
            if (im != null){
                im.setDisplayName(c(name));
                if (!merged.isEmpty()) im.setLore(new java.util.ArrayList<>(merged));
                im.getPersistentDataContainer().set(plugin.getKeyTicket(), org.bukkit.persistence.PersistentDataType.STRING, "next");
                it.setItemMeta(im);
            }
            it.setAmount(amount);
            t.getInventory().addItem(it);
            sender.sendMessage(c("&a확장권 지급: &7" + t.getName() + " &7(다음 단계) x" + amount));
            return true;
        }

        if (sub.equalsIgnoreCase("아이템")){
            if (!(sender.isOp() || sender.hasPermission("ultimatebackpack.admin"))) { sender.sendMessage(c("&c권한이 없습니다.")); return true; }
            if (!(sender instanceof org.bukkit.entity.Player)) { sender.sendMessage("플레이어만 사용 가능합니다."); return true; }
            org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
            org.bukkit.inventory.ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == org.bukkit.Material.AIR){ sender.sendMessage(c("&c손에 든 아이템이 없습니다.")); return true; }
            // apply bag flag + name/lore
            org.bukkit.inventory.ItemStack template = makeBagItem(1);
            org.bukkit.inventory.meta.ItemMeta src = template.getItemMeta();
            if (src != null){
                org.bukkit.inventory.meta.ItemMeta meta = hand.getItemMeta();
                if (meta != null){
                    meta.setDisplayName(src.getDisplayName());
                    meta.setLore(src.getLore());
                    meta.getPersistentDataContainer().set(plugin.getKeyBagFlag(), org.bukkit.persistence.PersistentDataType.BYTE, (byte)1);
                    hand.setItemMeta(meta);
                    sender.sendMessage(c("&a손 아이템을 가방 아이템으로 설정하고 이름/로어를 적용했습니다."));
                } else {
                    sender.sendMessage(c("&c이 아이템은 메타데이터를 변경할 수 없습니다."));
                }
            }
            return true;
        }

        if (sub.equalsIgnoreCase("크기")){
            if (!(sender.isOp() || sender.hasPermission("ultimatebackpack.admin"))) { sender.sendMessage(c("&c권한이 없습니다.")); return true; }
            if (args.length < 4){ sender.sendMessage(c("&c사용법: /가방 크기 <닉> <페이지> <9/18/27/36/45/54>  &7(페이지 1 = 메인가방)")); return true; }
            Player t = Bukkit.getPlayerExact(args[1]);
            if (t == null){ sender.sendMessage(c("&c해당 플레이어를 찾을 수 없습니다.")); return true; }
            int page; int size;
            try { page = Integer.parseInt(args[2]); } catch (NumberFormatException e){ sender.sendMessage(c("&c페이지는 숫자여야 합니다.")); return true; }
            try { size = Integer.parseInt(args[3]); } catch (NumberFormatException e){ sender.sendMessage(c("&c크기는 숫자여야 합니다.")); return true; }
            if (!ALLOWED.contains(size)){ sender.sendMessage(c("&c크기는 9/18/27/36/45/54 중 하나여야 합니다.")); return true; }
            if (page <= 1){ plugin.getStorage().setCurrentSize(t.getUniqueId(), size); }
            else { plugin.getPagerStore().setPageSize(t.getUniqueId(), page, size); }
            sender.sendMessage(c("&a설정 완료: &f" + t.getName() + " &7페이지 &e" + page + " &7→ &e" + size));
            return true;
        }
        if (sub.equalsIgnoreCase("지급")){
            if (!(sender.isOp() || sender.hasPermission("ultimatebackpack.admin"))) { sender.sendMessage(c("&c권한이 없습니다.")); return true; }
            if (args.length < 3){ sender.sendMessage(c("&c사용법: /가방 지급 <닉> <갯수>")); return true; }
            org.bukkit.entity.Player t = org.bukkit.Bukkit.getPlayerExact(args[1]);
            if (t == null){ sender.sendMessage(c("&c해당 플레이어를 찾을 수 없습니다.")); return true; }
            int amount = 1; try { amount = Integer.parseInt(args[2]); } catch (Exception ex) {}
            org.bukkit.inventory.ItemStack it = makeBagItem(amount);
            t.getInventory().addItem(it);
            sender.sendMessage(c("&a가방 아이템 지급 완료: &f" + t.getName() + " &7x" + amount));
            return true;
        }

        help(sender);
        return true;
    }
}
