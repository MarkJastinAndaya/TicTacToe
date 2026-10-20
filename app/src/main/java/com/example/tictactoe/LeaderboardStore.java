package com.example.tictactoe;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class LeaderboardStore {

    private static final String TAG = "LeaderboardStore";

    private static LeaderboardStore instance;

    private final DatabaseHelper helper;

    // Reads and writes execute in submission order.
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private LeaderboardStore(Context context) {
        helper = new DatabaseHelper(
                context.getApplicationContext()
        );
    }

    public static synchronized LeaderboardStore getInstance(
            Context context
    ) {
        if (instance == null) {
            instance = new LeaderboardStore(context);
        }

        return instance;
    }

    public void recordRound(
            String roundId,
            String playerOne,
            String playerTwo,
            int winner,
            Consumer<Boolean> callback
    ) {
        executor.execute(() -> {
            boolean success;

            try {
                SQLiteDatabase db =
                        helper.getWritableDatabase();

                // Repeated round IDs are ignored.
                db.execSQL(
                        "INSERT OR IGNORE INTO rounds "
                                + "(id, player_one, player_two, winner) "
                                + "VALUES (?, ?, ?, ?)",
                        new Object[]{
                                roundId,
                                playerOne,
                                playerTwo,
                                winner
                        }
                );

                success = true;
            } catch (SQLiteException exception) {
                Log.e(TAG, "Could not save round", exception);
                success = false;
            }

            final boolean result = success;

            mainHandler.post(
                    () -> callback.accept(result)
            );
        });
    }

    public void loadLeaderboard(
            Consumer<List<Entry>> callback
    ) {
        executor.execute(() -> {
            List<Entry> entries = new ArrayList<>();

            String sql =
                    "SELECT name, "
                            + "SUM(won) AS wins, "
                            + "SUM(drawn) AS draws, "
                            + "SUM(lost) AS losses "
                            + "FROM ("
                            + "SELECT player_one AS name, "
                            + "CASE WHEN winner = 1 THEN 1 ELSE 0 END AS won, "
                            + "CASE WHEN winner = 0 THEN 1 ELSE 0 END AS drawn, "
                            + "CASE WHEN winner = 2 THEN 1 ELSE 0 END AS lost "
                            + "FROM rounds "
                            + "UNION ALL "
                            + "SELECT player_two AS name, "
                            + "CASE WHEN winner = 2 THEN 1 ELSE 0 END AS won, "
                            + "CASE WHEN winner = 0 THEN 1 ELSE 0 END AS drawn, "
                            + "CASE WHEN winner = 1 THEN 1 ELSE 0 END AS lost "
                            + "FROM rounds"
                            + ") "
                            + "GROUP BY name COLLATE NOCASE "
                            + "ORDER BY wins DESC, draws DESC, "
                            + "name COLLATE NOCASE ASC";

            try {
                SQLiteDatabase db =
                        helper.getReadableDatabase();

                try (Cursor cursor = db.rawQuery(sql, null)) {
                    while (cursor.moveToNext()) {
                        entries.add(
                                new Entry(
                                        cursor.getString(0),
                                        cursor.getInt(1),
                                        cursor.getInt(2),
                                        cursor.getInt(3)
                                )
                        );
                    }
                }

                mainHandler.post(
                        () -> callback.accept(entries)
                );
            } catch (SQLiteException exception) {
                Log.e(
                        TAG,
                        "Could not load leaderboard",
                        exception
                );

                // Null means loading failed; an empty list
                // means no completed rounds exist.
                mainHandler.post(
                        () -> callback.accept(null)
                );
            }
        });
    }

    public static final class Entry {

        public final String name;
        public final int wins;
        public final int draws;
        public final int losses;

        public Entry(
                String name,
                int wins,
                int draws,
                int losses
        ) {
            this.name = name;
            this.wins = wins;
            this.draws = draws;
            this.losses = losses;
        }
    }

    private static final class DatabaseHelper
            extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(
                    context,
                    "tic_tac_toe_leaderboard.db",
                    null,
                    1
            );
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE rounds ("
                            + "id TEXT PRIMARY KEY NOT NULL, "
                            + "player_one TEXT NOT NULL, "
                            + "player_two TEXT NOT NULL, "
                            + "winner INTEGER NOT NULL "
                            + "CHECK(winner BETWEEN 0 AND 2)"
                            + ")"
            );
        }

        @Override
        public void onUpgrade(
                SQLiteDatabase db,
                int oldVersion,
                int newVersion
        ) {
            // Add migrations here if the database version
            // is increased in a future update.
        }
    }
}