package com.example.aletrail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RatingAdapter extends RecyclerView.Adapter<RatingAdapter.RatingViewHolder> {

    private List<BeerRatingEntity> ratings = new ArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @NonNull
    @Override
    public RatingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rating, parent, false);
        return new RatingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RatingViewHolder holder, int position) {
        holder.bind(ratings.get(position));
    }

    @Override
    public int getItemCount() {
        return ratings.size();
    }

    public void setRatings(List<BeerRatingEntity> newRatings) {
        this.ratings = newRatings != null ? newRatings : new ArrayList<>();
        notifyDataSetChanged();
    }

    class RatingViewHolder extends RecyclerView.ViewHolder {
        private final TextView breweryName;
        private final TextView beerName;
        private final RatingBar ratingStars;
        private final TextView ratingValue;
        private final TextView comment;
        private final TextView date;

        RatingViewHolder(@NonNull View itemView) {
            super(itemView);
            breweryName = itemView.findViewById(R.id.ratingBreweryName);
            beerName = itemView.findViewById(R.id.ratingBeerName);
            ratingStars = itemView.findViewById(R.id.ratingStars);
            ratingValue = itemView.findViewById(R.id.ratingValue);
            comment = itemView.findViewById(R.id.ratingComment);
            date = itemView.findViewById(R.id.ratingDate);
        }

        void bind(BeerRatingEntity rating) {
            // Вадим името на пивоварната във фонов поток.
            String breweryId = rating.getBreweryId();
            breweryName.setText(breweryId);
            executor.execute(() -> {
                try {
                    BreweryEntity brewery = Database.getInstance(itemView.getContext())
                            .AleDAO().getAleByIdSync(breweryId);
                    String name = (brewery != null && brewery.getName() != null)
                            ? brewery.getName() : breweryId;
                    itemView.post(() -> breweryName.setText(name));
                } catch (Exception ignored) {}
            });

            // Име на бира (ако е въведено).
            if (rating.getBeerName() != null && !rating.getBeerName().isEmpty()) {
                beerName.setText(itemView.getContext().getString(R.string.rating_item_beer, rating.getBeerName()));
                beerName.setVisibility(View.VISIBLE);
            } else {
                beerName.setText(R.string.rating_item_no_beer);
                beerName.setVisibility(View.VISIBLE);
            }

            // Оценка
            ratingStars.setRating(rating.getRating());
            ratingValue.setText(String.format(Locale.getDefault(), "%.1f", rating.getRating()));

            // Коментар
            if (rating.getComment() != null && !rating.getComment().isEmpty()) {
                comment.setText(rating.getComment());
                comment.setVisibility(View.VISIBLE);
            } else {
                comment.setVisibility(View.GONE);
            }

            // Дата
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            date.setText(sdf.format(new Date(rating.getRatingTimestamp())));
        }
    }
}
