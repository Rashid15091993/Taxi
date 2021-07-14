package com.example.taxi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private String getType;

    private CircleImageView circleImageView;
    private EditText nameEditText, phoneEditText, carEditText;
    private ImageView closeBtn, saveBtn;
    private TextView imageChangeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getType = getIntent().getStringExtra("type");

        circleImageView = (CircleImageView)findViewById(R.id.profile_image);
        nameEditText = (EditText)findViewById(R.id.name);
        phoneEditText = (EditText)findViewById(R.id.phone);
        carEditText = (EditText)findViewById(R.id.car_name);

        if(getType.equals("Drivers")) {
            carEditText.setVisibility(View.VISIBLE);
        }

        closeBtn = (ImageView)findViewById(R.id.close_button);
        saveBtn = (ImageView)findViewById(R.id.save_button);
        imageChangeBtn = (TextView)findViewById(R.id.change_photo_btn);

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getType.equals("Drivers")) {
                    startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
                }
                else {
                    startActivity(new Intent(SettingsActivity.this, CustomMapsActivity.class));
                }
            }
        });
    }
}