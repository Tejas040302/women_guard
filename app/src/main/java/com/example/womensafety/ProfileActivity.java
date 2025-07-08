package com.example.womensafety;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Html;
import android.text.SpannableString;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private EditText editTextName, editTextAge;
    private Spinner spinnerBloodGroup;
    private Button buttonSaveProfile, buttonAddContact;
    private List<Contact> contactsList = new ArrayList<>();
    private static final String PREF_NAME = "UserProfile";
    private static final int PICK_CONTACT = 1;
    private ListView listViewContacts;
    private ArrayAdapter<Contact> adapter;

    // Constants for SharedPreferences keys
    private static final String KEY_NAME = "Name";
    private static final String KEY_AGE = "Age";
    private static final String KEY_BLOOD_GROUP = "BloodGroup";
    private static final String KEY_CONTACTS = "Contacts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        editTextName = findViewById(R.id.nameEditText);
        editTextAge = findViewById(R.id.ageEditText);
        spinnerBloodGroup = findViewById(R.id.spinnerBloodGroup);
        buttonSaveProfile = findViewById(R.id.buttonSaveProfile);
        buttonAddContact = findViewById(R.id.selectContactsButton);
        listViewContacts = findViewById(R.id.listViewContacts);

        // Load blood group options into spinner
        ArrayAdapter<CharSequence> bloodGroupAdapter = ArrayAdapter.createFromResource(this,
                R.array.blood_groups, android.R.layout.simple_spinner_item);
        bloodGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(bloodGroupAdapter);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contactsList);
        listViewContacts.setAdapter(adapter);

        loadUserData();

        buttonSaveProfile.setOnClickListener(v -> {
            if (validateInputs()) {
                saveUserData(
                        editTextName.getText().toString(),
                        editTextAge.getText().toString(),
                        spinnerBloodGroup.getSelectedItem().toString(), // Get selected blood group
                        contactsList
                );
                Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
            }
        });

        buttonAddContact.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PICK_CONTACT);
            } else {
                openContactPicker();
            }
        });

        // Handle removing contacts on ListView item click
        listViewContacts.setOnItemClickListener((parent, view, position, id) -> {
            Contact contactToRemove = contactsList.get(position);
            contactsList.remove(contactToRemove);
            adapter.notifyDataSetChanged();
            saveUserData(editTextName.getText().toString(), editTextAge.getText().toString(), spinnerBloodGroup.getSelectedItem().toString(), contactsList);
            Toast.makeText(ProfileActivity.this, "Contact removed: " + contactToRemove.getName(), Toast.LENGTH_SHORT).show();
        });
    }



    private void openContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
            try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

                    if (name != null && number != null) {
                        Contact newContact = new Contact(name, number);
                        if (!contactsList.contains(newContact)) {  // Ensure no duplicates
                            contactsList.add(newContact);
                            adapter.notifyDataSetChanged();
                            saveUserData(editTextName.getText().toString(), editTextAge.getText().toString(), spinnerBloodGroup.getSelectedItem().toString(), contactsList);
                            Toast.makeText(this, "Contact added: " + name, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Contact already exists", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Invalid contact.", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error retrieving contact", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PICK_CONTACT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openContactPicker();
            } else {
                Toast.makeText(this, "Permission denied. Cannot access contacts.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveUserData(String name, String age, String bloodGroup, List<Contact> contactsList) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_AGE, age);
        editor.putString(KEY_BLOOD_GROUP, bloodGroup);
        Gson gson = new Gson();
        String jsonContacts = gson.toJson(contactsList);
        editor.putString(KEY_CONTACTS, jsonContacts);
        editor.apply();
    }

    private void loadUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String name = sharedPreferences.getString(KEY_NAME, "");
        String age = sharedPreferences.getString(KEY_AGE, "");
        String bloodGroup = sharedPreferences.getString(KEY_BLOOD_GROUP, "");
        editTextName.setText(name);
        editTextAge.setText(age);
        // Set the selected blood group in the spinner
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerBloodGroup.getAdapter();
        int spinnerPosition = adapter.getPosition(bloodGroup);
        spinnerBloodGroup.setSelection(spinnerPosition);

        Gson gson = new Gson();
        String jsonContacts = sharedPreferences.getString(KEY_CONTACTS, "[]");
        Type type = new TypeToken<List<Contact>>() {}.getType();
        List<Contact> loadedContacts = gson.fromJson(jsonContacts, type);

        // Clear list and add all contacts to ensure updated display, then sort alphabetically
        if (loadedContacts != null) {
            contactsList.clear();
            contactsList.addAll(loadedContacts);
            Collections.sort(contactsList, Comparator.comparing(Contact::getName));  // Sorting alphabetically by name
            adapter.notifyDataSetChanged();
        }
    }



    private boolean validateInputs() {
        String name = editTextName.getText().toString();
        String age = editTextAge.getText().toString();
        String bloodGroup = spinnerBloodGroup.getSelectedItem().toString();

        if (name.isEmpty() || age.isEmpty() || bloodGroup.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check that age is a valid number and between 1 and 100
        try {
            int ageValue = Integer.parseInt(age);
            if (ageValue < 1 || ageValue > 100) {
                Toast.makeText(this, "Please enter a valid age between 1 and 100.", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for age.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    public static class Contact {
        private String name;
        private String number;

        public Contact(String name, String number) {
            this.name = name;
            this.number = number;
        }

        public String getName() {
            return name;
        }

        public String getNumber() {
            return number;
        }

        @Override
        public String toString() {
            return name + " (" + number + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Contact contact = (Contact) o;
            return number.equals(contact.number); // Comparing by number to avoid duplicates
        }
    }
}
