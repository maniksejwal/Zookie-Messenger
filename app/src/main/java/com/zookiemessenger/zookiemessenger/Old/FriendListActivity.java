package com.zookiemessenger.zookiemessenger.Old;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zookiemessenger.zookiemessenger.MainActivity;
import com.zookiemessenger.zookiemessenger.R;
import com.zookiemessenger.zookiemessenger.Old.interfaces.IAppManager;
import com.zookiemessenger.zookiemessenger.Old.services.IMService;
import com.zookiemessenger.zookiemessenger.Old.tools.FriendController;
import com.zookiemessenger.zookiemessenger.Old.types.FriendInfo;
import com.zookiemessenger.zookiemessenger.Old.types.STATUS;

import timber.log.Timber;

public class FriendListActivity extends AppCompatActivity {
    private static final int ADD_NEW_FRIEND_ID = Menu.FIRST;
    private static final int EXIT_APP_ID = Menu.FIRST + 1;
    private static final int LOG_OUT_ID = Menu.FIRST + 2;
    private IAppManager imService = null;
    private FriendListAdapter friendAdapter;

    public String ownUsername = "";

    private class FriendListAdapter extends BaseAdapter {
        class ViewHolder {
            TextView text;
            ImageView icon;
        }

        private LayoutInflater mInflater;
        private Bitmap mOnlineIcon;
        private Bitmap mOfflineIcon;

        private FriendInfo[] friends = null;

        public FriendListAdapter(Context context) {
            super();

            mInflater = LayoutInflater.from(context);
            mOnlineIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.greenstar);
            mOfflineIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.redstar);
        }

        public void setFriendList(FriendInfo[] friends) {
            this.friends = friends;
        }

        public int getCount() {
            return friends.length;
        }

        public FriendInfo getItem(int position) {
            return friends[position];
        }

        public long getItemId(int position) {
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid unneccessary calls
            // to findViewById() on each row.
            ViewHolder holder;

            // When convertView is not null, we can reuse it directly, there is no need
            // to reinflate it. We only inflate a new View when the convertView supplied
            // by ListView is null.
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.friend_list_screen, null);

                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.text);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);

                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (ViewHolder) convertView.getTag();
            }

            // Bind the data efficiently with the holder.
            holder.text.setText(friends[position].userName);
            holder.icon.setImageBitmap(friends[position].status == STATUS.ONLINE ? mOnlineIcon : mOfflineIcon);

            return convertView;
        }

    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.i("Broadcast receiver ", "received a message");
            Bundle extra = intent.getExtras();
            if (extra != null) {
                String action = intent.getAction();
                if (action.equals(IMService.FRIEND_LIST_UPDATED)) {
                    // taking friend List from broadcast
                    //String rawFriendList = extra.getString(FriendInfo.FRIEND_LIST);
                    //FriendList.this.parseFriendInfo(rawFriendList);
                    FriendListActivity.this.updateData(FriendController.getFriendsInfo(),
                            FriendController.getUnapprovedFriendsInfo());

                }
            }
        }
    }

    public MessageReceiver messageReceiver = new MessageReceiver();

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            imService = ((IMService.IMBinder) service).getService();

            FriendInfo[] friends = FriendController.getFriendsInfo(); //imService.getLastRawFriendList();
            if (friends != null) {
                FriendListActivity.this.updateData(friends, null); // parseFriendInfo(friendList);
            }
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                if (user.getDisplayName() == null || user.getDisplayName().equals(""))
                    Timber.e("Username is null in FriendsListActivity.onServiceConnected()");
                Timber.d(user.getDisplayName());
                setTitle(user.getDisplayName() + "'s friends' list");
                ownUsername = user.getDisplayName();
            } else Timber.e("User is null in FriendsListActivity.onServiceConnected()");
        }

        public void onServiceDisconnected(ComponentName className) {
            imService = null;
            Toast.makeText(FriendListActivity.this, R.string.local_service_stopped,
                    Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(getClass().getName(), "Entered onCreate()");
        setContentView(R.layout.list_screen);

        friendAdapter = new FriendListAdapter(this);
    }

    public void updateData(FriendInfo[] friends, FriendInfo[] unApprovedFriends) {
        if (friends != null) {
            friendAdapter.setFriendList(friends);
            ListView listView = (ListView) findViewById(R.id.android_list);
            listView.setAdapter(friendAdapter);
        }

        if (unApprovedFriends != null) {
            NotificationManager NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (unApprovedFriends.length > 0) {
                String tmp = "";
                for (FriendInfo unApprovedFriend : unApprovedFriends) {
                    tmp = tmp.concat(unApprovedFriend.userName).concat(",");
                }
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.stat_sample)
                        .setContentTitle(getText(R.string.new_friend_request_exist));
                /*Notification notification = new Notification(R.drawable.stat_sample,
                        getText(R.string.new_friend_request_exist),
						System.currentTimeMillis());*/

                Intent i = new Intent(this, UnApprovedFriendList.class);
                i.putExtra(FriendInfo.FRIEND_LIST, tmp);

                PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                        i, 0);

                mBuilder.setContentText("You have new friend request(s)");
                /*notification.setLatestEventInfo(this, getText(R.string.new_friend_request_exist),
                                                "You have new friend request(s)",
												contentIntent);*/

                mBuilder.setContentIntent(contentIntent);


                NM.notify(R.string.new_friend_request_exist, mBuilder.build());
            } else {
                // if any request exists, then cancel it
                NM.cancel(R.string.new_friend_request_exist);
            }
        }
    }

/*
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Intent i = new Intent(this, Messaging.class);
        FriendInfo friend = friendAdapter.getItem(position);
        i.putExtra(FriendInfo.USERNAME, friend.userName);
        i.putExtra(FriendInfo.PORT, friend.port);
        i.putExtra(FriendInfo.IP, friend.ip);
        startActivity(i);
    }
*/

    @Override
    protected void onPause() {
        unregisterReceiver(messageReceiver);
        unbindService(mConnection);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(FriendListActivity.this, IMService.class), mConnection, Context.BIND_AUTO_CREATE);

        IntentFilter i = new IntentFilter();
        //i.addAction(IMService.TAKE_MESSAGE);
        i.addAction(IMService.FRIEND_LIST_UPDATED);

        registerReceiver(messageReceiver, i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Timber.v("entered onCreateOptionsMenu()");

        menu.add(0, ADD_NEW_FRIEND_ID, 0, R.string.add_new_friend);
        menu.add(0, EXIT_APP_ID, 0, R.string.exit_application);
        menu.add(0, LOG_OUT_ID, 0, R.string.sign_out);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ADD_NEW_FRIEND_ID: {
                Intent i = new Intent(FriendListActivity.this, AddFriend.class);
                startActivity(i);
                return true;
            }
            case EXIT_APP_ID: {
                imService.exit();
                finish();
                return true;
            }
            case LOG_OUT_ID: {
                FirebaseAuth.getInstance().signOut();
                finish();
                startActivity(new Intent(FriendListActivity.this, MainActivity.class));
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}