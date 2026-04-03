package com.example.aletrail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    public interface OnMarkReadClickListener {
        void onMarkReadClick(NotificationEntity notification);
    }

    private final List<NotificationEntity> notifications = new ArrayList<>();
    private final OnMarkReadClickListener markReadClickListener;

    public NotificationsAdapter(OnMarkReadClickListener markReadClickListener) {
        this.markReadClickListener = markReadClickListener;
    }

    public void setNotifications(List<NotificationEntity> items) {
        notifications.clear();
        if (items != null) {
            notifications.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(notifications.get(position));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView notificationType;
        private final TextView notificationTitle;
        private final TextView notificationDescription;
        private final TextView notificationTimestamp;
        private final Button markAsReadButton;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationType = itemView.findViewById(R.id.notificationTypeChip);
            notificationTitle = itemView.findViewById(R.id.notificationTitle);
            notificationDescription = itemView.findViewById(R.id.notificationDescription);
            notificationTimestamp = itemView.findViewById(R.id.notificationTimestamp);
            markAsReadButton = itemView.findViewById(R.id.markAsReadButton);
        }

        void bind(NotificationEntity notification) {
            notificationType.setText(mapType(notification.getType()));
            notificationTitle.setText(notification.getTitle());
            notificationDescription.setText(notification.getDescription());

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            notificationTimestamp.setText(sdf.format(new Date(notification.getTimestamp())));

            itemView.setAlpha(notification.isRead() ? 0.65f : 1f);
            markAsReadButton.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
            markAsReadButton.setOnClickListener(v -> {
                if (markReadClickListener != null) {
                    markReadClickListener.onMarkReadClick(notification);
                }
            });
        }

        private String mapType(String type) {
            if ("check-in".equals(type)) return itemView.getContext().getString(R.string.notification_type_checkin);
            if ("review".equals(type)) return itemView.getContext().getString(R.string.notification_type_review);
            if ("badge".equals(type)) return itemView.getContext().getString(R.string.notification_type_badge);
            return itemView.getContext().getString(R.string.notification_type_general);
        }
    }
}

