package com.minkang.ultimate.backpack.storage;

import com.minkang.ultimate.backpack.BackpackPlugin;
import com.minkang.ultimate.backpack.util.InventorySerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class PersonalStorage {
    private final BackpackPlugin plugin;
    private final File dirPlayers;
    private final Map<UUID, Inventory> openInv = new HashMap<>();

    public PersonalStorage(BackpackPlugin plugin) {
        this.plugin = plugin;
        this.dirPlayers = new File(plugin.getDataFolder(), "players");
        if (!dirPlayers.exists()) dirPlayers.mkdirs();
    }

    private File file(UUID id) { return new File(dirPlayers, id.toString() + ".yml"); }

    public int getCurrentSize(UUID id) {
        int def = plugin.getConfig().getInt("backpack.starter-size", 9);
        File f = file(id);
        if (!f.exists()) return def;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        return y.getInt("size", def);
    }

    public void setCurrentSize(UUID id, int size) {
        File f = file(id);
        YamlConfiguration y = f.exists() ? YamlConfiguration.loadConfiguration(f) : new YamlConfiguration();
        y.set("size", size);
        try { y.save(f); } catch (Exception ignored) {}
    }

    public ItemStack[] loadContents(UUID id) {
        File f = file(id);
        if (!f.exists()) return null;
        try {
            String data = YamlConfiguration.loadConfiguration(f).getString("data", null);
            if (data == null) return null;
            return InventorySerializer.itemStackArrayFromBase64(data);
        } catch (Exception e) {
            plugin.getLogger().warning("개인가방 불러오기 실패: " + e.getMessage());
            return null;
        }
    }

    public void saveContents(UUID id, ItemStack[] contents) {
        File f = file(id);
        YamlConfiguration y = f.exists() ? YamlConfiguration.loadConfiguration(f) : new YamlConfiguration();
        try {
            String data = InventorySerializer.itemStackArrayToBase64(contents);
            y.set("data", data);
            y.save(f);
        } catch (Exception e) { plugin.getLogger().warning("개인가방 저장 실패: " + e.getMessage()); }
    }

    public void open(Player p) {
        UUID id = p.getUniqueId();
        int size = getCurrentSize(id);
        size = nearestAllowed(size);
        setCurrentSize(id, size);
        String fmt = plugin.getConfig().getString("backpack.title-format", "&6[개인가방] &e%player% &7(%size%)");
        String title = ChatColor.translateAlternateColorCodes('&', fmt)
                .replace("%player%", p.getName())
                .replace("%size%", String.valueOf(size));
        Inventory inv = Bukkit.createInventory(null, size, title);
        ItemStack[] contents = loadContents(id);
        if (contents != null) {
            inv.setContents(contents);
        }
        openInv.put(id, inv);
        p.openInventory(inv);
    }

    public void saveAndClose(Player p) {
        UUID id = p.getUniqueId();
        Inventory inv = openInv.remove(id);
        if (inv != null) {
            saveContents(id, inv.getContents());
        }
    }

    public boolean isOpen(UUID id){ return openInv.containsKey(id); }

    public int nearestAllowed(int target){
        int[] allowed = new int[]{9,18,27,36,45,54};
        int best = allowed[0];
        for (int a : allowed){
            if (a <= target) best = a;
        }
        return best;
    }

    public Integer nextSize(int current){
        int[] allowed = new int[]{9,18,27,36,45,54};
        for (int a : allowed){
            if (a > current) return a;
        }
        return null;
    }
}