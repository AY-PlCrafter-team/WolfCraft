package com.yuta.jinro;

import com.yuta.jinro.command.JinroAdminCommand;
import com.yuta.jinro.command.JinroPlayerCommand;
import com.yuta.jinro.command.JinroRopCommand;
import com.yuta.jinro.command.Tab;
import com.yuta.jinro.game.TeleportManager;
import com.yuta.jinro.game.GameManager;
import com.yuta.jinro.game.PlayerJoinQuitListener;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class JinroPlugin extends JavaPlugin {

    private static JinroPlugin instance;
    private World lobbyWorld;
    private World gameWorld;

    public static JinroPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // ---------- コマンド登録 ----------
        JinroAdminCommand adminCommand = new JinroAdminCommand();
        getCommand("jinro_admin").setExecutor(adminCommand);
        getCommand("jad").setExecutor(adminCommand);

        JinroRopCommand ropCommand = new JinroRopCommand();
        getCommand("jinro_rop").setExecutor(ropCommand);
        getCommand("jrop").setExecutor(ropCommand);

        JinroPlayerCommand playerCommand = new JinroPlayerCommand();
        getCommand("jinro").setExecutor(playerCommand);
        getCommand("j").setExecutor(playerCommand);

        // ---------- TabCompleter登録 ----------
        Tab tabCompleter = new Tab();
        getCommand("jinro_admin").setTabCompleter(tabCompleter);
        getCommand("jad").setTabCompleter(tabCompleter);
        getCommand("jinro_rop").setTabCompleter(tabCompleter);
        getCommand("jrop").setTabCompleter(tabCompleter);
        getCommand("jinro").setTabCompleter(tabCompleter);
        getCommand("j").setTabCompleter(tabCompleter);

        // ---------- イベント登録 ----------
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(), this);
        Bukkit.getPluginManager().registerEvents(GameManager.getInstance().getRoleItemManager(), this);

        // ---------- ワールド取得 ----------
        lobbyWorld = Bukkit.getWorld("lobby");
        gameWorld = Bukkit.getWorld("game");
        if (lobbyWorld == null || gameWorld == null) {
            getLogger().warning("lobby または game ワールドが見つかりません！");
        }

        // ---------- ロビーへのスポーン ----------
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (GameManager.getInstance().isGameRunning()) {
                p.setGameMode(GameMode.SPECTATOR);
                p.sendMessage("§eゲーム中のため観戦モードになりました");
            } else if (lobbyWorld != null && lobbyWorld.getSpawnLocation() != null) {
                p.teleport(lobbyWorld.getSpawnLocation());
                p.setGameMode(GameMode.SURVIVAL);
            }
        }

        getLogger().info("JinroPlugin が有効化されました");
    }

    @Override
    public void onDisable() {
        getLogger().info("JinroPlugin を無効化しました");
    }

    public World getLobbyWorld() {
        return lobbyWorld;
    }

    public World getGameWorld() {
        return gameWorld;
    }
}