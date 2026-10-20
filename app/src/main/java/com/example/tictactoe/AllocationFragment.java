package com.example.tictactoe;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.tictactoe.databinding.FragmentAllocationBinding;

public class AllocationFragment extends Fragment {

    public static final String ARG_PLAYER_ONE = "player_one";
    public static final String ARG_PLAYER_TWO = "player_two";
    public static final String ARG_ONE_IS_X = "one_is_x";

    private static final String STATE_REMAINING = "remaining_ms";
    private static final long ALLOCATION_DURATION_MS = 3500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable advanceTask = this::openGame;

    private FragmentAllocationBinding binding;
    private long remainingMs = ALLOCATION_DURATION_MS;
    private long resumedAt;
    private boolean timerRunning;
    private boolean navigating;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            remainingMs = Math.max(
                    0L,
                    Math.min(
                            ALLOCATION_DURATION_MS,
                            savedInstanceState.getLong(
                                    STATE_REMAINING,
                                    ALLOCATION_DURATION_MS
                            )
                    )
            );
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAllocationBinding.inflate(
                inflater, container, false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        Bundle arguments = getArguments();

        if (arguments == null) {
            return;
        }

        boolean oneIsX = arguments.getBoolean(ARG_ONE_IS_X);

        binding.playerOneSymbol.setImageResource(
                oneIsX ? R.drawable.ic_symbol_x : R.drawable.ic_symbol_o
        );

        binding.playerTwoSymbol.setImageResource(
                oneIsX ? R.drawable.ic_symbol_o : R.drawable.ic_symbol_x
        );

        binding.playerOneAssignment.setText(
                getString(
                        R.string.allocation_player,
                        arguments.getString(ARG_PLAYER_ONE, ""),
                        oneIsX ? "X" : "O"
                )
        );

        binding.playerTwoAssignment.setText(
                getString(
                        R.string.allocation_player,
                        arguments.getString(ARG_PLAYER_TWO, ""),
                        oneIsX ? "O" : "X"
                )
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        navigating = false;

        handler.removeCallbacks(advanceTask);
        resumedAt = SystemClock.uptimeMillis();
        timerRunning = true;
        handler.postDelayed(advanceTask, remainingMs);
    }

    private long currentRemainingMs() {
        if (!timerRunning) {
            return remainingMs;
        }

        long elapsed = SystemClock.uptimeMillis() - resumedAt;
        return Math.max(0L, remainingMs - elapsed);
    }

    @Override
    public void onPause() {
        remainingMs = currentRemainingMs();
        timerRunning = false;
        handler.removeCallbacks(advanceTask);

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(STATE_REMAINING, currentRemainingMs());
        super.onSaveInstanceState(outState);
    }

    private void openGame() {
        if (binding == null || !isResumed() || navigating) {
            return;
        }

        FragmentManager manager = getParentFragmentManager();

        if (manager.isStateSaved()) {
            return;
        }

        Bundle arguments = getArguments();

        // Recover to setup if the screen has incomplete input.
        if (arguments == null
                || arguments.getString(ARG_PLAYER_ONE, "").isEmpty()
                || arguments.getString(ARG_PLAYER_TWO, "").isEmpty()) {
            navigating = true;
            manager.popBackStack();
            return;
        }

        navigating = true;
        remainingMs = 0L;
        timerRunning = false;

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
                        GameFragment.class,
                        new Bundle(arguments)
                )
                .addToBackStack("game")
                .commit();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(advanceTask);
        super.onDestroyView();
        binding = null;
    }
}
