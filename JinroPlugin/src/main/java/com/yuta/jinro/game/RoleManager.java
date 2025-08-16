package com.yuta.jinro.game;

import java.util.*;

public class RoleManager {

    private static RoleManager instance;

    public static RoleManager getInstance() {
        if (instance == null) instance = new RoleManager();
        return instance;
    }

    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    private RoleManager() {}

    public void registerPlayer(UUID uuid) {
        playerDataMap.putIfAbsent(uuid, new PlayerData(uuid));
    }

    public void unregisterPlayer(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public void setRole(UUID uuid, Role role) {
        PlayerData pd = playerDataMap.get(uuid);
        if (pd != null) pd.setRole(role);
    }

    /**
     * 自動で役職振り分け（参加人数に応じたテンプレートを取得）
     * 余りは村人で補完
     * 返り値: 割り当てられた役職リスト
     */
    public List<Role> autoAssignRoles(int playerCount) {
        List<PlayerData> players = new ArrayList<>(playerDataMap.values());
        Collections.shuffle(players);

        LinkedHashMap<Role, Integer> template = RoleDefault.getNearestTemplate(playerCount);
        Map<Role, Integer> assignedCounts = new HashMap<>();
        template.keySet().forEach(r -> assignedCounts.put(r, 0));

        List<Role> assignedRoles = new ArrayList<>();

        for (PlayerData pd : players) {
            Role assigned = null;
            for (Role role : template.keySet()) {
                int max = template.get(role);
                int assignedNum = assignedCounts.get(role);
                if (assignedNum < max) {
                    assigned = role;
                    assignedCounts.put(role, assignedNum + 1);
                    break;
                }
            }
            if (assigned == null)
                assigned = Role.MURABITO;
            pd.setRole(assigned);
            assignedRoles.add(assigned);
        }

        // プレイヤー数がテンプレより多い場合、不足分を村人で補完
        while (assignedRoles.size() < playerCount) {
            assignedRoles.add(Role.MURABITO);
        }

        return assignedRoles;
    }

    public List<PlayerData> getAlivePlayers() {
        List<PlayerData> alive = new ArrayList<>();
        for (PlayerData pd : playerDataMap.values()) {
            if (pd.isAlive()) alive.add(pd);
        }
        return alive;
    }

    public Set<UUID> getAllPlayerUUIDs() {
        return new HashSet<>(playerDataMap.keySet());
    }

    public boolean isSpectating(UUID uuid) {
        PlayerData pd = playerDataMap.get(uuid);
        return pd != null && pd.isSpectating();
    }
}