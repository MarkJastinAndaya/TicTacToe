package com.example.tictactoe;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.tictactoe.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowCompat.getInsetsController(
                getWindow(), binding.getRoot()
        ).setAppearanceLightStatusBars(false);

        WindowCompat.getInsetsController(
                getWindow(), binding.getRoot()
        ).setAppearanceLightNavigationBars(false);

        final int initialLeft = binding.getRoot().getPaddingLeft();
        final int initialTop = binding.getRoot().getPaddingTop();
        final int initialRight = binding.getRoot().getPaddingRight();
        final int initialBottom = binding.getRoot().getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                binding.getRoot(),
                (view, windowInsets) -> {
                    Insets safeInsets = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.displayCutout()
                                    | WindowInsetsCompat.Type.ime()
                    );

                    view.setPadding(
                            initialLeft + safeInsets.left,
                            initialTop + safeInsets.top,
                            initialRight + safeInsets.right,
                            initialBottom + safeInsets.bottom
                    );

                    return windowInsets;
                }
        );

        ViewCompat.requestApplyInsets(binding.getRoot());

        // Android restores existing Fragments after recreation.
        // Only add the initial menu on a fresh launch.
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(
                            R.id.fragmentContainer,
                            MenuFragment.class,
                            null
                    )
                    .commit();
        }
    }
}