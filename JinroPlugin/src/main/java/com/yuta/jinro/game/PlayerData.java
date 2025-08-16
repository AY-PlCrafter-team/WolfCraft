package com.yuta.jinro.game;

import java.util.UUID;

/**
 * プレイヤーごとの状態管理クラス
 * 役職、所持金、生存状態、観戦状態などを管理
 */
public class PlayerData {

    private final UUID uuid;       // プレイヤーUUID
    private Role role;             // プレイヤー役職
    private boolean alive;         // 生存状態
    private boolean spectating;    // 観戦状態
    private int money;             // M通貨

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.alive = true;
        this.spectating = false;
        this.money = 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isAlive() {
        return alive;
    }

    /**
     * プレイヤーの生存状態を設定
     * 死亡した場合は自動的に観戦状態に切り替える
     */
    public void setAlive(boolean alive) {
        this.alive = alive;
        if (!alive) {
            this.spectating = true;
        }
    }

    public boolean isSpectating() {
        return spectating;
    }

    public void setSpectating(boolean spectating) {
        this.spectating = spectating;
    }

    public int getMoney() {
        return money;
    }

    public void addMoney(int amount) {
        if (amount <= 0) return;
        this.money += amount;
    }

    public void removeMoney(int amount) {
        if (amount <= 0) return;
        this.money -= amount;
        if (this.money < 0) this.money = 0;
    }

    /**
     * プレイヤー情報の文字列を返す
     */
    public String getInfo() {
        String roleName = role != null ? role.name() : "不明";
        return "役職: " + roleName + " | お金: " + money + "M | " + (alive ? "生存中" : "死亡");
    }
}