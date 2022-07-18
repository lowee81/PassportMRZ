package com.zain.passportmrz;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        TextView nameTV = findViewById(R.id.name);
        nameTV.setText("Name: " + getIntent().getExtras().getString("name", "None"));

        TextView passportTV = findViewById(R.id.passportNumber);
        passportTV.setText("Passport Number: " + getIntent().getExtras().getString("passportNumber", "None"));

        TextView nationalityTV = findViewById(R.id.nationality);
        nationalityTV.setText("Nationality: " + getIntent().getExtras().getString("nationality", "None"));

        TextView dobTV = findViewById(R.id.dob);
        dobTV.setText("DOB: " + getIntent().getExtras().getString("dob", "None"));

        TextView sexTV = findViewById(R.id.sex);
        char sexChar = getIntent().getExtras().getChar("sex", '?');
        sexTV.setText("Sex: " + (sexChar == 'M' ? "Male" : sexChar == 'F' ? "Female" : "Unspecified"));

        TextView passportExpiryTV = findViewById(R.id.passportExpiry);
        passportExpiryTV.setText("Passport Expiry: " + getIntent().getExtras().getString("passportExpiry", "None"));

        TextView personalNumberTV = findViewById(R.id.personalNumber);
        personalNumberTV.setText("Personal Number: " + getIntent().getExtras().getString("personalNumber", "None").replaceAll("<",""));
    }
}