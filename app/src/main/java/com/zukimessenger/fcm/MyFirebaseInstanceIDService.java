package com.zukimessenger.fcm;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.zukimessenger.Helper;

import timber.log.Timber;

/**
 * Created by manik on 31/3/18.
 */

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Timber.d("Refreshed token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String refreshedToken){
        FirebaseDatabase.getInstance().getReference().child(Helper.USERS + "/"
                + FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()
                + "/" + Helper.NOTIFICATION_TOKENS).setValue(refreshedToken);
    }
}