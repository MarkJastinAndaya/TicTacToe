package com.example.tictactoe;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


public class GameViewModel extends ViewModel {

    public static final char EMPTY = '-';

    private static final String KEY_COUNTDOWN = "countdown_step";
    private static final String KEY_BOARD = "game_board";

    private static final int[][] WIN_LINES = {
            {0, 1, 2},
            {3, 4, 5},
            {6, 7, 8},

            {0, 3, 6},
            {1, 4, 7},
            {2, 5, 8},

            {0, 4, 8},
            {2, 4, 6}
    };

    private final SavedStateHandle savedState;
    private final MutableLiveData<Integer> countdownStep;
    private final MutableLiveData<String> board;

    private boolean singlePlayer;
    private boolean playerOneIsX;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final ScheduledExecutorService computerExecutor =
            Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> computerTask;

    private boolean gameActive;
    private boolean cleared;
    private long computerGeneration;

    public GameViewModel(SavedStateHandle savedState) {
        this.savedState = savedState;

        countdownStep = savedState.getLiveData(KEY_COUNTDOWN, 0);
        board = savedState.getLiveData(KEY_BOARD, "---------");
    }

    public void configure(boolean singlePlayer, boolean playerOneIsX) {
        this.singlePlayer = singlePlayer;
        this.playerOneIsX = playerOneIsX;
    }

    public LiveData<Integer> getCountdownStep() {
        return countdownStep;
    }

    public int getCurrentCountdownStep() {
        Integer step = countdownStep.getValue();
        return step == null ? 0 : step;
    }

    public boolean isCountdownComplete() {
        return getCurrentCountdownStep() >= 3;
    }

    public void advanceCountdown() {
        int step = getCurrentCountdownStep();

        if (step < 3) {
            savedState.set(KEY_COUNTDOWN, step + 1);
        }

        scheduleComputerMove();
    }

    public LiveData<String> getBoard() {
        return board;
    }

    public String getBoardValue() {
        String value = board.getValue();
        return value == null ? "---------" : value;
    }

