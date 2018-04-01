package com.zookiemessenger.zookiemessenger.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.zookiemessenger.zookiemessenger.Helper;
import com.zookiemessenger.zookiemessenger.MainActivity;
import com.zookiemessenger.zookiemessenger.chat.FriendlyMessage;

import java.util.Map;

import timber.log.Timber;

/**
 * Created by manik on 31/3/18.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    //private static final String JSON_KEY_AUTHOR = SquawkContract.COLUMN_AUTHOR;
    //private static final String JSON_KEY_AUTHOR_KEY = SquawkContract.COLUMN_AUTHOR_KEY;
    //private static final String JSON_KEY_MESSAGE = SquawkContract.COLUMN_MESSAGE;
    //private static final String JSON_KEY_DATE = SquawkContract.COLUMN_DATE;
    private static final int NOTIFICATION_MAX_CHARACTERS = 30;


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Timber.d("From: " + remoteMessage.getFrom());
        Map<String, String> data = remoteMessage.getData();
        String author = data.get("author");

        if (data.size() > 0) {
            Timber.d("Message data payload: " + data);

            // Send a notification that you got a new message
            sendNotification(data);
            insertSquawk(data);
        }
    }

    private void insertSquawk(final Map<String, String> data) {

        // Database operations should not be done on the main thread
        AsyncTask<Void, Void, Void> insertSquawkTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                ContentValues newMessage = new ContentValues();
                //newMessage.put(SquawkContract.COLUMN_AUTHOR, data.get(JSON_KEY_AUTHOR));
                //newMessage.put(SquawkContract.COLUMN_MESSAGE, data.get(JSON_KEY_MESSAGE).trim());
                //newMessage.put(SquawkContract.COLUMN_DATE, data.get(JSON_KEY_DATE));
                //newMessage.put(SquawkContract.COLUMN_AUTHOR_KEY, data.get(JSON_KEY_AUTHOR_KEY));
                //getContentResolver().insert(SquawkProvider.SquawkMessages.CONTENT_URI, newMessage);
                return null;
            }
        };

        insertSquawkTask.execute();
    }

    private void sendNotification(Map<String, String> data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Create the pending intent to launch the activity
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        FriendlyMessage friendlyMessage = new FriendlyMessage(
                data.get(Helper.TEXT),
                data.get(Helper.SENDER),
                data.get(Helper.TYPE),
                data.get(Helper.FILE_TYPE),
                data.get(Helper.URL),
                null);                  //TODO: Tags is null do something about it
        //String author = data.get(JSON_KEY_AUTHOR);
        //String message = data.get(JSON_KEY_MESSAGE);

        // If the message is longer than the max number of characters we want in our
        // notification, truncate it and add the unicode character for ellipsis
        //if (message.length() > NOTIFICATION_MAX_CHARACTERS) {
        //    message = message.substring(0, NOTIFICATION_MAX_CHARACTERS) + "\u2026";
        //}

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
        //        .setSmallIcon(R.drawable.ic_duck)
        //        .setContentTitle(String.format(getString(R.string.notification_message), author))
                .setContentText(friendlyMessage.getText())
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}