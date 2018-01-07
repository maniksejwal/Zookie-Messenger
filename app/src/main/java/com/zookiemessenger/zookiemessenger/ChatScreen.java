package com.zookiemessenger.zookiemessenger;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Created by manik on 24/12/17.
 */

public class ChatScreen extends AppCompatActivity {
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    private static final int NEW_POLL_ID = Menu.FIRST;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private FirebaseUser mFirebaseUser;
    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseStorage mFirebaseStorage;

    private DatabaseReference mUserDatabaseReference;
    private DatabaseReference mChatsDatabaseReference;
    private StorageReference mChatPhotosStorageReference;

    public String mChatKey = null;

    private ChildEventListener mChildEventListener;

    private String mContactKey;
    private String mContactName;
    private String mUserPhoneNumber;

    private String mType;
    private boolean isGroup = false, isNewGroup, isAdmin = false;
    private ArrayList<String> mGroupMemberList;

    public static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_screen);

        Intent intent = getIntent();
        isNewGroup = intent.getBooleanExtra("isNewGroup", false);
        mType = intent.getStringExtra("type");
        mGroupMemberList = intent.getStringArrayListExtra("memberList");
        mContactKey = intent.getStringExtra(getString(R.string.contact_key));
        mContactName = intent.getStringExtra("contactName");

        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();

        mUserDatabaseReference = mFirebaseDatabase.getReference().child(getString(R.string.users));
        mChatsDatabaseReference = mFirebaseDatabase.getReference().child(getString(R.string.chats));
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");

        mUserPhoneNumber = mFirebaseUser.getPhoneNumber();

        if (mType.equals("group")) {
            mChatKey = mContactKey;
            isGroup = true;
        }
        mUserDatabaseReference.child(mUserPhoneNumber + "/" + getString(R.string.chats) + "/" + mContactKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Timber.v("value " + dataSnapshot.getValue());

                        if (isNewGroup) {
                            //make a chat key
                            mChatKey = mChatsDatabaseReference.push().getKey();
                            Timber.v("mChatKey: " + mChatKey);
                            String groupKey = mChatKey; // DO NOT TOUCH THIS!!!!!!!!!!
                            Timber.v(groupKey);         // it handles null value due to delay in contacting the server

                            //mUserDatabaseReference.child(mUserPhoneNumber + "/" + getString(R.string.chats) + "/"
                            //        + mChatKey).setValue(mChatKey);

                            Timber.v("group member list\t" + mGroupMemberList);

                            mGroupMemberList.add(mUserPhoneNumber);
                            //Send chatKey to members
                            for (String member : mGroupMemberList)
                                mUserDatabaseReference.child(member + "/" + getString(R.string.chats) +
                                        "/" + mChatKey).setValue(mChatKey);

                            //Add members to chat
                            mChatsDatabaseReference.child(mChatKey + "/" + "members")
                                    .setValue(mGroupMemberList);

                            //Add meta
                            mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.meta) +
                                    "/" + getString(R.string.type)).setValue(getString(R.string.group));

                            mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.meta) +
                                    "/" + getString(R.string.admin)).push().setValue(mUserPhoneNumber);

                            isAdmin = true;
                        } else if (dataSnapshot.getValue() == null) {
                            //make a chat key and add to the users
                            mChatKey = mChatsDatabaseReference.push().getKey();
                            Timber.v("mChatKey: " + mChatKey);

                            mUserDatabaseReference.child(
                                    mContactKey + "/" + getString(R.string.chats) + "/" + mUserPhoneNumber)
                                    .setValue(mChatKey);
                            mUserDatabaseReference.child(
                                    mUserPhoneNumber + "/" + getString(R.string.chats) + "/" + mContactKey)
                                    .setValue(mChatKey);

                            mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.meta) +
                                    "/" + getString(R.string.type)).setValue("normal");

                        } else mChatKey = String.valueOf(dataSnapshot.getValue());

                        attachDatabaseReadListener();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        if (isGroup && !isAdmin) {
            mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.meta) + "/" + getString(R.string.admin))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot child : dataSnapshot.getChildren())
                                if (child.getValue() == mUserPhoneNumber) isAdmin = true;
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
        }


        setLayout();
    }

    void setLayout() {
        setTitle(mContactName);
        // Initialize references to views
        mProgressBar = findViewById(R.id.progressBar);
        mMessageListView = findViewById(R.id.messageListView);
        mPhotoPickerButton = findViewById(R.id.photoPickerButton);
        mMessageEditText = findViewById(R.id.messageEditText);
        mSendButton = findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        Timber.v("setLayout() mChatKey " + mChatKey);
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages, mUserPhoneNumber, mChatKey);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(
                        intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString()
                        , mUserPhoneNumber, "text", null);
                mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.messages)).push().setValue(friendlyMessage);
                // Clear input box
                mMessageEditText.setText("");
            }
        });
    }

    private void attachDatabaseReadListener() {
        if (mChildEventListener != null) return;
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                mMessageAdapter.add(friendlyMessage);
            }

            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            public void onCancelled(DatabaseError databaseError) {
            }
        };
        mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.messages)).addChildEventListener(mChildEventListener);
        Timber.v("messageDatabaseReadListener attached");
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.messages)).removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isGroup) menu.add(0, NEW_POLL_ID, 0, getString(R.string.new_poll));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case NEW_POLL_ID:
                Intent intent = new Intent(this, PollActivity.class);
                intent.putExtra("newPoll", true);
                intent.putExtra("chatKey", mChatKey);
                startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();

            // Get a reference to store file at chat_photos/<FILENAME>
            StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());

            // Upload file to Firebase Storage
            photoRef.putFile(selectedImageUri)
                    .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // When the image has successfully uploaded, we get its download URL
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();

                            // Set the download URL to the message box, so that the user can send it to the database
                            FriendlyMessage friendlyMessage = new FriendlyMessage(null
                                    , mUserPhoneNumber, "image", downloadUrl.toString());
                            mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.messages)).setValue(friendlyMessage);
                        }
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //mMessageAdapter.clear();
        detachDatabaseReadListener();
    }
}

//TODO: add meta to chat
//TODO: delete messages