package com.avengers3d.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Top-5 high scores and player names persisted to scores.txt. */
public final class ScoreBoard {
    private final Path file = Path.of("scores.txt");
    private final int[] top = new int[5];
    private final String[] names = {"---", "---", "---", "---", "---"};

    public int[] top() { return top; }
    public String name(int rank) { return names[rank]; }

    public void load() {
        try {
            if (!Files.exists(file)) return;
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < top.length && i < lines.size(); i++) {
                String[] entry = lines.get(i).split("\t", 2);
                top[i] = Integer.parseInt(entry[0].trim());
                if (entry.length == 2 && !entry[1].trim().isEmpty()) names[i] = entry[1].trim();
            }
        } catch (IOException | NumberFormatException ignored) { /* a corrupt score file is not fatal */ }
    }

    /** @return 0-based rank if the score made the table, else -1. */
    public int submit(int score) {
        return submit(score, "PLAYER");
    }

    /** @return 0-based rank if the score made the table, else -1. */
    public int submit(int score, String playerName) {
        if (score <= 0) return -1;
        String safeName = playerName == null ? "PLAYER" : playerName.trim();
        if (safeName.isEmpty()) safeName = "PLAYER";
        if (safeName.length() > 16) safeName = safeName.substring(0, 16);
        int rank = -1;
        for (int i = 0; i < top.length; i++) if (score > top[i]) { rank = i; break; }
        if (rank < 0) return -1;
        for (int i = top.length - 1; i > rank; i--) top[i] = top[i - 1];
        for (int i = names.length - 1; i > rank; i--) names[i] = names[i - 1];
        top[rank] = score;
        names[rank] = safeName;
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < top.length; i++) sb.append(top[i]).append('\t').append(names[i]).append('\n');
            Files.writeString(file, sb.toString());
        } catch (IOException ignored) { }
        return rank;
    }
}
