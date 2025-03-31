package com.example.smart_parking;

import com.example.smart_parking.Model.UserProfile;
import com.example.smart_parking.Model.Vehicle;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.EditText;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final String PREF_NAME = "SmartParkingPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER_ME = "remember_me";
    
    public static String log_username;
    public static String log_password;
    EditText username, password, reg_username, reg_password,
            reg_firstName, reg_lastName, reg_email, reg_phone;
    Button login, signUp, reg_register;
    CheckBox rememberMe;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
            showError("Error initializing app: " + e.getMessage());
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        login = findViewById(R.id.login);
        signUp = findViewById(R.id.signUp);
        rememberMe = findViewById(R.id.rememberMe);

        // Check if credentials are saved
        if (sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)) {
            String savedUsername = sharedPreferences.getString(KEY_USERNAME, "");
            String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");
            username.setText(savedUsername);
            password.setText(savedPassword);
            rememberMe.setChecked(true);
        }

        ClickLogin();

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClickSignUp();
            }
        });
    }

    private void ClickLogin() {
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String usernameStr = username.getText().toString().trim();
                String passwordStr = password.getText().toString().trim();

                if (usernameStr.isEmpty() || passwordStr.isEmpty()) {
                    showError("Please fill all fields");
                    return;
                }

                db.collection("users")
                    .document(usernameStr)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            UserProfile userProfile = documentSnapshot.toObject(UserProfile.class);
                            if (userProfile != null && passwordStr.equals(userProfile.getPassword())) {
                                log_username = usernameStr;
                                log_password = passwordStr;
                                
                                // Save credentials if Remember Me is checked
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                if (rememberMe.isChecked()) {
                                    editor.putString(KEY_USERNAME, usernameStr);
                                    editor.putString(KEY_PASSWORD, passwordStr);
                                    editor.putBoolean(KEY_REMEMBER_ME, true);
                                } else {
                                    editor.remove(KEY_USERNAME);
                                    editor.remove(KEY_PASSWORD);
                                    editor.putBoolean(KEY_REMEMBER_ME, false);
                                }
                                editor.apply();
                                
                                if (userProfile.isAdmin()) {
                                    startActivity(new Intent(MainActivity.this, AdminActivity.class));
                                } else {
                                    startActivity(new Intent(MainActivity.this, HomeScreen.class));
                                }
                                finish();
                            } else {
                                showError("Invalid password");
                            }
                        } else {
                            showError("User not found");
                        }
                    })
                    .addOnFailureListener(e -> {
                        showError("Error: " + e.getMessage());
                    });
            }
        });
    }

    private void ClickSignUp() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.register, null);
        dialog.setView(dialogView);
        final AlertDialog Adialog = dialog.create();

        reg_username = dialogView.findViewById(R.id.reg_username);
        reg_password = dialogView.findViewById(R.id.reg_password);
        reg_firstName = dialogView.findViewById(R.id.reg_firstName);
        reg_lastName = dialogView.findViewById(R.id.reg_lastName);
        reg_email = dialogView.findViewById(R.id.reg_email);
        reg_phone = dialogView.findViewById(R.id.reg_phone);
        reg_register = dialogView.findViewById(R.id.reg_register);

        reg_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateRegistrationFields()) {
                    String usernameStr = reg_username.getText().toString().trim();
                    String passwordStr = reg_password.getText().toString().trim();
                    
                    Log.d(TAG, "Starting registration process for user: " + usernameStr);
                    
                    // First check if username already exists
                    db.collection("users")
                        .document(usernameStr)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                Log.w(TAG, "Username already exists: " + usernameStr);
                                showError("Username already exists");
                            } else {
                                // Create user profile
                                UserProfile userProfile = new UserProfile(
                                    usernameStr,
                                    reg_firstName.getText().toString(),
                                    reg_lastName.getText().toString(),
                                    reg_email.getText().toString(),
                                    reg_phone.getText().toString()
                                );
                                userProfile.setPassword(passwordStr);

                                // Store in Firestore
                                db.collection("users")
                                    .document(usernameStr)
                                    .set(userProfile)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "User registered successfully: " + usernameStr);
                                        Toast.makeText(MainActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                        Adialog.dismiss();
                                        showVehicleRegistrationDialog(usernameStr);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error registering user", e);
                                        showError("Registration failed: " + e.getMessage());
                                    });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error checking username existence", e);
                            showError("Error checking username: " + e.getMessage());
                        });
                }
            }
        });

        Adialog.show();
    }

    private void showVehicleRegistrationDialog(String username) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.vehicle_registration, null);
        dialog.setView(dialogView);
        final AlertDialog Adialog = dialog.create();

        EditText vehicleName = dialogView.findViewById(R.id.vehicleName);
        EditText vehicleNumber = dialogView.findViewById(R.id.vehicleNumber);
        EditText vehicleType = dialogView.findViewById(R.id.vehicleType);
        Button addVehicle = dialogView.findViewById(R.id.addVehicle);

        addVehicle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateVehicleFields(vehicleName, vehicleNumber, vehicleType)) {
                    Vehicle vehicle = new Vehicle(
                        vehicleName.getText().toString(),
                        vehicleNumber.getText().toString(),
                        vehicleType.getText().toString()
                    );

                    db.collection("users")
                        .document(username)
                        .collection("vehicles")
                        .add(vehicle)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(MainActivity.this, "Vehicle added successfully", Toast.LENGTH_SHORT).show();
                            Adialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            showError("Failed to add vehicle: " + e.getMessage());
                        });
                }
            }
        });

        Adialog.show();
    }

    private boolean validateRegistrationFields() {
        boolean isValid = true;
        
        if (reg_username.getText().toString().trim().isEmpty()) {
            reg_username.setError("Username is required");
            isValid = false;
        }
        if (reg_password.getText().toString().trim().isEmpty()) {
            reg_password.setError("Password is required");
            isValid = false;
        }
        if (reg_firstName.getText().toString().trim().isEmpty()) {
            reg_firstName.setError("First name is required");
            isValid = false;
        }
        if (reg_lastName.getText().toString().trim().isEmpty()) {
            reg_lastName.setError("Last name is required");
            isValid = false;
        }
        if (reg_email.getText().toString().trim().isEmpty()) {
            reg_email.setError("Email is required");
            isValid = false;
        }
        if (reg_phone.getText().toString().trim().isEmpty()) {
            reg_phone.setError("Phone number is required");
            isValid = false;
        }
        
        return isValid;
    }

    private boolean validateVehicleFields(EditText vehicleName, EditText vehicleNumber, EditText vehicleType) {
        if (vehicleName.getText().toString().trim().isEmpty()) {
            vehicleName.setError("Vehicle name is required");
            return false;
        }
        if (vehicleNumber.getText().toString().trim().isEmpty()) {
            vehicleNumber.setError("Vehicle number is required");
            return false;
        }
        if (vehicleType.getText().toString().trim().isEmpty()) {
            vehicleType.setError("Vehicle type is required");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        Log.e(TAG, "Error: " + message);
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(getResources().getColor(R.color.red));
        snackbar.show();
    }
}
