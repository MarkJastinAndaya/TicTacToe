package com.example.tictactoe;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.tictactoe.databinding.FragmentGameBinding;

import android.content.res.ColorStateList;

import androidx.core.content.ContextCompat;

import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.TooltipCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class GameFragment extends Fragment {

    private static final long WORD_DURATION_MS = 800L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private FragmentGameBinding binding;
    private GameViewModel viewModel;
    private AppCompatImageButton[] cells;

    private boolean leaving;
    private int lastRenderedStep = -1;

    private AlertDialog gameActionDialog;

    private final Runnable countdownTask = () -> {
        if (binding == null || !isResumed() || leaving) {
            return;
        }

        viewModel.advanceCountdown();
        scheduleCountdown();
    };

    public GameFragment() {
        super(R.layout.fragment_game);
    }

    private final Runnable resultTask =
            this::openResultScreen;

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding = FragmentGameBinding.bind(view);

        // Scope this state to this game Fragment.
        viewModel = new ViewModelProvider(this)
                .get(GameViewModel.class);

        Bundle arguments = getArguments();

        viewModel.configure(
                arguments != null
                        && arguments.getBoolean(
                        SetupFragment.ARG_SINGLE_PLAYER, false
                ),
                arguments != null
                        && arguments.getBoolean(
                        AllocationFragment.ARG_ONE_IS_X, false
                )
        );

        cells = new AppCompatImageButton[] {
                binding.cell0, binding.cell1, binding.cell2,
                binding.cell3, binding.cell4, binding.cell5,
                binding.cell6, binding.cell7, binding.cell8
        };

        for (int i = 0; i < cells.length; i++) {
            final int index = i;

            cells[i].setEnabled(false);
            cells[i].setSupportImageTintList(null);

            cells[i].setOnClickListener(v -> {
                if (binding == null || !isResumed() || leaving) {
                    return;
                }

                if (viewModel.playHumanMove(index)) {
                    cells[index].animate().cancel();
                    cells[index].setScaleX(0.9f);
                    cells[index].setScaleY(0.9f);

                    cells[index].animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150L)
                            .start();
                }
            });
        }

        showAssignments();

        viewModel.getCountdownStep().observe(
                getViewLifecycleOwner(),
                step -> renderCountdown(step == null ? 0 : step)
        );

        viewModel.getBoard().observe(
                getViewLifecycleOwner(),
                value -> renderBoard()
        );

        binding.menuButton.setOnClickListener(v -> returnToMenu());

        binding.restartButton.setOnClickListener(
                v -> showRestartDialog()
        );

        binding.surrenderButton.setOnClickListener(
                v -> showSurrenderDialog()
        );

        TooltipCompat.setTooltipText(
                binding.menuButton,
                getString(R.string.action_home)
        );

        TooltipCompat.setTooltipText(
                binding.restartButton,
                getString(R.string.action_restart)
        );

        TooltipCompat.setTooltipText(
                binding.surrenderButton,
                getString(R.string.action_surrender)
        );

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        returnToMenu();
                    }
                }
        );
    }

    private void showAssignments() {
        Bundle arguments = getArguments();

        if (arguments == null) {
            return;
        }

        boolean oneIsX = arguments.getBoolean(
                AllocationFragment.ARG_ONE_IS_X
        );

        binding.gameAssignments.setText(
                getString(
                        R.string.game_assignments,
                        arguments.getString(
                                AllocationFragment.ARG_PLAYER_ONE, ""
                        ),
                        oneIsX ? "X" : "O",
                        arguments.getString(
                                AllocationFragment.ARG_PLAYER_TWO, ""
                        ),
                        oneIsX ? "O" : "X"
                )
        );
    }

    private void renderCountdown(int step) {
        if (binding == null) {
            return;
        }

        if (step >= 3) {
            binding.countdownText.animate().cancel();
            binding.countdownText.setVisibility(View.GONE);
            renderBoard();
            return;
        }

        binding.countdownText.setVisibility(View.VISIBLE);
        binding.turnText.setText(R.string.game_starting);

        int[] words = {
                R.string.countdown_tic,
                R.string.countdown_tac,
                R.string.countdown_toe
        };

        binding.countdownText.setText(words[step]);

        if (step != lastRenderedStep) {
            lastRenderedStep = step;

            binding.countdownText.animate().cancel();
            binding.countdownText.setAlpha(0f);
            binding.countdownText.setScaleX(0.85f);
            binding.countdownText.setScaleY(0.85f);

            binding.countdownText.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(250L)
                    .start();
        }
    }

    private String getPlayerName(char symbol) {
        Bundle arguments = getArguments();

        if (arguments == null) {
            return "";
        }

        boolean oneIsX = arguments.getBoolean(
                AllocationFragment.ARG_ONE_IS_X
        );

        boolean belongsToPlayerOne = (symbol == 'X') == oneIsX;

        return arguments.getString(
                belongsToPlayerOne
                        ? AllocationFragment.ARG_PLAYER_ONE
                        : AllocationFragment.ARG_PLAYER_TWO,
                ""
        );
    }

    private void renderBoard() {
        if (binding == null || cells == null) {
            return;
        }

        binding.restartButton.setEnabled(
                !leaving && !viewModel.isFinished()
        );

        binding.surrenderButton.setEnabled(
                !leaving
                        && viewModel.isCountdownComplete()
                        && !viewModel.isFinished()
        );

        binding.restartButton.setAlpha(
                binding.restartButton.isEnabled() ? 1f : 0.4f
        );

        binding.surrenderButton.setAlpha(
                binding.surrenderButton.isEnabled() ? 1f : 0.4f
        );

        String board = viewModel.getBoardValue();
        boolean[] winningTiles = viewModel.getWinningTiles();

        boolean inputAllowed = viewModel.canHumanMove() && !leaving;

        for (int i = 0; i < cells.length; i++) {
            char symbol = board.charAt(i);

            if (symbol == 'X') {
                cells[i].setImageResource(R.drawable.ic_symbol_x);
            } else if (symbol == 'O') {
                cells[i].setImageResource(R.drawable.ic_symbol_o);
            } else {
                cells[i].setImageDrawable(null);
            }

            cells[i].setEnabled(
                    inputAllowed && symbol == GameViewModel.EMPTY
            );

            int tileColor = ContextCompat.getColor(
                    requireContext(),
                    winningTiles[i]
                            ? R.color.ttt_winning_tile
                            : R.color.ttt_surface
            );

            cells[i].setSupportBackgroundTintList(
                    ColorStateList.valueOf(tileColor)
            );

            if (symbol == GameViewModel.EMPTY) {
                cells[i].setContentDescription(
                        getString(
                                R.string.cell_empty,
                                i / 3 + 1,
                                i % 3 + 1
                        )
                );
            } else {
                cells[i].setContentDescription(
                        getString(
                                R.string.cell_occupied,
                                i / 3 + 1,
                                i % 3 + 1,
                                String.valueOf(symbol)
                        )
                );
            }
        }

        if (!viewModel.isCountdownComplete()) {
            binding.turnText.setText(R.string.game_starting);
            return;
        }

        char winner = viewModel.getWinner();

        if (winner != GameViewModel.EMPTY) {
            binding.turnText.setText(
                    getString(
                            R.string.game_winner,
                            getPlayerName(winner),
                            String.valueOf(winner)
                    )
            );
        } else if (viewModel.isDraw()) {
            binding.turnText.setText(R.string.game_draw);
        } else if (viewModel.isComputerTurn()) {
            binding.turnText.setText(
                    getString(
                            R.string.computer_thinking,
                            String.valueOf(viewModel.getCurrentSymbol())
                    )
            );
        } else {
            char symbol = viewModel.getCurrentSymbol();

            binding.turnText.setText(
                    getString(
                            R.string.player_turn,
                            getPlayerName(symbol),
                            String.valueOf(symbol)
                    )
            );
        }

        saveFinishedRound();
        scheduleResultScreen();
    }

    private void scheduleCountdown() {
        handler.removeCallbacks(countdownTask);

        if (binding != null
                && isResumed()
                && !leaving
                && !viewModel.isCountdownComplete()) {
            handler.postDelayed(countdownTask, WORD_DURATION_MS);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        leaving = false;

        if (binding != null) {
            viewModel.setGameActive(true);
            binding.menuButton.setEnabled(true);
            renderBoard();
        }

        scheduleCountdown();
    }

    @Override
    public void onPause() {
        if (gameActionDialog != null) {
            gameActionDialog.dismiss();
            gameActionDialog = null;
        }

        if (viewModel != null) {
            viewModel.setGameActive(false);
        }

        handler.removeCallbacks(countdownTask);

        if (binding != null) {
            binding.countdownText.animate().cancel();
        }

        super.onPause();
    }

    private void returnToMenu() {
        if (binding == null || !isResumed() || leaving) {
            return;
        }

        FragmentManager manager = getParentFragmentManager();

        if (manager.isStateSaved()) {
            return;
        }

        leaving = true;
        viewModel.setGameActive(false);
        handler.removeCallbacks(countdownTask);
        binding.menuButton.setEnabled(false);

        manager.popBackStack(
                "setup",
                FragmentManager.POP_BACK_STACK_INCLUSIVE
        );
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(countdownTask);

        if (binding != null) {
            binding.countdownText.animate().cancel();
        }

        if (cells != null) {
            for (AppCompatImageButton cell : cells) {
                cell.animate().cancel();
            }
        }

        super.onDestroyView();

        cells = null;
        binding = null;
        lastRenderedStep = -1;
    }

    private void scheduleResultScreen() {
        handler.removeCallbacks(resultTask);

        if (binding != null
                && isResumed()
                && !leaving
                && viewModel.isFinished()) {
            handler.postDelayed(resultTask, 1200L);
        }
    }

    private void openResultScreen() {
        if (binding == null
                || !isResumed()
                || leaving
                || !viewModel.isFinished()) {
            return;
        }

        FragmentManager manager =
                getParentFragmentManager();

        if (manager.isStateSaved()) {
            return;
        }

        Bundle originalArgs = getArguments();

        if (originalArgs == null) {
            returnToMenu();
            return;
        }

        char winner = viewModel.getWinner();

        Bundle resultArgs = new Bundle(originalArgs);

        resultArgs.putString(
                ResultFragment.ARG_WINNER_SYMBOL,
                String.valueOf(winner)
        );

        resultArgs.putString(
                ResultFragment.ARG_WINNER_NAME,
                winner == GameViewModel.EMPTY
                        ? ""
                        : getPlayerName(winner)
        );

        leaving = true;
        viewModel.setGameActive(false);

        handler.removeCallbacks(countdownTask);
        handler.removeCallbacks(resultTask);
        binding.menuButton.setEnabled(false);

        manager.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(
                        R.id.fragmentContainer,
                        ResultFragment.class,
                        resultArgs
                )
                .addToBackStack("result")
                .commit();
    }

    private void saveFinishedRound() {
        if (!viewModel.isFinished()) {
            return;
        }

        Bundle args = getArguments();

        if (args == null) {
            return;
        }

        String playerOne = args.getString(
                AllocationFragment.ARG_PLAYER_ONE,
                ""
        );

        String playerTwo = args.getString(
                AllocationFragment.ARG_PLAYER_TWO,
                ""
        );

        if (playerOne.trim().isEmpty()
                || playerTwo.trim().isEmpty()) {
            return;
        }

        boolean playerOneIsX = args.getBoolean(
                AllocationFragment.ARG_ONE_IS_X,
                false
        );

        char winnerSymbol = viewModel.getWinner();

        int winner = 0;

        if (winnerSymbol != GameViewModel.EMPTY) {
            boolean playerOneWon =
                    (winnerSymbol == 'X') == playerOneIsX;

            winner = playerOneWon ? 1 : 2;
        }

        LeaderboardStore.getInstance(requireContext())
                .recordRound(
                        viewModel.getRoundId(),
                        playerOne,
                        playerTwo,
                        winner,
                        success -> {
                            if (!success
                                    && binding != null
                                    && isAdded()) {
                                Toast.makeText(
                                        requireContext(),
                                        R.string.leaderboard_save_error,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    private boolean canUseGameActions() {
        return binding != null
                && isResumed()
                && !leaving
                && !viewModel.isFinished()
                && !getParentFragmentManager().isStateSaved();
    }

    private void showRestartDialog() {
        if (!canUseGameActions()
                || (gameActionDialog != null
                && gameActionDialog.isShowing())) {
            return;
        }

        gameActionDialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.restart_title)
                        .setMessage(R.string.restart_message)
                        .setNegativeButton(
                                android.R.string.cancel,
                                null
                        )
                        .setPositiveButton(
                                R.string.action_restart,
                                (dialog, which) -> restartGame()
                        )
                        .show();
    }

    private void showSurrenderDialog() {
        if (!canUseGameActions()
                || !viewModel.isCountdownComplete()
                || (gameActionDialog != null
                && gameActionDialog.isShowing())) {
            return;
        }

        Bundle args = getArguments();

        boolean singlePlayer = args != null
                && args.getBoolean(
                SetupFragment.ARG_SINGLE_PLAYER,
                false
        );

        int message = singlePlayer
                ? R.string.surrender_message_single
                : R.string.surrender_message_two;

        gameActionDialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.surrender_title)
                        .setMessage(message)
                        .setNegativeButton(
                                android.R.string.cancel,
                                null
                        )
                        .setPositiveButton(
                                R.string.action_surrender,
                                (dialog, which) -> {
                                    if (canUseGameActions()) {
                                        viewModel.surrender();
                                    }
                                }
                        )
                        .show();
    }

    private void restartGame() {
        if (!canUseGameActions()) {
            return;
        }

        Bundle originalArgs = getArguments();

        if (originalArgs == null) {
            return;
        }

        Bundle nextArgs = new Bundle(originalArgs);

        nextArgs.putBoolean(
                AllocationFragment.ARG_ONE_IS_X,
                new java.util.Random().nextBoolean()
        );

        FragmentManager manager =
                getParentFragmentManager();

        leaving = true;

        viewModel.setGameActive(false);

        handler.removeCallbacks(countdownTask);
        handler.removeCallbacks(resultTask);

        binding.menuButton.setEnabled(false);
        binding.restartButton.setEnabled(false);
        binding.surrenderButton.setEnabled(false);

        manager.popBackStack(
                "allocation",
                FragmentManager.POP_BACK_STACK_INCLUSIVE
        );

        manager.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(
                        R.id.fragmentContainer,
                        AllocationFragment.class,
                        nextArgs
                )
                .addToBackStack("allocation")
                .commit();
    }
}