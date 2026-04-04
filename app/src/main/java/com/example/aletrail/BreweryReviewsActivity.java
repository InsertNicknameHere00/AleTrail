package com.example.aletrail;

import android.os.Bundle;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Екран, който показва всички ревюта за една пивоварна.
public class BreweryReviewsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brewery_reviews);

        // Скриваме системната лента.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat ic =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ic.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars());
        ic.setSystemBarsBehavior(
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        String breweryId = getIntent().getStringExtra("breweryId");
        String breweryName = getIntent().getStringExtra("breweryName");
        if (breweryId == null) {
            finish();
            return;
        }

        TextView titleText = findViewById(R.id.reviewsTitle);
        TextView emptyText = findViewById(R.id.reviewsEmptyText);
        TextView avgRatingText = findViewById(R.id.avgRatingText);
        RatingBar avgRatingBar = findViewById(R.id.avgRatingBar);
        RecyclerView recyclerView = findViewById(R.id.reviewsRecyclerView);

        titleText.setText(getString(R.string.brewery_reviews_title, breweryName != null ? breweryName : "Brewery"));

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        RatingAdapter adapter = new RatingAdapter();
        recyclerView.setAdapter(adapter);

        Database database = Database.getInstance(this);

        // Слушаме списъка с ревюта за тази пивоварна.
        database.beerRatingDAO().getRatingsForBrewery(breweryId).observe(this, ratings -> {
            if (ratings != null && !ratings.isEmpty()) {
                adapter.setRatings(ratings);
                emptyText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });

        // Слушаме средната оценка.
        database.beerRatingDAO().getAverageRatingForBrewery(breweryId).observe(this, avg -> {
            if (avg != null && avg > 0) {
                avgRatingText.setText(getString(R.string.brewery_avg_rating, avg));
                avgRatingBar.setRating(avg);
                avgRatingText.setVisibility(View.VISIBLE);
                avgRatingBar.setVisibility(View.VISIBLE);
            } else {
                avgRatingText.setVisibility(View.GONE);
                avgRatingBar.setVisibility(View.GONE);
            }
        });
    }
}
