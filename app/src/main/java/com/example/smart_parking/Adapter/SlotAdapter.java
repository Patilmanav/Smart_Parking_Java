package com.example.smart_parking.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smart_parking.Model.ParkingSlot;
import com.example.smart_parking.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class SlotAdapter extends BaseAdapter {
    private Context context;
    private List<ParkingSlot> slots;
    private String locationId;
    private String username;
    private FirebaseFirestore db;

    public SlotAdapter(Context context, List<ParkingSlot> slots, String locationId, String username) {
        this.context = context;
        this.slots = slots;
        this.locationId = locationId;
        this.username = username;
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public int getCount() {
        return slots.size();
    }

    @Override
    public Object getItem(int position) {
        return slots.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.slot_item, parent, false);
        }

        ParkingSlot slot = slots.get(position);
        TextView slotNumber = convertView.findViewById(R.id.slotNumber);
        TextView slotStatus = convertView.findViewById(R.id.slotStatus);
        Button bookButton = convertView.findViewById(R.id.bookButton);

        slotNumber.setText("Slot " + slot.getSlotNumber());
        slotStatus.setText(slot.getStatus());

        // Set button state based on slot status
        if (slot.getStatus().equals("Available")) {
            bookButton.setEnabled(true);
            bookButton.setBackgroundColor(Color.GREEN);
            bookButton.setText("Book");
        } else if (slot.getStatus().equals("Booked")) {
            if (slot.getBookedBy().equals(username)) {
                bookButton.setEnabled(true);
                bookButton.setBackgroundColor(Color.RED);
                bookButton.setText("Cancel");
            } else {
                bookButton.setEnabled(false);
                bookButton.setBackgroundColor(Color.GRAY);
                bookButton.setText("Booked");
            }
        }

        bookButton.setOnClickListener(v -> {
            if (slot.getStatus().equals("Available")) {
                showBookingDialog(slot);
            } else if (slot.getStatus().equals("Booked") && slot.getBookedBy().equals(username)) {
                showCancelDialog(slot);
            }
        });

        return convertView;
    }

    private void showBookingDialog(ParkingSlot slot) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.booking_dialog, null);
        dialog.setView(dialogView);
        final AlertDialog Adialog = dialog.create();

        EditText hoursInput = dialogView.findViewById(R.id.hoursInput);
        EditText minutesInput = dialogView.findViewById(R.id.minutesInput);
        Button confirmButton = dialogView.findViewById(R.id.confirmBooking);

        confirmButton.setOnClickListener(v -> {
            String hoursStr = hoursInput.getText().toString();
            String minutesStr = minutesInput.getText().toString();

            if (hoursStr.isEmpty() || minutesStr.isEmpty()) {
                showError("Please enter duration");
                return;
            }

            int hours = Integer.parseInt(hoursStr);
            int minutes = Integer.parseInt(minutesStr);

            if (hours == 0 && minutes == 0) {
                showError("Duration must be greater than 0");
                return;
            }

            // Calculate total duration in minutes
            int totalMinutes = (hours * 60) + minutes;

            // Update slot status
            slot.setStatus("Booked");
            slot.setBookedBy(username);
            slot.setBookingDuration(totalMinutes);

            db.collection("locations")
                .document(locationId)
                .collection("slots")
                .document(String.valueOf(slot.getSlotNumber()))
                .set(slot)
                .addOnSuccessListener(aVoid -> {
                    notifyDataSetChanged();
                    Adialog.dismiss();
                    showSuccess("Slot booked successfully");
                })
                .addOnFailureListener(e -> {
                    showError("Failed to book slot: " + e.getMessage());
                });
        });

        Adialog.show();
    }

    private void showCancelDialog(ParkingSlot slot) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel your booking?")
            .setPositiveButton("Yes", (dialog1, which) -> {
                // Update slot status
                slot.setStatus("Available");
                slot.setBookedBy(null);
                slot.setBookingDuration(0);

                db.collection("locations")
                    .document(locationId)
                    .collection("slots")
                    .document(String.valueOf(slot.getSlotNumber()))
                    .set(slot)
                    .addOnSuccessListener(aVoid -> {
                        notifyDataSetChanged();
                        showSuccess("Booking cancelled successfully");
                    })
                    .addOnFailureListener(e -> {
                        showError("Failed to cancel booking: " + e.getMessage());
                    });
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void showError(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
} 