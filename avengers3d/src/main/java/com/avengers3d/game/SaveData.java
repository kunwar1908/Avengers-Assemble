package com.avengers3d.game;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Player progress and the top-5 arena scoreboard, stored in MySQL. */
public final class SaveData {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/avengers3d?serverTimezone=UTC";
    private final String jdbcUrl = System.getenv("AVENGERS_DB_URL") == null
            ? DEFAULT_URL : System.getenv("AVENGERS_DB_URL");

    public String playerName = "";
    public int unlocked = 1;                       // highest story chapter available
    public boolean completed;
    public final int[] topScore = new int[5];
    public final String[] topName = {"---", "---", "---", "---", "---"};

    public void load() {
        try (Connection connection = connect()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT player_name, unlocked, completed FROM game_save WHERE id = 1");
                 ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    playerName = result.getString("player_name");
                    unlocked = Math.max(1, Math.min(4, result.getInt("unlocked")));
                    completed = result.getBoolean("completed");
                }
            }
            applyScores(readTopScores(connection));
        } catch (SQLException e) {
            throw databaseError("load save data", e);
        }
    }

    public void save() {
        String sql = "INSERT INTO game_save (id, player_name, unlocked, completed) VALUES (1, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), "
                + "unlocked = VALUES(unlocked), completed = VALUES(completed)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerName);
            statement.setInt(2, unlocked);
            statement.setBoolean(3, completed);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("save progress", e);
        }
    }

    public void unlock(int chapter) {
        if (chapter > unlocked) { unlocked = Math.min(4, chapter); save(); }
    }

    /** @return 0-based rank if the score entered the table, else -1. */
    public int submitScore(String name, int score) {
        if (score <= 0) return -1;
        String scoreName = name == null || name.isBlank() ? "PLAYER" : name.trim();
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                int rank;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM leaderboard WHERE score >= ?")) {
                    statement.setInt(1, score);
                    try (ResultSet result = statement.executeQuery()) {
                        result.next();
                        rank = result.getInt(1);
                    }
                }
                if (rank >= topScore.length) {
                    connection.commit();
                    return -1;
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO leaderboard (player_name, score) VALUES (?, ?)")) {
                    statement.setString(1, scoreName);
                    statement.setInt(2, score);
                    statement.executeUpdate();
                }

                List<ScoreEntry> scores = retainTopScores(connection);
                connection.commit();
                applyScores(scores);
                return rank;
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw databaseError("submit score", e);
        }
    }

    private Connection connect() throws SQLException {
        String user = System.getenv("AVENGERS_DB_USER");
        String password = System.getenv("AVENGERS_DB_PASSWORD");
        if (user == null || user.isBlank() || password == null) {
            throw new IllegalStateException(
                    "Set AVENGERS_DB_USER and AVENGERS_DB_PASSWORD before starting the game.");
        }
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    private List<ScoreEntry> readTopScores(Connection connection) throws SQLException {
        List<ScoreEntry> scores = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT player_name, score FROM leaderboard ORDER BY score DESC, id ASC LIMIT 5")) {
            while (result.next()) {
                scores.add(new ScoreEntry(result.getString("player_name"), result.getInt("score")));
            }
        }
        return scores;
    }

    private List<ScoreEntry> retainTopScores(Connection connection) throws SQLException {
        List<ScoreEntry> scores = new ArrayList<>(topScore.length);
        List<Long> excessIds = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT id, player_name, score FROM leaderboard ORDER BY score DESC, id ASC")) {
            while (result.next()) {
                if (scores.size() < topScore.length) {
                    scores.add(new ScoreEntry(result.getString("player_name"), result.getInt("score")));
                } else {
                    excessIds.add(result.getLong("id"));
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM leaderboard WHERE id = ?")) {
            for (long id : excessIds) {
                statement.setLong(1, id);
                statement.addBatch();
            }
            statement.executeBatch();
        }
        return scores;
    }

    private void applyScores(List<ScoreEntry> scores) {
        Arrays.fill(topScore, 0);
        Arrays.fill(topName, "---");
        for (int i = 0; i < scores.size(); i++) {
            topScore[i] = scores.get(i).score();
            topName[i] = scores.get(i).name();
        }
    }

    private IllegalStateException databaseError(String operation, SQLException cause) {
        return new IllegalStateException(
                "Could not " + operation + " in MySQL. Check the AVENGERS_DB_* settings, "
                        + "that MySQL is running, and that the avengers3d schema has been created.",
                cause);
    }

    private record ScoreEntry(String name, int score) {}
}
