package com.yuta.jinro.command;

import com.yuta.jinro.JinroPlugin;
import com.yuta.jinro.game.GameManager;
import com.yuta.jinro.game.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;

import java.util.*;

public class JinroPlayerCommand implements CommandExecutor {

    private final Map<UUID, UUID> votes = new HashMap<>();
    private BukkitRunnable voteTask;
    private boolean votingActive = false;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤー専用です");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§e/jinro [vote|spec|sell|money|info|jinro]");
            return true;
        }

        GameManager gm = GameManager.getInstance();

        switch (args[0].toLowerCase()) {

            // 投票開始
            case "vote" -> {
                if (!gm.isGameRunning()) {
                    player.sendMessage("§cゲーム中ではありません");
                    return true;
                }
                if (votingActive) {
                    player.sendMessage("§c投票は既に開始されています");
                    return true;
                }
                startVote();
                return true;
            }

            // 観戦切替
            case "spec" -> {
                if (!gm.isGameRunning()) {
                    player.sendMessage("§cゲーム中ではありません");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§e/jinro spec [yes|no]");
                    return true;
                }
                boolean wantSpec = args[1].equalsIgnoreCase("yes");
                if (wantSpec) {
                    player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                    player.sendMessage("§a観戦モードになりました");
                } else {
                    PlayerData pd = gm.getPlayerData(player.getUniqueId());
                    if (pd != null && pd.isAlive()) player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    player.sendMessage("§aゲームに参加しました");
                }
                return true;
            }

            // 鉱石売却
            case "sell" -> {
                if (!gm.isGameRunning()) {
                    player.sendMessage("§cゲーム中のみ使用可能です");
                    return true;
                }
                player.sendMessage("§aGUIを開いて鉱石を売却できます（未実装）");
                return true;
            }

            // 所持金表示
            case "money" -> {
                if (!gm.isGameRunning()) {
                    player.sendMessage("§cゲーム中のみ使用可能です");
                    return true;
                }
                PlayerData pd = gm.getPlayerData(player.getUniqueId());
                if (pd != null) player.sendMessage("§e所持金: " + pd.getMoney());
                return true;
            }

            // 役職情報
            case "info" -> {
                if (!gm.isGameRunning()) {
                    player.sendMessage("§cゲーム中のみ使用可能です");
                    return true;
                }
                PlayerData pd = gm.getPlayerData(player.getUniqueId());
                if (pd != null) {
                    player.sendMessage("§aあなたの役職: " + pd.getRole().name());
                    player.sendMessage("§e所持金: " + pd.getMoney());
                }
                return true;
            }

            // 人狼チャット
            case "jinro" -> {
                if (!gm.isGameRunning()) {
                    player.sendMessage("§cゲーム中ではありません");
                    return true;
                }
                PlayerData pd = gm.getPlayerData(player.getUniqueId());
                if (pd == null || !gm.isWerewolfRole(pd.getRole())) {
                    player.sendMessage("§cあなたは人狼ではありません");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§e/jinro jinro [メッセージ]");
                    return true;
                }
                String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                for (PlayerData targetPd : gm.getAllPlayerData()) {
                    if (targetPd.isAlive() && gm.isWerewolfRole(targetPd.getRole())) {
                        Player target = Bukkit.getPlayer(targetPd.getUuid());
                        if (target != null) target.sendMessage("§c[人狼] " + player.getName() + ": " + msg);
                    }
                }
                return true;
            }

            default -> player.sendMessage("§e/jinro [vote|spec|sell|money|info|jinro]");
        }

        return true;
    }

    /** 投票開始 */
    private void startVote() {
        votingActive = true;
        votes.clear();
        GameManager gm = GameManager.getInstance();
        List<PlayerData> alivePlayers = new ArrayList<>();
        for (PlayerData pd : gm.getAllPlayerData()) if (pd.isAlive()) alivePlayers.add(pd);

        for (PlayerData pd : alivePlayers) {
            Player p = Bukkit.getPlayer(pd.getUuid());
            if (p == null) continue;

            TextComponent msg = new TextComponent("§a投票してください: ");
            for (PlayerData targetPd : alivePlayers) {
                if (targetPd.getUuid().equals(pd.getUuid())) continue;
                Player target = Bukkit.getPlayer(targetPd.getUuid());
                if (target == null) continue;

                TextComponent button = new TextComponent("[" + target.getName() + "]");
                button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/jinro doVote " + target.getUniqueId()));
                msg.addExtra(button);
                msg.addExtra(" ");
            }
            p.spigot().sendMessage(msg);
        }

        voteTask = new BukkitRunnable() {
            int remaining = 120;

            @Override
            public void run() {
                remaining--;
                if (remaining <= 0) {
                    finishVote();
                    cancel();
                }
            }
        };
        voteTask.runTaskTimer(JinroPlugin.getInstance(), 20L, 20L);
    }

    /** 投票処理 */
    public void doVote(Player voter, UUID targetUUID) {
        if (!votingActive) {
            voter.sendMessage("§c投票時間ではありません");
            return;
        }
        if (votes.containsKey(voter.getUniqueId())) {
            voter.sendMessage("§c既に投票済みです");
            return;
        }
        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null) {
            voter.sendMessage("§cそのプレイヤーは存在しません");
            return;
        }

        votes.put(voter.getUniqueId(), targetUUID);
        voter.sendMessage("§a" + target.getName() + " に投票しました");

        GameManager gm = GameManager.getInstance();
        int aliveCount = (int) gm.getAllPlayerData().stream().filter(PlayerData::isAlive).count();
        if (votes.size() > aliveCount / 2) {
            finishVote();
            if (voteTask != null) voteTask.cancel();
        }
    }

    /** 投票終了 */
    private void finishVote() {
        votingActive = false;

        Map<UUID, Integer> countMap = new HashMap<>();
        for (UUID target : votes.values()) countMap.put(target, countMap.getOrDefault(target, 0) + 1);

        if (countMap.isEmpty()) {
            Bukkit.broadcastMessage("§e投票スキップ: 誰も投票しませんでした");
            return;
        }

        int max = Collections.max(countMap.values());
        List<UUID> top = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : countMap.entrySet()) if (e.getValue() == max) top.add(e.getKey());

        if (top.size() != 1) {
            Bukkit.broadcastMessage("§e投票スキップ: 同数票がありました");
            return;
        }

        UUID selected = top.get(0);
        GameManager gm = GameManager.getInstance();
        PlayerData pd = gm.getPlayerData(selected);
        if (pd != null) {
            pd.setAlive(false);
            Player victim = Bukkit.getPlayer(selected);
            if (victim != null) {
                victim.setGameMode(org.bukkit.GameMode.SPECTATOR);
                victim.sendMessage("§cあなたは投票により吊られました");
            }
            Bukkit.broadcastMessage("§6" + (victim != null ? victim.getName() : "プレイヤー") + " が吊られました");
        }
    }
}