package com.example.strangerconnection.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.strangerconnection.R;
import com.example.strangerconnection.databinding.ActivityConnectingBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class ConnectingActivity extends AppCompatActivity {
    ActivityConnectingBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    boolean isOk=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        auth=FirebaseAuth.getInstance();
        database=FirebaseDatabase.getInstance();
        binding=ActivityConnectingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String profile=getIntent().getStringExtra("profile");
        Glide.with(this).load(profile).into(binding.profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        String username=auth.getUid();

        database.getReference().child("users")
                .orderByChild("status")
                .equalTo(0).limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.getChildrenCount()>0){
                            isOk=true;
                            for(DataSnapshot childsnap:snapshot.getChildren()){
                                database.getReference().child("users")
                                        .child(childsnap.getKey())
                                        .child("incoming")
                                        .setValue(username);
                                database.getReference().child("users")
                                        .child(childsnap.getKey())
                                        .child("status")
                                        .setValue(1);
                                String incoming=childsnap.child("incoming").getValue(String.class);
                                String createdBy=childsnap.child("createdBy").getValue(String.class);

                                boolean isAvailable=childsnap.child("isAvailable").getValue(Boolean.class);
                                Intent intent=new Intent(ConnectingActivity.this,CallActivity.class);
                                intent.putExtra("username",username);
                                intent.putExtra("incoming",incoming);
                                intent.putExtra("createdBy",createdBy);
                                intent.putExtra("isAvailable",isAvailable);
                                startActivity(intent);
                                finish();
                            }
                        }else{
                            //not available
                            HashMap<String,Object>room=new HashMap<>();
                            room.put("incoming",username);
                            room.put("createdBy",username);
                            room.put("isAvailable",true);
                            room.put("status",0);

                            database.getReference()
                                    .child("users")
                                    .child(username)
                                    .setValue(room).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            database.getReference()
                                                    .child("users")
                                                    .child(username).addValueEventListener(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            if(snapshot.child("status").exists()){

                                                                if(snapshot.child("status").getValue((Integer.class))==1){
                                                                    if(isOk){
                                                                        return;
                                                                    }
                                                                    isOk=true;
                                                                    String incoming=snapshot.child("incoming").getValue(String.class);
                                                                    String createdBy=snapshot.child("createdBy").getValue(String.class);
                                                                    boolean isAvailable=snapshot.child("isAvailable").getValue(Boolean.class);
                                                                    Intent intent=new Intent(ConnectingActivity.this,CallActivity.class);
                                                                    intent.putExtra("username",username);
                                                                    intent.putExtra("incoming",incoming);
                                                                    intent.putExtra("createdBy",createdBy);
                                                                    intent.putExtra("isAvailable",isAvailable);
                                                                    startActivity(intent);
                                                                    finish();
                                                                }
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                        }
                                                    });
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}