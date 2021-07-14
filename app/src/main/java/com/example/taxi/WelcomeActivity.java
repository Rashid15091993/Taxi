package com.example.taxi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WelcomeActivity extends AppCompatActivity {

    Button driverButton, customButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        driverButton = (Button)findViewById(R.id.driverButton);
        customButton = (Button)findViewById(R.id.customButton);

        driverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent driverIntent = new Intent(WelcomeActivity.this, DriverRegLogActivity.class);
                startActivity(driverIntent);
            }
        });

        customButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent customIntent = new Intent(WelcomeActivity.this, CustomRegLogActivity.class);
                startActivity(customIntent);
            }
        });
    }
}