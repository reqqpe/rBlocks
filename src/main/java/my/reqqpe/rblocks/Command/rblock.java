package my.reqqpe.rblocks.Command;

import my.reqqpe.rblocks.Main;
import my.reqqpe.rblocks.Util.BackPack;
import my.reqqpe.rblocks.Util.DataBaseManager;
import my.reqqpe.rblocks.Util.HexColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class rblock implements CommandExecutor {
    private final Main plugin;
    private final DataBaseManager dataBaseManager;
    private final BackPack backPack;

    public rblock(Main plugin, DataBaseManager dataBaseManager, BackPack backPack) {
        this.plugin = plugin;
        this.dataBaseManager = dataBaseManager;
        this.backPack = backPack;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        String no_permission = plugin.getConfig().getString("messages.no-permission", "");
        if (!commandSender.hasPermission(plugin.getConfig().getString("permission.rblock", "rblock"))) {
            commandSender.sendMessage(HexColor.color(no_permission));
            return false;
        }
        if (args.length == 0) {
            commandSender.sendMessage(HexColor.color("&cИспользование /rblock help"));
            return false;
        }
        if (args[0].equalsIgnoreCase("help")) {
            if (!commandSender.hasPermission(plugin.getConfig().getString("permission.help", "rblock.help"))) {
                commandSender.sendMessage(HexColor.color(no_permission));
                return false;
            }
            for (String message: plugin.getConfig().getStringList("messages.help")) {
                if (message != null) {
                    commandSender.sendMessage(HexColor.color(message));
                }
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!commandSender.hasPermission(plugin.getConfig().getString("permission.reload", "rblock.reload"))) {
                commandSender.sendMessage(HexColor.color(no_permission));
                return false;
            }
            plugin.saveDefaultConfig();
            plugin.reloadConfig();

            try {
                dataBaseManager.disconnect();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            try {
                dataBaseManager.connect();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            commandSender.sendMessage(HexColor.color(plugin.getConfig().getString("messages.reload")));
            return true;
        }
        if (args[0].equalsIgnoreCase("backpack_reset")) {
            if (!commandSender.hasPermission(plugin.getConfig().getString("permission.backpack_reset", "rblock.backpack_reset"))) {
                commandSender.sendMessage(HexColor.color(no_permission));
                return false;
            }
            if (args.length < 2) {
                commandSender.sendMessage(HexColor.color("&cИспользование /rblock help"));
                return false;
            }

            String playerName = args[1];
            Player player = Bukkit.getPlayer(playerName);

            if (player == null || !player.isOnline()) {
                commandSender.sendMessage(HexColor.color(plugin.getConfig().getString("messages.no-player", "").replace("{player}", playerName)));
                return false;
            }
            if (!backPack.isPlayer(player.getUniqueId())) {
                return false;
            }

            backPack.getBackpack().remove(player.getUniqueId());
            return true;
        }
        if (args[0].equalsIgnoreCase("database_reset")) {
            if (!commandSender.hasPermission(plugin.getConfig().getString("permission.database", "rblock.database"))) {
                commandSender.sendMessage(HexColor.color(no_permission));
            }
            if (args.length < 2) {
                commandSender.sendMessage(HexColor.color("&cИспользование /rblock help"));
                return false;
            }
            String playerName = args[1];
            Player player = Bukkit.getPlayer(playerName);

            if (player == null || !player.isOnline()) {
                commandSender.sendMessage(HexColor.color(plugin.getConfig().getString("messages.no-player", "").replace("{player}", playerName)));
                return false;
            }
            try (Connection connection = dataBaseManager.getConnection()) {
                if (resetLocalBlocksForPlayer(connection, playerName)) {
                    commandSender.sendMessage(HexColor.color(plugin.getConfig().getString("messages.reset-local-block", "").replace("{player}", playerName)));
                } else {
                    commandSender.sendMessage(HexColor.color(plugin.getConfig().getString("messages.no-player-database", "").replace("{player}", playerName)));
                }
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }

        return false;
    }
    private boolean resetLocalBlocksForPlayer(Connection connection, String playerName) throws SQLException {
        String update = "UPDATE player_blocks SET local_blocks = 0 WHERE player_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(update)) {
            stmt.setString(1, playerName);
            return stmt.executeUpdate() > 0; // Возвращает true, если запись обновлена
        }
    }
}
