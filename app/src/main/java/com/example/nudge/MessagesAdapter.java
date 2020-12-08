package com.example.nudge;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.UserListViewHolder> {

    private List<Chat> mChat;
    private Context mContext;
    public MessagesAdapter(Context mContext, List<Chat> mChat) {
        this.mChat = mChat;
        this.mContext = mContext;
    }
    private FirebaseFirestore db;
    private static final String TAG = UserListAdapter.class.getName();

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    FirebaseUser currectUser;

    @NonNull
    @Override
    public UserListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutView;
        if(viewType == MSG_TYPE_RIGHT) {
            layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_right, null, false);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutView.setLayoutParams(lp);

        }
        else {
            layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_left, null, false);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutView.setLayoutParams(lp);
        }
        db = FirebaseFirestore.getInstance();
        UserListViewHolder rcv = new MessagesAdapter.UserListViewHolder(layoutView);
        return rcv;
    }

    @Override
    public void onBindViewHolder(@NonNull UserListViewHolder holder, int position) {
        Chat chat = mChat.get(position);

        holder.showMessage.setText(chat.getMessage());
    }

    @Override
    public int getItemCount() {
        return mChat.size();
    }

    public class UserListViewHolder extends RecyclerView.ViewHolder {
        public TextView showMessage, mPhone;
        public RelativeLayout mLayout;
        public UserListViewHolder(View view){
            super(view);
            showMessage = view.findViewById(R.id.txtMessage);
            mPhone = view.findViewById(R.id.phone);
            mLayout = view.findViewById(R.id.itemLayout);
        }

    }

    @Override
    public int getItemViewType(int position) {
        currectUser = FirebaseAuth.getInstance().getCurrentUser();
        if(mChat.get(position).getSender().equals(currectUser.getUid())){
            return MSG_TYPE_RIGHT;
        }
        else{
            return  MSG_TYPE_LEFT;
        }
    }
}
