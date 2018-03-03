package com.zookiemessenger.zookiemessenger.chat;

/**
 * Created by manik on 1/1/18.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

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
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_message,
                    parent, false);
        }

        ImageView photoImageView = convertView.findViewById(R.id.photo_view);
        VideoView videoVideoView = convertView.findViewById(R.id.video_view);
        TextView messageTextView = convertView.findViewById(R.id.messageTextView);
        TextView authorTextView = convertView.findViewById(R.id.nameTextView);

        final FriendlyMessage message = getItem(position);

        String type = message.getType();
        Timber.v("message " + message);
        Timber.v("type = " + type);

        StorageReference ref = null;
        String path = null;
        String name = null;
        File file = null;
        if (message.getUrl() != null) {
            ref = mFirebaseStorage.getReferenceFromUrl(message.getUrl());
            path = Helper.APP_FOLDER + File.separator + "Files";
            name = ref.getName();
            file = new File(path + File.separator + name);
        }

        switch (type) {
            case Helper.POLL:
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
            case Helper.FILE:
                String fileType = message.getFileType();
                switch (fileType) {
                    case Helper.GRAPHIC:
                        //gif
                        if (name.endsWith(".gif"))
                            gif(path, ref, file, photoImageView, messageTextView);
                            //Video
                        else if (name.endsWith(".mp4") || name.endsWith(".avi") ||
                                name.endsWith(".flv") || name.endsWith(".wmv") ||
                                name.endsWith(".mov") || name.endsWith(".mpg")) {
                            Helper.saveVideo(getContext(), path, ref, file, videoVideoView);
                            videoVideoView.setVisibility(View.VISIBLE);
                        }
                        //Image
                        else image(path, ref, file, photoImageView, messageTextView);

                        break;
                    case Helper.IMAGE:
                        image(path, ref, file, photoImageView, messageTextView);

                        break;
                    case Helper.VIDEO:
                        Helper.saveVideo(getContext(), path, ref, file, videoVideoView);
                        videoVideoView.setVisibility(View.VISIBLE);

                        break;
                    case Helper.FILE:
                        Helper.saveFile(getContext(), path, ref, file);
                        Glide.with(photoImageView.getContext())
                                .load(file)
                                .thumbnail(1 / 10)
                                .into(photoImageView);
                        photoImageView.setVisibility(View.VISIBLE);
                }

                break;
            default:
                messageTextView.setVisibility(View.VISIBLE);
                messageTextView.setText(message.getText());
        }

        setSender(message, authorTextView, convertView);
        return convertView;
    }

    private void setSender(FriendlyMessage message, TextView authorTextView, View convertView){
        authorTextView.setText(message.getName());
        LinearLayout messageLayout = convertView.findViewById(R.id.message);
        if (message.getName().equals(mUserPhoneNumber)) {
            messageLayout.setGravity(Gravity.END);
            convertView.findViewById(R.id.spacer_left).setVisibility(View.VISIBLE);
            convertView.findViewById(R.id.message).setBackgroundResource(R.drawable.right_bubble);

            //convertView.findViewById(R.id.nameTextView).
        } else {
            messageLayout.setGravity(Gravity.START);
            convertView.findViewById(R.id.spacer_right).setVisibility(View.VISIBLE);
            convertView.findViewById(R.id.message).setBackgroundResource(R.drawable.left_bubble);

        }
    }

    private void image(String path, StorageReference ref, File file, ImageView photoImageView,
                       TextView messageTextView) {
        Helper.saveFile(getContext(), path, ref, file);
        Glide.with(photoImageView.getContext())
                .load(file)
                .thumbnail(1/10)
                .into(photoImageView);
        photoImageView.setVisibility(View.VISIBLE);
    }

    private void gif(String path, StorageReference ref, File file, ImageView photoImageView,
                     TextView messageTextView) {
        Helper.saveFile(getContext(), path, ref, file);
        Glide
                .with(photoImageView.getContext())
                .load(file)
                .thumbnail(1/10)
                .into(photoImageView);

        photoImageView.setVisibility(View.VISIBLE);
    }
}

//TODO: play videos and gifs
//TODO: improve message layout
//TODO: receive files
//TODO: show name if is group