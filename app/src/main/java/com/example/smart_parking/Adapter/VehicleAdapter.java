package com.example.smart_parking.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.smart_parking.R;
import com.example.smart_parking.Model.Vehicle;

import java.util.List;

public class VehicleAdapter extends ArrayAdapter<Vehicle> {
    private Context context;
    private List<Vehicle> vehicles;

    public VehicleAdapter(Context context, List<Vehicle> vehicles) {
        super(context, R.layout.vehicle_item, vehicles);
        this.context = context;
        this.vehicles = vehicles;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.vehicle_item, parent, false);
            holder = new ViewHolder();
            holder.nameTextView = convertView.findViewById(R.id.vehicleName);
            holder.numberTextView = convertView.findViewById(R.id.vehicleNumber);
            holder.typeTextView = convertView.findViewById(R.id.vehicleType);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Vehicle vehicle = vehicles.get(position);
        holder.nameTextView.setText(vehicle.getVehicleName());
        holder.numberTextView.setText(vehicle.getVehicleNumber());
        holder.typeTextView.setText(vehicle.getVehicleType());

        return convertView;
    }

    static class ViewHolder {
        TextView nameTextView;
        TextView numberTextView;
        TextView typeTextView;
    }
} 