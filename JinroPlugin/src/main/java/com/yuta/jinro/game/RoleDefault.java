package com.yuta.jinro.game;

import java.util.LinkedHashMap;
import java.util.Map;

public class RoleDefault {

    public static final Map<Integer, LinkedHashMap<Role, Integer>> ROLE_TEMPLATES = Map.of(
        2, new LinkedHashMap<>(Map.of(
            Role.MURABITO, 1,
            Role.JINROU, 1
        )),
        4, new LinkedHashMap<>(Map.of(
            Role.MURABITO, 2,
            Role.JINROU, 1,
            Role.URANAI, 1
        )),
        6, new LinkedHashMap<>(Map.of(
            Role.MURABITO, 3,
            Role.URANAI, 1,
            Role.KISHI, 1,
            Role.JINROU, 1
        )),
        7, new LinkedHashMap<>(Map.of(
            Role.MURABITO, 3,
            Role.URANAI, 1,
            Role.KISHI, 1,
            Role.JINROU, 1,
            Role.KYOJIN, 1
        )),
        8, new LinkedHashMap<>(Map.of(
            Role.MURABITO, 4,
            Role.URANAI, 1,
            Role.KISHI, 1,
            Role.REIMAI, 1,
            Role.JINROU, 1
        )),
        10, new LinkedHashMap<>(Map.of(
            Role.MURABITO, 5,
            Role.URANAI, 1,
            Role.KISHI, 1,
            Role.REIMAI, 1,
            Role.JINROU, 2,
            Role.KYOJIN, 1
        ))
    );

    public static LinkedHashMap<Role, Integer> getNearestTemplate(int playerCount) {
        return ROLE_TEMPLATES.entrySet().stream()
                .min((a, b) -> Math.abs(a.getKey() - playerCount) - Math.abs(b.getKey() - playerCount))
                .map(e -> e.getValue())
                .orElseThrow(() -> new IllegalArgumentException("適切なテンプレートがありません: " + playerCount));
    }
}