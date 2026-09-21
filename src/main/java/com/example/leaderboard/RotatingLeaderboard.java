package com.example.leaderboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.stream.Collectors;

public class RotatingLeaderboard extends JavaPlugin implements Listener {

    private int currentBoardIndex = 0;
    
    // Only 3 categories now (Blocks Destroyed removed)
    private final String[] boardTitles = {
            ChatColor.GOLD + "" + ChatColor.BOLD + "TOP KILLS",
            ChatColor.GREEN + "" + ChatColor.BOLD + "TOP TIME PLAYED",
            ChatColor.RED + "" + ChatColor.BOLD + "MOST DEATHS"
    };

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        // Run every 10 seconds (200 ticks) continuously
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayerScoreboard(player);
            }
            // Continuous loop: 0 -> 1 -> 2 -> 0 -> 1 -> 2...
            currentBoardIndex = (currentBoardIndex + 1) % boardTitles.length;
        }, 0L, 200L);

        getLogger().info("RotatingLeaderboard enabled with 3 rotating categories!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerScoreboard(event.getPlayer());
    }

    private void updatePlayerScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = player.getScoreboard();
        if (board.equals(manager.getMainScoreboard())) {
            board = manager.getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective objective = board.getObjective("rot_board");
        
        // Register objective if it doesn't exist yet
        if (objective == null) {
            objective = board.registerNewObjective("rot_board", "dummy", boardTitles[currentBoardIndex]);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            // Clear existing score entries before writing new ones
            for (String entry : board.getEntries()) {
                board.resetScores(entry);
            }
            // Update title to current category
            objective.setDisplayName(boardTitles[currentBoardIndex]);
        }

        List<StatEntry> topPlayers = getTopPlayersForCategory(currentBoardIndex);

        int scoreIndex = topPlayers.size();
        for (StatEntry entry : topPlayers) {
            String lineText = ChatColor.YELLOW + entry.name + ChatColor.WHITE + ": " + ChatColor.GREEN + entry.formattedValue;
            
            if (lineText.length() > 40) {
                lineText = lineText.substring(0, 40);
            }

            Score score = objective.getScore(lineText);
            score.setScore(scoreIndex);
            scoreIndex--;
        }
    }

    private List<StatEntry> getTopPlayersForCategory(int categoryIndex) {
        OfflinePlayer[] players = Bukkit.getOfflinePlayers();
        List<StatEntry> entries = new ArrayList<>();

        for (OfflinePlayer op : players) {
            if (op.getName() == null) continue;

            long value = 0;
            String formattedValue = "0";

            switch (categoryIndex) {
                case 0: // Top Kills
                    value = op.getStatistic(Statistic.PLAYER_KILLS);
                    formattedValue = String.valueOf(value);
                    break;
                case 1: // Time Played
                    long ticks = op.getStatistic(Statistic.PLAY_ONE_MINUTE);
                    long totalSeconds = ticks / 20;
                    long hours = totalSeconds / 3600;
                    long minutes = (totalSeconds % 3600) / 60;
                    value = totalSeconds;
                    formattedValue = hours + "h " + minutes + "m";
                    break;
                case 2: // Most Deaths
                    value = op.getStatistic(Statistic.DEATHS);
                    formattedValue = String.valueOf(value);
                    break;
            }

            if (value >= 0) {
                entries.add(new StatEntry(op.getName(), value, formattedValue));
            }
        }

        return entries.stream()
                .sorted((a, b) -> Long.compare(b.rawValue, a.rawValue))
                .limit(10)
                .collect(Collectors.toList());
    }

    private static class StatEntry {
        String name;
        long rawValue;
        String formattedValue;

        StatEntry(String name, long rawValue, String formattedValue) {
            this.name = name;
            this.rawValue = rawValue;
            this.formattedValue = formattedValue;
        }
    }
}
