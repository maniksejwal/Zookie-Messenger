package com.zukimessenger.chat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zukimessenger.R;

import java.util.ArrayList;

import timber.log.Timber;

public class TagsActivity extends AppCompatActivity {

    ArrayList<String> mTagList = new ArrayList<>();
    ListView mListView;
    ImageButton mAddTagButton;
    ImageButton mTagsDoneButton;

    private int mRequestCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getMyIntent();
        setLayout();
    }

    private void setLayout() {
        setContentView(R.layout.activity_tags);

        mListView = findViewById(R.id.tag_list);
        mAddTagButton = findViewById(R.id.add_tag_button);
        mTagsDoneButton = findViewById(R.id.tags_done);
        final EditText tagEditText = findViewById(R.id.add_tag_edit_text);


        mAddTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tagName = tagEditText.getText().toString();
                mTagList.add(tagName);
                TagAdapter adapter = new TagAdapter(getApplicationContext(), mTagList);
                mListView.setAdapter(adapter);
                mTagsDoneButton.setVisibility(View.VISIBLE);
                mAddTagButton.setVisibility(View.GONE);
                tagEditText.setText("");
            }
        });

        mTagsDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Timber.v("mTagsDoneButton pressed");
                Intent resultIntent = new Intent();
                resultIntent.putExtra(getString(R.string.tag_list), mTagList.toArray(new String[mTagList.size()]));
                resultIntent.putExtra("requestCode", mRequestCode);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });

        tagEditText.addTextChangedListener(new TextWatcher() {
            boolean empty = false;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                empty = charSequence.length() == 0;
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (empty) {
                    mTagsDoneButton.setVisibility(View.GONE);
                    mAddTagButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                empty = editable.length() == 0;
                if (empty) {
                    mTagsDoneButton.setVisibility(View.VISIBLE);
                    mAddTagButton.setVisibility(View.GONE);
                }
            }
        });
    }

    private void getMyIntent() {
        Intent intent = getIntent();
        mRequestCode = intent.getIntExtra("requestCode", 0);
        if (mRequestCode == 0) {
            Toast.makeText(this, "Please try again", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Toast.makeText(this, "File not shared", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class TagAdapter extends ArrayAdapter<String> {
        public TagAdapter(@NonNull Context context, ArrayList<String> tags) {
            super(context, 0, tags);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.tag_layout
                        , parent, false);
            }

            ((TextView) convertView.findViewById(R.id.tag_text_view)).setText(getItem(position));
            return convertView;
        }
    }
}