    public char getCurrentSymbol() {
        String value = getBoardValue();
        int occupied = 0;

        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != EMPTY) {
                occupied++;
            }
        }

        return occupied % 2 == 0 ? 'X' : 'O';
    }

    public char getWinner() {
        String value = getBoardValue();

        String surrenderWinner =
                savedState.get("surrender_winner");

        if ("X".equals(surrenderWinner)
                || "O".equals(surrenderWinner)) {
            return surrenderWinner.charAt(0);
        }

        for (int[] line : WIN_LINES) {
            char symbol = value.charAt(line[0]);

            if (symbol != EMPTY
                    && symbol == value.charAt(line[1])
                    && symbol == value.charAt(line[2])) {
                return symbol;
            }
        }

        return EMPTY;
    }

    public boolean isDraw() {
        return getWinner() == EMPTY
                && getBoardValue().indexOf(EMPTY) == -1;
    }

    public boolean isFinished() {
        return getWinner() != EMPTY || isDraw();
    }

    public boolean[] getWinningTiles() {
        boolean[] winning = new boolean[9];

        if (savedState.contains("surrender_winner")) {
            return winning;
        }

        String value = getBoardValue();

        for (int[] line : WIN_LINES) {
            char symbol = value.charAt(line[0]);

            if (symbol != EMPTY
                    && symbol == value.charAt(line[1])
                    && symbol == value.charAt(line[2])) {
                for (int index : line) {
                    winning[index] = true;
                }
            }
        }

        return winning;
    }

    public boolean isComputerTurn() {
        if (!singlePlayer || !isCountdownComplete() || isFinished()) {
            return false;
        }

        char computerSymbol = playerOneIsX ? 'O' : 'X';
        return getCurrentSymbol() == computerSymbol;
    }

    public boolean canHumanMove() {
        return isCountdownComplete()
                && !isFinished()
                && !isComputerTurn();
    }

    public boolean playHumanMove(int index) {
        if (!gameActive || index < 0 || index >= 9
                || !canHumanMove()) {
            return false;
        }

        String currentBoard = getBoardValue();

        if (currentBoard.charAt(index) != EMPTY) {
            return false;
        }

        char[] updatedBoard = currentBoard.toCharArray();
        updatedBoard[index] = getCurrentSymbol();

        savedState.set(KEY_BOARD, new String(updatedBoard));

        scheduleComputerMove();
        return true;
    }

    public void setGameActive(boolean active) {
        if (cleared) {
            return;
        }

        gameActive = active;

        if (active) {
            scheduleComputerMove();
        } else {
            cancelComputerMove();
        }
    }

    private void scheduleComputerMove() {
        if (cleared || !gameActive || computerTask != null
                || !isComputerTurn()) {
            return;
        }

        // Capture immutable state before entering the worker thread.
        final String boardSnapshot = getBoardValue();
        final char computerSymbol = getCurrentSymbol();
        final long generation = ++computerGeneration;

        long delayMillis =
                ThreadLocalRandom.current().nextLong(800L, 1201L);

        computerTask = computerExecutor.schedule(() -> {
            int index = chooseComputerMove(
                    boardSnapshot,
                    computerSymbol
            );

            // SavedStateHandle and LiveData changes happen on the main thread.
            mainHandler.post(() -> {
                if (cleared || !gameActive
                        || generation != computerGeneration) {
                    return;
                }

                computerTask = null;

                // Reject results belonging to an outdated board.
                if (!isComputerTurn()
                        || !boardSnapshot.equals(getBoardValue())
                        || index < 0 || index >= 9
                        || getBoardValue().charAt(index) != EMPTY) {
                    return;
                }

                char[] updatedBoard =
                        getBoardValue().toCharArray();

                updatedBoard[index] = computerSymbol;

                savedState.set(
                        KEY_BOARD,
                        new String(updatedBoard)
                );
            });
        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    private void cancelComputerMove() {
        // Invalidates results already posted by a cancelled task.
        computerGeneration++;

        if (computerTask != null) {
            computerTask.cancel(true);
            computerTask = null;
        }
    }

    private static int chooseComputerMove(
            String snapshot,
            char computerSymbol
    ) {
        char[] board = snapshot.toCharArray();

        // First, take a winning move.
        int winningMove = findFinishingMove(
                board,
                computerSymbol
        );

        if (winningMove != -1) {
            return winningMove;
        }

        // Otherwise, block the opponent's winning move.
        char humanSymbol =
                computerSymbol == 'X' ? 'O' : 'X';

        int blockingMove = findFinishingMove(
                board,
                humanSymbol
        );

        if (blockingMove != -1) {
            return blockingMove;
        }

        if (board[4] == EMPTY) {
            return 4;
        }

        int[] available = new int[9];
        int count = 0;

        for (int i = 0; i < board.length; i++) {
            if (board[i] == EMPTY) {
                available[count++] = i;
            }
        }

        if (count == 0) {
            return -1;
        }

        return available[
                ThreadLocalRandom.current().nextInt(count)
                ];
    }

    private static int findFinishingMove(
            char[] board,
            char symbol
    ) {
        for (int i = 0; i < board.length; i++) {
            if (board[i] != EMPTY) {
                continue;
            }

            board[i] = symbol;
            boolean wins = hasWinningLine(board, symbol);
            board[i] = EMPTY;

            if (wins) {
                return i;
            }
        }

        return -1;
    }

    private static boolean hasWinningLine(
            char[] board,
            char symbol
    ) {
        for (int[] line : WIN_LINES) {
            if (board[line[0]] == symbol
                    && board[line[1]] == symbol
                    && board[line[2]] == symbol) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onCleared() {
        cleared = true;
        gameActive = false;

        cancelComputerMove();
        computerExecutor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);

        super.onCleared();
    }

    public String getRoundId() {
        String roundId = savedState.get("saved_round_id");

        if (roundId == null) {
            roundId = java.util.UUID.randomUUID().toString();
            savedState.set("saved_round_id", roundId);
        }

        return roundId;
    }

    public boolean surrender() {
        if (!gameActive
                || !isCountdownComplete()
                || isFinished()) {
            return false;
        }

        char losingSymbol;

        if (singlePlayer) {
            // The human surrenders, including during AI thinking.
            losingSymbol = playerOneIsX ? 'X' : 'O';
        } else {
            losingSymbol = getCurrentSymbol();
        }

        char winningSymbol =
                losingSymbol == 'X' ? 'O' : 'X';

        cancelComputerMove();

        savedState.set(
                "surrender_winner",
                String.valueOf(winningSymbol)
        );

        // Notify the existing board observer to render and save
        // the result, then open the result screen.
        savedState.set(KEY_BOARD, getBoardValue());

        return true;
    }
}