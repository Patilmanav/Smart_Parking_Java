package com.example.smart_parking;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.LayoutInflater;
import android.widget.*;

import com.example.smart_parking.Adapter.LocationAdapter;
import com.example.smart_parking.Model.ParkingLocation;
import com.example.smart_parking.Model.Vehicle;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.Collections;
import java.util.Comparator;

public class HomeScreen extends AppCompatActivity {
    private static final String TAG = "HomeScreen";
    private static final long BOOKING_TIMEOUT = 120000; // 2 minutes in milliseconds
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private FirebaseFirestore db;
    private ListView locationsList;
    private List<ParkingLocation> locations;
    private LocationAdapter locationAdapter;
    private String username;
    private String uname;
    private String upass;
    private String dataTime;
    private Button s1, s2, s3, s4, s5, s6;
    private EditText v_name, v_number, timePickerHours, timePickerMin;
    private Button sub_details;
    private ParkingLocation selectedLocation;
    private Map<String, Long> userLastBookingTime = new HashMap<>();
    private FusedLocationProviderClient fusedLocationClient;
    private android.location.Location userLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_screen);

        db = FirebaseFirestore.getInstance();
        username = MainActivity.log_username;
        locations = new ArrayList<>();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize views
        locationsList = findViewById(R.id.locationsList);
        FloatingActionButton profileButton = findViewById(R.id.profileButton);
        Button logoutButton = findViewById(R.id.logoutButton);

        // Setup adapter
        locationAdapter = new LocationAdapter(this, locations);
        locationsList.setAdapter(locationAdapter);

        // Set click listener for location selection
        locationAdapter.setOnLocationClickListener(location -> {
            selectedLocation = location;
            Intent intent = new Intent(HomeScreen.this, SlotSelectionActivity.class);
            intent.putExtra("locationId", selectedLocation.getLocationId());
            intent.putExtra("name", selectedLocation.getName());
            intent.putExtra("address", selectedLocation.getAddress());
            intent.putExtra("totalSlots", selectedLocation.getTotalSlots());
            intent.putExtra("hourlyRate", selectedLocation.getHourlyRate());
            intent.putExtra("mapsLink", selectedLocation.getMapsLink());
            startActivity(intent);
        });

        // Set show location button click listener
        locationAdapter.setOnShowLocationClickListener(location -> {
            if (location.getMapsLink() != null && !location.getMapsLink().isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(location.getMapsLink()));
                    intent.setPackage("com.google.android.apps.maps");
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(location.getMapsLink()));
                    startActivity(intent);
                }
            } else {
                showError("Location map link not available");
            }
        });

        // Check location permission and get user location
        checkLocationPermission();

        // Load locations
        loadLocations();

        // Set click listener for profile button
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeScreen.this, UserProfileActivity.class);
            startActivity(intent);
        });

        // Set click listener for logout button
        logoutButton.setOnClickListener(v -> {
            showLogoutConfirmation();
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getUserLocation();
        }
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            userLocation = location;
                            updateLocationDistances();
                        }
                    });
        }
    }

    private void updateLocationDistances() {
        if (userLocation != null) {
            for (ParkingLocation location : locations) {
                double distance = location.calculateDistance(
                    userLocation.getLatitude(),
                    userLocation.getLongitude()
                );
                location.setDistance(distance);
            }
            
            // Sort locations by distance
            Collections.sort(locations, (l1, l2) -> 
                Double.compare(l1.getDistance(), l2.getDistance()));
            
            locationAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            }
        }
    }

    private void loadLocations() {
        db.collection("locations")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                locations.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    ParkingLocation location = document.toObject(ParkingLocation.class);
                    location.setLocationId(document.getId());
                    locations.add(location);
                }
                if (userLocation != null) {
                    updateLocationDistances();
                }
                locationAdapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading locations", e);
                showError("Error loading locations: " + e.getMessage());
            });
    }

    private void showLocationDetails(ParkingLocation location) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.location_details, null);
        dialog.setView(dialogView);
        final AlertDialog Adialog = dialog.create();

        // Set location details
        TextView locationName = dialogView.findViewById(R.id.locationName);
        TextView locationAddress = dialogView.findViewById(R.id.locationAddress);
        TextView totalSlots = dialogView.findViewById(R.id.totalSlots);
        TextView hourlyRate = dialogView.findViewById(R.id.hourlyRate);
        LinearLayout slotsContainer = dialogView.findViewById(R.id.slotsContainer);

        locationName.setText(location.getName());
        locationAddress.setText(location.getAddress());
        totalSlots.setText("Total Slots: " + location.getTotalSlots());
        hourlyRate.setText("Rate: $" + location.getHourlyRate() + "/hour");

        // Clear existing buttons
        slotsContainer.removeAllViews();

        // Create buttons for each slot
        for (int i = 1; i <= location.getTotalSlots(); i++) {
            Button slotButton = new Button(this);
            slotButton.setId(View.generateViewId());
            slotButton.setText("Slot " + i);
            slotButton.setBackgroundColor(Color.parseColor("#006600")); // Green color for available slots
            
            // Set button layout parameters
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 8, 0, 8);
            slotButton.setLayoutParams(params);

            // Add click listener
            final int slotNumber = i;
            slotButton.setOnClickListener(v -> checkBookingStatus(slotButton, v));

            slotsContainer.addView(slotButton);
        }

        // Check booking status for all slots
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("parking_slots").get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String slotId = document.getId();
                        String bookingStatus = document.getString("booking_status");
                        
                        // Find the corresponding button
                        for (int i = 0; i < slotsContainer.getChildCount(); i++) {
                            Button button = (Button) slotsContainer.getChildAt(i);
                            if (button.getText().toString().equals(slotId)) {
                                if ("Booked".equals(bookingStatus)) {
                                    button.setBackgroundColor(Color.RED);
                                } else if ("Processing".equals(bookingStatus)) {
                                    // Get timestamp with null check
                                    Long timestamp = document.getLong("timestamp");
                                    if (timestamp != null) {
                                        // Check if processing time has expired (30 seconds)
                                        if (System.currentTimeMillis() - timestamp > 30000) {
                                            // Reset slot status
                                            Map<String, Object> data = new HashMap<>();
                                            data.put("booking_status", "Available");
                                            data.put("username", null);
                                            data.put("vehicle_number", null);
                                            data.put("timestamp", null);
                                            db.collection("parking_slots").document(slotId).set(data);
                                            button.setBackgroundColor(Color.parseColor("#006600"));
                                        } else {
                                            button.setBackgroundColor(Color.YELLOW);
                                        }
                                    } else {
                                        // If no timestamp, treat as available
                                        Map<String, Object> data = new HashMap<>();
                                        data.put("booking_status", "Available");
                                        data.put("username", null);
                                        data.put("vehicle_number", null);
                                        data.put("timestamp", null);
                                        db.collection("parking_slots").document(slotId).set(data);
                                        button.setBackgroundColor(Color.parseColor("#006600"));
                                    }
                                }
                                break;
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Error getting documents: ", task.getException());
                }
            });

        Adialog.show();
    }

    private void alreadyBooked(Button btn){
        Log.d("MyApp",btn.getText()+" Tapped");

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.already_booked, null);
        dialog.setView(dialogView);
        final AlertDialog Adialog = dialog.create();

        sub_details = dialogView.findViewById(R.id.detail_submit);

        sub_details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MyApp","on Clicked");
                Adialog.dismiss();

            }
        });

        Adialog.show();

    }
    private void cancelBooking(Button btn){

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.cancel_booking, null);
        dialog.setView(dialogView);
        final AlertDialog Adialog = dialog.create();
        TextView u_name = dialogView.findViewById(R.id.u_name);
        TextView showTime = dialogView.findViewById(R.id.show_time);
        TextView Timestamp = dialogView.findViewById(R.id.timeStamp);

        Button checkout = dialogView.findViewById(R.id.checkout_button);
        Button close = dialogView.findViewById(R.id.cancel_button);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("parking_slots").document(btn.getText().toString()).get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(task.isSuccessful()){

                                    u_name.setText("USERNAME: "+task.getResult().getString("username").trim());
                                    Timestamp.setText("TimeStamp: "+task.getResult().getString("TimeStamp").trim());
                                    showTime.setText("Booked For: "+task.getResult().getString("timeHours").trim()+"Hours"+
                                            task.getResult().getString("timeMin").trim()+"Min");
                                }
                            }
                        });

        checkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                btn.setBackgroundColor(Color.parseColor("#006600"));
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, Object> data = new HashMap<>();
                data.put("booking_status","null");
                db.collection("parking_slots").document(btn.getText().toString()).set(data)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d("Update","Success");
                                Adialog.dismiss();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("Update","Failed!!");

                            }
                        });
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MyApp","on Clicked");
                Adialog.dismiss();

            }
        });

        Adialog.show();
    }

    private void checkBookingStatus(Button btn, View v) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("parking_slots").get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    boolean not_booked = true;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        if (btn.getText().toString().equals(document.getId())) {
                            String status = document.getString("booking_status");
                            if ("Booked".equals(status)) {
                                if (document.getString("username").equals(MainActivity.log_username)) {
                                    cancelBooking(btn);
                                } else {
                                    alreadyBooked(btn);
                                }
                                not_booked = false;
                                break;
                            } else if ("Processing".equals(status)) {
                                // Get timestamp with null check
                                Long timestamp = document.getLong("timestamp");
                                if (timestamp != null) {
                                    // Check if processing time has expired
                                    if (System.currentTimeMillis() - timestamp > 30000) {
                                        // Reset slot status and allow booking
                                        Map<String, Object> data = new HashMap<>();
                                        data.put("booking_status", "Available");
                                        data.put("username", null);
                                        data.put("vehicle_number", null);
                                        data.put("timestamp", null);
                                        db.collection("parking_slots").document(btn.getText().toString()).set(data);
                                        not_booked = true;
                                    } else {
                                        alreadyBooked(btn);
                                        not_booked = false;
                                    }
                                } else {
                                    // If no timestamp, treat as available
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("booking_status", "Available");
                                    data.put("username", null);
                                    data.put("vehicle_number", null);
                                    data.put("timestamp", null);
                                    db.collection("parking_slots").document(btn.getText().toString()).set(data);
                                    not_booked = true;
                                }
                                break;
                            }
                        }
                    }
                    if (not_booked) {
                        showVehicleSelectionDialog(btn);
                    }
                } else {
                    Log.e(TAG, "Error getting documents: ", task.getException());
                }
            });
    }

    private void showVehicleSelectionDialog(Button btn) {
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
        FirebaseFirestore db = FirebaseFirestore.getInstance();
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

                db.collection("parking_slots")
                    .document(btn.getText().toString())
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        btn.setBackgroundColor(Color.YELLOW);
                        Adialog.dismiss();
                        showDurationSelectionDialog(btn, selectedLocation.getHourlyRate());
                    });
            }
        });

        cancelButton.setOnClickListener(v -> Adialog.dismiss());

        Adialog.show();
    }

    private void showDurationSelectionDialog(Button btn, double hourlyRate) {
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
                    double totalAmount = hours * hourlyRate;
                    totalAmountText.setText(String.format("Total Amount: $%.2f", totalAmount));
                } catch (NumberFormatException e) {
                    totalAmountText.setText("Total Amount: $0.00");
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

                double totalAmount = hours * hourlyRate;
                initiatePayment(btn, hours, totalAmount);
            } catch (NumberFormatException e) {
                showError("Please enter a valid number of hours");
            }
        });

        cancelButton.setOnClickListener(v -> {
            // Reset slot status
            Map<String, Object> data = new HashMap<>();
            data.put("booking_status", "Available");
            data.put("username", null);
            data.put("vehicle_number", null);
            data.put("timestamp", null);
            db.collection("parking_slots")
                .document(btn.getText().toString())
                .set(data);
            btn.setBackgroundColor(Color.parseColor("#006600"));
            Adialog.dismiss();
        });

        Adialog.show();
    }

    private void initiatePayment(Button btn, int hours, double amount) {
        // Here you would integrate with your payment gateway
        // For now, we'll simulate a successful payment
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Payment")
            .setMessage(String.format("Processing payment of $%.2f...", amount))
            .setCancelable(false);

        final AlertDialog paymentDialog = dialog.create();
        paymentDialog.show();

        // Simulate payment processing
        new Handler().postDelayed(() -> {
            // Update slot status to Booked
            Map<String, Object> data = new HashMap<>();
            data.put("booking_status", "Booked");
            data.put("username", MainActivity.log_username);
            data.put("hours", hours);
            data.put("amount", amount);
            data.put("booking_time", System.currentTimeMillis());

            db.collection("parking_slots")
                .document(btn.getText().toString())
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    btn.setBackgroundColor(Color.RED);
                    paymentDialog.dismiss();
                    showSuccess("Slot booked successfully!");
                    userLastBookingTime.put(MainActivity.log_username, System.currentTimeMillis());
                })
                .addOnFailureListener(e -> {
                    paymentDialog.dismiss();
                    showError("Failed to complete booking: " + e.getMessage());
                });
        }, 2000); // Simulate 2-second payment processing
    }

    private void showSuccess(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(getResources().getColor(R.color.green));
        snackbar.show();
    }


    private void showError(String message) {
        Log.e(TAG, "Error: " + message);
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(getResources().getColor(R.color.red));
        snackbar.show();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes", (dialog, which) -> {
                // Clear user data
                MainActivity.log_username = null;
                MainActivity.log_password = null;
                
                // Return to login screen
                Intent intent = new Intent(HomeScreen.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            })
            .setNegativeButton("No", null)
            .show();
    }
}