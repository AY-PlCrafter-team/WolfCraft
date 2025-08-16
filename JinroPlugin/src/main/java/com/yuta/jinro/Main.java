package com.yuta.jinro;

import com.yuta.jinro.game.PlayerJoinQuitListener;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;

    @Override
    public void onEnable() {
        instance = this;

        // イベント登録
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(), this);

        getLogger().info("Jinro plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Jinro plugin disabled!");
    }

    public static Main getInstance() {
        return instance;
    }
}