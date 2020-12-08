package com.example.nudge;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Message_activity extends AppCompatActivity {

    private ImageView imgProfilePhoto;
    private TextView txtUsername;
    private Button btnSend;
    private EditText edTxtMessage;

    private String receiverID;
    private String chatRoomID;


    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private static final String TAG = Message_activity.class.getName();

    MessagesAdapter messagesAdapter;
    List<Chat> mChat;

    RecyclerView recyclerView;
    Intent intent;

    private FirebaseUser mUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        db = FirebaseFirestore.getInstance();


        fetchMessages();
        imgProfilePhoto = (ImageView) findViewById(R.id.imgProfilePhoto);
        txtUsername = (TextView) findViewById(R.id.txtUsername);
        btnSend = (Button) findViewById(R.id.btnSend);
        edTxtMessage = (EditText) findViewById(R.id.edTxtMessage);

        recyclerView = (RecyclerView)findViewById(R.id.recyclerMessages);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        edTxtMessage.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                // you may want to play with the offset parameter
                layoutManager.scrollToPositionWithOffset(position, 0);
                edTxtMessage.setFocusableInTouchMode(true);
                edTxtMessage.post(() -> {
                    edTxtMessage.requestFocus();
                    UiUtils.showKeyboard(edTxtMessage);
                });
            }
        });
        edTxtMessage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    edTxtMessage.setFocusableInTouchMode(false);
                    UiUtils.hideKeyboard();
                }
            }
        });

        intent = getIntent();
        String receiver = intent.getStringExtra("username");
        receiverID = intent.getStringExtra("userID");
        txtUsername.setText(receiver);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = edTxtMessage.getText().toString();
                Log.d(TAG, "onClick: Clicked send button");
                if (!message.equals("")) {
                    sendMessage(currentUser.getUid(), receiverID, message);
                }
                edTxtMessage.setText("");
            }
        });
    }

    private void sendMessage(String uid, String receiverID, String message) {
    }

    private void fetchMessages() {
        Log.d(TAG, "fetchMessages: COLLECTIONREFERENCE");
        db.collection("Chatrooms").whereArrayContains("users", mAuth.getUid()).
                addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.w(TAG, "Listen failed.", error);
                            return;
                        }
                        for (QueryDocumentSnapshot doc : value) {
                            List<String> users = (List<String>) doc.get("users");
                            if (users.contains(receiverID)) {
                                Log.d(TAG, "onEvent: docID -> " + doc.getId());
                                chatRoomID = doc.getId();
                                break;
                            }
                        }
                    }
                });
        mChat = new ArrayList<>();
        db.collection("Messages").
                orderBy("timestamp", Query.Direction.DESCENDING).limit(20).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.w(TAG, "Listen failed.", error);
                    return;
                }
                Log.d(TAG, "onEvent: ENTERING FOR LOOP CHATROOMID");
                for (QueryDocumentSnapshot doc : value) {
                    Log.d(TAG, String.format("onEvent: chatRoomID -> %s\n message -> %s",doc.get("chatRoomID"),doc.get("message")));
                }

            }
        });
    }
}

