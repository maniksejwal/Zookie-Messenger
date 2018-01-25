package com.zookiemessenger.zookiemessenger.chat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.zookiemessenger.zookiemessenger.R;

import java.io.File;
import java.util.ArrayList;

import timber.log.Timber;

public class TagsActivity extends AppCompatActivity {
    //public static final int RC_SIGN_IN = 1;
    private static final int RC_FILE_PICKER = 1;
    private static final int RC_GRAPHIC_PICKER = 2;
    private static final int RC_IMAGE_CAPTURE = 3;
    private static final int RC_VIDEO_CAPTURE = 4;
    private static final int RC_TAGS = 5;
    private static final int RC_CONTACT = 5;
    private static final int RC_AUDIO_PICKER = 4;
    private static final int RC_DOCUMENT_PICKER = 5;

    ArrayList<String> mTagList = new ArrayList<>();
    ListView mListView;
    FloatingActionButton mAddTagButton;
    FloatingActionButton mTagsDoneButton;

    private FirebaseUser mFirebaseUser;
    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseStorage mFirebaseStorage;

    private DatabaseReference mChatsDatabaseReference;
    private StorageReference mChatStorageReference;

    private String mChatKey = null;
    private StorageReference mFileRef;

    private String mUserPhoneNumber;

    private Uri mSelectedFileUri;
    private String mTempGraphicPath;

    private int mRequestCode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mChatsDatabaseReference = mFirebaseDatabase.getReference().child(getString(R.string.chats));


        mUserPhoneNumber = mFirebaseUser.getPhoneNumber();

        getMyIntent();
        mChatStorageReference = mFirebaseStorage.getReference().child(mChatKey);

        setLayout();
    }

    private void setLayout() {
        setContentView(R.layout.activity_tags);

        mListView = findViewById(R.id.tag_list);
        mAddTagButton = findViewById(R.id.add_tag_button);
        mTagsDoneButton = findViewById(R.id.tags_done);

        mAddTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText tagEditText = findViewById(R.id.tag_edit_text);
                String tagName = tagEditText.getText().toString();
                mTagList.add(tagName);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),
                        R.layout.tag_layout);
                mListView.setAdapter(adapter);
                mTagsDoneButton.setVisibility(View.VISIBLE);
                mAddTagButton.setVisibility(View.GONE);
            }
        });

        mTagsDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(getString(R.string.tag_list), mTagList);
                uploadFile();
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void getMyIntent() {
        Intent intent = getIntent();

        mChatKey = intent.getStringExtra("chatKey");

        Intent data = intent.getParcelableExtra("data");
        mSelectedFileUri = data.getData();
        mFileRef = mChatStorageReference.child(mSelectedFileUri.getLastPathSegment());

        mTempGraphicPath = intent.getStringExtra("tempGraphicPath");

        mRequestCode = intent.getIntExtra("requestCode", 0);
        if (mRequestCode == 0) {
            Toast.makeText(this, "Please try again", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void uploadFile(){
        switch (mRequestCode) {
            case RC_GRAPHIC_PICKER:
                mFileRef.putFile(mSelectedFileUri)
                        .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                FriendlyMessage friendlyMessage = new FriendlyMessage(null,
                                        mUserPhoneNumber, "image", downloadUrl.toString(),
                                        (String[]) mTagList.toArray());
                                Timber.v("mChatKey " + mChatKey);
                                mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.messages))
                                        .push().setValue(friendlyMessage);
                            }
                        });
                break;
            case RC_FILE_PICKER:
                mFileRef.putFile(mSelectedFileUri)
                        .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // When the image has successfully uploaded, we get its download URL
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                // Set the download URL to the message box, so that the user can send it to the database
                                FriendlyMessage friendlyMessage = new FriendlyMessage(null,
                                        mUserPhoneNumber, "file", downloadUrl.toString(),
                                        (String[]) mTagList.toArray());
                                Timber.v("mChatKey " + mChatKey);
                                mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.messages))
                                        .push().setValue(friendlyMessage);
                            }
                        });
                break;
            case RC_IMAGE_CAPTURE:
                mFileRef.putFile(Uri.fromFile(new File(mTempGraphicPath)))
                        .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // When the image has successfully uploaded, we get its download URL
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                // Set the download URL to the message box, so that the user can send it to the database
                                FriendlyMessage friendlyMessage = new FriendlyMessage(null,
                                        mUserPhoneNumber, "image", downloadUrl.toString(),
                                        (String[]) mTagList.toArray());
                                Timber.v("mChatKey " + mChatKey);
                                mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.messages))
                                        .push().setValue(friendlyMessage);
                            }
                        });
                break;
            case RC_VIDEO_CAPTURE:
                mFileRef.putFile(Uri.fromFile(new File(mTempGraphicPath)))
                        .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Timber.v("video uploaded");
                                // When the image has successfully uploaded, we get its download URL
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                // Set the download URL to the message box, so that the user can send it to the database
                                FriendlyMessage friendlyMessage = new FriendlyMessage(null,
                                        mUserPhoneNumber, "image", downloadUrl.toString(),
                                        (String[]) mTagList.toArray());
                                Timber.v("mChatKey " + mChatKey);
                                mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.messages)).push().setValue(friendlyMessage);
                            }
                        });
                break;

            /*case RC_CONTACT:
                if (resultCode != RESULT_OK) return;
                selectedFileUri = data.getData();
                fileRef = mChatStorageReference.child(selectedFileUri.getLastPathSegment());
                fileRef.putFile(selectedFileUri)
                        .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // When the image has successfully uploaded, we get its download URL
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                // Set the download URL to the message box, so that the user can send it to the database
                                FriendlyMessage friendlyMessage = new FriendlyMessage(null
                                        , mUserPhoneNumber, "file", downloadUrl.toString());
                                Timber.v("mChatKey " + mChatKey);
                                mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.messages)).push().setValue(friendlyMessage);
                            }
                        });
                break;*/
        }

    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "File not shared", Toast.LENGTH_SHORT).show();
        super.onBackPressed();
    }
}
