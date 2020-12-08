package com.example.nudge;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;

public class UserInfo extends AppCompatActivity {

    private static final String TAG = UserInfo.class.getName();

    private Button btnContinue;
    private EditText edTxtName;
    private ProgressBar progressBar;
    private TextView txtError;

    private String country_code;
    private String phone_number;
    private String name;

    private FirebaseAuth mAuth;
    private DocumentReference docIdRef;

    private FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        final Bundle params = this.getIntent().getExtras();
        country_code = params.getString("country_code");
        phone_number = params.getString("phone_number");

        btnContinue = (Button) findViewById(R.id.btnContinue);
        edTxtName = (EditText) findViewById(R.id.edTxtName);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        txtError = (TextView) findViewById(R.id.txtError);

        btnContinue.setEnabled(false);

        db = FirebaseFirestore.getInstance();

        edTxtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                boolean isFilled = true;
                if(edTxtName.getText().toString().equals("")){
                    btnContinue.setEnabled(false);
                    isFilled = false;
                }
                if(isFilled) {
                    enableSubmitIfReady();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                btnContinue.setEnabled(false);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtError.setText("");
                progressBar.setVisibility(View.VISIBLE);
                btnContinue.setEnabled(false);

                final boolean[] flag = {true,false};
                name = edTxtName.getText().toString().toLowerCase();
                Log.d(TAG, String.format("onClick: country code : %s\nphone number : %s",country_code,phone_number));
                CollectionReference usersRef = db.collection("Users");
                Log.d(TAG, "onClick: Collections");
                Query query = usersRef.whereEqualTo("name", name);
                Log.d(TAG, "onClick: query");
                query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        Log.d(TAG, "onComplete: TASK COMPLETE. Next if Statement to follow.\n");
                        if(task.isSuccessful()){
                            for(DocumentSnapshot documentSnapshot : task.getResult()){
                                String user = documentSnapshot.getString("name");
                                String dbCountryCode = documentSnapshot.getString("country-code");
                                String dbPhoneNumber = documentSnapshot.getString("phone-number");
                                Log.d(TAG, "onComplete: DOCUMENT SNAPSHOT" + documentSnapshot.getId());
                                Log.d(TAG, String.format("onComplete: dbCountryCode -> %s\ndbPhoneNumber -> %s",dbCountryCode,dbPhoneNumber));

                                if(user.equals(name) && !(dbCountryCode.equals(country_code) && dbPhoneNumber.equals(phone_number))){
                                    Log.d(TAG, "User Exists");
                                    txtError.setText("Username already exists\r");
                                    flag[0] = false;
                                    progressBar.setVisibility(View.INVISIBLE);
                                    btnContinue.setEnabled(true);
                                    break;
//                                    Toast.makeText(MainActivity.this, "Username exists", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        else{
                            Log.d(TAG, "onComplete: Task failed");
                        }

                        if(flag[0]){
                            Log.d(TAG, "User does not Exists");
                            //You can store new user information here
                            db.collection("Users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                            String dbCountryCode = documentSnapshot.getString("country-code");
                                            String dbPhoneNumber = documentSnapshot.getString("phone-number");
                                            Log.d(TAG, "onComplete: DOCUMENT SNAPSHOT" + documentSnapshot.getId());
                                            Log.d(TAG, String.format("onComplete: dbCountryCode -> %s\ndbPhoneNumber -> %s", dbCountryCode, dbPhoneNumber));

                                            if (dbCountryCode.equals(country_code) && dbPhoneNumber.equals(phone_number)) {
                                                Log.d(TAG, "onComplete: Existing user. Matching phone number");
                                                String documentReference = documentSnapshot.getId();
                                                flag[1] = true;
                                                docIdRef = db.collection("Users").document(documentReference);
                                                break;
                                            }
                                        }
                                    }

                                    if(flag[1]){
                                        sameUserUpdate();
                                    }
                                    else if(flag[0]){
                                        addNewUser();
                                    }
                                }
                            });
                        }
                    }
                });

            }
        });

    }

    private void addNewUser() {
        mAuth = FirebaseAuth.getInstance();
        String uid = mAuth.getUid();
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("country-code", country_code);
        user.put("phone-number", phone_number);
        user.put("photo", "default");
        user.put("uid", uid);
        Log.d(TAG, "addNewUser: UID -> "+uid);
        db = FirebaseFirestore.getInstance();
        db.collection("Users")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        sendUserHome();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
    }

    private void sameUserUpdate() {
        db.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                // Note: this could be done without a transaction
                //       by updating the population using FieldValue.increment()
                transaction.update(docIdRef, "name", name);
                Log.d(TAG, "apply: Transaction successful. Name updated");

                // Success
                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "Transaction success!");
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Log.d(TAG, "onSuccess: USERNAME -> "+name);
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build();

                user.updateProfile(profileUpdates)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "User profile updated.");
                                }
                            }
                        });
                sendUserHome();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Transaction failure.", e);
                    }
                });


    }

    private void sendUserHome(){
        Intent homeIntent = new Intent(UserInfo.this,MainActivity.class);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);
    }

    private void enableSubmitIfReady(){
        btnContinue.setEnabled(true);
    }
}