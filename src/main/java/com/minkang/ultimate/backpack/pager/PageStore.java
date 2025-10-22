package com.minkang.ultimate.backpack.pager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PageStore {
    private final File dir;
    private final Map<UUID, Integer> openPage = new HashMap<>();

    public PageStore(File dataFolder){
        this.dir = new File(dataFolder, "pages");
        if (!this.dir.exists()) this.dir.mkdirs();
    }

    private File file(UUID id){ return new File(dir, id.toString()+".yml"); }

    private YamlConfiguration load(UUID id){
        File f = file(id);
        if (!f.exists()) return new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(f);
    }

    private void save(UUID id, YamlConfiguration y){
        try { y.save(file(id)); } catch (IOException ignored) {}
    }

    private static String c(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }

    public int getOpenPage(Player p){ return openPage.getOrDefault(p.getUniqueId(), 0); }
    public void setOpenPage(Player p, int page){ if (page <= 1) openPage.remove(p.getUniqueId()); else openPage.put(p.getUniqueId(), page); }

    public int getPageSize(UUID id, int page, int def){
        YamlConfiguration y = load(id);
        int s = y.getInt("pages."+page+".size", def);
        // normalize to allowed sizes
        int[] allowed = new int[]{9,18,27,36,45,54};
        int result = def;
        for (int a : allowed){ if (a == s) { result = a; break; } }
        return result;
    }

    public void setPageSize(UUID id, int page, int size){
        YamlConfiguration y = load(id);
        y.set("pages."+page+".size", size);
        save(id, y);
    }

    public void savePage(UUID id, int page, ItemStack[] contents){
        YamlConfiguration y = load(id);
        // Always store as a List for compatibility
        List<ItemStack> list = new ArrayList<>();
        if (contents != null) {
            list.addAll(Arrays.asList(contents));
        }
        y.set("pages."+page+".inv", list);
        save(id, y);
    }

    public ItemStack[] loadPage(UUID id, int page, int size){
        YamlConfiguration y = load(id);
        Object raw = y.get("pages."+page+".inv");
        ItemStack[] arr;
        if (raw == null){
            arr = new ItemStack[size];
        } else if (raw instanceof ItemStack[]){
            ItemStack[] src = (ItemStack[]) raw;
            arr = Arrays.copyOf(src, size);
        } else if (raw instanceof java.util.List){
            java.util.List<?> list = (java.util.List<?>) raw;
            arr = new ItemStack[size];
            for (int i=0; i<Math.min(size, list.size()); i++){
                Object o = list.get(i);
                arr[i] = (o instanceof ItemStack) ? (ItemStack)o : null;
            }
        } else {
            arr = new ItemStack[size];
        }
        return arr;
    }

    public void openPage(Player p, int page, String titleFormat){
        int defSize = 9;
        int size = getPageSize(p.getUniqueId(), page, defSize);
        String titleCfg = titleFormat;
        if (titleCfg == null || titleCfg.isEmpty()){
            titleCfg = "&6가방 &7(Page {page})";
        }
        String title = c(titleCfg.replace("{page}", String.valueOf(page)));
        Inventory inv = Bukkit.createInventory(p, size, title);
        inv.setContents(loadPage(p.getUniqueId(), page, size));
        setOpenPage(p, page);
        p.openInventory(inv);
    }

    public void nextPage(Player p, String titleFormat){ openPage(p, Math.max(2, getOpenPage(p)+1), titleFormat); }
    public void prevPage(Player p, String titleFormat){ openPage(p, Math.max(2, getOpenPage(p)-1), titleFormat); }
}