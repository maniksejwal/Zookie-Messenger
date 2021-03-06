package com.zukimessenger;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.zukimessenger.chat.ChatListItem;
import com.zukimessenger.chat.ChatScreen;
import com.zukimessenger.contacts.ContactContract.ContactEntry;
import com.zukimessenger.contacts.ContactsActivity;

import java.util.ArrayList;

import timber.log.Timber;

public class Chats extends AppCompatActivity {
    private static final int CONTACTS_ID = Menu.FIRST;
    private static final int NEW_GROUP_ID = CONTACTS_ID + 1;
    private static final int LOG_OUT_ID = NEW_GROUP_ID + 1;

    private ListView mListView;
    //private ProgressDialog pDialog;
    private Handler updateBarHandler;

    FirebaseUser mFirebaseUser;
    FirebaseDatabase mFirebaseDatabase;

    DatabaseReference mUserDatabaseReference;
    private DatabaseReference mChatsDatabaseReference;

    ArrayList<ChatListItem> mChatList = new ArrayList<>();
    private ArrayList<ChatListItem> recentChats = new ArrayList<>();

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
                //intent.putExtra(getString(R.string.contact_key), mChatList.get(position).getPhoneNumber());
                //intent.putExtra(getString(R.string.type), mChatList.get(position).getType());
                //intent.putExtra("contactName", mChatList.get(position).getName());
                intent.putExtra(Helper.CHAT, mChatList.get(position));
                intent.putExtra("recentChats", recentChats);
                startActivity(intent);
                for (int i = 0; i < recentChats.size(); i++) {
                    if (recentChats.get(i) == mChatList.get(position)) {
                        recentChats.remove(i);
                        break;
                    }
                    recentChats.add(mChatList.get(position));
                }
            }
        });
        setTitle(mFirebaseUser.getDisplayName());
    }

    private void getChats() {
        Timber.v("getChats()");

        mUserDatabaseReference.child(getString(R.string.chats)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.v("dataSnapshot = " + dataSnapshot);
                for (final DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    Timber.v("childSnapshot = " + childSnapshot);
                    if (childSnapshot.getValue() == null) continue;

                    Timber.v("phoneNumber " + childSnapshot.child("phoneNumber").getValue() +
                            "\nchatID" + childSnapshot.child(getString(R.string.chats)).getValue());

                    mChatsDatabaseReference.child(childSnapshot.getKey() + "/" + getString(R.string.meta)
                            + "/" + getString(R.string.type)).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot type) {
                            addChat(childSnapshot, type);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void addChat(DataSnapshot childSnapshot, DataSnapshot type) {
        final ChatListItem chat = new ChatListItem(childSnapshot.getKey(),
                childSnapshot.getValue() + "", "" + type.getValue());
        if (!chat.getType().equals(getString(R.string.group))) {
            Timber.v("phoneNumber " + chat.getPhoneNumber());
            Timber.v("type " + chat.getType());
            String st = ContactEntry.COLUMN_CONTACT_PHONE_NUMBER + "='" + chat.getPhoneNumber() + "'";
            Timber.v("selection " + st);
            Cursor c = getContentResolver().query(ContactEntry.CONTENT_URI,
                    new String[]{
                            //ContactEntry._ID,
                            //ContactEntry.COLUMN_CONTACT_PHONE_NUMBER,
                            ContactEntry.COLUMN_CONTACT_NAME
                    },
                    st,
                    null,
                    null
            );

            int index = c.getColumnIndex(ContactEntry.COLUMN_CONTACT_NAME);
            String s = "";

            if (c.moveToFirst()) {
                s = c.getString(index);
                do {
                    Timber.v("contact name " + c.getString(index));
                } while (c.moveToNext());
            }

            updateAdapter(s, chat);
            c.close();
        } else {
            mChatsDatabaseReference.child(childSnapshot.getKey() + "/" + getString(R.string.meta)
                    + "/" + getString(R.string.name)).addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            updateAdapter(dataSnapshot.getValue() + "", chat);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
        }
    }

    private void updateAdapter(String name, ChatListItem chat) {
        chat.setName(name);
        mChatList.add(chat);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ChatAdapter adapter = new ChatAdapter(getApplicationContext(), mChatList);
                mListView.setAdapter(adapter);
            }
        });
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

    private class ChatAdapter extends ArrayAdapter<ChatListItem> {

        ChatAdapter(@NonNull Context context, ArrayList<ChatListItem> chats) {
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

            ((TextView) listItemView.findViewById(R.id.name)).setText(getItem(position).getName());
            ((TextView) listItemView.findViewById(R.id.phone_number))
                    .setText(getItem(position).getPhoneNumber());
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
}

//TODO: delete chats