package com.yuta.jinro.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class TeleportManager {

    private static TeleportManager instance;

    public static TeleportManager getInstance() {
        if (instance == null) instance = new TeleportManager();
        return instance;
    }

    public static void teleportAllToPlay(List<Player> players) {
        World playWorld = Bukkit.getWorld("Play");
        if (playWorld == null) return;
        Location spawn = playWorld.getSpawnLocation();
        for (Player p : players) {
            if (p.isOnline()) p.teleport(spawn);
        }
    }

    public static void teleportAllToLobby(List<Player> players) {
        World lobbyWorld = Bukkit.getWorld("Lobby");
        if (lobbyWorld == null) return;
        Location spawn = lobbyWorld.getSpawnLocation();
        for (Player p : players) {
            if (p.isOnline()) p.teleport(spawn);
        }
    }
}