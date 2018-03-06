package com.zookiemessenger.zookiemessenger.chat;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

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
import com.zookiemessenger.zookiemessenger.FilesScreen;
import com.zookiemessenger.zookiemessenger.Helper;
import com.zookiemessenger.zookiemessenger.R;
import com.zookiemessenger.zookiemessenger.poll.PollActivity;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

import static com.zookiemessenger.zookiemessenger.Helper.MESSAGES;
import static com.zookiemessenger.zookiemessenger.Helper.TAGS;

/**
 * Created by manik on 24/12/17.
 */

public class ChatScreen extends AppCompatActivity implements Serializable {
    //public static final int RC_SIGN_IN = 1;
    private static final int RC_FILE_PICKER = 1;
    private static final int RC_GRAPHIC_PICKER = 2;
    private static final int RC_IMAGE_CAPTURE = 3;
    private static final int RC_VIDEO_CAPTURE = 4;
    private static final int RC_TAGS = 5;
    private static final int RC_CONTACT = 5;
    private static final int RC_AUDIO_PICKER = 4;
    private static final int RC_DOCUMENT_PICKER = 5;

    //public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    private static final int DETAILS_ID = R.id.action_files;
    private static final int NEW_POLL_ID = DETAILS_ID + 1;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private EditText mMessageEditText;
    private FloatingActionButton mSendButton;

    private FirebaseUser mFirebaseUser;
    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseStorage mFirebaseStorage;

    private DatabaseReference mUserDatabaseReference;
    private DatabaseReference mChatsDatabaseReference;
    private StorageReference mChatStorageReference;
    private StorageReference mFileRef;

    public String mChatKey = null;
    private ArrayList<ChatListItem> recentChats = new ArrayList<>();

    private ChildEventListener mChildEventListener;

    private String mContactKey, mContactName, mUserPhoneNumber;

    private String mType;
    private boolean isGroup = false, isNewGroup, isAdmin = false;
    private ArrayList<String> mGroupMemberList;

    private String mTempGraphicPath;
    private static final String FILE_PROVIDER_AUTHORITY = "com.zookiemessenger.zookiemessenger.fileprovider";

