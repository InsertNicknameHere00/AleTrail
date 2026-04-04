package com.example.aletrail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BreweryAdapter extends RecyclerView.Adapter<BreweryAdapter.BreweryViewHolder> {

    private List<BreweryEntity> breweries = new ArrayList<>();
    private OnBreweryClickListener listener;
    private boolean showRemoveFavoriteButton = false;

    public interface OnBreweryClickListener {
        void onBreweryClick(BreweryEntity brewery);
        void onFavoriteClick(BreweryEntity brewery);
        void onBreweryLongPress(BreweryEntity brewery, View anchorView);
    }

    public BreweryAdapter(OnBreweryClickListener listener) {
        this.listener = listener;
    }

    public BreweryAdapter(OnBreweryClickListener listener, boolean showRemoveFavoriteButton) {
        this.listener = listener;
        this.showRemoveFavoriteButton = showRemoveFavoriteButton;
    }

    @NonNull
    @Override
    public BreweryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_brewery, parent, false);
        return new BreweryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BreweryViewHolder holder, int position) {
        BreweryEntity brewery = breweries.get(position);
        holder.bind(brewery);
    }

    @Override
    public int getItemCount() {
        return breweries.size();
    }

    public void setBreweries(List<BreweryEntity> newBreweries) {
        final List<BreweryEntity> finalNewBreweries = (newBreweries == null) ? new ArrayList<>() : newBreweries;

        // Use DiffUtil for efficient updates
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return breweries.size();
            }

            @Override
            public int getNewListSize() {
                return finalNewBreweries.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return breweries.get(oldItemPosition).getId().equals(
                        finalNewBreweries.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                BreweryEntity oldBrewery = breweries.get(oldItemPosition);
                BreweryEntity newBrewery = finalNewBreweries.get(newItemPosition);
                return oldBrewery.getName().equals(newBrewery.getName()) &&
                        oldBrewery.isFavorite() == newBrewery.isFavorite();
            }
        });

        this.breweries = finalNewBreweries;
        diffResult.dispatchUpdatesTo(this);
    }

    class BreweryViewHolder extends RecyclerView.ViewHolder {
        private TextView breweryName;
        private TextView breweryType;
        private TextView breweryAddress;
        private TextView breweryPhone;
        private TextView breweryWebsite;
        private ImageButton favoriteButton;
        private Button removeFavoriteButton;
        private ImageView breweryIcon;

        public BreweryViewHolder(@NonNull View itemView) {
            super(itemView);
            breweryName = itemView.findViewById(R.id.breweryName);
            breweryType = itemView.findViewById(R.id.breweryType);
            breweryAddress = itemView.findViewById(R.id.breweryAddress);
            breweryPhone = itemView.findViewById(R.id.breweryPhone);
            breweryWebsite = itemView.findViewById(R.id.breweryWebsite);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
            removeFavoriteButton = itemView.findViewById(R.id.removeFavoriteButton);
            breweryIcon = itemView.findViewById(R.id.breweryIcon);
        }

        private void applyFavoriteVisualState(boolean isFavorite) {
            favoriteButton.setImageResource(isFavorite
                    ? android.R.drawable.btn_star_big_on
                    : android.R.drawable.btn_star_big_off);
            int tint = isFavorite ? 0xFFFFC107 : 0xFF6FA8FF;
            favoriteButton.setColorFilter(tint, android.graphics.PorterDuff.Mode.SRC_IN);
        }

        public void bind(BreweryEntity brewery) {
            breweryName.setText(brewery.getName());

            // Format brewery type
            String type = brewery.getBrewery_type() != null ?
                    brewery.getBrewery_type().toUpperCase() : "BREWERY";
            breweryType.setText(type);

            // Build full address
            StringBuilder address = new StringBuilder();
            if (brewery.getStreet() != null && !brewery.getStreet().isEmpty()) {
                address.append(brewery.getStreet());
            }
            if (brewery.getCity() != null && !brewery.getCity().isEmpty()) {
                if (address.length() > 0) address.append(", ");
                address.append(brewery.getCity());
            }
            if (brewery.getState() != null && !brewery.getState().isEmpty()) {
                if (address.length() > 0) address.append(", ");
                address.append(brewery.getState());
            }
            if (brewery.getPostal_code() != null && !brewery.getPostal_code().isEmpty()) {
                if (address.length() > 0) address.append(" ");
                address.append(brewery.getPostal_code());
            }

            if (address.length() > 0) {
                breweryAddress.setText(address.toString());
                breweryAddress.setVisibility(View.VISIBLE);
            } else {
                breweryAddress.setVisibility(View.GONE);
            }

            // Phone
            if (brewery.getPhone() != null && !brewery.getPhone().isEmpty()) {
                breweryPhone.setText(brewery.getPhone());
                breweryPhone.setVisibility(View.VISIBLE);
            } else {
                breweryPhone.setVisibility(View.GONE);
            }

            // Website
            if (brewery.getWebsite_url() != null && !brewery.getWebsite_url().isEmpty()) {
                breweryWebsite.setText(brewery.getWebsite_url());
                breweryWebsite.setVisibility(View.VISIBLE);
            } else {
                breweryWebsite.setVisibility(View.GONE);
            }

            // Show/hide remove favorite button based on adapter setting
            if (showRemoveFavoriteButton) {
                removeFavoriteButton.setVisibility(View.VISIBLE);
                removeFavoriteButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onFavoriteClick(brewery);
                    }
                });
            } else {
                removeFavoriteButton.setVisibility(View.GONE);
            }

            // Различен цвят за favorite on/off.
            applyFavoriteVisualState(brewery.isFavorite());

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onBreweryClick(brewery);
            });

            favoriteButton.setOnClickListener(v -> {
                if (listener != null) {
                    // UI feedback веднага при toggle.
                    boolean newState = !brewery.isFavorite();
                    brewery.setFavorite(newState);
                    applyFavoriteVisualState(newState);
                    listener.onFavoriteClick(brewery);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onBreweryLongPress(brewery, itemView);
                return true;
            });
        }
    }
}
