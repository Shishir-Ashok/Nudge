package com.example.nudge;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class AllUsersActivity extends AppCompatActivity {

    private RecyclerView mUserList;
    private RecyclerView.Adapter mUserListAdapter;
    private RecyclerView.LayoutManager mUserListLayoutManager;

    private FirebaseFirestore db;
    private static final String TAG = AllUsersActivity.class.getName();
    ArrayList<UserObject> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);

        userList = new ArrayList<>();
        initializeRecyclerView();
        getContactList();
    }

    private void getContactList() {
        HashMap<String,String> NUMBERS = new HashMap<String,String>(); ;
        Log.d(TAG, "getContactList: BEFORE CURSOR");
        db = FirebaseFirestore.getInstance();
        CollectionReference colRef = db.collection("Users");
        colRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Log.d(TAG, "Number -> " + document.getString("phone-number"));
                        NUMBERS.put(document.getString("phone-number"),document.getString("uid"));
                    }
                }
                Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null, null, null, null);
                Log.d(TAG, "getContactList: AFTER CURSOR");
                assert phones != null;
                while (phones.moveToNext()) {
                    String phone = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.
                            Phone.NUMBER));
                    if (NUMBERS.containsKey(phone)) {
                        String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.
                                Phone.DISPLAY_NAME));
                        String uid = NUMBERS.get(phone);
                        Log.d(TAG, String.format("getContactList: Phone Number -> %s\nName -> %s\nUID -> %s", phone,
                                name,uid));
                        UserObject mContact = new UserObject(uid, name, phone);
                        userList.add(mContact);
                        mUserListAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }


    private void initializeRecyclerView() {
        Log.d(TAG, "initializeRecyclerView: INITIALIZED!");
        mUserList = (RecyclerView) findViewById(R.id.recyclerUserList);
        mUserList.setNestedScrollingEnabled(false);
        mUserList.setHasFixedSize(false);
        mUserListLayoutManager = new LinearLayoutManager(getApplicationContext(),LinearLayoutManager
                .VERTICAL, false);
        mUserList.setLayoutManager(mUserListLayoutManager);
        mUserListAdapter = new UserListAdapter(AllUsersActivity.this,userList);
        mUserList.setAdapter(mUserListAdapter);
    }
}