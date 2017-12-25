package com.zookiemessenger.zookiemessenger;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.zookiemessenger.zookiemessenger.Old.tools.FriendController;
import com.zookiemessenger.zookiemessenger.Old.types.FriendInfo;

import java.util.ArrayList;

import timber.log.Timber;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * Created by manik on 25/12/17.
 */
public class ContactsActivity extends AppCompatActivity {
    private static final int REQUEST_READ_CONTACTS = 444;
    private ListView mListView;
    private ProgressDialog pDialog;
    private Handler updateBarHandler;
    private static final int LOG_OUT_ID = Menu.FIRST;
    public String ownUsername = "";
    ArrayList<Contact> contactList;
    Cursor cursor;
    int counter;
    FirebaseUser user;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        user.reload();
        setTitle(user.getDisplayName() + "'s contacts");
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Reading contacts...");
        pDialog.setCancelable(false);
        pDialog.show();
        Timber.d("ListViewID" + findViewById(R.id.list).getId());
        mListView = findViewById(R.id.list);
        updateBarHandler = new Handler();
        // Since reading contacts takes more time, let's run it on a separate thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                getContacts();
            }
        }).start();
        // Set onClickListener to the list item.
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                //TODO Do whatever you want with the list data
                Toast.makeText(getApplicationContext(), "item clicked : \n" + contactList.get(position), Toast.LENGTH_SHORT).show();
            }
        });
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
                getContacts();
            }
        }
    }

    public void getContacts() {
        if (!mayRequestContacts()) {
            return;
        }
        contactList = new ArrayList<>();
        String phoneNumber;
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
        //Uri EmailCONTENT_URI = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        //String EmailCONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
        //String DATA = ContactsContract.CommonDataKinds.Email.DATA;
        ContentResolver contentResolver = getContentResolver();
        cursor = contentResolver.query(CONTENT_URI, null, null, null, null);
        // Iterate every contact in the phone
        if (cursor == null || cursor.getCount() <= 0) return;
        counter = 0;
        String previous = "";
        PhoneNumberUtil pnu = PhoneNumberUtil.getInstance();

        while (cursor.moveToNext()) {
            Contact contact = new Contact();
            // Update the progress message
            updateBarHandler.post(new Runnable() {
                public void run() {
                    pDialog.setMessage("Reading contacts : " + counter++ + "/" + cursor.getCount());
                }
            });
            String contact_id = cursor.getString(cursor.getColumnIndex(_ID));
            String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
            int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));
            if (hasPhoneNumber <= 0) continue;

            //This is to read multiple phone numbers associated with the same contact
            Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null, Phone_CONTACT_ID + " = ?", new String[]{contact_id}, null);
            if (phoneCursor == null) continue;

            while (phoneCursor.moveToNext()) {
                phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
                Timber.v("name " + name + " phoneNumber " + phoneNumber
                        + " previous " + previous);

                //Check if number is repeated
                //PhoneNumberUtil.MatchType mt = pnu.isNumberMatch(phoneNumber, previous);
                //if (mt == PhoneNumberUtil.MatchType.NSN_MATCH || mt == PhoneNumberUtil.MatchType.EXACT_MATCH)
                //    continue;

                contact.phoneNumber = phoneNumber;
                contact.name = name;
                previous = phoneNumber;
                // Add the contact to the ArrayList
                contactList.add(contact);
            }
            phoneCursor.close();
        }
        // ListView has to be updated using a ui thread
        runOnUiThread(new Runnable() {
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
        }, 500);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            //imService = ((IMService.IMBinder) service).getService();

            FriendInfo[] friends = FriendController.getFriendsInfo(); //imService.getLastRawFriendList();
            if (friends != null) {
                // parseFriendInfo(friendList);
                //getApplicationContext().updateData(friends, null);
            }
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                if (user.getDisplayName() == null || user.getDisplayName().equals(""))
                    Timber.e("Username is null in FriendsListActivity.onServiceConnected()");
                Timber.d(user.getDisplayName());
                setTitle(user.getDisplayName() + "'s contacts");
                ownUsername = user.getDisplayName();
            } else throw new RuntimeException("User is null in ContactsActivity");
        }

        public void onServiceDisconnected(ComponentName className) {
            //imService = null;
            Toast.makeText(getApplicationContext(), R.string.local_service_stopped,
                    Toast.LENGTH_SHORT).show();
        }
    };

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

    private class Contact {
        String name, phoneNumber;
    }

    private class ContactAdapter extends ArrayAdapter<Contact> {

        ContactAdapter(@NonNull Context context, ArrayList<Contact> contacts) {
            super(context, 0, contacts);
            Timber.v("ContactAdapter created");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View listItemView = convertView;
            if (listItemView == null) {
                listItemView = LayoutInflater.from(getContext()).inflate(R.layout.contacts_list_item
                        , parent, false);
            }

            ((TextView) listItemView.findViewById(R.id.name)).setText(getItem(position).name);
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
}
