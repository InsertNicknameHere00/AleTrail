package com.example.aletrail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LoyaltyCardAdapter extends RecyclerView.Adapter<LoyaltyCardAdapter.CardViewHolder> {

    private List<LoyaltyCardEntity> cards = new ArrayList<>();
    private OnCardClickListener listener;

    public interface OnCardClickListener {
        void onCardClick(LoyaltyCardEntity card);
        void onQRCodeClick(LoyaltyCardEntity card);
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

        // Use DiffUtil for efficient updates
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

    class CardViewHolder extends RecyclerView.ViewHolder {
        private TextView cardBreweryName;
        private TextView stampCount;
        private TextView stampEmojis;
        private ProgressBar stampProgressBar;
        private ImageButton qrButton;
        private Button shareButton;
        private Button viewHistoryButton;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardBreweryName = itemView.findViewById(R.id.cardBreweryName);
            stampCount = itemView.findViewById(R.id.stampCount);
            stampEmojis = itemView.findViewById(R.id.stampEmojis);
            stampProgressBar = itemView.findViewById(R.id.stampProgressBar);
            qrButton = itemView.findViewById(R.id.qrButton);
            shareButton = itemView.findViewById(R.id.shareButton);
            viewHistoryButton = itemView.findViewById(R.id.viewHistoryButton);
        }

        public void bind(LoyaltyCardEntity card) {
            // TODO: Load brewery name from database
            cardBreweryName.setText("Brewery #" + card.getBreweryId());

            stampCount.setText(card.getStamps() + "/" + card.getMaxStamps());

            // Calculate progress percentage
            int progress = (int) ((card.getStamps() / (float) card.getMaxStamps()) * 100);
            stampProgressBar.setProgress(progress);

            // Generate stamp emojis
            StringBuilder emojis = new StringBuilder();
            for (int i = 0; i < card.getMaxStamps(); i++) {
                if (i < card.getStamps()) {
                    emojis.append("⭐");
                } else {
                    emojis.append("⚪");
                }
            }
            stampEmojis.setText(emojis.toString());

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCardClick(card);
            });

            qrButton.setOnClickListener(v -> {
                if (listener != null) listener.onQRCodeClick(card);
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
    }
}
