package com.example.tictactoe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.tictactoe.databinding.FragmentMenuBinding;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class MenuFragment extends Fragment {

    private FragmentMenuBinding binding;
    private boolean navigating;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentMenuBinding.inflate(
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

        binding.singlePlayerButton.setOnClickListener(
                v -> openSetup(true)
        );

        binding.twoPlayerButton.setOnClickListener(
                v -> openSetup(false)
        );
    }

    private void openSetup(boolean singlePlayer) {
        if (binding == null || !isResumed() || navigating) {
            return;
        }

        FragmentManager manager = getParentFragmentManager();

        if (manager.isStateSaved()) {
            return;
        }

        navigating = true;
        binding.singlePlayerButton.setEnabled(false);
        binding.twoPlayerButton.setEnabled(false);

        Bundle arguments = new Bundle();
        arguments.putBoolean(
                SetupFragment.ARG_SINGLE_PLAYER,
                singlePlayer
        );

        manager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragmentContainer,
                        SetupFragment.class,
                        arguments
                )
                .addToBackStack("setup")
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();

        navigating = false;

        if (binding != null) {
            binding.singlePlayerButton.setEnabled(true);
            binding.twoPlayerButton.setEnabled(true);
        }

        loadLeaderboard();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadLeaderboard() {
        if (binding == null) {
            return;
        }

        final FragmentMenuBinding target = binding;

        LeaderboardStore.getInstance(requireContext())
                .loadLeaderboard(entries -> {
                    if (binding != target || !isAdded()) {
                        return;
                    }

                    if (entries == null) {
                        Toast.makeText(
                                requireContext(),
                                R.string.leaderboard_load_error,
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    target.leaderboardRows.removeAllViews();

                    target.leaderboardContainer.setVisibility(
                            entries.isEmpty()
                                    ? View.GONE
                                    : View.VISIBLE
                    );

                    int rank = 1;

                    for (LeaderboardStore.Entry entry : entries) {
                        View row = getLayoutInflater().inflate(
                                R.layout.item_leaderboard,
                                target.leaderboardRows,
                                false
                        );

                        TextView rankText =
                                row.findViewById(R.id.rankText);

                        TextView nameText =
                                row.findViewById(R.id.playerNameText);

                        TextView statsText =
                                row.findViewById(R.id.playerStatsText);

                        rankText.setText(
                                getString(
                                        R.string.leaderboard_rank,
                                        rank
                                )
                        );

                        nameText.setText(entry.name);

                        statsText.setText(
                                getString(
                                        R.string.leaderboard_stats,
                                        entry.wins,
                                        entry.draws,
                                        entry.losses
                                )
                        );

                        target.leaderboardRows.addView(row);
                        rank++;
                    }
                    target.leaderboardScroll.scrollTo(0, 0);
                });
    }
}
