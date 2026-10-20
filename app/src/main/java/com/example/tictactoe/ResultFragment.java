package com.example.tictactoe;

import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.tictactoe.databinding.FragmentResultBinding;

import java.util.Random;

public class ResultFragment extends Fragment {

    public static final String ARG_WINNER_SYMBOL =
            "winner_symbol";

    public static final String ARG_WINNER_NAME =
            "winner_name";

    private FragmentResultBinding binding;
    private boolean leaving;

    public ResultFragment() {
        super(R.layout.fragment_result);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding = FragmentResultBinding.bind(view);

        Bundle args = getArguments();

        String symbol = args == null
                ? "-"
                : args.getString(ARG_WINNER_SYMBOL, "-");

        String winnerName = args == null
                ? ""
                : args.getString(ARG_WINNER_NAME, "");

        if ("X".equals(symbol) || "O".equals(symbol)) {
            binding.winnerSymbol.setImageResource(
                    "X".equals(symbol)
                            ? R.drawable.ic_symbol_x
                            : R.drawable.ic_symbol_o
            );

            binding.resultTitle.setText(
                    getString(
                            R.string.game_winner,
                            winnerName,
                            symbol
                    )
            );
        } else {
            binding.winnerSymbol.setVisibility(View.GONE);
            binding.resultTitle.setText(R.string.game_draw);
        }

        binding.playAgainButton.setOnClickListener(
                v -> playAgain()
        );

        binding.menuButton.setOnClickListener(
                v -> returnToMenu()
        );

        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(
                        getViewLifecycleOwner(),
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                returnToMenu();
                            }
                        }
                );
    }

    private boolean canNavigate() {
        return binding != null
                && isResumed()
                && !leaving
                && !getParentFragmentManager().isStateSaved();
    }

    private void disableButtons() {
        leaving = true;
        binding.playAgainButton.setEnabled(false);
        binding.menuButton.setEnabled(false);
    }

    private void playAgain() {
        if (!canNavigate()) {
            return;
        }

        Bundle previousArgs = getArguments();

        if (previousArgs == null) {
            returnToMenu();
            return;
        }

        // Keep the same mode and names; allocate symbols again.
        Bundle nextArgs = new Bundle(previousArgs);

        nextArgs.remove(ARG_WINNER_SYMBOL);
        nextArgs.remove(ARG_WINNER_NAME);

        nextArgs.putBoolean(
                AllocationFragment.ARG_ONE_IS_X,
                new Random().nextBoolean()
        );

        FragmentManager manager =
                getParentFragmentManager();

        disableButtons();

        // Remove the previous allocation, game, and result.
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

    private void returnToMenu() {
        if (!canNavigate()) {
            return;
        }

        FragmentManager manager =
                getParentFragmentManager();

        disableButtons();

        manager.popBackStack(
                "setup",
                FragmentManager.POP_BACK_STACK_INCLUSIVE
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        leaving = false;

        if (binding != null) {
            binding.playAgainButton.setEnabled(true);
            binding.menuButton.setEnabled(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}