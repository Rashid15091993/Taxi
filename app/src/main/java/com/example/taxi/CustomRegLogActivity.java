package com.example.taxi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CustomRegLogActivity extends AppCompatActivity {
    TextView customStatus, noAccount;
    Button signInCustomBtn, signUpdCustomBtn;
    EditText mailEditText, passwordEditText;

    FirebaseAuth mAuth;
    DatabaseReference CustomDatabaseRef;
    String OnlineCustomersID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_reg_log);

        customStatus = (TextView)findViewById(R.id.statusCustomText);
        noAccount = (TextView)findViewById(R.id.noAccountText);
        signInCustomBtn = (Button)findViewById(R.id.signInCustomButton);
        signUpdCustomBtn = (Button)findViewById(R.id.signUpdateCustomButton);

        mailEditText = (EditText)findViewById(R.id.editTextMailCustom);
        passwordEditText = (EditText)findViewById(R.id.textPasswordCustom);

        mAuth = FirebaseAuth.getInstance();


        signUpdCustomBtn.setVisibility(View.INVISIBLE);
        signUpdCustomBtn.setEnabled(false);

        noAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInCustomBtn.setVisibility(View.INVISIBLE);
                noAccount.setVisibility(View.INVISIBLE);

                signUpdCustomBtn.setVisibility(View.VISIBLE);
                signUpdCustomBtn.setEnabled(true);

                customStatus.setText("Регистрация для клиентов");
            }
        });

        signInCustomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mail = mailEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                SignInCustom(mail, password);

            }

            private void SignInCustom(String mail, String password) {
                mAuth.signInWithEmailAndPassword(mail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull  Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(CustomRegLogActivity.this,"Yспешно",Toast.LENGTH_SHORT).show();
                            Intent customIntent = new Intent(CustomRegLogActivity.this, CustomMapsActivity.class);
                            startActivity(customIntent);
                        }
                        else {
                            Toast.makeText(CustomRegLogActivity.this,"Не получилось",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        signUpdCustomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mail = mailEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                RegisterCustom(mail, password);

            }
        });
    }
    private void RegisterCustom(String mail, String password) {

        mAuth.createUserWithEmailAndPassword(mail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    OnlineCustomersID = mAuth.getCurrentUser().getUid();

                    CustomDatabaseRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Customers").child(OnlineCustomersID);
                    CustomDatabaseRef.setValue(true);

                    Intent customIntent = new Intent(CustomRegLogActivity.this, CustomMapsActivity.class);
                    startActivity(customIntent);
                    Toast.makeText(CustomRegLogActivity.this,"Регистрация прошла успешно",Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(CustomRegLogActivity.this,"Не получилось",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


}