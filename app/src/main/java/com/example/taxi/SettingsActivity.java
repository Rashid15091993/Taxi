package com.example.taxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.net.URI;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private String getType;

    private CircleImageView circleImageView;
    private EditText nameEditText, phoneEditText, carEditText;
    private ImageView closeBtn, saveBtn;
    private TextView imageChangeBtn;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    private Uri imageUri;
    private String myUrl = "";
    private StorageTask uploadTask;
    private StorageReference storageProfileImageRef;
    private String checker = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getType = getIntent().getStringExtra("type");

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(getType);

        storageProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Pictures");

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

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checker.equals("clicked")) {
                    ValidateControllers();
                }
                else {
                    ValidateAndSaveOnlyInformation();
                }
            }
        });

        imageChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checker = "clicked";

                CropImage.activity().setAspectRatio(1,1).start(SettingsActivity.this);

            }
        });

        getUserInformation();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && requestCode == RESULT_OK && data != null) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            imageUri = result.getUri();

            circleImageView.setImageURI(imageUri);
        }else {
            if(getType.equals("Drivers")) {
                startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
            }else {
                startActivity(new Intent(SettingsActivity.this, CustomMapsActivity.class));
            }

            Toast.makeText(this, "Произошла Ошибка!", Toast.LENGTH_SHORT).show();
        }

    }

    private void ValidateControllers() {
        if(TextUtils.isEmpty(nameEditText.getText().toString())) {
            Toast.makeText(this, "Заполните поле имя", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty(phoneEditText.getText().toString())) {
            Toast.makeText(this, "Заполните поле телефона", Toast.LENGTH_SHORT).show();
        }

        else if(getType.equals("Drivers") && TextUtils.isEmpty(carEditText.getText().toString())) {
            Toast.makeText(this, "Заполните поле марку машины", Toast.LENGTH_SHORT).show();
        }

        else if(checker.equals("clicked")) {
            uploadProfileImage();
        }
    }

    private void uploadProfileImage() {
        if(imageUri != null) {
            final StorageReference fileRef = storageProfileImageRef.child(mAuth.getCurrentUser().getUid() + ".jpg");

            uploadTask = fileRef.putFile(imageUri);

            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {
                    if(!task.isSuccessful()) {
                        throw  task.getException();
                    }
                    return fileRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task < Uri> task) {
                    if(task.isSuccessful()) {
                        Uri downloadUrl = task.getResult();
                        myUrl = downloadUrl.toString();

                        HashMap<String, Object> userMap = new HashMap<>();

                        userMap.put("uid", mAuth.getCurrentUser().getUid());
                        userMap.put("name", nameEditText.getText().toString());
                        userMap.put("phone", phoneEditText.getText().toString());
                        userMap.put("image", myUrl);

                        if(getType.equals("Drivers")) {
                            userMap.put("carName", carEditText.getText().toString());
                        }
                        databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);

                    }
                }
            });
        }
        else {
            Toast.makeText(this, "Изображение не выбрано", Toast.LENGTH_SHORT).show();
        }
    }
    private void ValidateAndSaveOnlyInformation() {
        if(TextUtils.isEmpty(nameEditText.getText().toString())) {
            Toast.makeText(this, "Заполните поле имя", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty(phoneEditText.getText().toString())) {
            Toast.makeText(this, "Заполните поле телефона", Toast.LENGTH_SHORT).show();
        }

        else if(getType.equals("Drivers") && TextUtils.isEmpty(carEditText.getText().toString())) {
            Toast.makeText(this, "Заполните поле марку машины", Toast.LENGTH_SHORT).show();
        }
        else {
            HashMap<String, Object> userMap = new HashMap<>();

            userMap.put("uid", mAuth.getCurrentUser().getUid());
            userMap.put("name", nameEditText.getText().toString());
            userMap.put("phone", phoneEditText.getText().toString());

            if(getType.equals("Drivers")) {
                userMap.put("carName", carEditText.getText().toString());
            }
            databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);

            if(getType.equals("Drivers")) {
                startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
            }
            else {
                startActivity(new Intent(SettingsActivity.this, CustomMapsActivity.class));
            }


        }
    }

    private void getUserInformation() {
        databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    String name = snapshot.child("name").getValue().toString();
                    String phone = snapshot.child("phone").getValue().toString();

                    nameEditText.setText(name);
                    phoneEditText.setText(phone);

                    if(getType.equals("Drivers")) {
                        String carname = snapshot.child("carName").getValue().toString();
                        carEditText.setText(carname);
                    }
                    if(snapshot.hasChild("image")) {
                        String image = snapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(circleImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}