package com.example.nudge;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
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
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class phone_verification extends AppCompatActivity {

    private static final String TAG = phone_verification.class.getName();

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private EditText mCountryCode;
    private EditText mPhoneNumber;
    private ProgressBar mProgGenerateOTP;
    private TextView mTxtError;
    private Button mBtnGenerateOTP;
    private String phone_number;
    private String country_code;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verification);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        mCountryCode = (EditText) findViewById(R.id.edTxtCountryCode);
        mPhoneNumber = (EditText) findViewById(R.id.edTxtPhoneNumber);
        mBtnGenerateOTP = (Button) findViewById(R.id.btnGenerateOtp);
        mProgGenerateOTP = (ProgressBar) findViewById(R.id.progressBarGenerateOTP);
        mTxtError = (TextView) findViewById(R.id.txtError);


        mBtnGenerateOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTxtError.setVisibility(View.INVISIBLE);
                Log.d(TAG, "onClick: Generate OTP button clicked.");
                country_code = mCountryCode.getText().toString();
                phone_number = mPhoneNumber.getText().toString();

                String complete_phone = "+" + country_code + phone_number;
                Log.d(TAG, String.format("onClick: COMPLETE Phone number : %s",complete_phone));
                if(country_code.isEmpty() && phone_number.isEmpty()) {
                    setTextError("Invalid country code\nand phone number");
                }
                else if(country_code.isEmpty()){
                    setTextError("Invalid country Code\r");
                }
                else if(phone_number.isEmpty()){
                    setTextError("Invalid phone number\r");
                }
                else if(phone_number.length() < 10){
                    setTextError("Invalid phone number\r");
                }
                else {
                    mProgGenerateOTP.setVisibility(View.VISIBLE);
                    mBtnGenerateOTP.setEnabled(false);
                    PhoneAuthOptions options =
                            PhoneAuthOptions.newBuilder(mAuth)
                                    .setPhoneNumber(complete_phone)       // Phone number to verify
                                    .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                                    .setActivity(phone_verification.this)                 // Activity (for callback binding)
                                    .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                                    .build();
                    PhoneAuthProvider.verifyPhoneNumber(options);
                    Log.d(TAG, "onClick: INSIDE GENERATE OTP CLICK SUCCESSFUL");
                }
            }
        });

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                mTxtError.setVisibility(View.INVISIBLE);
                mProgGenerateOTP.setVisibility(View.VISIBLE);
                mBtnGenerateOTP.setEnabled(false);
                Log.d(TAG, "onVerificationCompleted: CHECK");
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                mProgGenerateOTP.setVisibility(View.INVISIBLE);
                mBtnGenerateOTP.setEnabled(true);
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    setTextError("Something went wrong\r");
                    // ...
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // ...
                    setTextError("Something went wrong\r");
                    Log.d(TAG, "onVerificationFailed: The SMS quota for the project has been exceeded");
                }

            }

            @Override
            public void onCodeSent(@NonNull final String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
//                super.onCodeSent(verificationId, token);
                mResendToken = token;
                new android.os.Handler().postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                Intent otp = new Intent(phone_verification.this, otp_verify.class);
                                otp.putExtra("phone_number",phone_number);
                                otp.putExtra("country_code",country_code);
                                otp.putExtra("authVerificationID",verificationId);
                                Log.d(TAG, String.format("onCodeSent: credentials -> %s\nphone_number -> %s\ncountry_code -> %s ", verificationId,phone_number,country_code));
                                startActivity(otp);
                                finish();
                            }
                        },
                        10000);
            }

        };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(phone_verification.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = task.getResult().getUser();
                            onStart();
                            Log.d(TAG, "signInWithCredential:success");

                            // ...
                        }
                        else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                setTextError("Something went wrong\r");
                                mProgGenerateOTP.setVisibility(View.INVISIBLE);
                                mBtnGenerateOTP.setEnabled(true);
                                // The verification code entered was invalid
                            }
                        }
                    }
                } );
    }

    public void setTextError(String message) {
        mTxtError.setVisibility(View.VISIBLE);
        mTxtError.setText(message);
    }


    @Override
    public void onStart(){
        super.onStart();
        if(currentUser != null){
            Intent homeIntent = new Intent(phone_verification.this,MainActivity.class);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(homeIntent);
            finish();
        }

    }
}