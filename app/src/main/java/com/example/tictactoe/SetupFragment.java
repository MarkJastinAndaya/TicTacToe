package com.example.tictactoe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.tictactoe.databinding.FragmentSetupBinding;
import com.google.android.material.textfield.TextInputEditText;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class SetupFragment extends Fragment {

    public static final String ARG_SINGLE_PLAYER = "single_player";

    private FragmentSetupBinding binding;
    private boolean singlePlayer;
    private boolean leaving;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSetupBinding.inflate(
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
        singlePlayer = arguments != null
                && arguments.getBoolean(ARG_SINGLE_PLAYER, false);

        binding.setupTitle.setText(
                singlePlayer
                        ? R.string.single_player
                        : R.string.two_player
        );

        binding.playerTwoLayout.setVisibility(
                singlePlayer ? View.GONE : View.VISIBLE
        );

        binding.computerNotice.setVisibility(
                singlePlayer ? View.VISIBLE : View.GONE
        );

        binding.playerOneInput.setImeOptions(
                (singlePlayer
                        ? EditorInfo.IME_ACTION_DONE
                        : EditorInfo.IME_ACTION_NEXT)
                        | EditorInfo.IME_FLAG_NO_EXTRACT_UI
        );

        binding.playerTwoInput.setImeOptions(
                EditorInfo.IME_ACTION_DONE
                        | EditorInfo.IME_FLAG_NO_EXTRACT_UI
        );

        binding.readyButton.setOnClickListener(v -> validateNames());
        binding.backButton.setOnClickListener(v -> returnToMenu());

        TextInputEditText lastInput = singlePlayer
                ? binding.playerOneInput
                : binding.playerTwoInput;

        lastInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validateNames();
                return true;
            }
            return false;
        });
    }

    private String readName(TextInputEditText input) {
        if (input.getText() == null) {
            return "";
        }

        // Collapse whitespace, then remove leading/trailing spaces.
        return input.getText()
                .toString()
                .replaceAll("[\\p{Z}\\s]+", " ")
                .trim();
    }

    private void validateNames() {
        if (binding == null || !isResumed() || leaving) {
            return;
        }

        binding.playerOneLayout.setError(null);
        binding.playerTwoLayout.setError(null);

        String playerOne = readName(binding.playerOneInput);
        String playerTwo = singlePlayer
                ? getString(R.string.computer_name)
                : readName(binding.playerTwoInput);

        boolean valid = true;

        if (playerOne.isEmpty()) {
            binding.playerOneLayout.setError(
                    getString(R.string.error_name_required)
            );
            valid = false;
        } else if (singlePlayer
                && playerOne.equalsIgnoreCase(playerTwo)) {
            binding.playerOneLayout.setError(
                    getString(R.string.error_name_reserved)
            );
            valid = false;
        }

        if (!singlePlayer) {
            if (playerTwo.isEmpty()) {
                binding.playerTwoLayout.setError(
                        getString(R.string.error_name_required)
                );
                valid = false;
            } else if (!playerOne.isEmpty()
                    && playerOne.equalsIgnoreCase(playerTwo)) {
                binding.playerTwoLayout.setError(
                        getString(R.string.error_names_same)
                );
                valid = false;
            }
        }

        if (!valid) {
            if (binding.playerOneLayout.getError() != null) {
                binding.playerOneInput.requestFocus();
            } else {
                binding.playerTwoInput.requestFocus();
            }
            return;
        }

        binding.playerOneInput.setText(playerOne);

        if (!singlePlayer) {
            binding.playerTwoInput.setText(playerTwo);
        }

        openAllocation(playerOne, playerTwo);
    }

    private void openAllocation(String playerOne, String playerTwo) {
        if (binding == null || !isResumed() || leaving) {
            return;
        }

        FragmentManager manager = getParentFragmentManager();

        if (manager.isStateSaved()) {
            return;
        }

        leaving = true;
        binding.readyButton.setEnabled(false);
        binding.backButton.setEnabled(false);

        binding.playerOneInput.clearFocus();
        binding.playerTwoInput.clearFocus();

        WindowCompat.getInsetsController(
                requireActivity().getWindow(),
                binding.getRoot()
        ).hide(WindowInsetsCompat.Type.ime());

        Bundle arguments = new Bundle();

        arguments.putBoolean(ARG_SINGLE_PLAYER, singlePlayer);
        arguments.putString(
                AllocationFragment.ARG_PLAYER_ONE, playerOne
        );
        arguments.putString(
                AllocationFragment.ARG_PLAYER_TWO, playerTwo
        );

        // Randomize once per new round.
        arguments.putBoolean(
                AllocationFragment.ARG_ONE_IS_X,
                new java.util.Random().nextBoolean()
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
                        arguments
                )
                .addToBackStack("allocation")
                .commit();
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
        binding.readyButton.setEnabled(false);
        binding.backButton.setEnabled(false);

        manager.popBackStack();
    }

    @Override
    public void onResume() {
        super.onResume();

        leaving = false;

        if (binding != null) {
            binding.readyButton.setEnabled(true);
            binding.backButton.setEnabled(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}