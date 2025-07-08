package com.example.womensafety;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button buttonSOS, buttonProfile, buttonCall, buttonImSafe;
    private ImageButton buttonAbout;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView greetingTextView, locationTextView;
    private SoundPool soundPool;
    private int soundID, safeSoundID;

    private static final String CHANNEL_ID = "default_channel";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        buttonSOS = findViewById(R.id.buttonSOS);
        buttonProfile = findViewById(R.id.buttonProfile);
        buttonCall = findViewById(R.id.buttonCall);
        buttonImSafe = findViewById(R.id.buttonImSafe);
        buttonAbout = findViewById(R.id.buttonAbout);
        greetingTextView = findViewById(R.id.greetingTextView);
        //locationTextView = findViewById(R.id.locationTextView);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup SoundPool for sounds
        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        soundID = soundPool.load(this, R.raw.siren, 1); // SOS sound

        // Create Notification Channel
        createNotificationChannel();

        // Request notification permission if necessary (Android 13 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }

        updateGreeting();

        // Set button click listeners
        buttonSOS.setOnClickListener(v -> handleSOSButtonClick());
        buttonImSafe.setOnClickListener(v -> sendImSafeMessage());
        buttonProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        buttonCall.setOnClickListener(v -> callEmergency());
        buttonAbout.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, EmergencyActivity.class)));

        requestLocationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGreeting();
    }

    private void updateGreeting() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);
        String name = sharedPreferences.getString("Name", "User");
        String greetingMessage = "Hello\n\t\t" + name;
        greetingTextView.setText(greetingMessage);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void handleSOSButtonClick() {
        if (checkAndRequestPermissions()) {
            sendSOS();
        }
    }

    private boolean checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return false;
        }
    }

    private void sendSOS() {
        // Check if location services are enabled
        if (!isLocationEnabled()) {
            promptEnableLocation();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        String locationLink = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                        String message = formatMessage("Help! I'm in danger.", locationLink);
                        sendMessageToContacts(message);
                    } else {
                        Toast.makeText(this, "Unable to retrieve location.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isLocationEnabled() {
        android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
    }

    private void promptEnableLocation() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    private void sendImSafeMessage() {
        String message = formatMessage("I'm safe", "");
        sendMessageToContacts(message);
        soundPool.play(safeSoundID, 1f, 1f, 0, 0, 1f); // Play "I'm Safe" sound
    }

    private String formatMessage(String baseMessage, String locationLink) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);
        String name = sharedPreferences.getString("Name", "Unknown");
        String age = sharedPreferences.getString("Age", "Unknown");
        String bloodGroup = sharedPreferences.getString("BloodGroup", "Unknown");

        return baseMessage + " - Name: " + name + ", Age: " + age + ", Blood Group: " + bloodGroup + ". " + locationLink;
    }

    private void sendMessageToContacts(String message) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);
        Gson gson = new Gson();
        String jsonContacts = sharedPreferences.getString("Contacts", "[]");
        Type type = new TypeToken<List<ProfileActivity.Contact>>() {}.getType();
        List<ProfileActivity.Contact> contactsList = gson.fromJson(jsonContacts, type);

        // Check if no contacts are selected
        if (contactsList.isEmpty()) {
            Toast.makeText(this, "No contacts selected. Please add contacts to your profile.", Toast.LENGTH_LONG).show();
            return;
        }

        for (ProfileActivity.Contact contact : contactsList) {
            SmsManager smsManager = SmsManager.getDefault();
            try {
                smsManager.sendTextMessage(contact.getNumber(), null, message, null, null);
                Toast.makeText(this, "Message sent to " + contact.getName(), Toast.LENGTH_SHORT).show();
                sendNotification(contact.getName());
            } catch (Exception e) {
                Toast.makeText(this, "Failed to send message to " + contact.getName(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendNotification(String contactName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.pop2)
                .setContentTitle("Message Sent")
                .setContentText("Message sent to " + contactName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(0, builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Default Channel", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void callEmergency() {
        String emergencyNumber = "7411411500";

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + emergencyNumber));
            startActivity(callIntent);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSOS();
            } else {
                Toast.makeText(this, "Permissions are required for SOS functionality", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 2 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callEmergency();
        }
    }
}
                                                    