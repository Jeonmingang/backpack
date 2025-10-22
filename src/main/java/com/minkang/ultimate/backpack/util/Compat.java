
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
    private Object storage;
    private Object pager;
    private MethodHandle mhGetCurrentSize, mhSetCurrentSize, mhNearestAllowed, mhNextSize;
    private MethodHandle mhGetPageSize, mhSetPageSize, mhOpenPage, mhGetOpenPage;

    public Compat(JavaPlugin plugin){
        this.plugin = plugin;
        tryInit();
    }
    private void tryInit(){
        try { storage = plugin.getClass().getMethod("getStorage").invoke(plugin);} catch(Throwable ignored){}
        try { pager   = plugin.getClass().getMethod("getPagerStore").invoke(plugin);} catch(Throwable ignored){}
        tryBind();
    }
    private void tryBind(){
        try {
            if (storage != null){
                Class<?> s = storage.getClass();
                mhGetCurrentSize = lk.unreflect(s.getMethod("getCurrentSize", java.util.UUID.class)).bindTo(storage);
                mhSetCurrentSize = lk.unreflect(s.getMethod("setCurrentSize", java.util.UUID.class, int.class)).bindTo(storage);
                try { mhNearestAllowed = lk.unreflect(s.getMethod("nearestAllowed", int.class)).bindTo(storage);} catch(Throwable ignored){}
                try { mhNextSize = lk.unreflect(s.getMethod("nextSize", int.class)).bindTo(storage);} catch(Throwable ignored){}
            }
        } catch(Throwable ignored){}
        try {
            if (pager != null){
                Class<?> p = pager.getClass();
                try { mhGetPageSize = lk.unreflect(p.getMethod("getPageSize", java.util.UUID.class, int.class, int.class)).bindTo(pager);} catch(Throwable ignored){}
                try { mhSetPageSize = lk.unreflect(p.getMethod("setPageSize", java.util.UUID.class, int.class, int.class)).bindTo(pager);} catch(Throwable ignored){}
                try { mhOpenPage    = lk.unreflect(p.getMethod("openPage", org.bukkit.entity.Player.class, int.class, java.lang.String.class)).bindTo(pager);} catch(Throwable ignored){}
                try { mhGetOpenPage = lk.unreflect(p.getMethod("getOpenPage", org.bukkit.entity.Player.class)).bindTo(pager);} catch(Throwable ignored){}
            }
        } catch(Throwable ignored){}
    }
    public int getCurrentSize(UUID u, int def){ try { if (mhGetCurrentSize!=null) return (int)mhGetCurrentSize.invoke(u);} catch(Throwable ignored){} return def; }
    public void setCurrentSize(UUID u, int v){ try { if (mhSetCurrentSize!=null) mhSetCurrentSize.invoke(u,v);} catch(Throwable ignored){} }
    public int nearestAllowed(int v){
        try { if (mhNearestAllowed!=null) return (int)mhNearestAllowed.invoke(v);} catch(Throwable ignored){}
        int[] a={9,18,27,36,45,54}; int best=9; for(int x:a){ if(x<=v) best=x;} return best;
    }
    public int nextSize(int v){
        try { if (mhNextSize!=null) return (int)mhNextSize.invoke(v);} catch(Throwable ignored){}
        int[] a={9,18,27,36,45,54}; for(int x:a){ if(x>v) return x;} return v;
    }
    public int getPageSize(UUID u,int page,int def){ try { if (mhGetPageSize!=null) return (int)mhGetPageSize.invoke(u,page,def);} catch(Throwable ignored){} return def; }
    public void setPageSize(UUID u,int page,int size){ try { if (mhSetPageSize!=null) mhSetPageSize.invoke(u,page,size);} catch(Throwable ignored){} }
    public void openPage(Player p,int page,String title){ try { if (mhOpenPage!=null) mhOpenPage.invoke(p,page,title);} catch(Throwable ignored){} }
    public int getOpenPage(Player p){ try { if (mhGetOpenPage!=null) return (int)mhGetOpenPage.invoke(p);} catch(Throwable ignored){} return 1; }
}