    private Uri mSelectedFileUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_screen);

        getMyIntent();

        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();

        mUserDatabaseReference = mFirebaseDatabase.getReference().child(getString(R.string.users));
        mChatsDatabaseReference = mFirebaseDatabase.getReference().child(getString(R.string.chats));

        mUserPhoneNumber = mFirebaseUser.getPhoneNumber();

        if (mType != null && mType.equals("group")) {
            mChatKey = mContactKey;
            isGroup = true;
        }

        getChatKey();

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

    private void getMyIntent() {
        Intent intent = getIntent();
        isNewGroup = intent.getBooleanExtra("isNewGroup", false);
        //mType = intent.getStringExtra("type");
        mGroupMemberList = intent.getStringArrayListExtra("memberList");
        //mContactKey = intent.getStringExtra(getString(R.string.contact_key));
        //mContactName = intent.getStringExtra("contactName");
        ChatListItem chat = intent.getParcelableExtra("chat");
        mType = chat.getType();
        mContactKey = chat.getPhoneNumber();
        mContactName = chat.getName();
        recentChats = intent.getParcelableExtra("recentChats");
        recentChats.add(chat);
    }

    private void getChatKey() {
        mUserDatabaseReference.child(mUserPhoneNumber + "/" + getString(R.string.chats) + "/" + mContactKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Timber.v("isNewGroup" + isNewGroup);
                        Timber.v("setting chat " + dataSnapshot.getValue());

                        if (isNewGroup) newGroup();
                        else if (dataSnapshot.getValue() == null) newPrivateChat();
                        else getChat(dataSnapshot);

                        mChatStorageReference = mFirebaseStorage.getReference().child(mChatKey);

                        attachDatabaseReadListener();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void newGroup() {
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
                "/" + Helper.TYPE).setValue(getString(R.string.group));

        mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.meta) +
                "/" + getString(R.string.admin)).push().setValue(mUserPhoneNumber);

        isAdmin = true;

        mChatsDatabaseReference.child(mChatKey).keepSynced(true);
    }

    private void newPrivateChat() {
        //make a chat key
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

        mChatsDatabaseReference.child(mChatKey).keepSynced(true);
    }

    private void getChat(DataSnapshot dataSnapshot) {
        Timber.v("not a new chat");
        mChatKey = String.valueOf(dataSnapshot.getValue());
        Timber.v("mChatKey: " + mChatKey);

        mChatsDatabaseReference.child(mChatKey).keepSynced(true);
    }

    private void attachDatabaseReadListener() {
        if (mChildEventListener != null) return;
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Timber.v("" + dataSnapshot.getValue());
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


    private void setLayout() {
        setTitle(mContactName);
        // Initialize references to views
        mMessageListView = findViewById(R.id.messageListView);

        mMessageEditText = findViewById(R.id.messageEditText);
        mSendButton = findViewById(R.id.sendButton);

        //(findViewById(R.id.messageEditText)).getBackground().clearColorFilter();

        // Initialize message ListView and its adapter
        Timber.v("setLayout() mChatKey " + mChatKey);
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages, mUserPhoneNumber, mChatKey);
        mMessageListView.setAdapter(mMessageAdapter);


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

        //mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString()
                        , mUserPhoneNumber, "text", null, null, null);
                mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.messages)).push().setValue(friendlyMessage);
                // Clear input box
                mMessageEditText.setText("");
            }
        });

        findViewById(R.id.recent_chat_FAB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ChatScreen.class);
                intent.putExtra("chat", recentChats.get(recentChats.size() - 2));
                intent.putExtra("recentChats", recentChats);
                startActivity(intent);
            }
        });

        attachFileClickListeners();
    }

    private void attachFileClickListeners() {
        findViewById(R.id.share_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*, video/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent,
                        "Complete action using"), RC_GRAPHIC_PICKER);
            }
        });

        /*
        findViewById(R.id.share_audio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/.mp3");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent,
                        "Complete action using"), RC_AUDIO_PICKER);
            }
        });

        findViewById(R.id.share_document).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/.pdf, text/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent,
                        "Complete action using"), RC_DOCUMENT_PICKER);
            }
        });*/

        findViewById(R.id.share_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                if (intent.resolveActivity(getPackageManager()) != null)
                    startActivityForResult(Intent.createChooser(intent,
                            "Complete action using"), RC_FILE_PICKER);
            }
        });

        findViewById(R.id.share_new_pic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File photoFile = null;
                try {
                    photoFile = GraphicUtils.createTempImageFile(getApplicationContext());
                } catch (IOException e) {
                    // Error occurred while creating the File
                    e.printStackTrace();
                }
                if (photoFile == null) return;
                // Get the path of the temporary file
                mTempGraphicPath = photoFile.getAbsolutePath();
                // Get the content URI for the image file
                Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
                        FILE_PROVIDER_AUTHORITY, photoFile);
                // Add the URI so the camera can store the image
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                // Launch the camera activity
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_IMAGE_CAPTURE);
            }
        });

        findViewById(R.id.share_new_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                File videoFile = null;
                try {
                    videoFile = GraphicUtils.createTempVideoFile(getApplicationContext());
                } catch (IOException e) {
                    // Error occurred while creating the File
                    e.printStackTrace();
                }
                if (videoFile == null) return;
                // Get the path of the temporary file
                mTempGraphicPath = videoFile.getAbsolutePath();
                // Get the content URI for the image file
                Uri videoURI = FileProvider.getUriForFile(getApplicationContext(),
                        FILE_PROVIDER_AUTHORITY, videoFile);
                // Add the URI so the camera can store the image
                intent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI);
                // Launch the camera activity
                startActivityForResult(Intent.createChooser(intent, "Complete action using"),
                        RC_VIDEO_CAPTURE);
            }
        });

        /*
        findViewById(R.id.share_contact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                startActivityForResult(Intent.createChooser(intent, "Complete action using")
                        , RC_CONTACT);
            }
        });*/
    }


    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mChatsDatabaseReference.child(mChatKey + "/" + getString(R.string.messages)).removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat_screen, menu);
        if (isGroup) menu.add(0, NEW_POLL_ID, 0, getString(R.string.new_poll));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_files:
                Intent intent1 = new Intent(this, FilesScreen.class);
                intent1.putExtra(Helper.CHATKEY, mChatKey);
                startActivity(intent1);
                break;
            case NEW_POLL_ID:
                Intent intent2 = new Intent(this, PollActivity.class);
                intent2.putExtra("newPoll", true);
                intent2.putExtra(Helper.CHATKEY, mChatKey);
                startActivity(intent2);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RC_GRAPHIC_PICKER:
                if (resultCode != RESULT_OK) return;
                mSelectedFileUri = data.getData();
                mFileRef = mChatStorageReference.child(getFileName(mSelectedFileUri));
                openTagsActivity(requestCode);
                break;
            case RC_FILE_PICKER:
                if (resultCode != RESULT_OK) return;
                mSelectedFileUri = data.getData();
                mFileRef = mChatStorageReference.child(mSelectedFileUri.getLastPathSegment());
                openTagsActivity(requestCode);
                break;
            case RC_IMAGE_CAPTURE:
                if (resultCode != RESULT_OK) {
                    GraphicUtils.deleteGraphicFile(getApplicationContext(), mTempGraphicPath);
                    return;
                }
                mSelectedFileUri = Uri.fromFile(new File(mTempGraphicPath));
                mFileRef = mChatStorageReference.child(mSelectedFileUri.getLastPathSegment());
                openTagsActivity(requestCode);
                break;
            case RC_VIDEO_CAPTURE:
                if (resultCode != RESULT_OK) {
                    GraphicUtils.deleteGraphicFile(getApplicationContext(), mTempGraphicPath);
                    return;
                }
                mSelectedFileUri = Uri.fromFile(new File(mTempGraphicPath));
                mFileRef = mChatStorageReference.child(mSelectedFileUri.getLastPathSegment());
                openTagsActivity(requestCode);
                break;
            case RC_TAGS:
                if (resultCode != RESULT_OK) return;
                List<String> tagList = Arrays.asList(data.getStringArrayExtra(getString(R.string.tag_list)));
                int oldRequestCode = data.getIntExtra(Helper.REQUEST_CODE, -1);
                //mFileRef.updateMetadata(new StorageMetadata.Builder().setCustomMetadata(Helper.TAGS,
                //      String.valueOf(tagList)).build());
                uploadFile(tagList, oldRequestCode);


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

    private void openTagsActivity(int requestCode) {
        Intent intent = new Intent(this, TagsActivity.class);
        intent.putExtra("requestCode", requestCode);
        startActivityForResult(intent, RC_TAGS);
    }

    private void uploadFile(final List<String> tagList, final int requestCode) {
        Timber.v("uploadFile() entered");
        Timber.v("requestCode " + requestCode);
        Timber.v("graphic path " + mTempGraphicPath);

        Timber.v("mFileRef " + mFileRef);
        mFileRef.putFile(mSelectedFileUri)
                .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        Timber.v("downloadUrl " + downloadUrl);
                        FriendlyMessage friendlyMessage;
                        switch (requestCode) {
                            case RC_GRAPHIC_PICKER:
                                friendlyMessage = new FriendlyMessage(null,
                                        mUserPhoneNumber, Helper.FILE, Helper.GRAPHIC,
                                        downloadUrl.toString(), tagList);
                                break;
                            case RC_FILE_PICKER:
                                friendlyMessage = new FriendlyMessage(null,
                                        mUserPhoneNumber, Helper.FILE, Helper.FILE,
                                        downloadUrl.toString(), tagList);
                                break;
                            case RC_IMAGE_CAPTURE:
                                friendlyMessage = new FriendlyMessage(null,
                                        mUserPhoneNumber, Helper.FILE, Helper.IMAGE,
                                        downloadUrl.toString(), tagList);
                                break;
                            case RC_VIDEO_CAPTURE:
                                friendlyMessage = new FriendlyMessage(null,
                                        mUserPhoneNumber, Helper.FILE, Helper.VIDEO,
                                        downloadUrl.toString(), tagList);
                                break;
                            default:
                                throw new RuntimeException("Wrong request code. The request code for this file type is not defined");
                        }
                        Timber.v("mChatKey " + mChatKey);
                        String messageKey = mChatsDatabaseReference.child(mChatKey + "/" + MESSAGES)
                                .push().getKey();
                        mChatsDatabaseReference.child(mChatKey + "/" + MESSAGES + "/" + messageKey)
                                .setValue(friendlyMessage);
                        addTag(tagList, messageKey);
                        //Toast.makeText(getApplicationContext(), "Done", Toast.LENGTH_SHORT).show();
                        //finish();
                    }
                });


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


    private void addTag(final List<String> tagList, final String messageKey) {
        mChatsDatabaseReference.child(mChatKey + "/" + TAGS).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (String tag : tagList) {
                            boolean tagFound = false;
                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                if (child.getValue().toString().equals(tag)) {
                                    tagFound = true;
                                    break;
                                }
                            }
                            Timber.v("tagFound " + tagFound);
                            if (!tagFound) mChatsDatabaseReference.child(mChatKey + "/" + TAGS)
                                    .push().setValue(tag);
                            mChatsDatabaseReference.child(mChatKey + "/" + TAGS + "/" + tag)
                                    .push().setValue(messageKey);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}

//TODO: add meta to chat
//TODO: delete messages
//TODO: add message layout
//TODO: receive videos
//TODO: send docs, locations, etc