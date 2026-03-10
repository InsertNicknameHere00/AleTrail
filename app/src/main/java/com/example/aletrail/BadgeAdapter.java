package com.example.aletrail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        private Chip earnedChip;

        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            badgeIcon = itemView.findViewById(R.id.badgeIcon);
            badgeName = itemView.findViewById(R.id.badgeName);
            badgeDescription = itemView.findViewById(R.id.badgeDescription);
            earnedChip = itemView.findViewById(R.id.earnedChip);
        }

        public void bind(BadgeEntity badge) {
            badgeIcon.setText(badge.getBadgeIcon() != null ? badge.getBadgeIcon() : "🏆");
            badgeName.setText(badge.getBadgeName());
            badgeDescription.setText(badge.getBadgeDescription());

            // Resolve theme colors
            android.content.Context ctx = itemView.getContext();
            int goldColor = resolveThemeColor(ctx, R.attr.aleBadgeNameEarned, 0xFFFFD54F);
            int greyColor = resolveThemeColor(ctx, R.attr.aleBadgeNameUnearned, 0xFF9E9E9E);

            if (badge.isEarned()) {
                // Earned: full brightness, golden glow, show earned chip with date
                itemView.setAlpha(1.0f);
                badgeIcon.setAlpha(1.0f);
                badgeName.setTextColor(goldColor);
                earnedChip.setVisibility(View.VISIBLE);

                // Show earned date
                if (badge.getEarnedTimestamp() > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    String dateStr = sdf.format(new Date(badge.getEarnedTimestamp()));
                    earnedChip.setText(ctx.getString(R.string.badge_earned_with_date, dateStr));
                } else {
                    earnedChip.setText(R.string.badge_earned);
                }
            } else {
                // Unearned: greyed out, desaturated
                itemView.setAlpha(0.5f);
                badgeIcon.setAlpha(0.3f);
                badgeName.setTextColor(greyColor);
                earnedChip.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onBadgeClick(badge);
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
