package com.minkang.ultimate.backpack;

import com.minkang.ultimate.backpack.commands.BagCommand;
import com.minkang.ultimate.backpack.listeners.BackpackListener;
import com.minkang.ultimate.backpack.pager.PageStore;
import com.minkang.ultimate.backpack.pager.PagerListener;
import com.minkang.ultimate.backpack.pager.CommandInterceptor;
import com.minkang.ultimate.backpack.storage.PersonalStorage;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class BackpackPlugin extends JavaPlugin {

    private static BackpackPlugin instance;
    private NamespacedKey keyBag;
    private NamespacedKey keyTicket;
    private NamespacedKey keyBagFlag;
    private PersonalStorage storage;
    private PageStore pagerStore;

    public static BackpackPlugin getInstance() { return instance; }
    public NamespacedKey getKeyBag() { return keyBag; }
    public NamespacedKey getKeyTicket() { return keyTicket; }
    public PersonalStorage getStorage() { return storage; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        keyBag = new NamespacedKey(this, "bag");
        keyTicket = new NamespacedKey(this, "ticket");
        storage = new PersonalStorage(this);
        getServer().getPluginManager().registerEvents(new BackpackListener(this), this);
        getServer().getPluginManager().registerEvents(new PagerListener(this, pagerStore), this);
        getServer().getPluginManager().registerEvents(new CommandInterceptor(this, pagerStore), this);
        if (getCommand("가방") != null) getCommand("가방").setExecutor(new BagCommand(this));
        getLogger().info("[UltimateBackpack v1.2.0] Enabled (Korean commands + /가방 설정).");
    }

    @Override
    public void onDisable() {
        getLogger().info("[UltimateBackpack v1.2.0] Disabled.");
    }

    public PageStore getPagerStore(){ return pagerStore; }

    public org.bukkit.NamespacedKey getKeyBagFlag(){ return keyBagFlag; }
}
