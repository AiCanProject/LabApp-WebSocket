package com.aican.aicanapp.Authentication;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.aican.aicanapp.Dashboard;
import com.aican.aicanapp.FirebaseAccounts.PrimaryAccount;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String uid = FirebaseAuth.getInstance(PrimaryAccount.getInstance(this)).getUid();
        if (uid == null) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            startActivity(new Intent(this, Dashboard.class));
        }
    }
}