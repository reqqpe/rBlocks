package my.reqqpe.rblocks.Event;

import my.reqqpe.rblocks.Main;
import my.reqqpe.rblocks.Util.BackPack;
import my.reqqpe.rblocks.Util.DataBaseManager;
import my.reqqpe.rblocks.Util.HexColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class BlockBreak implements Listener {
    private final Main plugin;
    private final BackPack backPack;
    private Connection connection;
    private final DataBaseManager dataBaseManager;

    public BlockBreak(Main plugin, BackPack backPack, DataBaseManager dataBaseManager) {
        this.plugin = plugin;
        this.backPack = backPack;
        this.dataBaseManager = dataBaseManager;
    }

    @EventHandler
    private void onBlockBreak(BlockBreakEvent e) {
        Material block = e.getBlock().getType();
        Player player = e.getPlayer();
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        UUID uuid = player.getUniqueId();

        if (player.hasPermission(Objects.requireNonNull(plugin.getConfig().getString("permission.bypass")))) return;

        if (plugin.getConfig().contains("backpack.block_value." + block)) {
            e.setCancelled(true);

            boolean breakTool = false;
            boolean regionCheck = false;
            for (String tool :  plugin.getConfig().getConfigurationSection("tools").getKeys(false)) {

                Material toolMaterial = Material.valueOf(plugin.getConfig().getString("tools." + tool + ".material"));
                String toolName = plugin.getConfig().getString("tools." + tool + ".name");

                // Проверка на предмет из конфига с предметом из руки
                assert toolName != null;
                if (plugin.isTool(itemStack, toolMaterial, toolName)) {

                    // Проверка эффекты
                    plugin.removeEffect(player, tool);

                    // Проверка на то что кирка может ломать этот блок
                    if (plugin.isBlockTool(tool, block)) {
                        breakTool = true;
                    }
                }
            }
            for (String region : plugin.getConfig().getStringList("regions")) {
                if (plugin.isBlockInRegion(e.getBlock(), region)) {
                    regionCheck = true;
                }
            }
            if (!breakTool) {
                player.sendMessage(HexColor.color(getConfString("messages.no-pickaxe")));
                return;
            }
            if (!regionCheck) {
                player.sendMessage(HexColor.color(getConfString("messages.no-region")));
                return;
            }

            int getAmountAdd = plugin.getConfig().getInt("backpack.block_value." + block);
            int maxBackpack = 0;

            for (String permission : plugin.getConfig().getConfigurationSection("backpack.max_value").getKeys(true)) {
                if (player.hasPermission(permission)) {
                    maxBackpack = Math.max(maxBackpack, plugin.getConfig().getInt("backpack.max_value." + permission));
                }
            }
            if (maxBackpack == 0) {
                player.sendMessage(HexColor.color(getConfString("messages.no-backpack")));
                return;
            }

            // Получаем текущее кол-во
            int currentAmount = backPack.getBackpackFill(uuid);
            int addEnd = currentAmount + getAmountAdd;// Число, которое получилось после прибавления
            setBackPack(maxBackpack, addEnd, currentAmount, player, String.valueOf(getAmountAdd), e.getBlock());

            try (Connection connection = dataBaseManager.getConnection()) {
                // Проверка, есть ли запись об игроке
                if (!playerExists(connection, String.valueOf(uuid))) {
                    insertPlayer(connection, String.valueOf(uuid), player.getName());
                }

                // Обновление статистики
                incrementBlockCounters(String.valueOf(uuid));
            } catch (SQLException event) {
                event.printStackTrace();
            }
        }
    }


    private void setBackPack(Integer maxBackpack, Integer addEnd, Integer currentAmount, Player player, String getAmountAdd, Block block) {
        if (currentAmount == maxBackpack) {
            player.sendMessage(HexColor.color(plugin.getConfig().getString("messages.backpack-full")));
        }
        else if (addEnd < maxBackpack) {
            block.setType(Material.AIR);
            backPack.addValue(player.getUniqueId(), addEnd);
            String message = getConfString("messages.backpack-nofull")
                    .replace("{amount}", getAmountAdd);
            player.sendMessage(HexColor.color(message));
        }
        else if (addEnd >= maxBackpack) {
            block.setType(Material.AIR);
            backPack.addValue(player.getUniqueId(), maxBackpack);
            player.sendMessage(HexColor.color(getConfString("messages.backpack-full")));
        }
    }


    private String getConfString(String string) {
        return plugin.getConfig().getString(string);
    }
    private boolean playerExists(Connection connection, String uuid) throws SQLException {
        String query = "SELECT 1 FROM player_blocks WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insertPlayer(Connection connection, String uuid, String playerName) throws SQLException {
        String insert = "INSERT INTO player_blocks (uuid, player_name) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setString(1, uuid);
            stmt.setString(2, playerName);
            stmt.executeUpdate();
        }
    }

    private void incrementBlockCounters(String uuid) {
        String update = "UPDATE player_blocks " +
                "SET local_blocks = local_blocks + 1, global_blocks = global_blocks + 1 " +
                "WHERE uuid = ?";
        try (Connection connection = dataBaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(update)) {
            stmt.setString(1, uuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
