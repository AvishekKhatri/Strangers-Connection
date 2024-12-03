package com.example.strangerconnection.activities;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.strangerconnection.R;
import com.example.strangerconnection.databinding.ActivityCallBinding;
import com.example.strangerconnection.models.InterfaceJava;
import com.example.strangerconnection.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.UUID;

public class CallActivity extends AppCompatActivity {
    ActivityCallBinding binding;
    String uniqueId="";
    FirebaseAuth auth;
    String username="";
    String friendsUsername="";
    boolean isPeerConnected=false;
    DatabaseReference firebaseRef;
    boolean isAudio=true;
    boolean isVideo=true;
    String createdBy;
    boolean pageExit=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        auth=FirebaseAuth.getInstance();

        super.onCreate(savedInstanceState);
        binding=ActivityCallBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseRef= FirebaseDatabase.getInstance().getReference().child("users");
        username=getIntent().getStringExtra("username");
        String incoming=getIntent().getStringExtra("incoming");
        createdBy=getIntent().getStringExtra("createdBy");
//        friendsUsername="";
//        if(incoming.equalsIgnoreCase(friendsUsername)){
//            friendsUsername=incoming;
//        }
        friendsUsername=incoming;

        setupWebView();

        binding.micBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isAudio=!isAudio;
                callJavascriptFunction("javascript:toggleAudio(\""+isAudio+"\")");
                if(isAudio){
                    binding.micBtn.setImageResource(R.drawable.btn_unmute_normal);
                }else{
                    binding.micBtn.setImageResource(R.drawable.btn_mute_normal);
                }
            }
        });

        binding.videoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isVideo=!isVideo;
                callJavascriptFunction("javascript:toggleVideo(\""+isVideo+"\")");
                if(isVideo){
                    binding.videoBtn.setImageResource(R.drawable.btn_video_normal);
                }else{
                    binding.videoBtn.setImageResource(R.drawable.btn_video_muted);
                }
            }
        });
        binding.endCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    void setupWebView(){
        binding.webview.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onPermissionRequest(PermissionRequest request){
                request.grant((request.getResources()));
            }
        });

        binding.webview.getSettings().setJavaScriptEnabled(true);
        binding.webview.getSettings().setMediaPlaybackRequiresUserGesture(false);
        binding.webview.addJavascriptInterface(new InterfaceJava(this),"Android");

        loadVideoCall();
    }
    public void loadVideoCall(){
        String filePath="file:android_asset/call.html";
        binding.webview.loadUrl(filePath);

        binding.webview.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                initializePeer();
            }
        });
    }
    void initializePeer(){
        uniqueId=generateUniqueId();
        callJavascriptFunction("javascript:init(\""+uniqueId+"\")");
        if(createdBy.equalsIgnoreCase(username)){
            if(pageExit){
                return;
            }
            firebaseRef.child(username).child("connId").setValue(uniqueId);
            firebaseRef.child(username).child("isAvailable").setValue(true);

            binding.loadingGroup.setVisibility(View.GONE);
            binding.controls.setVisibility(View.VISIBLE);

            FirebaseDatabase.getInstance().getReference().child("profiles").child(friendsUsername).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user=snapshot.getValue(User.class);
                    Glide.with(CallActivity.this).load(user.getProfile()).into(binding.friendProfile);
                    binding.friendName.setText(user.getName());
                    binding.friendCity.setText(user.getCity());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }else{
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    friendsUsername=createdBy;
                    FirebaseDatabase.getInstance().getReference().child("profiles").child(friendsUsername).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User user=snapshot.getValue(User.class);
                            Glide.with(CallActivity.this).load(user.getProfile()).into(binding.friendProfile);
                            binding.friendName.setText(user.getName());
                            binding.friendCity.setText(user.getCity());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                    FirebaseDatabase.getInstance().getReference().child("users")
                            .child(friendsUsername)
                            .child("connId")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if(snapshot.getValue()!=null){
                                        sendCallRequest();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                }
            },2000);
        }
    }

    public void onPeerConnected(){
        isPeerConnected=true;
    }

    void sendCallRequest(){
        if(!isPeerConnected){
            Toast.makeText(this, "You are not connected. Please check you internet.", Toast.LENGTH_SHORT).show();
            return;
        }
        listenConnId();
    }

    void listenConnId(){
        firebaseRef.child(friendsUsername).child("connId").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.getValue()==null){
                    return;
                }
                binding.loadingGroup.setVisibility(View.GONE);
                binding.controls.setVisibility(View.VISIBLE);
                String connId=snapshot.getValue(String.class);
                callJavascriptFunction("javascript:startCall(\""+connId+"\")");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    void callJavascriptFunction(String function){
        binding.webview.post(new Runnable() {
            @Override
            public void run() {
                binding.webview.evaluateJavascript(function,null);
            }
        });
    }
    String generateUniqueId(){
        return UUID.randomUUID().toString();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        pageExit=true;
        firebaseRef.child(createdBy).setValue(null);
        finish();
    }

}