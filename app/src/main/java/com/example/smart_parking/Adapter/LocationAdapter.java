package com.example.smart_parking.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.smart_parking.Model.ParkingLocation;
import com.example.smart_parking.R;

import java.util.List;

public class LocationAdapter extends ArrayAdapter<ParkingLocation> {
    private Context context;
    private List<ParkingLocation> locations;
    private OnLocationLongClickListener longClickListener;
    private OnLocationClickListener clickListener;
    private OnShowLocationClickListener showLocationClickListener;

    public interface OnLocationLongClickListener {
        void onLocationLongClick(ParkingLocation location);
    }

    public interface OnLocationClickListener {
        void onLocationClick(ParkingLocation location);
    }

    public interface OnShowLocationClickListener {
        void onShowLocationClick(ParkingLocation location);
    }

    public LocationAdapter(Context context, List<ParkingLocation> locations) {
        super(context, R.layout.item_location, locations);
        this.context = context;
        this.locations = locations;
    }

    public void setOnLocationLongClickListener(OnLocationLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnLocationClickListener(OnLocationClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnShowLocationClickListener(OnShowLocationClickListener listener) {
        this.showLocationClickListener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_location, parent, false);
            holder = new ViewHolder();
            holder.nameTextView = convertView.findViewById(R.id.locationName);
            holder.addressTextView = convertView.findViewById(R.id.locationAddress);
            holder.slotsTextView = convertView.findViewById(R.id.totalSlots);
            holder.rateTextView = convertView.findViewById(R.id.hourlyRate);
            holder.distanceTextView = convertView.findViewById(R.id.distanceText);
            holder.showLocationButton = convertView.findViewById(R.id.showLocationButton);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ParkingLocation location = locations.get(position);
        holder.nameTextView.setText(location.getName());
        holder.addressTextView.setText(location.getAddress());
        holder.slotsTextView.setText("Total Slots: " + location.getTotalSlots());
        holder.rateTextView.setText("Rate: $" + location.getHourlyRate() + "/hour");
        
        // Set distance if available
        if (location.getDistance() > 0) {
            holder.distanceTextView.setText(String.format("%.1f km away", location.getDistance()));
        } else {
            holder.distanceTextView.setText("Distance calculating...");
        }

        // Set click listener for the entire item
        convertView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onLocationClick(location);
            }
        });

        // Set long click listener
        convertView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLocationLongClick(location);
            }
            return true;
        });

        // Set show location button click listener
        holder.showLocationButton.setOnClickListener(v -> {
            if (showLocationClickListener != null) {
                showLocationClickListener.onShowLocationClick(location);
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView nameTextView;
        TextView addressTextView;
        TextView slotsTextView;
        TextView rateTextView;
        TextView distanceTextView;
        Button showLocationButton;
    }
} 