package com.example.nudge;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class otp_verify extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private String authVerificationID;
    private static final String TAG = otp_verify.class.getName();
    private ProgressBar progBarVerifyOTP;
    private TextView errorMessage;
    private Button verifyOTP;
    private EditText otp;
//    private final Bundle params = this.getIntent().getExtras();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verify);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        Intent intent = getIntent();
        authVerificationID = intent.getStringExtra("authVerificationID");
        Log.d(TAG, String.format("onCreate: authVerificationID  : %s",authVerificationID));
        progBarVerifyOTP = (ProgressBar) findViewById(R.id.progressBarVerifyOTP);
        errorMessage = (TextView) findViewById(R.id.txtOtpError);
        verifyOTP = (Button) findViewById(R.id.btnVerifyOTP);
        otp = (EditText) findViewById(R.id.edTxtOTP);

        verifyOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d(TAG, "onClick: Verify OTP button clicked");
                String otp_string = otp.getText().toString();
                if(otp_string.isEmpty()) {
                    errorMessage.setVisibility(View.VISIBLE);
                    errorMessage.setText("Invalid OTP\r");
                }
                else {
                    progBarVerifyOTP.setVisibility(View.VISIBLE);
                    verifyOTP.setEnabled(false);
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(authVerificationID, otp_string);
                    signInWithPhoneAuthCredential(credential);
                }
            }
        });
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(otp_verify.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = task.getResult().getUser();
                        Log.d(TAG, "signInWithCredential:success. User -> "+user);
                        sendUserHome();
                        // ...
                    }
                    else {
                        // Sign in failed, display a message and update the UI
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage.setVisibility(View.VISIBLE);
                            errorMessage.setText("Incorrect OTP\r");
                            progBarVerifyOTP.setVisibility(View.INVISIBLE);
                            verifyOTP.setEnabled(true);
                            // The verification code entered was invalid
                        }
                    }
                }
            } );
    }

    @Override
    public void onStart(){
        super.onStart();
        if(currentUser != null){
            sendUserHome();
        }

    }
    public void sendUserHome(){
        Log.d(TAG, "sendUserHome: REACHED THE FUNCTION!");
        Intent homeIntent = new Intent(otp_verify.this,UserInfo.class);
        Intent intent = getIntent();
        String phone_number = intent.getStringExtra("phone_number");
        String country_code = intent.getStringExtra("country_code");
        Log.d(TAG, String.format("sendUserHome: Phone -> %s\nCountry -> %s",phone_number,country_code));
        homeIntent.putExtra("phone_number",phone_number);
        homeIntent.putExtra("country_code",country_code);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);
        finish();
    }
}