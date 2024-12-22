package my.reqqpe.rblocks.Util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataBaseManager {
    private Connection connection;

    public void connect() throws SQLException {

        // Путь к базе данных
        File pluginFolder = new File("plugins/rBlocks");
        if (!pluginFolder.exists()) {
            if (pluginFolder.mkdirs()) {
                System.out.println("Папка rBlocks была создана.");
            } else {
                throw new SQLException("Не удалось создать папку для базы данных.");
            }
        }

        String url = "jdbc:sqlite:" + pluginFolder.getPath() + "/blocks.db";
        connection = DriverManager.getConnection(url);
        createTable();
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS player_blocks (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "player_name VARCHAR(16), " +
                "local_blocks INT DEFAULT 0, " +
                "global_blocks  INT DEFAULT 0)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect(); // Переподключаемся, если соединение закрыто
        }
        return connection;
    }

    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
