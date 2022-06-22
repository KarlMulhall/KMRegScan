package com.example.kmregscan;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MenuActivity extends AppCompatActivity implements View.OnClickListener {

    private Button logout, collectCar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        collectCar = findViewById(R.id.collectCar);
        logout = findViewById(R.id.logout);

        collectCar.setOnClickListener(this);
        logout.setOnClickListener(this);

    }

    public void onClick(View v) {
        switch (v.getId()){
            case R.id.collectCar:
                startActivity(new Intent(this, MainActivity.class));
                break;
            case R.id.logout:
                userLogout();
                break;
        }
    }

    private void userLogout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(MenuActivity.this, LoginActivity.class));
    }
}
