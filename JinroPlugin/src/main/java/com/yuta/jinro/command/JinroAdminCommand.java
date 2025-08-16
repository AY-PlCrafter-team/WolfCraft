package com.yuta.jinro.command;

import com.yuta.jinro.game.GameManager;
import com.yuta.jinro.game.Role;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class JinroAdminCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーからのみ実行可能です。");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§e/jinro_admin start - ゲーム開始");
            player.sendMessage("§e/jinro_admin stop  - ゲーム終了");
            player.sendMessage("§e/jinro_admin rop <player> <role> - 役職設定");
            player.sendMessage("§e/jinro_admin rop list/add/remove/getop/reop");
            player.sendMessage("§e/jinro_admin chat <text> - 運営チャット");
            return true;
        }

        String sub = args[0].toLowerCase();
        GameManager gm = GameManager.getInstance();

        switch (sub) {
            case "start" -> {
                if (!player.hasPermission("jinro.admin")) {
                    player.sendMessage("§c権限がありません。");
                    return true;
                }
                List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                List<Role> roles = new ArrayList<>();
                roles.add(Role.MURABITO);
                roles.add(Role.MURABITO);
                roles.add(Role.JINROU);
                roles.add(Role.URANAI);

                gm.startGame(onlinePlayers, roles);
                player.sendMessage("§aゲームを開始しました！");

                // 自動テレポート
                gm.teleportPlayersToPlay(onlinePlayers);
            }

            case "stop" -> {
                if (!player.hasPermission("jinro.admin")) {
                    player.sendMessage("§c権限がありません。");
                    return true;
                }
                gm.endGame("管理者による中断");
                player.sendMessage("§eゲームを終了しました");

                // 終了後はロビーに戻す
                gm.teleportPlayersToLobby(new ArrayList<>(Bukkit.getOnlinePlayers()));
            }

            case "rop" -> {
                if (!player.hasPermission("jinro.rop")) {
                    player.sendMessage("§c権限がありません。");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage("§e使い方: /jinro_admin rop <list/add/remove/getop/reop> [player]");
                    return true;
                }

                String action = args[1].toLowerCase();
                switch (action) {
                    case "list" -> gm.listRopPlayers(player);
                    case "add", "remove", "getop", "reop" -> {
                        if (args.length < 3) {
                            player.sendMessage("§e対象プレイヤーを指定してください");
                            return true;
                        }
                        String targetName = args[2];
                        Player target = Bukkit.getPlayerExact(targetName);
                        if (target == null || !target.isOnline()) {
                            player.sendMessage("§c対象プレイヤーが見つかりません");
                            return true;
                        }

                        switch (action) {
                            case "add" -> gm.addRop(target);
                            case "remove" -> gm.removeRop(target);
                            case "getop" -> gm.getOp(target);
                            case "reop" -> gm.revokeOp(target);
                        }
                        player.sendMessage("§aRop操作を実行しました: " + action + " -> " + target.getName());
                    }
                    default -> player.sendMessage("§c不明なRopサブコマンドです");
                }
            }

            case "chat" -> {
                if (args.length < 2) {
                    player.sendMessage("§e/jinro_admin chat <text>");
                    return true;
                }
                String message = String.join(" ", args).substring(args[0].length() + 1);
                gm.sendAdminChat(player, message);
            }

            default -> player.sendMessage("§c不明なサブコマンドです");
        }

        return true;
    }
}