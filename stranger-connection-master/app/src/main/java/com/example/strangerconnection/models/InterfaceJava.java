package com.example.strangerconnection.models;

import android.webkit.JavascriptInterface;

import com.example.strangerconnection.activities.CallActivity;

public class InterfaceJava {
    CallActivity callActivity;
    public InterfaceJava(CallActivity callActivity){
        this.callActivity=callActivity;
    }
    @JavascriptInterface
    public void onPeerConnected(){
        callActivity.onPeerConnected();
    }

}
