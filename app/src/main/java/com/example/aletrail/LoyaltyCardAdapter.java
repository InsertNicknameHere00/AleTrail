package com.example.aletrail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoyaltyCardAdapter extends RecyclerView.Adapter<LoyaltyCardAdapter.CardViewHolder> {

    private List<LoyaltyCardEntity> cards = new ArrayList<>();
    private OnCardClickListener listener;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public interface OnCardClickListener {
        void onCardClick(LoyaltyCardEntity card);
        void onShareClick(LoyaltyCardEntity card);
        void onHistoryClick(LoyaltyCardEntity card);
        void onDeleteClick(LoyaltyCardEntity card);
    }

    public LoyaltyCardAdapter(OnCardClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_loyalty_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        LoyaltyCardEntity card = cards.get(position);
        holder.bind(card);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    public void setCards(List<LoyaltyCardEntity> newCards) {
        final List<LoyaltyCardEntity> finalNewCards = (newCards == null) ? new ArrayList<>() : newCards;

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return cards.size();
            }

            @Override
            public int getNewListSize() {
                return finalNewCards.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return cards.get(oldItemPosition).getCardId() ==
                        finalNewCards.get(newItemPosition).getCardId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                LoyaltyCardEntity oldCard = cards.get(oldItemPosition);
                LoyaltyCardEntity newCard = finalNewCards.get(newItemPosition);
                return oldCard.getStamps() == newCard.getStamps() &&
                        oldCard.getMaxStamps() == newCard.getMaxStamps();
            }
        });

        this.cards = finalNewCards;
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * Calculates the current discount percentage based on stamps.
     * Every 2 stamps = 5% discount, up to stamp 10 (max 25%).
     * Resets every 10 stamps cycle.
     */
    static int calculateDiscount(int totalStamps) {
        int cycleStamps = totalStamps % 10;
        return (cycleStamps / 2) * 5;
    }

    class CardViewHolder extends RecyclerView.ViewHolder {
        private TextView cardBreweryName;
        private TextView stampCount;
        private TextView stampEmojis;
        private ProgressBar stampProgressBar;
        private Button shareButton;
        private Button viewHistoryButton;
        private TextView discountText;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardBreweryName = itemView.findViewById(R.id.cardBreweryName);
            stampCount = itemView.findViewById(R.id.stampCount);
            stampEmojis = itemView.findViewById(R.id.stampEmojis);
            stampProgressBar = itemView.findViewById(R.id.stampProgressBar);
            shareButton = itemView.findViewById(R.id.shareButton);
            viewHistoryButton = itemView.findViewById(R.id.viewHistoryButton);
            discountText = itemView.findViewById(R.id.discountText);
        }

        public void bind(LoyaltyCardEntity card) {
            // Load brewery name from database asynchronously
            String breweryId = card.getBreweryId();
            cardBreweryName.setText(R.string.loyalty_card_loading);

            executor.execute(() -> {
                try {
                    BreweryEntity brewery = Database.getInstance(itemView.getContext()).AleDAO().getAleByIdSync(breweryId);
                    final String displayName = (brewery != null && brewery.getName() != null && !brewery.getName().isEmpty()) ?
                            brewery.getName() : (itemView.getContext().getString(R.string.loyalty_card_brewery_fallback, breweryId));

                    itemView.post(() -> cardBreweryName.setText(displayName));
                } catch (Exception e) {
                    itemView.post(() -> cardBreweryName.setText(itemView.getContext().getString(R.string.loyalty_card_brewery_fallback, breweryId)));
                }
            });

            stampCount.setText(card.getStamps() + "/" + card.getMaxStamps());

            // Calculate progress percentage
            int progress = (int) ((card.getStamps() / (float) card.getMaxStamps()) * 100);
            stampProgressBar.setProgress(progress);

            // Show compact stamp display for the current discount cycle (10 stamps per cycle)
            int cycleStamps = card.getStamps() % 10;
            int cycleMax = 10;
            StringBuilder emojis = new StringBuilder();
            for (int i = 0; i < cycleMax; i++) {
                if (i < cycleStamps) {
                    emojis.append("🍺");
                } else {
                    emojis.append("○");
                }
            }
            stampEmojis.setText(emojis.toString());

            // Calculate and show discount with dynamic styling
            int discount = calculateDiscount(card.getStamps());
            android.content.Context ctx = itemView.getContext();
            if (discount > 0) {
                discountText.setText("🎁 " + ctx.getString(R.string.discount_label, discount));
                discountText.setTextColor(resolveThemeColor(ctx, R.attr.aleDiscountGreen, 0xFF66BB6A));
                discountText.setTextSize(15);
                discountText.setVisibility(View.VISIBLE);
            } else {
                discountText.setText("🍺 " + ctx.getString(R.string.discount_cycle_label, cycleStamps));
                discountText.setTextColor(resolveThemeColor(ctx, R.attr.aleTextHint, 0xFFB0BEC5));
                discountText.setTextSize(13);
                discountText.setVisibility(View.VISIBLE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCardClick(card);
            });


            shareButton.setOnClickListener(v -> {
                if (listener != null) listener.onShareClick(card);
            });

            viewHistoryButton.setOnClickListener(v -> {
                if (listener != null) listener.onHistoryClick(card);
            });

            // Long press to delete
            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onDeleteClick(card);
                return true;
            });
        }

        private int resolveThemeColor(android.content.Context context, int attr, int fallback) {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            if (context.getTheme().resolveAttribute(attr, typedValue, true)) {
                return typedValue.data;
            }
            return fallback;
        }
    }
}
