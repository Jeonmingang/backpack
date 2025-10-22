
package com.minkang.ultimate.backpack.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.UUID;

public class Compat {
    private final JavaPlugin plugin;
    private final MethodHandles.Lookup lk = MethodHandles.publicLookup();

    private Object storage; // host storage
    private Object pager;   // host pager

    private MethodHandle mhGetCurrentSize, mhSetCurrentSize, mhNearestAllowed, mhNextSize;
    private MethodHandle mhGetPageSize, mhSetPageSize, mhOpenPage, mhGetOpenPage;

    public Compat(JavaPlugin plugin){
        this.plugin = plugin;
        tryInit();
    }

    private void tryInit(){
        try {
            Method m1 = plugin.getClass().getMethod("getStorage");
            storage = m1.invoke(plugin);
        } catch (Throwable ignored) {}
        try {
            Method m2 = plugin.getClass().getMethod("getPagerStore");
            pager = m2.invoke(plugin);
        } catch (Throwable ignored) {}

        tryBind();
    }

    private void tryBind(){
        try {
            if (storage != null){
                Class<?> sCls = storage.getClass();
                mhGetCurrentSize = lk.unreflect(sCls.getMethod("getCurrentSize", UUID.class)).bindTo(storage);
                mhSetCurrentSize = lk.unreflect(sCls.getMethod("setCurrentSize", UUID.class, int.class)).bindTo(storage);
                // optional helpers
                try { mhNearestAllowed = lk.unreflect(sCls.getMethod("nearestAllowed", int.class)).bindTo(storage);} catch(Throwable ignored){}
                try { mhNextSize = lk.unreflect(sCls.getMethod("nextSize", int.class)).bindTo(storage);} catch(Throwable ignored){}
            }
        } catch (Throwable ignored){}

        try {
            if (pager != null){
                Class<?> pCls = pager.getClass();
                // getPageSize(UUID, int, int defaultIfMissing)
                try { mhGetPageSize = lk.unreflect(pCls.getMethod("getPageSize", UUID.class, int.class, int.class)).bindTo(pager);} catch(Throwable ignored){}
                try { mhSetPageSize = lk.unreflect(pCls.getMethod("setPageSize", UUID.class, int.class, int.class)).bindTo(pager);} catch(Throwable ignored){}
                try { mhOpenPage = lk.unreflect(pCls.getMethod("openPage", Player.class, int.class, String.class)).bindTo(pager);} catch(Throwable ignored){}
                try { mhGetOpenPage = lk.unreflect(pCls.getMethod("getOpenPage", Player.class)).bindTo(pager);} catch(Throwable ignored){}
            }
        } catch (Throwable ignored){}
    }

    public int getCurrentSize(UUID u, int def){
        try { if (mhGetCurrentSize != null) return (int) mhGetCurrentSize.invoke(u); } catch(Throwable ignored){}
        return def;
    }

    public void setCurrentSize(UUID u, int size){
        try { if (mhSetCurrentSize != null) mhSetCurrentSize.invoke(u, size); } catch(Throwable ignored){}
    }

    public int nearestAllowed(int v){
        try { if (mhNearestAllowed != null) return (int) mhNearestAllowed.invoke(v);} catch(Throwable ignored){}
        int[] allowed = new int[]{9,18,27,36,45,54};
        int best = 9;
        for (int a: allowed){ if (a<=v) best = a; }
        return best;
    }

    public int nextSize(int v){
        try { if (mhNextSize != null) return (int) mhNextSize.invoke(v);} catch(Throwable ignored){}
        int[] allowed = new int[]{9,18,27,36,45,54};
        for (int a: allowed){ if (a>v) return a; }
        return v;
    }

    public int getPageSize(UUID u, int page, int def){
        try { if (mhGetPageSize != null) return (int) mhGetPageSize.invoke(u, page, def);} catch(Throwable ignored){}
        return def;
    }

    public void setPageSize(UUID u, int page, int size){
        try { if (mhSetPageSize != null) mhSetPageSize.invoke(u, page, size);} catch(Throwable ignored){}
    }

    public void openPage(Player p, int page, String title){
        try { if (mhOpenPage != null) mhOpenPage.invoke(p, page, title);} catch(Throwable ignored){}
    }

    public int getOpenPage(Player p){
        try { if (mhGetOpenPage != null) return (int) mhGetOpenPage.invoke(p);} catch(Throwable ignored){}
        return 1;
    }
}
