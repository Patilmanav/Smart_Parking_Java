# Smart Parking App

A comprehensive Android application for managing parking spaces with real-time booking, payment integration, and admin controls.

## Features

### For Users
- User authentication (Login/Register)
- Vehicle management (Add/Remove vehicles)
- Real-time parking slot availability
- Slot booking with Razorpay payment integration
- View booking history
- Unpark vehicles with automatic overtime charges
- Google Maps integration for location navigation

### For Admins
- Location management (Add/Edit/Remove parking locations)
- Slot management (Reserve/Make Available)
- Set parking rates
- Monitor booking status
- Add Google Maps links for locations

## Setup Instructions

### 1. Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project
   - Click "Create Project"
   - Enter your project name
   - Enable Google Analytics (optional)
   - Click "Create"

3. Add Android app to Firebase project
   - Click "Add app" and select Android
   - Enter package name (com.example.smart_parking)
   - Enter app nickname (optional)
   - Click "Register app"

4. Download google-services.json
   - The file will download automatically
   - Place it in your project's `app` directory
   - Path: `app/google-services.json`

5. Enable Authentication
   - Go to Authentication in Firebase Console
   - Click "Get Started"
   - Enable Email/Password authentication
   - Add your first admin user

6. Set up Cloud Firestore
   - Go to Firestore Database in Firebase Console
   - Click "Create Database"
   - Choose production/test mode
   - Select database location
   - Set up security rules:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 2. Razorpay Setup

1. Create a [Razorpay Account](https://razorpay.com/)
2. Get your API keys from the Dashboard
3. Replace the test key in `SlotSelectionActivity.java`:
```java
checkout.setKeyID("YOUR_RAZORPAY_KEY_ID");
```

### 3. Project Setup

1. Clone the repository
```bash
git clone [repository-url]
```

2. Open project in Android Studio

3. Add Firebase dependencies in `build.gradle` (app level)
```gradle
implementation platform('com.google.firebase:firebase-bom:32.7.0')
implementation 'com.google.firebase:firebase-analytics'
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-firestore'
```

4. Add Razorpay dependency
```gradle
implementation 'com.razorpay:checkout:1.6.40'
```

5. Sync project with Gradle files

### 4. Database Structure

Firestore collections structure:
- `users`: User profiles and authentication data
- `locations`: Parking location details
- `parking_slots`: Individual slot information
- `vehicles`: User's registered vehicles

### 5. Running the App

1. Build and run the project
2. Create an admin account through Firebase Console
3. Login as admin to add parking locations
4. Regular users can register and start booking slots

## Usage Guide

### Admin Operations
1. Login with admin credentials
2. Add parking locations:
   - Set location name
   - Add address
   - Specify total slots
   - Set hourly rate
   - Add Google Maps link
3. Manage slots:
   - Long press on slots to reserve/make available
   - Monitor booking status

### User Operations
1. Register/Login
2. Add vehicles:
   - Enter vehicle number
   - Select vehicle type
3. Book slots:
   - Select location
   - Choose available slot
   - Select vehicle
   - Enter parking duration
   - Complete payment
4. Unpark vehicle:
   - Click on booked slot
   - Pay any overtime charges
   - Confirm unparking

## Security Considerations

1. Keep your Firebase configuration file secure
2. Never commit API keys to version control
3. Implement proper security rules in Firebase
4. Use test keys for Razorpay during development

## Troubleshooting

Common issues and solutions:
1. Firebase connection issues:
   - Verify google-services.json placement
   - Check internet connectivity
   - Verify Firebase project settings

2. Payment integration issues:
   - Confirm Razorpay key implementation
   - Check for test mode settings
   - Verify payment callback implementation

## Support

For any queries or support:
- Create an issue in the repository
- Contact the development team
- Check Firebase and Razorpay documentation

## License

[Add your license information here]
