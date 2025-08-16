package com.yuta.jinro.game;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoleItemManager implements Listener {

    private final Map<UUID, ItemStack> roleItems = new HashMap<>();

    // 役職ごとにIDを振ったアイテム作成
    public ItemStack createRoleItem(String roleName, int roleId) {
        ItemStack item = new ItemStack(Material.BEDROCK); // 岩盤
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b" + roleName);
            meta.setCustomModelData(roleId); // ID振り
            item.setItemMeta(meta);
        }
        return item;
    }

    // プレイヤーに役職アイテムを配布（9番固定）
    public void giveRoleItem(Player player, String roleName, int roleId) {
        ItemStack item = createRoleItem(roleName, roleId);
        player.getInventory().setItem(8, item); // 9番スロット
        roleItems.put(player.getUniqueId(), item);
    }

    // 捨てられないようにキャンセル
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (roleItems.containsKey(e.getPlayer().getUniqueId()) &&
            e.getItemDrop().getItemStack().isSimilar(roleItems.get(e.getPlayer().getUniqueId()))) {
            e.setCancelled(true);
        }
    }

    // 右クリック判定などに使う例
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItem(8); // 9番スロット
        if (item != null && roleItems.containsKey(p.getUniqueId()) &&
            item.isSimilar(roleItems.get(p.getUniqueId()))) {

            // ここで役職アイテムを使った能力処理
            // 例: GameManager.getInstance().useAbility(p.getUniqueId());
            e.setCancelled(true); // 不要なら消す
        }
    }
}