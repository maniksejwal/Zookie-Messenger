package com.zookiemessenger.zookiemessenger.poll;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.zookiemessenger.zookiemessenger.R;
import com.zookiemessenger.zookiemessenger.chat.FriendlyMessage;

import timber.log.Timber;

import static android.view.View.GONE;

public class PollActivity extends AppCompatActivity {
    private boolean newPoll = true;
    private boolean isAdmin = true;

    private String mChatKey, mPollKey = "", mUserPhoneNumber;

    private RadioGroup radioGroup;
    private EditText titleEditText, detailsEditText;
    private Button clearButton;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mChatDatabaseReference;
    private DatabaseReference mPollDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll);

        getMyIntent();

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mChatDatabaseReference = mFirebaseDatabase.getReference().child(getString(R.string.chats) + "/" + mChatKey);
        mPollDatabaseReference = mChatDatabaseReference.child(getString(R.string.polls) + "/" + mPollKey);
        Timber.v(mChatKey);

        mChatDatabaseReference.child(getString(R.string.meta) + "/" + getString(R.string.admin)).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child : dataSnapshot.getChildren())
                            if (child.getValue().toString().equals(mUserPhoneNumber)) {
                                isAdmin = true;
                                Timber.v("isAdmin set");
                                return;
                            }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                }
        );

        setLayout();
    }

    private void getMyIntent() {
        Intent intent = getIntent();
        newPoll = intent.getBooleanExtra("newPoll", false);
        isAdmin = intent.getBooleanExtra("isAdmin", false);
        mChatKey = intent.getStringExtra("chatKey");
        mPollKey = intent.getStringExtra("pollKey");
        mUserPhoneNumber = intent.getStringExtra("userPhoneNumber");
        Timber.v("chatKey " + mChatKey);
        Timber.v("pollKey " + mPollKey);
    }

    private void setLayout() {
        Timber.v("setLayout() entered");

        radioGroup = findViewById(R.id.poll_options_radio_group);
        titleEditText = findViewById(R.id.poll_title_edit_text);
        detailsEditText = findViewById(R.id.poll_details_edit_text);

        findViewById(R.id.new_option_layout).setVisibility(View.GONE);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        setClearButton();

        findViewById(R.id.poll_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pollDone();
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i != -1) clearButton.setEnabled(true);
                else clearButton.setEnabled(false);
            }
        });

        if (isAdmin || newPoll) {
            setLayoutForAdmin();
            return;
        }

        //Not admin of poll
        setLayoutForOthers();
    }

    private void setClearButton() {
        clearButton = findViewById(R.id.clear_vote_button);
        clearButton.setEnabled(false);

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selected = radioGroup.getCheckedRadioButtonId();
                Timber.v("selected " + selected);
                String s = getString(R.string.options) + "/" +
                        ((RadioButton) radioGroup.getChildAt(selected - 1)).getText().toString();
                Timber.v(s);
                try {
                    mPollDatabaseReference.child(s).addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    Timber.v("" + dataSnapshot);
                                    if (dataSnapshot.getChildrenCount() == 1) {
                                        dataSnapshot.getRef().setValue("");
                                    }

                                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                                        if (child.getValue().toString().equals(mUserPhoneNumber)) {
                                            child.getRef().setValue(null);
                                            return;
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                }
                            });
                } catch (Exception e) {
                    Timber.i("couldn't delete the vote because it was not on the server");
                    e.printStackTrace();
                }

                clearButton.setEnabled(false);
                radioGroup.clearCheck();
            }
        });
    }


    private void setLayoutForAdmin() {
        Timber.v("isAdmin " + isAdmin + "newPoll" + newPoll);
        findViewById(R.id.poll_title_text_view).setVisibility(GONE);
        findViewById(R.id.poll_details_text_view).setVisibility(GONE);

        findViewById(R.id.new_poll_option_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newPollOption();
            }
        });

        findViewById(R.id.add_poll_option).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addPollOption();
            }
        });

        findViewById(R.id.cancel_poll_option).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.action_button_bar).setVisibility(View.VISIBLE);
                findViewById(R.id.new_option_layout).setVisibility(View.GONE);
            }
        });

        if (!newPoll) {
            setOptions();
            result();
        }
    }

    private void newPollOption() {
        Timber.v("new_poll_option clicked");
        findViewById(R.id.new_option_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.action_button_bar).setVisibility(GONE);
        ((EditText) findViewById(R.id.new_option_edit_text)).setText("");
        findViewById(R.id.new_option_edit_text).requestFocus();
        //show keypad
        View v = getCurrentFocus();
        if (v != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showSoftInput(v, 0);
        }
    }

    private void addPollOption() {
        EditText editText = findViewById(R.id.new_option_edit_text);
        String option = editText.getText().toString();
        if (option.equals("")) editText.setError("Please name the option");
        else {
            float density = getResources().getDisplayMetrics().density;
            RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins((int) (8 * density), (int) (8 * density)
                    , (int) (8 * density), (int) (8 * density));

            RadioButton radioButton = new RadioButton(getApplicationContext());
            radioButton.setText(option);
            radioButton.setLayoutParams(layoutParams);
            radioGroup.addView(radioButton);

            findViewById(R.id.new_option_layout).setVisibility(View.GONE);
            findViewById(R.id.action_button_bar).setVisibility(View.VISIBLE);
        }
    }

    private void pollDone() {
        if (isAdmin) {
            if (newPoll) {
                Timber.v("setting OnClickListener on poll_done");
                mPollKey = mChatDatabaseReference.child(getString(R.string.polls)).push().getKey();
                mPollDatabaseReference = mChatDatabaseReference.child(getString(R.string.polls)
                        + "/" + mPollKey);
                mPollDatabaseReference.setValue(new Poll(radioGroup, titleEditText,
                        detailsEditText));
                mChatDatabaseReference.child(getString(R.string.messages)).push().setValue(new FriendlyMessage(
                        titleEditText.getText().toString(), mUserPhoneNumber,
                        "poll", mPollKey));
                castVote();
            } else {
                //add all the new options
                for (int i = 0; i < radioGroup.getChildCount(); i++) {
                    final String s = ((RadioButton) radioGroup.getChildAt(i)).getText().toString();
                    mPollDatabaseReference.child(getString(R.string.options) + "/" + s)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.getValue() == null)
                                        mPollDatabaseReference.child(getString(R.string.options))
                                                .push().setValue(s, "");
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                }

                            });
                }
                castVote();
            }
            finish();
        }
    }


    private void setLayoutForOthers() {
        titleEditText.setVisibility(GONE);
        detailsEditText.setVisibility(GONE);
        findViewById(R.id.new_poll_option_button).setVisibility(GONE);

        setTextViews();
        setOptions();
        result();
    }

    private void setTextViews() {
        Timber.v(mPollDatabaseReference + "");
        mPollDatabaseReference.child("title").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String s = "" + dataSnapshot.getValue();
                Timber.v("title: " + dataSnapshot);
                ((TextView) findViewById(R.id.poll_title_text_view)).setText(s);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        mPollDatabaseReference.child("details").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.v("details: " + dataSnapshot.getValue());
                String s = dataSnapshot.getValue().toString();
                TextView detailsTextView = findViewById(R.id.poll_details_edit_text);
                if (!s.equals("")) detailsTextView.setText(s);
                else detailsTextView.setVisibility(GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    private void setOptions() {
        mPollDatabaseReference.child(getString(R.string.options)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.v("options: " + dataSnapshot.getValue());
                boolean isSelected = false, found = false;
                int radioButtonCounter = 0;
                for (DataSnapshot option : dataSnapshot.getChildren()) {
                    radioButtonCounter++;
                    String s = "";
                    if (option.hasChildren()) {
                        s = option.getKey();
                        if (!isSelected)
                            for (DataSnapshot voter : option.getChildren()) {
                                Timber.v("voter " + voter);
                                if (voter.getValue().toString().equals(mUserPhoneNumber)) {
                                    isSelected = true;
                                }
                                Timber.v("isSelected " + isSelected);
                            }

                    } else s = option.getKey() + "";

                    RadioButton radioButton = new RadioButton(getApplicationContext());
                    float density = getResources().getDisplayMetrics().density;
                    RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams(
                            RadioGroup.LayoutParams.MATCH_PARENT,
                            RadioGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins((int) (8 * density), (int) (8 * density)
                            , (int) (8 * density), (int) (8 * density));
                    radioButton.setLayoutParams(layoutParams);

                    radioButton.setText(s);
                    radioGroup.addView(radioButton);
                    if (!found && isSelected) {
                        radioGroup.check(radioButtonCounter);
                        Timber.v("radioButtonCounter " + radioButtonCounter);
                        found = true;
                        clearButton.setEnabled(true);
                    }
                    Timber.v("checked id " + radioGroup.getCheckedRadioButtonId());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void castVote() {
        final RadioButton radioButton = findViewById(radioGroup.getCheckedRadioButtonId());
        if (radioButton != null) {
            //remove previous vote from server
            mPollDatabaseReference.child(getString(R.string.options)).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Timber.v("" + dataSnapshot);
                    for (DataSnapshot option : dataSnapshot.getChildren()) {

                        for (DataSnapshot voter : option.getChildren()) {
                            if (!voter.getValue().toString().equals(mUserPhoneNumber)) continue;

                            Timber.v("voter " + voter);
                            if (option.getChildrenCount() == 1) {
                                option.getRef().setValue("");
                            } else voter.getRef().setValue(null);

                            Timber.v("option.getChildrenCount() " + option.getChildrenCount());
                            break;
                        }
                    }
                    //cast vote
                    mPollDatabaseReference.child(getString(R.string.options) + "/" +
                            radioButton.getText().toString()).push().setValue(mUserPhoneNumber);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });

        }
    }

    private void result() {
        mPollDatabaseReference.child(getString(R.string.options)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot option : dataSnapshot.getChildren()) {
                    View view = LayoutInflater.from(getApplicationContext())
                            .inflate(R.layout.result_item, null, false);
                    Timber.v("option" + option.getKey());
                    ((TextView) view.findViewById(R.id.option_name)).setText(option.getKey());
                    ((TextView) view.findViewById(R.id.option_voter_count))
                            .setText(String.valueOf(option.getChildrenCount()));
                    ((LinearLayout) findViewById(R.id.score_layout)).addView(view);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    //TODO: close poll
}
