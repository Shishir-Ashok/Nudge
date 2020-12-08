package com.example.nudge;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class get_started extends AppCompatActivity {
    private static final String TAG = get_started.class.getName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started);

        Button getStarted = (Button) findViewById(R.id.btnGetStarted);

        getStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Get Started button clicked");
                Intent i = new Intent(get_started.this, phone_verification.class);
                startActivity(i);
                finish();
            }
        });
    }

}