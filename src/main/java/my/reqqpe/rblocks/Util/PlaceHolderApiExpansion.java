package my.reqqpe.rblocks.Util;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import my.reqqpe.rblocks.Main;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PlaceHolderApiExpansion extends PlaceholderExpansion {
    private final Main plugin;
    private final BackPack backPack;
    private final DataBaseManager dataBaseManager;

    public PlaceHolderApiExpansion(Main plugin, BackPack backPack, DataBaseManager dataBaseManager) {
        this.plugin = plugin;
        this.backPack = backPack;
        this.dataBaseManager = dataBaseManager;
    }



    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors()); //
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "block";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion(); //
    }

    @Override
    public boolean persist() {
        return true; //
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        if (params.startsWith("backpack_")) {
            return handleBackpackPlaceholder(player, params.substring(9));
        } else if (params.startsWith("break_")) {
            return handleBreakPlaceholder(player, params.substring(6));
        }

        return null;
    }
    private String handleBackpackPlaceholder(OfflinePlayer player, String sub) {
        // Пример: %block_backpack_max%
        switch (sub) {
            case "max":
                int maxCapacity = 0;
                for (String permission : plugin.getConfig().getConfigurationSection("backpack.max_value").getKeys(true)) {
                    if (player.getPlayer().hasPermission(permission)) {
                        String configPath = "backpack.max_value." + permission;
                        int capacity = plugin.getConfig().getInt(configPath);
                        maxCapacity = Math.max(maxCapacity, capacity);
                    }
                }
                return String.valueOf(maxCapacity);
            case "current":
                return String.valueOf(backPack.getBackpackFill(player.getUniqueId()));
            default:
                return null; // Неизвестный параметр
        }
    }

    // Обработка плейсхолдеров, связанных с блоками
    private String handleBreakPlaceholder(OfflinePlayer player, String sub) {
        String uuid = player.getUniqueId().toString();

        try (Connection connection = dataBaseManager.getConnection()) {
            switch (sub) {
                case "local":
                    return String.valueOf(getLocalBlocks(connection, uuid));
                case "global":
                    return String.valueOf(getGlobalBlocks(connection, uuid));
                default:
                    return null; // Неизвестный параметр
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Ошибка!";
        }
    }

    private int getLocalBlocks(Connection connection, String uuid) throws SQLException {
        String query = "SELECT local_blocks FROM player_blocks WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("local_blocks");
                }
            }
        }
        return 0;
    }

    private int getGlobalBlocks(Connection connection, String uuid) throws SQLException {
        String query = "SELECT global_blocks FROM player_blocks WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("global_blocks");
                }
            }
        }
        return 0;
    }
}

