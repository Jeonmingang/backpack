
package com.minkang.ultimate.backpack.pager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PageStore {
    private final File dir;
    private final Map<UUID, Integer> openPage = new HashMap<>();

    public PageStore(File dataFolder){
        this.dir = new File(dataFolder, "pages");
        if (!this.dir.exists()) this.dir.mkdirs();
    }

    private String c(String s){ return ChatColor.translateAlternateColorCodes('&', s); }

    private File file(UUID id){ return new File(dir, id.toString()+".yml"); }
    private YamlConfiguration load(UUID id){
        File f = file(id);
        if (!f.exists()) return new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(f);
    }
    private void save(UUID id, YamlConfiguration yml){
        try{ yml.save(file(id)); } catch (IOException ignored){}
    }

    public int getOpenPage(Player p){ return openPage.getOrDefault(p.getUniqueId(), 1); }
    public void setOpenPage(Player p, int page){ openPage.put(p.getUniqueId(), page); }

    public int getPageSize(UUID id, int page){
        YamlConfiguration y = load(id);
        return y.getInt("pages."+page+".size", 9);
    }

    public void setPageSize(UUID id, int page, int size){
        YamlConfiguration y = load(id);
        y.set("pages."+page+".size", size);
        save(id, y);
    }

    public void savePage(UUID id, int page, ItemStack[] contents){
        YamlConfiguration y = load(id);
        y.set("pages."+page+".inv", contents);
        save(id, y);
    }

    public ItemStack[] loadPage(UUID id, int page, int size){
        YamlConfiguration y = load(id);
        ItemStack[] arr = ((ItemStack[]) y.get("pages."+page+".inv"));
        if (arr == null) arr = new ItemStack[size];
        if (arr.length != size){
            ItemStack[] resized = new ItemStack[size];
            for (int i=0;i<Math.min(arr.length, size);i++) resized[i]=arr[i];
            arr = resized;
        }
        return arr;
    }

    public void openPage(Player p, int page, String titleFormat){
        if (page <= 1){
            p.performCommand("가방");
            return;
        }
        int size = getPageSize(p.getUniqueId(), page);
        String title = c(titleFormat.replace("{page}", String.valueOf(page)));
        Inventory inv = Bukkit.createInventory(p, size, title);
        inv.setContents(loadPage(p.getUniqueId(), page, size));
        setOpenPage(p, page);
        p.openInventory(inv);
    }

    public void nextPage(Player p, String titleFormat){
        int cur = getOpenPage(p);
        openPage(p, Math.max(2, cur+1), titleFormat);
    }

    public void prevPage(Player p, String titleFormat){
        int cur = getOpenPage(p);
        openPage(p, Math.max(2, cur-1), titleFormat);
    }
}
