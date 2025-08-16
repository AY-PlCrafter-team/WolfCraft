package com.yuta.jinro.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GameManager gm = GameManager.getInstance();

        if (gm.isGameRunning()) {
            // ゲーム中に戻ってきた場合
            if (gm.getAllPlayerUUIDs().contains(player.getUniqueId())) {
                // 元プレイヤーなら状態復元
                PlayerData pd = gm.getPlayerData(player.getUniqueId());
                if (pd != null && pd.isAlive()) {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.sendMessage("§aゲームに復帰しました！");
                } else {
                    player.setGameMode(GameMode.SPECTATOR);
                    player.sendMessage("§cあなたは死亡しているため観戦モードです");
                }
            } else {
                // 新規途中参加者 → 観戦モード
                gm.handleJoin(player);
            }
        } else {
            // ゲーム中でない場合はロビーへ
            if (Bukkit.getWorld("lobby") != null) {
                player.teleport(Bukkit.getWorld("lobby").getSpawnLocation());
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage("§aロビーにようこそ！");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameManager gm = GameManager.getInstance();

        if (gm.isGameRunning() && gm.getAllPlayerUUIDs().contains(player.getUniqueId())) {
            gm.handleQuit(player);
        }
    }
}