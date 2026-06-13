package com.qralarm.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;
import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {

    private List<Alarm> alarms = new ArrayList<>();
    private final AlarmActionListener listener;

    private static final String[] DAY_LABELS_TR = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};

    public AlarmAdapter(AlarmActionListener listener) {
        this.listener = listener;
    }

    public void setAlarms(List<Alarm> alarms) {
        this.alarms = alarms;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Alarm alarm = alarms.get(position);

        holder.tvTime.setText(alarm.getTimeString());

        if (alarm.label != null && !alarm.label.isEmpty()) {
            holder.tvLabel.setText(alarm.label);
            holder.tvLabel.setVisibility(View.VISIBLE);
        } else {
            holder.tvLabel.setVisibility(View.GONE);
        }

        // Repeat days
        StringBuilder daysText = new StringBuilder();
        if (alarm.repeats()) {
            boolean[] days = alarm.getRepeatDaysArray();
            for (int i = 0; i < 7; i++) {
                if (days[i]) {
                    if (daysText.length() > 0) daysText.append(", ");
                    daysText.append(DAY_LABELS_TR[i]);
                }
            }
        } else {
            daysText.append(holder.itemView.getContext().getString(R.string.one_time_alarm));
        }
        holder.tvDays.setText(daysText.toString());

        // QR indicator
        holder.ivQrIndicator.setVisibility(alarm.hasQrCode() ? View.VISIBLE : View.GONE);

        // Switch
        holder.switchEnabled.setOnCheckedChangeListener(null);
        holder.switchEnabled.setChecked(alarm.isEnabled);
        holder.switchEnabled.setOnCheckedChangeListener((v, checked) -> {
            if (!checked && AlarmService.isAlarmRinging(alarm.id)) {
                // Block disabling an alarm that is currently ringing.
                holder.switchEnabled.setOnCheckedChangeListener(null);
                holder.switchEnabled.setChecked(true);
                holder.switchEnabled.setOnCheckedChangeListener((vv, c) -> {
                    if (listener != null) listener.onAlarmToggle(alarm, c);
                });
                android.widget.Toast.makeText(v.getContext(),
                        R.string.cannot_disable_while_ringing,
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) listener.onAlarmToggle(alarm, checked);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAlarmEdit(alarm);
        });

        holder.btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add(0, 1, 0, R.string.edit);
            popup.getMenu().add(0, 2, 1, R.string.delete);
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    if (listener != null) listener.onAlarmEdit(alarm);
                    return true;
                } else if (item.getItemId() == 2) {
                    if (AlarmService.isAlarmRinging(alarm.id)) {
                        android.widget.Toast.makeText(v.getContext(),
                                R.string.cannot_delete_while_ringing,
                                android.widget.Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    if (listener != null) listener.onAlarmDelete(alarm);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return alarms.size();
    }

    static class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvLabel, tvDays;
        SwitchCompat switchEnabled;
        ImageView ivQrIndicator, btnMore;

        AlarmViewHolder(View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_item_time);
            tvLabel = itemView.findViewById(R.id.tv_item_label);
            tvDays = itemView.findViewById(R.id.tv_item_days);
            switchEnabled = itemView.findViewById(R.id.switch_item_enabled);
            ivQrIndicator = itemView.findViewById(R.id.iv_qr_indicator);
            btnMore = itemView.findViewById(R.id.btn_item_more);
        }
    }

    public interface AlarmActionListener {
        void onAlarmToggle(Alarm alarm, boolean enabled);
        void onAlarmEdit(Alarm alarm);
        void onAlarmDelete(Alarm alarm);
    }
}
