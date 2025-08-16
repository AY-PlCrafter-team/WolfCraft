package com.yuta.jinro.shop;

import com.yuta.jinro.game.PlayerData;
import com.yuta.jinro.game.RoleManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopManager implements Listener {

    private static ShopManager instance;

    public static ShopManager getInstance() {
        if (instance == null) {
            instance = new ShopManager();
        }
        return instance;
    }

    private ShopManager() {}

    // 材料の販売価格
    private final Map<Material, Integer> sellPrices = Map.of(
            Material.COAL, 5,
            Material.IRON_ORE, 20,
            Material.GOLD_ORE, 50,
            Material.DIAMOND, 100,
            Material.EMERALD, 200
    );

    // 開いたプレイヤーのショップインベントリ管理
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    // プレイヤーが鉱石を入れたら自動でお金に
    public void openShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, "鉱石をここに入れて売る");
        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        UUID uuid = player.getUniqueId();
        Inventory inv = openInventories.get(uuid);
        if (inv != null && e.getInventory().equals(inv)) {
            e.setCancelled(true); // アイテムの取り出し禁止
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        UUID uuid = player.getUniqueId();
        Inventory inv = openInventories.get(uuid);
        if (inv != null && e.getInventory().equals(inv)) {
            int totalMoney = 0;
            for (ItemStack item : inv.getContents()) {
                if (item == null) continue;
                Material mat = item.getType();
                if (sellPrices.containsKey(mat)) {
                    int price = sellPrices.get(mat);
                    totalMoney += price * item.getAmount();
                }
            }
            PlayerData pd = RoleManager.getInstance().getPlayerData(uuid);
            if (pd != null) {
                pd.addMoney(totalMoney);
            }
            player.sendMessage("§a売却完了！ " + totalMoney + " Mを獲得しました。");
            openInventories.remove(uuid);
        }
    }
}