package com.example.nudge;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.core.OrderBy;
import com.google.firebase.firestore.model.Document;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    private RecyclerView mUserList;
    private RecyclerView.Adapter mUserListAdapter;
    private RecyclerView.LayoutManager mUserListLayoutManager;

    ArrayList<UserObject> userList;
    private List<String> users;
    private List<String> chatRooms;
    private List<String> message;
    private Map<String,Object> unreadMap;

    private Button btnLogout;
    private Button btnNewConversation;
    private int i=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnNewConversation = (Button) findViewById(R.id.btnNewConversation);
        try {
            Log.d(TAG, String.format("onCreate: USERNAME -> %s\nmAuth.getUid -> %s",currentUser.getDisplayName(),currentUser.getUid()));
        }
        catch (NullPointerException e){
            Log.w(TAG, "onCreate: EXCEPTION "+e);
            sendUserToStart();
        }
        db = FirebaseFirestore.getInstance();
        getPermission();
        userList = new ArrayList<>();
        initializeRecyclerView();
        loadContacts();

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Logged out");
                mAuth.signOut();
                sendUserToStart();
            }
        });

        btnNewConversation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), AllUsersActivity.class));
            }
        });

    }

    private void loadContacts() {
        db.collection("Chatrooms").whereArrayContains("users", mAuth.getUid()).
                orderBy("timestamp", Query.Direction.DESCENDING).limit(20).addSnapshotListener(new EventListener<QuerySnapshot>() {
                    
            @Override
            public void onEvent(@Nullable QuerySnapshot value,
                                @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: KAME");
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                Log.d(TAG, "onEvent: Current User ID -> "+currentUser.getUid());
                Log.d(TAG, "onEvent: VALUE -> "+value.getQuery());
                chatRooms = new ArrayList<>();
                users = new ArrayList<>();
                message = new ArrayList<>();
                for (QueryDocumentSnapshot doc : value) {
                    Log.d(TAG, "onEvent: DOC -> "+doc);
                    chatRooms.add(doc.getId());
                    unreadMap = (Map<String, Object>) doc.get("unread");
                    message.add(unreadMap.get("message").toString());
                    Log.d(TAG, "onEvent: unread -> "+unreadMap.get("message"));
                    String x = doc.get("users").toString();
                    x = x.substring(1, x.length() - 1);
                    String[] a = x.split(",");
                    Log.d(TAG, String.format("onEvent: X -> %s",a[0]));
                    if(a[0].equals(mAuth.getUid())){
                        users.add(a[1].replaceAll("\\s",""));
                    }
                    else{
                        users.add(a[0].replaceAll("\\s",""));
                    }
                }
                Log.d(TAG, "Chat IDs: " + chatRooms);
                Log.d(TAG, "onEvent: USERS -> "+users);
                Log.d(TAG, "onEvent: Messages -> "+message);
                for ( String id : users ) {
                    db.collection("Users").whereEqualTo("uid",id).addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                            if (e != null) {
                                Log.w(TAG, "Listen failed.", e);
                                return;
                            }

                            for(QueryDocumentSnapshot doc : value) {
                                Log.d(TAG, "onEvent: DOC -> "+doc.get("phone-number"));
                                String contactName = getContactName(doc.get("phone-number").toString());
                                Log.d(TAG, "onEvent: CONTACTNAME -> "+contactName);
                                Log.d(TAG, String.format("onEvent:BEFORE i -> %d\nmessage -> %s",i,message.get(i)));
                                UserObject mContact = new UserObject(id, contactName, message.get(i++));
                                userList.add(mContact);
                                mUserListAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                    i = 0;
                    final SharedPreferences sharedPreferences = getSharedPreferences("USER",MODE_PRIVATE);
                    Gson gson = new Gson();
                    String json = gson.toJson(chatRooms);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("chatRoomID",json );
                    editor.commit();
                }

            }
        });
    }

    private String getContactName(String phone) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
        String name = "?";

        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }

    private void initializeRecyclerView() {
        Log.d(TAG, "initializeRecyclerView: INITIALIZED!");
        mUserList = (RecyclerView) findViewById(R.id.chatList);
        mUserList.setNestedScrollingEnabled(false);
        mUserList.setHasFixedSize(false);
        mUserListLayoutManager = new LinearLayoutManager(getApplicationContext(),LinearLayoutManager
                .VERTICAL, false);
        mUserList.setLayoutManager(mUserListLayoutManager);
        mUserListAdapter = new UserListAdapter(MainActivity.this,userList);
        mUserList.setAdapter(mUserListAdapter);
    }

    private void getPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[] {Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS},1);
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        if(currentUser == null){
            sendUserToStart();
        }
    }

    public void sendUserToStart(){
        Intent loginIntent = new Intent(MainActivity.this,get_started.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }
}
