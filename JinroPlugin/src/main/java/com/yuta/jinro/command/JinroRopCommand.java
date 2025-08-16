package com.yuta.jinro.command;

import com.yuta.jinro.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class JinroRopCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§e/jinro_rop start - ゲーム開始");
            sender.sendMessage("§e/jinro_rop stop [murabito/jinro/uragiri] - ゲーム強制終了");
            sender.sendMessage("§e/jinro_rop settings <sub> [subsub] - 各種設定");
            sender.sendMessage("§e/jinro_rop rop <player> [add/remove/getop/reop/list] - ROP操作");
            return true;
        }

        String sub = args[0].toLowerCase();
        GameManager gm = GameManager.getInstance();

        switch (sub) {
            case "start" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cこのコマンドはプレイヤー専用です");
                    return true;
                }

                if (!player.hasPermission("jinro.rop.start")) {
                    player.sendMessage("§c権限がありません");
                    return true;
                }

                if (gm.isGameRunning()) {
                    sender.sendMessage("§cすでにゲームが開始されています");
                    return true;
                }

                List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                gm.startGame(onlinePlayers, null); // void のまま呼ぶ
                sender.sendMessage("§aゲームを開始しました！");
            }

            case "stop" -> {
                if (!sender.hasPermission("jinro.rop.stop")) {
                    sender.sendMessage("§c権限がありません");
                    return true;
                }

                if (!gm.isGameRunning()) {
                    sender.sendMessage("§cゲームは開始されていません");
                    return true;
                }

                String winner = "不明";
                if (args.length > 1) {
                    switch (args[1].toLowerCase()) {
                        case "murabito" -> winner = "村人陣営";
                        case "jinro" -> winner = "人狼陣営";
                        case "uragiri" -> winner = "第三陣営";
                        default -> {
                            sender.sendMessage("§c無効な陣営です (murabito/jinro/uragiri)");
                            return true;
                        }
                    }
                }
                gm.endGame(winner);
                sender.sendMessage("§eゲームを強制終了しました。勝利陣営: " + winner);
            }

            case "settings" -> {
                if (!sender.hasPermission("jinro.rop.settings")) {
                    sender.sendMessage("§c権限がありません");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage("§cサブコマンドを指定してください");
                    return true;
                }

                String sub1 = args[1].toLowerCase();
                String sub2 = (args.length >= 3) ? args[2].toLowerCase() : null;
                sender.sendMessage("§a設定を変更しました: " + sub1 + ((sub2 != null) ? " " + sub2 : ""));
            }

            case "rop" -> {
                if (!sender.hasPermission("jinro.rop.manage")) {
                    sender.sendMessage("§c権限がありません");
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage("§c/jinro_rop rop <player> [add/remove/getop/reop/list]");
                    return true;
                }

                String targetName = args[1];
                Player target = Bukkit.getPlayer(targetName);
                if (target == null) {
                    sender.sendMessage("§cそのプレイヤーはオンラインではありません");
                    return true;
                }

                String action = args[2].toLowerCase();
                switch (action) {
                    case "add" -> {
                        gm.addRop(target);
                        sender.sendMessage("§a" + target.getName() + " をROPに追加しました");
                    }
                    case "remove" -> {
                        gm.removeRop(target);
                        sender.sendMessage("§a" + target.getName() + " をROPから削除しました");
                    }
                    case "getop" -> {
                        gm.getOp(target);
                        sender.sendMessage("§a" + target.getName() + " をOPにしました");
                    }
                    case "reop" -> {
                        gm.revokeOp(target);
                        sender.sendMessage("§a" + target.getName() + " のOPを剥奪しました");
                    }
                    case "list" -> {
                        List<String> ropList = gm.listRopPlayers((sender instanceof Player p) ? p : null);
                        sender.sendMessage("§aROP一覧: " + String.join(", ", ropList));
                    }
                    default -> sender.sendMessage("§c無効なROPサブコマンドです");
                }
            }

            default -> sender.sendMessage("§c不明なサブコマンドです");
        }

        return true;
    }
}