package com.zookiemessenger.zookiemessenger.chat;

/**
 * Created by manik on 1/1/18.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.zookiemessenger.zookiemessenger.Helper;
import com.zookiemessenger.zookiemessenger.R;
import com.zookiemessenger.zookiemessenger.poll.PollActivity;

import java.io.File;
import java.util.List;

import timber.log.Timber;


public class MessageAdapter extends ArrayAdapter<FriendlyMessage> {
    private String mChatKey, mUserPhoneNumber;
    private FirebaseStorage mFirebaseStorage;

    public MessageAdapter(Context context, int resource, List<FriendlyMessage> objects,
                          String userPhoneNumber, String chatKey) {
        super(context, resource, objects);
        mChatKey = chatKey;
        mUserPhoneNumber = userPhoneNumber;
        mFirebaseStorage = FirebaseStorage.getInstance();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_message, parent, false);
        }

        ImageView photoImageView = convertView.findViewById(R.id.photoImageView);
        TextView messageTextView = convertView.findViewById(R.id.messageTextView);
        TextView authorTextView = convertView.findViewById(R.id.nameTextView);

        final FriendlyMessage message = getItem(position);

        String type = message.getType();

        switch (type) {
            case "poll":
                Timber.v("case poll");
                messageTextView.setVisibility(View.VISIBLE);
                photoImageView.setVisibility(View.GONE);
                messageTextView.setText(message.getText());
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getContext(), PollActivity.class);
                        intent.putExtra("pollKey", message.getUrl());
                        intent.putExtra("chatKey", mChatKey);
                        intent.putExtra("userPhoneNumber", mUserPhoneNumber);
                        getContext().startActivity(intent);
                    }
                });
                break;
            case "image":
                Timber.v("case image");
                Timber.v("message " + message);
                StorageReference ref = mFirebaseStorage.getReferenceFromUrl(message.getUrl());
                final String path = Helper.APP_FOLDER + File.separator + "Files";
                final String name = ref.getName();
                final File file = new File(path + name);
                Helper.saveFile(getContext(), path, ref, file);
                Glide.with(photoImageView.getContext())
                        .load(file)
                        .placeholder(R.drawable.common_google_signin_btn_icon_dark_normal)  //TODO: change this
                        .into(photoImageView);
                messageTextView.setVisibility(View.GONE);
                photoImageView.setVisibility(View.VISIBLE);
                break;
            default:
                messageTextView.setVisibility(View.VISIBLE);
                photoImageView.setVisibility(View.GONE);
                messageTextView.setText(message.getText());
        }

        authorTextView.setText(message.getName());
        return convertView;
    }
}
