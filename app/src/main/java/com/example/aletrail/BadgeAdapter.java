package com.example.aletrail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private List<BadgeEntity> badges = new ArrayList<>();
    private OnBadgeClickListener listener;

    public interface OnBadgeClickListener {
        void onBadgeClick(BadgeEntity badge);
    }

    public BadgeAdapter(OnBadgeClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        BadgeEntity badge = badges.get(position);
        holder.bind(badge);
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    public void setBadges(List<BadgeEntity> newBadges) {
        final List<BadgeEntity> finalNewBadges = (newBadges == null) ? new ArrayList<>() : newBadges;

        // Use DiffUtil for efficient updates
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return badges.size();
            }

            @Override
            public int getNewListSize() {
                return finalNewBadges.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return badges.get(oldItemPosition).getBadgeId() ==
                        finalNewBadges.get(newItemPosition).getBadgeId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                BadgeEntity oldBadge = badges.get(oldItemPosition);
                BadgeEntity newBadge = finalNewBadges.get(newItemPosition);
                return oldBadge.isEarned() == newBadge.isEarned();
            }
        });

        this.badges = finalNewBadges;
        diffResult.dispatchUpdatesTo(this);
    }

    class BadgeViewHolder extends RecyclerView.ViewHolder {
        private TextView badgeIcon;
        private TextView badgeName;
        private TextView badgeDescription;
        private ProgressBar badgeProgress;
        private Chip earnedChip;

        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            badgeIcon = itemView.findViewById(R.id.badgeIcon);
            badgeName = itemView.findViewById(R.id.badgeName);
            badgeDescription = itemView.findViewById(R.id.badgeDescription);
            badgeProgress = itemView.findViewById(R.id.badgeProgress);
            earnedChip = itemView.findViewById(R.id.earnedChip);
        }

        public void bind(BadgeEntity badge) {
            badgeIcon.setText(badge.getBadgeIcon() != null ? badge.getBadgeIcon() : "🏆");
            badgeName.setText(badge.getBadgeName());
            badgeDescription.setText(badge.getBadgeDescription());

            if (badge.isEarned()) {
                badgeProgress.setVisibility(View.GONE);
                earnedChip.setVisibility(View.VISIBLE);
                itemView.setAlpha(1.0f);
            } else {
                badgeProgress.setVisibility(View.VISIBLE);
                earnedChip.setVisibility(View.GONE);
                itemView.setAlpha(0.6f);
                // TODO: Set actual progress based on user stats
                badgeProgress.setProgress(50);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onBadgeClick(badge);
            });
        }
    }
}
