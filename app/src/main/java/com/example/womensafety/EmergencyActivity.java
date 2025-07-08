package com.example.womensafety;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class EmergencyActivity extends AppCompatActivity {

    Button buttonPolice, buttonAmbulance, buttonFire, buttonWomenHelp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.emergencyactivity);

        // Initialize buttons from XML layout
        buttonPolice = findViewById(R.id.buttonPolice);
        buttonAmbulance = findViewById(R.id.buttonAmbulance);
        buttonFire = findViewById(R.id.buttonFire);
        buttonWomenHelp = findViewById(R.id.buttonWomenHelp);

        // Set click listeners for each button
        buttonPolice.setOnClickListener(this::onEmergencyNumberClick);
        buttonAmbulance.setOnClickListener(this::onEmergencyNumberClick);
        buttonFire.setOnClickListener(this::onEmergencyNumberClick);
        buttonWomenHelp.setOnClickListener(this::onEmergencyNumberClick);

        // Check for CALL_PHONE permission at startup
        checkAndRequestCallPermission();
    }

    // Method to check and request CALL_PHONE permission
    private void checkAndRequestCallPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Request permission if not already granted
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, 1);
        }
    }

    // Method for handling emergency number button clicks
    public void onEmergencyNumberClick(View view) {
        String phoneNumber = "";

        // Set the phone number based on the button clicked
        if (view.getId() == R.id.buttonPolice) {
            phoneNumber = "112";
        } else if (view.getId() == R.id.buttonAmbulance) {
            phoneNumber = "102";
        } else if (view.getId() == R.id.buttonFire) {
            phoneNumber = "101";
        } else if (view.getId() == R.id.buttonWomenHelp) {
            phoneNumber = "1091";
        }

        if (!phoneNumber.isEmpty()) {
            // Attempt to make the call
            makeEmergencyCall(phoneNumber);
        } else {
            Toast.makeText(this, "Emergency number not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeEmergencyCall(String phoneNumber) {
        // Check if permission is granted to make phone calls
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            // Directly call the emergency number
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(callIntent);
        } else {
            Toast.makeText(this, "Permission to make calls is required", Toast.LENGTH_SHORT).show();
            // Request permission if not granted
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, 1);
        }
    }
    public void openWebsite(View view) {
        String url = "";

        // Set URL based on which TextView was clicked
        if (view.getId() == R.id.textViewWebsite1) {
            url = "https://112.gov.in/";
        } else if (view.getId() == R.id.textViewWebsite2) {
            url = "https://www.socialmediamatters.in/women-safety";
        } else if (view.getId() == R.id.textViewWebsite3) {
            url = "https://www.stopviolence.org/";
        } else if (view.getId() == R.id.textViewWebsite4) {
            url = "https://www.unwomen.org/";
        }

        if (!url.isEmpty()) {
            // Open the URL in a browser
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } else {
            Toast.makeText(this, "Website not found", Toast.LENGTH_SHORT).show();
        }
    }

    public void openLinkedInProfile(View view) {
        String linkedInUrl = "";

        // Set LinkedIn URLs based on the text view clicked using if-else
        if (view.getId() == R.id.textViewDeveloper1) {
            linkedInUrl = "https://www.linkedin.com/in/shridhar-patil-81a7762a7/";
        } else if (view.getId() == R.id.textViewDeveloper2) {
            linkedInUrl = "https://www.linkedin.com/in/shubham-swami-3aba241b8/";
        } else if (view.getId() == R.id.textViewDeveloper3) {
            linkedInUrl = "https://www.linkedin.com/in/prajwal-hiremath-93927a203/";
        } else if (view.getId() == R.id.textViewDeveloper4) {
            linkedInUrl = "https://www.linkedin.com/in/tejas-samant/";
        }

        if (!linkedInUrl.isEmpty()) {
            // Open the LinkedIn profile in a browser
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkedInUrl));
            startActivity(browserIntent);
        } else {
            Toast.makeText(this, "Developer profile not found", Toast.LENGTH_SHORT).show();
        }
    }


    // Handle the permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Check if the CALL_PHONE permission is granted
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied! Unable to make a call.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
