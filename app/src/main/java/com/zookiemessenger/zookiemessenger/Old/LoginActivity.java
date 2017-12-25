package com.zookiemessenger.zookiemessenger.Old;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.zookiemessenger.zookiemessenger.R;
import com.zookiemessenger.zookiemessenger.Old.interfaces.IAppManager;
import com.zookiemessenger.zookiemessenger.Old.services.IMService;

import java.io.UnsupportedEncodingException;


public class LoginActivity extends Activity {

    protected static final int NOT_CONNECTED_TO_SERVICE = 0;
    protected static final int FILL_BOTH_USERNAME_AND_PASSWORD = 1;
    public static final String AUTHENTICATION_FAILED = "0";
    public static final String FRIEND_LIST = "FRIEND_LIST";
    protected static final int MAKE_SURE_USERNAME_AND_PASSWORD_CORRECT = 2;
    protected static final int NOT_CONNECTED_TO_NETWORK = 3;
    private EditText usernameText;
    private EditText countryCode;
    private Button cancelButton;
    private IAppManager imService;
    public static final int SIGN_UP_ID = Menu.FIRST;
    public static final int EXIT_APP_ID = Menu.FIRST + 1;


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            imService = ((IMService.IMBinder) service).getService();

            if (imService.isUserAuthenticated()) {
                Intent i = new Intent(LoginActivity.this, FriendListActivity.class);
                startActivity(i);
                LoginActivity.this.finish();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            imService = null;
            Toast.makeText(LoginActivity.this, R.string.local_service_stopped,
                    Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Start and bind the  imService 
         **/
        startService(new Intent(LoginActivity.this, IMService.class));

        setContentView(R.layout.login_screen);
        setTitle("Login");

        Button loginButton = findViewById(R.id.login);
        //cancelButton = (Button) findViewById(R.id.cancel_login);
        usernameText = findViewById(R.id.userName);
        countryCode = findViewById(R.id.country_code);

        loginButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (imService == null) {
                    Toast.makeText(getApplicationContext(), R.string.not_connected_to_service, Toast.LENGTH_LONG).show();
                    //showDialog(NOT_CONNECTED_TO_SERVICE);
                    return;
                }
                if (!imService.isNetworkConnected()) {
                    Toast.makeText(getApplicationContext(), "No Network available", Toast.LENGTH_LONG).show();
                } else if (usernameText.length() > 0 && countryCode.length() > 0) {
                    Thread loginThread = new Thread() {
                        private Handler handler = new Handler();

                        @Override
                        public void run() {
                            String result = null;
                            try {
                                result = imService.authenticateUser(usernameText.getText().toString(), countryCode.getText().toString());
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            if (result == null || result.equals(AUTHENTICATION_FAILED)) {
                                /*
								 * Authenticatin failed, inform the user
								 */
                                handler.post(new Runnable() {
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), R.string.make_sure_username_and_password_correct, Toast.LENGTH_LONG).show();

                                        //showDialog(MAKE_SURE_USERNAME_AND_PASSWORD_CORRECT);
                                    }
                                });
                            } else {
								/*
								 * if result not equal to authentication failed,
								 * result is equal to friend list of the user
								 */
                                handler.post(new Runnable() {
                                    public void run() {
                                        Intent i = new Intent(LoginActivity.this, FriendListActivity.class);
                                        //i.putExtra(FRIEND_LIST, result);
                                        startActivity(i);
                                        LoginActivity.this.finish();
                                    }
                                });
                            }
                        }
                    };
                    loginThread.start();

                } else {
					/*
					 * Username or Password is not filled, alert the user
					 */
                    Toast.makeText(getApplicationContext(), R.string.fill_both_username_and_password, Toast.LENGTH_LONG).show();
                    //showDialog(FILL_BOTH_USERNAME_AND_PASSWORD);
                }
            }
        });

        /*cancelButton.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                imService.exit();
                finish();

            }

        });*/
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        int message = -1;
        switch (id) {
            case NOT_CONNECTED_TO_SERVICE:
                message = R.string.not_connected_to_service;
                break;
            case FILL_BOTH_USERNAME_AND_PASSWORD:
                message = R.string.fill_both_username_and_password;
                break;
            case MAKE_SURE_USERNAME_AND_PASSWORD_CORRECT:
                message = R.string.make_sure_username_and_password_correct;
                break;
            case NOT_CONNECTED_TO_NETWORK:
                message = R.string.not_connected_to_network;
                break;
            default:
                break;
        }

        if (message == -1) {
            return null;
        } else {
            return new AlertDialog.Builder(LoginActivity.this)
                    .setMessage(message)
                    .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
    				/* User clicked OK so do some stuff */
                        }
                    })
                    .create();
        }
    }

    @Override
    protected void onPause() {
        unbindService(mConnection);
        super.onPause();
    }

    @Override
    protected void onResume() {
        bindService(new Intent(LoginActivity.this, IMService.class), mConnection, Context.BIND_AUTO_CREATE);

        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);

        menu.add(0, SIGN_UP_ID, 0, R.string.sign_up);
        menu.add(0, EXIT_APP_ID, 0, R.string.exit_application);


        return result;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        switch (item.getItemId()) {
            case SIGN_UP_ID:
                Intent i = new Intent(LoginActivity.this, SignUp.class);
                startActivity(i);
                return true;
            case EXIT_APP_ID:
                cancelButton.performClick();
                return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }
}