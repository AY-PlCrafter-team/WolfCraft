package com.yuta.jinro.game;

import com.yuta.jinro.JinroPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class GameManager {

    public enum Phase {
        DAY, NIGHT
    }

    private static GameManager instance;

    public static GameManager getInstance() {
        if (instance == null)
            instance = new GameManager();
        return instance;
    }

    private final RoleItemManager roleItemManager = new RoleItemManager();
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final Set<UUID> ropPlayers = new HashSet<>();
    private boolean gameRunning = false;
    private Phase currentPhase = Phase.DAY;

    private BukkitRunnable phaseTask;
    private int phaseDuration = 120;

    private Map<UUID, UUID> votes = new HashMap<>();
    private BukkitRunnable voteTask;
    private int voteDuration = 120;

    // -----------------------------
    // テレポートラッパー
    // -----------------------------
    public void teleportPlayersToPlay(List<Player> players) {
        TeleportManager.teleportAllToPlay(players);
    }

    public void teleportPlayersToLobby(List<Player> players) {
        TeleportManager.teleportAllToLobby(players);
    }

    // -----------------------------
    // ゲーム開始
    // -----------------------------

    public void startGame(List<Player> players, List<Role> roles) {
        if (gameRunning)
            return;

        int playerCount = players.size();
        if (playerCount < 2) {
            Bukkit.broadcastMessage("§cプレイヤーが少なすぎます！2人以上で開始してください");
            return;
        }
        if (playerCount > 40) {
            Bukkit.broadcastMessage("§cプレイヤーが多すぎます！41人以上はできません。");
            return;
        }

        loadRandomMap();
        teleportPlayersToPlay(players);

        gameRunning = true;
        currentPhase = Phase.DAY;

        List<Role> assignedRoles;
        if (roles != null && !roles.isEmpty()) {
            assignedRoles = roles;
        } else {
            // 参加人数に応じたテンプレを取得し、不足分は村人で補完
            Map<Role, Integer> baseTemplate = RoleDefault.getNearestTemplate(playerCount);
            int totalAssigned = baseTemplate.values().stream().mapToInt(Integer::intValue).sum();
            int villagersToAdd = playerCount - totalAssigned;
            if (villagersToAdd > 0) {
                baseTemplate.put(Role.MURABITO, baseTemplate.getOrDefault(Role.MURABITO, 0) + villagersToAdd);
            }

            // リストに展開
            assignedRoles = new ArrayList<>();
            for (Map.Entry<Role, Integer> entry : baseTemplate.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++)
                    assignedRoles.add(entry.getKey());
            }

            Collections.shuffle(assignedRoles);
        }

        int idx = 0;
        for (Player p : players) {
            if (!p.isOnline())
                continue;
            PlayerData pd = new PlayerData(p.getUniqueId());
            Role assignedRole = assignedRoles.get(idx % assignedRoles.size());
            pd.setRole(assignedRole);
            pd.setAlive(true);
            playerDataMap.put(p.getUniqueId(), pd);
            roleItemManager.giveRoleItem(p, assignedRole.name(), idx + 1);
            p.setGameMode(GameMode.SURVIVAL);
            p.sendMessage("§aあなたの役職: " + assignedRole.name());
            idx++;
        }

        Bukkit.broadcastMessage("§6人狼ゲームが開始されました！");
        checkWinCondition();
        startPhaseTimer();
    }

    private void loadRandomMap() {
        String[] mapCandidates = { "game", "game2", "game3", "game4", "game5" };
        File source = null;

        List<File> existingMaps = new ArrayList<>();
        for (String mapName : mapCandidates) {
            File f = new File(Bukkit.getWorldContainer(), mapName);
            if (f.exists() && f.isDirectory())
                existingMaps.add(f);
        }

        if (existingMaps.isEmpty()) {
            Bukkit.getLogger().warning("使用可能なマップが存在しません！");
            return;
        }

        source = existingMaps.get(new Random().nextInt(existingMaps.size()));
        File target = new File(Bukkit.getWorldContainer(), "Play");

        if (target.exists())
            deleteFolder(target);

        try {
            copyFolder(source.toPath(), target.toPath());

            File uidFile = new File(target, "uid.dat");
            if (uidFile.exists())
                uidFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (Bukkit.getWorld("Play") != null) {
            Bukkit.unloadWorld("Play", false);
        }

        Bukkit.createWorld(new org.bukkit.WorldCreator("Play"));
    }

    private void copyFolder(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            Bukkit.getLogger().warning("コピー元フォルダが存在しません: " + source);
            return;
        }

        Files.walk(source).forEach(path -> {
            try {
                Path relative = source.relativize(path);
                Path dest = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    if (!Files.exists(dest))
                        Files.createDirectories(dest);
                } else {
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void deleteFolder(File folder) {
        if (!folder.exists())
            return;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory())
                    deleteFolder(f);
                else
                    f.delete();
            }
        }
        folder.delete();
    }

    private void startPhaseTimer() {
        phaseTask = new BukkitRunnable() {
            int remaining = phaseDuration;

            @Override
            public void run() {
                if (!gameRunning) {
                    cancel();
                    return;
                }
                remaining--;
                if (remaining <= 0) {
                    switchPhase();
                    remaining = phaseDuration;
                }
            }
        };
        phaseTask.runTaskTimer(JinroPlugin.getInstance(), 20L, 20L);
    }

    private void switchPhase() {
        currentPhase = (currentPhase == Phase.DAY) ? Phase.NIGHT : Phase.DAY;
        Bukkit.broadcastMessage("§bフェーズが切り替わりました: " + currentPhase.name());
        checkWinCondition();
    }

    public void endGame(String winner) {
        if (!gameRunning)
            return;
        gameRunning = false;
        if (phaseTask != null)
            phaseTask.cancel();
        if (voteTask != null)
            voteTask.cancel();
        Bukkit.broadcastMessage("§eゲーム終了！勝利陣営: " + winner);
        playerDataMap.clear();
        votes.clear();
        ropPlayers.clear();
        teleportPlayersToLobby(new ArrayList<>(Bukkit.getOnlinePlayers()));
    }

    // -----------------------------
    // 投票システム
    // -----------------------------
    public void startVoting() {
        votes.clear();
        broadcastAlivePlayersForVoting();
        voteTask = new BukkitRunnable() {
            int remaining = voteDuration;

            @Override
            public void run() {
                remaining--;
                if (!gameRunning) {
                    cancel();
                    return;
                }
                if (remaining <= 0 || votes.size() > getAlivePlayers().size() / 2) {
                    endVoting();
                    cancel();
                }
            }
        };
        voteTask.runTaskTimer(JinroPlugin.getInstance(), 20L, 20L);
        Bukkit.broadcastMessage("§a投票が開始されました！2分以内に投票してください。");
    }

    private void broadcastAlivePlayersForVoting() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!playerDataMap.containsKey(p.getUniqueId()) || !p.isOnline())
                continue;
            p.sendMessage("§e誰に投票しますか？ クリックで投票可能です:");
            for (PlayerData target : getAlivePlayers()) {
                Player targetPlayer = Bukkit.getPlayer(target.getUuid());
                if (targetPlayer == null)
                    continue;
                TextComponent tc = new TextComponent("[" + targetPlayer.getName() + "]");
                tc.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/jinro vote " + target.getUuid()));
                tc.setHoverEvent(
                        new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                                new ComponentBuilder("クリックして投票").create()));
                p.spigot().sendMessage(tc);
            }
        }
    }

    public void vote(Player voter, UUID target) {
        if (!playerDataMap.containsKey(voter.getUniqueId()))
            return;
        PlayerData pd = playerDataMap.get(voter.getUniqueId());
        if (!pd.isAlive())
            return;
        if (votes.containsKey(voter.getUniqueId())) {
            voter.sendMessage("§cすでに投票済みです");
            return;
        }
        if (!playerDataMap.containsKey(target) || !playerDataMap.get(target).isAlive()) {
            voter.sendMessage("§cそのプレイヤーには投票できません");
            return;
        }
        votes.put(voter.getUniqueId(), target);
        Player tp = Bukkit.getPlayer(target);
        voter.sendMessage("§a投票しました: " + (tp != null ? tp.getName() : target.toString()));
    }

    private void endVoting() {
        Bukkit.broadcastMessage("§e投票終了！");
        if (votes.isEmpty()) {
            Bukkit.broadcastMessage("§6誰にも投票されませんでした → スキップ");
            return;
        }
        Map<UUID, Integer> count = new HashMap<>();
        for (UUID t : votes.values())
            count.put(t, count.getOrDefault(t, 0) + 1);
        int max = count.values().stream().max(Integer::compareTo).orElse(0);
        List<UUID> top = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : count.entrySet())
            if (e.getValue() == max)
                top.add(e.getKey());
        if (top.size() != 1) {
            Bukkit.broadcastMessage("§6同票または票が足りず → スキップ");
        } else {
            UUID target = top.get(0);
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer != null) {
                Bukkit.broadcastMessage("§c" + targetPlayer.getName() + " が最多票で処刑されました！");
                handleDeath(targetPlayer);
            }
        }
    }

    private List<PlayerData> getAlivePlayers() {
        List<PlayerData> alive = new ArrayList<>();
        for (PlayerData pd : playerDataMap.values())
            if (pd.isAlive())
                alive.add(pd);
        return alive;
    }

    private void handleDeath(Player player) {
        PlayerData pd = playerDataMap.get(player.getUniqueId());
        if (pd != null)
            pd.setAlive(false);
        player.setGameMode(GameMode.SPECTATOR);
        checkWinCondition();
    }

    private void checkWinCondition() {
        if (!gameRunning)
            return;
        long aliveVillagers = playerDataMap.values().stream().filter(pd -> pd.isAlive() && isVillagerRole(pd.getRole()))
                .count();
        long aliveWerewolves = playerDataMap.values().stream()
                .filter(pd -> pd.isAlive() && isWerewolfRole(pd.getRole())).count();
        long aliveThirdParty = playerDataMap.values().stream()
                .filter(pd -> pd.isAlive() && isThirdPartyRole(pd.getRole())).count();

        if (aliveWerewolves == 0 && aliveThirdParty == 0)
            endGame("村人陣営");
        else if (aliveVillagers == 0 && aliveThirdParty == 0)
            endGame("人狼陣営");
        else if (aliveVillagers == 0 && aliveWerewolves == 0 && aliveThirdParty > 0)
            endGame("第三陣営");
    }

    private boolean isVillagerRole(Role role) {
        return switch (role) {
            case MURABITO, URANAI, KISHI, REIMAI, NISEMONO, SISTER, KYOZO -> true;
            default -> false;
        };
    }

    public boolean isWerewolfRole(Role role) {
        return switch (role) {
            case JINROU, KYOJIN, MAGICIAN -> true;
            default -> false;
        };
    }

    private boolean isThirdPartyRole(Role role) {
        return switch (role) {
            case SURVIVOR, BAKUDANMA, URAGIRI -> true;
            default -> false;
        };
    }

    // -----------------------------
    // ROP 管理
    // -----------------------------
    public void addRop(Player player) {
        ropPlayers.add(player.getUniqueId());
    }

    public void removeRop(Player player) {
        ropPlayers.remove(player.getUniqueId());
    }

    public boolean isRop(Player player) {
        return ropPlayers.contains(player.getUniqueId());
    }

    public List<String> listRopPlayers() {
        List<String> list = new ArrayList<>();
        for (UUID uuid : ropPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                list.add(p.getName());
        }
        return list;
    }

    public List<String> listRopPlayers(Player ignored) {
        return listRopPlayers();
    }

    // -----------------------------
    // 追加ユーティリティ
    // -----------------------------
    public boolean isGameRunning() {
        return gameRunning;
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public Collection<PlayerData> getAllPlayerData() {
        return Collections.unmodifiableCollection(playerDataMap.values());
    }

    public Set<UUID> getAllPlayerUUIDs() {
        return Collections.unmodifiableSet(playerDataMap.keySet());
    }

    public void handleJoin(Player player) {
        if (!gameRunning)
            return;
        PlayerData pd = playerDataMap.get(player.getUniqueId());
        if (pd == null)
            return;
        if (pd.isAlive()) {
            player.setGameMode(GameMode.SURVIVAL);
        } else {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    public void handleQuit(Player player) {
        if (!gameRunning)
            return;
        PlayerData pd = playerDataMap.get(player.getUniqueId());
        if (pd == null)
            return;
        if (pd.isAlive())
            pd.setAlive(false);
        checkWinCondition();
    }

    public void getOp(Player player) {
        if (player != null)
            player.setOp(true);
    }

    public void revokeOp(Player player) {
        if (player != null)
            player.setOp(false);
    }

    public void sendAdminChat(Player sender, String message) {
        String name = sender != null ? sender.getName() : "Console";
        String formatted = "§d[Admin] " + name + ": " + message;
        for (UUID uuid : ropPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                p.sendMessage(formatted);
        }
        Bukkit.getConsoleSender().sendMessage(formatted);
    }

    public RoleItemManager getRoleItemManager() {
        return roleItemManager;
    }
}