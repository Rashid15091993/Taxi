package com.example.taxi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverRegLogActivity extends AppCompatActivity {
    TextView driverStatus, noAccount;
    Button signInDriverBtn, signUpdDriverBtn;
    EditText mailEditText, passwordEditText;

    FirebaseAuth mAuth;
    DatabaseReference DriverDatabaseRef;
    String OnlineDriverID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_reg_log);

        driverStatus = (TextView)findViewById(R.id.statusText);
        noAccount = (TextView)findViewById(R.id.noAccountText);
        signInDriverBtn = (Button)findViewById(R.id.signInDriverButton);
        signUpdDriverBtn = (Button)findViewById(R.id.signUpdateDriverButton);

        mailEditText = (EditText)findViewById(R.id.editTextMailDriver);
        passwordEditText = (EditText)findViewById(R.id.textPasswordDriver);

        mAuth = FirebaseAuth.getInstance();

        signUpdDriverBtn.setVisibility(View.INVISIBLE);
        signUpdDriverBtn.setEnabled(false);

        noAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInDriverBtn.setVisibility(View.INVISIBLE);
                noAccount.setVisibility(View.INVISIBLE);

                signUpdDriverBtn.setVisibility(View.VISIBLE);
                signUpdDriverBtn.setEnabled(true);

                driverStatus.setText("Регистрация для водителей");
            }
        });

        signUpdDriverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mail = mailEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                RegisterDriver(mail, password);

            }
        });

        signInDriverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mail = mailEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                SignInDriver(mail, password);
            }

            private void SignInDriver(String mail, String password) {
                mAuth.signInWithEmailAndPassword(mail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull  Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(DriverRegLogActivity.this,"Yспешно",Toast.LENGTH_SHORT).show();
                            Intent driverIntet = new Intent(DriverRegLogActivity.this, DriverMapsActivity.class);
                            startActivity(driverIntet);
                        }
                        else {
                            Toast.makeText(DriverRegLogActivity.this,"Не получилось",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void RegisterDriver(String mail, String password) {

        mAuth.createUserWithEmailAndPassword(mail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull  Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    OnlineDriverID = mAuth.getCurrentUser().getUid();

                    DriverDatabaseRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Drivers").child(OnlineDriverID);
                    DriverDatabaseRef.setValue(true);

                    Intent driverIntent = new Intent(DriverRegLogActivity.this, DriverMapsActivity.class);
                    startActivity(driverIntent);

                    Toast.makeText(DriverRegLogActivity.this,"Регистрация прошла успешно",Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(DriverRegLogActivity.this,"Не получилось",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}