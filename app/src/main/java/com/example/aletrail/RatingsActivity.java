package com.example.aletrail;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RatingsActivity extends AppCompatActivity {

    private RecyclerView ratingsRecyclerView;
    private TextView ratingsEmpty;
    private RatingAdapter adapter;
    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ratings);

        // Hide status bar
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat ic =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ic.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars());
        ic.setSystemBarsBehavior(
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        database = Database.getInstance(this);

        ratingsRecyclerView = findViewById(R.id.ratingsRecyclerView);
        ratingsEmpty = findViewById(R.id.ratingsEmpty);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        adapter = new RatingAdapter();
        ratingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ratingsRecyclerView.setAdapter(adapter);

        String userId = AppwriteService.getInstance(this).getSavedUserId();
        if (userId != null) {
            database.beerRatingDAO().getRatingsForUser(userId).observe(this, ratings -> {
                if (ratings != null && !ratings.isEmpty()) {
                    adapter.setRatings(ratings);
                    ratingsEmpty.setVisibility(View.GONE);
                    ratingsRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    ratingsEmpty.setVisibility(View.VISIBLE);
                    ratingsRecyclerView.setVisibility(View.GONE);
                }
            });
        }
    }
}

