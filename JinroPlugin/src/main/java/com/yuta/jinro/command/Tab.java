package com.yuta.jinro.command;

import com.yuta.jinro.game.Role;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Tab implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // -------------------------------
        // /jinro 系
        // -------------------------------
        if (command.getName().equalsIgnoreCase("jinro")) {
            if (args.length == 1) {
                completions = Arrays.asList("sell", "spec", "vote", "info", "list", "money");
            } else if (args.length == 2) {
                switch (args[0].toLowerCase()) {
                    case "vote":
                        completions = Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .collect(Collectors.toList());
                        break;
                    case "roles":
                        completions = Arrays.stream(Role.values())
                                .map(Role::name)
                                .collect(Collectors.toList());
                        break;
                }
            }
        }

        // -------------------------------
        // /jinro_admin & /jad 系
        // -------------------------------
        else if (command.getName().equalsIgnoreCase("jinro_admin") ||
                 command.getName().equalsIgnoreCase("jad")) {
            if (args.length == 1) {
                completions = Arrays.asList("start", "stop", "rop", "chat");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("rop")) {
                    completions = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("rop")) {
                completions = Arrays.asList("add", "remove", "getop", "reop", "list");
            }
        }

        // -------------------------------
        // /jinro_rop & /jrop 系
        // -------------------------------
        else if (command.getName().equalsIgnoreCase("jinro_rop") ||
                 command.getName().equalsIgnoreCase("jrop")) {
            if (args.length == 1) {
                completions = Arrays.asList("start", "stop", "settings");
            } else if (args.length == 2) {
                switch (args[0].toLowerCase()) {
                    case "stop":
                        completions = Arrays.asList("murabito", "jinrou", "uragiri");
                        break;
                    case "settings":
                        completions = Arrays.asList("role", "time", "limit");
                        break;
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("settings")) {
                completions = Arrays.asList("easy", "normal", "hard");
            }
        }

        // 部分一致で絞り込み
        if (args.length > 0) {
            String lastWord = args[args.length - 1].toLowerCase();
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(lastWord))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}