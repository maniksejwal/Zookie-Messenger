package com.zookiemessenger.zookiemessenger;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
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
import com.zookiemessenger.zookiemessenger.chat.ChatScreen;
import com.zookiemessenger.zookiemessenger.contacts.ContactsActivity;

import java.util.ArrayList;
import java.util.Map;

import timber.log.Timber;

public class Chats extends AppCompatActivity {
    private static final int REQUEST_READ_CONTACTS = 444;
    private static final int CONTACTS_ID = Menu.FIRST;
    private static final int NEW_GROUP_ID = CONTACTS_ID + 1;
    private static final int LOG_OUT_ID = NEW_GROUP_ID + 1;

    private ListView mListView;
    //private ProgressDialog pDialog;
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
        mChatsDatabaseReference = mFirebaseDatabase.getReference().child(getString(R.string.chats));
        mChatsDatabaseReference.keepSynced(true);

        //pDialog = new ProgressDialog(this);
        //pDialog.setCancelable(false);
        //pDialog.show();
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
                intent.putExtra(getString(R.string.contact_key), mChatList.get(position).phoneNumber);
                intent.putExtra(getString(R.string.type), mChatList.get(position).type);
                startActivity(intent);
            }
        });
        setTitle(mFirebaseUser.getDisplayName());
    }

    private void getChats() {
        Timber.v("getContacts");

        /*firebaseMultiQuery = new FirebaseMultiQuery(mUserDatabaseReference);
        final Task<Map<DatabaseReference, DataSnapshot>> allLoad = firebaseMultiQuery.start();
        allLoad.addOnCompleteListener(this, new Chats.AllOnCompleteListener());*/

        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;

        //Uri EmailCONTENT_URI = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        //String EmailCONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
        //String DATA = ContactsContract.CommonDataKinds.Email.DATA;
        ContentResolver contentResolver = getContentResolver();
        cursor = contentResolver.query(CONTENT_URI, null, null, null, null);
        // Iterate every contact in the phone


        mUserDatabaseReference.child(getString(R.string.chats)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.v("dataSnapshot = " + dataSnapshot);
                for (final DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    Timber.v("childSnapshot = " + childSnapshot);
                    Timber.v("phoneNumber " + childSnapshot.child("phoneNumber").getValue() +
                            "\nchatID" + childSnapshot.child(getString(R.string.chats)).getValue());

                    if (childSnapshot.getValue() == null &&
                            childSnapshot.child(getString(R.string.chats)).getValue() == null)
                        continue;

                    mChatsDatabaseReference.child(childSnapshot.getKey() + "/" + getString(R.string.meta)
                            + "/" + getString(R.string.type)).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot type) {
                            mChatList.add(new Chat(childSnapshot.getKey(),
                                    childSnapshot.getValue() + "", "" + type.getValue()));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ChatAdapter adapter = new ChatAdapter(getApplicationContext(), mChatList);
                                    mListView.setAdapter(adapter);
                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Timber.v("entered onCreateOptionsMenu()");
        menu.add(0, CONTACTS_ID, 0, "Contacts");
        menu.add(0, NEW_GROUP_ID, 0, R.string.new_group);
        menu.add(0, LOG_OUT_ID, 0, R.string.sign_out);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CONTACTS_ID:
                startActivity(new Intent(this, ContactsActivity.class));
                break;
            case NEW_GROUP_ID:
                Intent intent = new Intent(this, ContactsActivity.class);
                intent.putExtra("group", true);
                startActivity(intent);
                break;
            case LOG_OUT_ID:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
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
            /*runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ChatAdapter adapter = new ChatAdapter(getApplicationContext(), mChatList);
                    mListView.setAdapter(adapter);
                }
            });*/
            // Dismiss the progressbar after 500 milliseconds
            /*updateBarHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pDialog.cancel();
                }
            }, 500);*/

        }
    }

    private class Chat {
        String phoneNumber;
        String chatID;
        String type;

        Chat(String phone, String chat, String type) {
            phoneNumber = phone;
            chatID = chat;
            this.type = type;
        }
    }
}

//TODO: delete chats