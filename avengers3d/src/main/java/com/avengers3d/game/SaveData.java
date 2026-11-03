package com.avengers3d.game;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Player name, story progress and the top-5 arena scoreboard, stored in savegame.properties. */
public final class SaveData {
    private final Path file = Path.of("savegame.properties");
    public String playerName = "";
    public int unlocked = 1;                       // highest story chapter available
    public boolean completed;
    public final int[] topScore = new int[5];
    public final String[] topName = {"---", "---", "---", "---", "---"};

    public void load() {
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file)) {
            Properties p = new Properties();
            p.load(r);
            playerName = p.getProperty("name", "");
            unlocked = Math.max(1, Math.min(4, Integer.parseInt(p.getProperty("unlocked", "1"))));
            completed = Boolean.parseBoolean(p.getProperty("completed", "false"));
            for (int i = 0; i < 5; i++) {
                topScore[i] = Integer.parseInt(p.getProperty("score" + i, "0"));
                topName[i] = p.getProperty("sname" + i, "---");
            }
        } catch (IOException | NumberFormatException ignored) { /* a damaged save is not fatal */ }
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("name", playerName);
        p.setProperty("unlocked", Integer.toString(unlocked));
        p.setProperty("completed", Boolean.toString(completed));
        for (int i = 0; i < 5; i++) { p.setProperty("score" + i, Integer.toString(topScore[i])); p.setProperty("sname" + i, topName[i]); }
        try (Writer w = Files.newBufferedWriter(file)) { p.store(w, "Marvel Avengers 3D save"); } catch (IOException ignored) { }
    }

    public void unlock(int chapter) {
        if (chapter > unlocked) { unlocked = Math.min(4, chapter); save(); }
    }

    /** @return 0-based rank if the score entered the table, else -1. */
    public int submitScore(String name, int score) {
        if (score <= 0) return -1;
        int rank = -1;
        for (int i = 0; i < topScore.length; i++) if (score > topScore[i]) { rank = i; break; }
        if (rank < 0) return -1;
        for (int i = topScore.length - 1; i > rank; i--) { topScore[i] = topScore[i - 1]; topName[i] = topName[i - 1]; }
        topScore[rank] = score; topName[rank] = name.isEmpty() ? "PLAYER" : name;
        save();
        return rank;
    }
}
