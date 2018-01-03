package com.zookiemessenger.zookiemessenger;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;

import timber.log.Timber;

import static android.Manifest.permission.READ_CONTACTS;

public class Chats extends AppCompatActivity {
    private static final int REQUEST_READ_CONTACTS = 444;
    private static final int LOG_OUT_ID = Menu.FIRST;

    private ListView mListView;
    private ProgressDialog pDialog;
    private Handler updateBarHandler;

    Cursor cursor;

    FirebaseUser mFirebaseUser;
    FirebaseDatabase mFirebaseDatabase;

    DatabaseReference mUserDatabaseReference;
    private DatabaseReference mChatsDatabaseReference;


    FirebaseMultiQuery firebaseMultiQuery;

    ArrayList<Chat> mChatList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);

        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        if (mFirebaseUser == null) throw new RuntimeException("mFirebaseUser is null");

        mUserDatabaseReference = mFirebaseDatabase.getReference().child(getString(R.string.users) + "/" + mFirebaseUser.getPhoneNumber());
        mChatsDatabaseReference = mFirebaseDatabase.getReference().child("chats");



        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);
        pDialog.show();
        Timber.d("ListViewID" + findViewById(R.id.list).getId());
        mListView = findViewById(R.id.list);
        updateBarHandler = new Handler();
        // Since reading contacts takes more time, let's run it on a separate thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                getChats();
            }
        }).start();

        Timber.v("setting clickListeners");

        // Set onClickListener to the list item.
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent intent = new Intent(getApplicationContext(), ChatScreen.class);
                intent.putExtra("contactPhoneNumber", mChatList.get(position).phoneNumber);
                startActivity(intent);
            }
        });
        setTitle(mFirebaseUser.getDisplayName());
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                updateBarHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pDialog.cancel();
                    }
                }, 500);

                Snackbar.make(mListView, "This app requires the ability to read contacts",
                        Snackbar.LENGTH_SHORT).setAction("Grant", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getChats();
                    }
                }).setAction("exit", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finish();
                    }
                });
            }
        }
    }

    private void getChats() {
        Timber.v("getContacts");
        if (!mayRequestContacts()) {
            return;
        }

        firebaseMultiQuery = new FirebaseMultiQuery(mUserDatabaseReference);
        final Task<Map<DatabaseReference, DataSnapshot>> allLoad = firebaseMultiQuery.start();
        allLoad.addOnCompleteListener(this, new Chats.AllOnCompleteListener());

        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;

        //Uri EmailCONTENT_URI = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        //String EmailCONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
        //String DATA = ContactsContract.CommonDataKinds.Email.DATA;
        ContentResolver contentResolver = getContentResolver();
        cursor = contentResolver.query(CONTENT_URI, null, null, null, null);
        // Iterate every contact in the phone


        mUserDatabaseReference.child("chats").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.v("dataSnapshot = " + dataSnapshot);
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    Timber.v("childSnapshot = " + childSnapshot);
                    Timber.v("phoneNumber " + childSnapshot.child("phoneNumber").getValue() +
                            "\nchatID" + childSnapshot.child("chat").getValue());

                    if (childSnapshot.getValue() == null &&
                            childSnapshot.child("chat").getValue() == null) continue;

                    Chat chat = new Chat(childSnapshot.getKey(),
                            childSnapshot.getValue().toString());
                    mChatList.add(chat);
                }
                                /*runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ContactAdapter adapter = new ContactAdapter(getApplicationContext(), contactList);
                                        mListView.setAdapter(adapter);
                                    }
                                });*/
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        /*// ListView has to be updated using a ui thread
        runOnUiThread(new Runnable(-) {
            @Override
            public void run() {
                ContactAdapter adapter = new ContactAdapter(getApplicationContext(), contactList);
                mListView.setAdapter(adapter);
            }
        });
        // Dismiss the progressbar after 500 milliseconds
        updateBarHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                pDialog.cancel();
            }
        }, 500);*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firebaseMultiQuery != null) firebaseMultiQuery.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Timber.v("entered onCreateOptionsMenu()");
        menu.add(0, LOG_OUT_ID, 0, R.string.sign_out);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case LOG_OUT_ID: {
                FirebaseAuth.getInstance().signOut();
                finish();
                startActivity(new Intent(this, MainActivity.class));
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private class ChatAdapter extends ArrayAdapter<Chat> {

        ChatAdapter(@NonNull Context context, ArrayList<Chat> chats) {
            super(context, 0, chats);
            Timber.v("ContactAdapter created");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View listItemView = convertView;
            if (listItemView == null) {
                listItemView = LayoutInflater.from(getContext()).inflate(R.layout.contacts_list_item
                        , parent, false);
            }

            //((TextView) listItemView.findViewById(R.id.name)).setText(getItem(position).displayName);
            ((TextView) listItemView.findViewById(R.id.phone_number)).setText(getItem(position).phoneNumber);
            /*ImageView img = listItemView.findViewById(R.id.image);
            Picasso
                    .with(getApplicationContext())
                    .load(getItem(position).mImageId)
                    .placeholder(R.mipmap.launcher_ic)
                    .fit()
                    .centerCrop()
                    //.centerInside()                 // or .centerCrop() to avoid a stretched imageÒ
                    .into(img);
                    */

            return listItemView;
        }
    }

    private class AllOnCompleteListener implements OnCompleteListener<Map<DatabaseReference, DataSnapshot>> {
        @Override
        public void onComplete(@NonNull Task<Map<DatabaseReference, DataSnapshot>> task) {
            if (task.isSuccessful()) {
                final Map<DatabaseReference, DataSnapshot> result = task.getResult();
                // Look up DataSnapshot objects using the same DatabaseReferences you passed into FirebaseMultiQuery
            } else {
                if (task.getException() != null)
                    task.getException().printStackTrace();
                // log the error or whatever you need to do
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ChatAdapter adapter = new ChatAdapter(getApplicationContext(), mChatList);
                    mListView.setAdapter(adapter);
                }
            });
            // Dismiss the progressbar after 500 milliseconds
            updateBarHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pDialog.cancel();
                }
            }, 500);

        }
    }

    private class Chat {
        String phoneNumber;
        String chatID;

        Chat(String phone, String chat) {
            phoneNumber = phone;
            chatID = chat;
        }
    }
}
