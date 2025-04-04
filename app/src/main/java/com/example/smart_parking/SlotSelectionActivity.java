package com.example.smart_parking;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smart_parking.Model.Vehicle;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlotSelectionActivity extends AppCompatActivity implements PaymentResultListener {
    private static final String TAG = "SlotSelectionActivity";
    private static final long BOOKING_TIMEOUT = 300000; // 5 minutes in milliseconds
    private Map<String, Long> userLastBookingTime = new HashMap<>();

    private TextView locationName, locationAddress, totalSlots, hourlyRate;
    private GridLayout slotsContainer;
    private FirebaseFirestore db;
    private String locationId;
    private double hourlyRateValue;
    private String mapsLink;
    private Button viewOnMapButton;

    // Payment-related variables
    private int currentSlotNumber;
    private int currentHours;
    private double currentTotalAmount;
    private boolean isUnpark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slot_selection);

        // Initialize Razorpay
        Checkout.preload(getApplicationContext());
        Checkout checkout = new Checkout();
        checkout.setKeyID("rzp_test_f8lIskfQfhBNJA"); // Replace with your actual key

        isUnpark = false;
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get data from Intent
        Intent intent = getIntent();
        locationId = intent.getStringExtra("locationId");
        String name = intent.getStringExtra("name");
        String address = intent.getStringExtra("address");
        int slots = intent.getIntExtra("totalSlots", 0);
        hourlyRateValue = intent.getDoubleExtra("hourlyRate", 0.0);
        mapsLink = intent.getStringExtra("mapsLink");

        // Initialize UI elements
        locationName = findViewById(R.id.locationName);
        locationAddress = findViewById(R.id.locationAddress);
        totalSlots = findViewById(R.id.totalSlots);
        hourlyRate = findViewById(R.id.hourlyRate);
        slotsContainer = findViewById(R.id.slotsContainer);
        viewOnMapButton = findViewById(R.id.viewOnMapButton);

        // Set location details
        locationName.setText(name);
        locationAddress.setText(address);
        totalSlots.setText("Total Slots: " + slots);
        hourlyRate.setText("Rate: ₹" + hourlyRateValue + "/hour");

        // Set up map button
        if (mapsLink != null && !mapsLink.isEmpty()) {
            viewOnMapButton.setVisibility(View.VISIBLE);
            viewOnMapButton.setOnClickListener(v -> openLocationInMaps());
        } else {
            viewOnMapButton.setVisibility(View.GONE);
        }

        // Load slots dynamically
        loadSlots(slots);
    }

    private void openLocationInMaps() {
        if (mapsLink != null && !mapsLink.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsLink));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // If Google Maps app is not installed, open in browser
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsLink));
                startActivity(intent);
            }
        } else {
            showError("Location map link not available");
        }
    }

    private void loadSlots(int totalSlots) {
        slotsContainer.removeAllViews();

        // Calculate number of rows needed
        int numRows = (totalSlots + 1) / 2; // Round up division
        slotsContainer.setRowCount(numRows);

        for (int i = 1; i <= totalSlots; i++) {
            final int slotNumber = i; // Create a final copy for the lambda
            Button slotButton = new Button(this);
            slotButton.setId(slotNumber);
            slotButton.setText("Slot " + slotNumber);
            slotButton.setTextSize(16);
            slotButton.setPadding(16, 16, 16, 16);
            slotButton.setBackgroundResource(R.drawable.slot_available);
            slotButton.setTextColor(Color.WHITE);

            // Set layout parameters for grid
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            slotButton.setLayoutParams(params);

            // Add real-time listener for slot status
            String slotId = locationId + "_slot" + slotNumber;
            db.collection("parking_slots")
                    .document(slotId)
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Log.e(TAG, "Error listening to slot status", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String status = documentSnapshot.getString("booking_status");
                            String username = documentSnapshot.getString("username");
                            if (status != null) {
                                switch (status) {
                                    case "Booked":
                                        slotButton.setBackgroundResource(R.drawable.slot_booked);
                                        if (username != null && username.equals(MainActivity.log_username)) {
                                            // If slot is booked by current user, show unpark button
                                            slotButton.setText("Unpark Vehicle");
                                            slotButton.setEnabled(true);
                                            slotButton.setOnClickListener(v -> showUnparkDialog(slotNumber, documentSnapshot));
                                        } else {
                                            slotButton.setEnabled(false);
                                            slotButton.setText("Slot " + slotNumber);
                                            slotButton.setOnClickListener(v -> showError("This slot is booked by another user"));
                                        }
                                        break;
                                    case "Processing":
                                        slotButton.setBackgroundResource(R.drawable.slot_processing);
                                        if (username != null && username.equals(MainActivity.log_username)) {
                                            // If slot is in processing state and belongs to current user
                                            slotButton.setText("Continue Booking");
                                            slotButton.setEnabled(true);
                                            slotButton.setOnClickListener(v -> showDurationSelectionDialog(slotNumber));
                                        } else {
                                            slotButton.setEnabled(false);
                                            slotButton.setText("Slot " + slotNumber);
                                            slotButton.setOnClickListener(v -> showError("This slot is being processed by another user"));
                                        }
                                        break;
                                    case "Reserved":
                                        slotButton.setBackgroundResource(R.drawable.slot_reserved);
                                        slotButton.setEnabled(false);
                                        slotButton.setText("Reserved");
                                        slotButton.setOnClickListener(v -> showError("This slot is reserved by admin"));
                                        break;
                                    case "Available":
                                        slotButton.setBackgroundResource(R.drawable.slot_available);
                                        slotButton.setEnabled(true);
                                        slotButton.setText("Slot " + slotNumber);
                                        slotButton.setOnClickListener(v -> showVehicleSelectionDialog(slotNumber));
                                        break;
                                    default:
                                        slotButton.setBackgroundResource(R.drawable.slot_available);
                                        slotButton.setEnabled(true);
                                        slotButton.setText("Slot " + slotNumber);
                                        slotButton.setOnClickListener(v -> showVehicleSelectionDialog(slotNumber));
                                }
                            }
                        } else {
                            // If document doesn't exist, create it with initial status
                            Map<String, Object> data = new HashMap<>();
                            data.put("booking_status", "Available");
                            data.put("locationId", locationId);
                            data.put("slotNumber", slotNumber);
                            db.collection("parking_slots")
                                    .document(slotId)
                                    .set(data);
                            slotButton.setBackgroundResource(R.drawable.slot_available);
                            slotButton.setEnabled(true);
                            slotButton.setText("Slot " + slotNumber);
                            slotButton.setOnClickListener(v -> showVehicleSelectionDialog(slotNumber));
                        }
                    });

            slotsContainer.addView(slotButton);
        }
    }

    private void showUnparkDialog(int slotNumber, DocumentSnapshot documentSnapshot) {
        long bookingTime = documentSnapshot.getLong("booking_time");
        int bookedHours = documentSnapshot.getLong("hours").intValue();
        double bookedAmount = documentSnapshot.getDouble("amount");

        // Calculate actual parking duration
        long currentTime = System.currentTimeMillis();
        long actualDurationMillis = currentTime - bookingTime;
        double actualHours = actualDurationMillis / (1000.0 * 60 * 60);

        // Calculate charges
        double actualAmount = actualHours * hourlyRateValue;
        double extraAmount = Math.max(0, actualAmount - bookedAmount);

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Unpark Vehicle")
                .setMessage(String.format("Parking Duration: %.1f hours\n" +
                                "Booked Hours: %d\n" +
                                "Extra Hours: %.1f\n" +
                                "Extra Amount: ₹%.2f",
                        actualHours, bookedHours, Math.max(0, actualHours - bookedHours), extraAmount))
                .setPositiveButton("Pay & Unpark", (dialogInterface, i) -> {
                    if (extraAmount > 0) {
                        // If there are extra charges, initiate payment
                        initiateUnparkPayment(slotNumber, extraAmount, actualHours);
                    } else {
                        // If no extra charges, just unpark
                        unparkVehicle(slotNumber, actualHours);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void initiateUnparkPayment(int slotNumber, double amount, double actualHours) {
        try {
            Log.d("Payment","Initiating UnPark Payment");

            isUnpark = true;
            JSONObject options = new JSONObject();

            options.put("name", "Smart Parking");
            options.put("description", "Extra Parking Charges");
            options.put("currency", "INR");
            options.put("amount", (int)(amount * 100)); // Amount in paise
            options.put("theme.color", "#3399cc");
            options.put("prefill.email", MainActivity.log_username + "@example.com");
            options.put("prefill.contact", "9999999999");
            options.put("prefill.name", MainActivity.log_username);

            JSONObject retryObj = new JSONObject();
            retryObj.put("enabled", true);
            retryObj.put("max_count", 4);
            options.put("retry", retryObj);

            // Store unpark details for callback
            currentSlotNumber = slotNumber;
            currentHours = (int) actualHours;
            currentTotalAmount = amount;

            Checkout checkout = new Checkout();
            checkout.setImage(R.drawable.ic_launcher_foreground);
            checkout.open(this, options);

        } catch(Exception e) {
            Log.e(TAG, "Error in starting Razorpay Checkout", e);
            showError("Payment initialization failed. Please try again.");
        }
    }

    private void unparkVehicle(int slotNumber, double actualHours) {
        String slotId = locationId + "_slot" + slotNumber;

        Map<String, Object> data = new HashMap<>();
        data.put("booking_status", "Available");
        data.put("username", null);
        data.put("vehicle_number", null);
        data.put("booking_time", null);
        data.put("hours", null);
        data.put("amount", null);
        data.put("locationId", locationId);
        data.put("slotNumber", slotNumber);
        data.put("actual_hours", actualHours);
        data.put("unpark_time", System.currentTimeMillis());

        db.collection("parking_slots")
                .document(slotId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    showSuccessDialog("Vehicle unparked successfully!");
                })
                .addOnFailureListener(e -> showErrorDialog("Failed to unpark vehicle: " + e.getMessage()));
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        Log.d("ISUNPARK","is unpark: "+isUnpark);
        if (isUnpark) {
            // This is an unpark payment
            String slotId = locationId + "_slot" + currentSlotNumber;

            Map<String, Object> data = new HashMap<>();
            data.put("booking_status", "Available");
            data.put("username", null);
            data.put("vehicle_number", null);
            data.put("booking_time", null);
            data.put("hours", null);
            data.put("amount", null);
            data.put("locationId", locationId);
            data.put("slotNumber", currentSlotNumber);
            data.put("actual_hours", currentHours);
            data.put("unpark_time", System.currentTimeMillis());
            data.put("extra_payment_id", razorpayPaymentID);
            data.put("extra_amount", currentTotalAmount);

            db.collection("parking_slots")
                    .document(slotId)
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        showSuccessDialog("Extra charges paid and vehicle unparked successfully!");
                    })
                    .addOnFailureListener(e -> showErrorDialog("Failed to update unpark status: " + e.getMessage()));
        } else {
            // This is a booking payment (existing code)
            String slotId = locationId + "_slot" + currentSlotNumber;

            Map<String, Object> bookingData = new HashMap<>();
            bookingData.put("booking_status", "Booked");
            bookingData.put("username", MainActivity.log_username);
            bookingData.put("hours", currentHours);
            bookingData.put("amount", currentTotalAmount);
            bookingData.put("booking_time", System.currentTimeMillis());
            bookingData.put("locationId", locationId);
            bookingData.put("slotNumber", currentSlotNumber);
            bookingData.put("payment_status", "Completed");
            bookingData.put("razorpay_payment_id", razorpayPaymentID);

            db.collection("parking_slots")
                    .document(slotId)
                    .set(bookingData)
                    .addOnSuccessListener(aVoid -> {
                        Button slotButton = findViewById(currentSlotNumber);
                        if (slotButton != null) {
                            slotButton.setBackgroundResource(R.drawable.slot_booked);
                            slotButton.setEnabled(false);
                        }
                        showSuccessDialog("Payment successful! Your slot has been booked.");
                        userLastBookingTime.put(MainActivity.log_username, System.currentTimeMillis());
                    })
                    .addOnFailureListener(e -> showErrorDialog("Failed to update booking status: " + e.getMessage()));
        }
    }

    @Override
    public void onPaymentError(int code, String response) {
        // Payment failed or cancelled
        String slotId = locationId + "_slot" + currentSlotNumber;

        Map<String, Object> resetData = new HashMap<>();
        resetData.put("booking_status", "Available");
        resetData.put("username", null);
        resetData.put("vehicle_number", null);
        resetData.put("timestamp", null);
        resetData.put("locationId", locationId);
        resetData.put("slotNumber", currentSlotNumber);
        resetData.put("payment_status", "Failed");

        // Reset Firestore slot status
        db.collection("parking_slots")
                .document(slotId)
                .set(resetData)
                .addOnSuccessListener(aVoid -> {
                    // Update UI
                    Button slotButton = findViewById(currentSlotNumber);
                    if (slotButton != null) {
                        slotButton.setBackgroundResource(R.drawable.slot_available);
                    }
                    showErrorDialog("Payment failed or cancelled. Please try again.");
                })
                .addOnFailureListener(e -> showErrorDialog("Failed to reset slot status: " + e.getMessage()));
    }

    /**
     * Show a success dialog.
     */
    private void showSuccessDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Show an error dialog.
     */
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showVehicleSelectionDialog(int slotNumber) {
        // Check if user has booked recently
        Long lastBookingTime = userLastBookingTime.get(MainActivity.log_username);
        if (lastBookingTime != null && System.currentTimeMillis() - lastBookingTime < BOOKING_TIMEOUT) {
            long remainingTime = (BOOKING_TIMEOUT - (System.currentTimeMillis() - lastBookingTime)) / 1000;
            showError("Please wait " + remainingTime + " seconds before booking another slot");
            return;
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.vehicle_selection, null);
        dialog.setView(dialogView);
        final AlertDialog Adialog = dialog.create();

        Spinner vehicleSpinner = dialogView.findViewById(R.id.vehicleSpinner);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Load user's vehicles
        List<String> vehicleNumbers = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicleNumbers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vehicleSpinner.setAdapter(adapter);

        db.collection("users")
                .document(MainActivity.log_username)
                .collection("vehicles")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Vehicle vehicle = document.toObject(Vehicle.class);
                        vehicleNumbers.add(vehicle.getVehicleNumber());
                    }
                    adapter.notifyDataSetChanged();
                });

        confirmButton.setOnClickListener(v -> {
            String selectedVehicle = vehicleSpinner.getSelectedItem().toString();
            if (!selectedVehicle.isEmpty()) {
                // Set slot status to Processing
                Map<String, Object> data = new HashMap<>();
                data.put("booking_status", "Processing");
                data.put("username", MainActivity.log_username);
                data.put("vehicle_number", selectedVehicle);
                data.put("timestamp", System.currentTimeMillis());
                data.put("locationId", locationId);
                data.put("slotNumber", slotNumber);

                String slotId = locationId + "_slot" + slotNumber;
                db.collection("parking_slots")
                        .document(slotId)
                        .set(data)
                        .addOnSuccessListener(aVoid -> {
                            // Update button appearance
                            Button slotButton = findViewById(slotNumber);
                            if (slotButton != null) {
                                slotButton.setBackgroundResource(R.drawable.slot_processing);
                            }
                            Adialog.dismiss();
                            showDurationSelectionDialog(slotNumber);
                        });
            }
        });

        cancelButton.setOnClickListener(v -> Adialog.dismiss());

        Adialog.show();
    }

    private void showDurationSelectionDialog(int slotNumber) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.duration_selection, null);
        dialog.setView(dialogView);
        final AlertDialog Adialog = dialog.create();

        EditText hoursInput = dialogView.findViewById(R.id.hoursInput);
        TextView totalAmountText = dialogView.findViewById(R.id.totalAmount);
        Button payButton = dialogView.findViewById(R.id.payButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Update total amount when hours change
        hoursInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int hours = Integer.parseInt(s.toString());
                    double totalAmount = hours * hourlyRateValue;
                    totalAmountText.setText(String.format("Total Amount: ₹%.2f", totalAmount));
                } catch (NumberFormatException e) {
                    totalAmountText.setText("Total Amount: ₹0.00");
                        }
                    }
                });

        payButton.setOnClickListener(v -> {
            try {
                int hours = Integer.parseInt(hoursInput.getText().toString());
                if (hours <= 0) {
                    showError("Please enter a valid number of hours");
                    return;
                }

                double totalAmount = hours * hourlyRateValue;
                Adialog.dismiss();
                initiatePayment(slotNumber, hours, totalAmount);
            } catch (NumberFormatException e) {
                showError("Please enter a valid number of hours");
            }
        });

        cancelButton.setOnClickListener(v -> {
            // Reset slot status
            String slotId = locationId + "_slot" + slotNumber;
            Map<String, Object> data = new HashMap<>();
            data.put("booking_status", "Available");
            data.put("username", null);
            data.put("vehicle_number", null);
            data.put("timestamp", null);
            data.put("locationId", locationId);
            data.put("slotNumber", slotNumber);

            db.collection("parking_slots")
                    .document(slotId)
                    .set(data);

            // Update button appearance
            Button slotButton = findViewById(slotNumber);
            if (slotButton != null) {
                slotButton.setBackgroundResource(R.drawable.slot_available);
            }
            Adialog.dismiss();
        });

        Adialog.show();
    }

    private void initiatePayment(int slotNumber, int hours, double totalAmount) {
        // Store payment details
        currentSlotNumber = slotNumber;
        currentHours = hours;
        currentTotalAmount = totalAmount;

        try {
            Log.d("Payment","Initiating Payment");
            isUnpark = false;
            JSONObject options = new JSONObject();

            options.put("name", "Smart Parking");
            options.put("description", "Parking Fee for " + hours + " hours");
            options.put("currency", "INR");
            options.put("amount", (int)(totalAmount * 100)); // Amount in paise
            options.put("theme.color", "#3399cc");
            options.put("prefill.email", MainActivity.log_username + "@example.com");
            options.put("prefill.contact", "9999999999");
            options.put("prefill.name", MainActivity.log_username);

            JSONObject retryObj = new JSONObject();
            retryObj.put("enabled", true);
            retryObj.put("max_count", 4);
            options.put("retry", retryObj);

            Checkout checkout = new Checkout();
            checkout.setImage(R.drawable.ic_launcher_foreground);
            checkout.open(this, options);

        } catch(Exception e) {
            Log.e(TAG, "Error in starting Razorpay Checkout", e);
            showError("Payment initialization failed. Please try again.");
        }
    }
}