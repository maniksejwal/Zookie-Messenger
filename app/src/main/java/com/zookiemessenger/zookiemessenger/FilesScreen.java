package com.zookiemessenger.zookiemessenger;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

import timber.log.Timber;

import static java.lang.Math.abs;

public class FilesScreen extends AppCompatActivity {
    private FirebaseUser mFirebaseUser;
    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseStorage mFirebaseStorage;

    private DatabaseReference mChatsDatabaseReference;
    private StorageReference mStorageReference;

    private String mChatKey = "";

    private Spinner mTypeSpinner;
    private EditText mEditText;

    private ArrayList<FilePointer> filePointerArrayList = new ArrayList<>();

    private FileAdapter fileAdapter;
    private TagAdapter tagAdapter;

    private ValueEventListener fileSearchEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_screen);
        mEditText = findViewById(R.id.tag_edit_text);
        getMyIntent();
        setFirebase();
        setLayout();

        tagAdapter = new TagAdapter(new ArrayList<String>());
        ((RecyclerView) findViewById(R.id.search_tag_list)).setAdapter(tagAdapter);

        fileAdapter = new FileAdapter(filePointerArrayList);
        ((GridView) findViewById(R.id.files_grid)).setAdapter(fileAdapter);


        listFiles();
    }

    private void getMyIntent() {
        Intent intent = getIntent();
        mChatKey = intent.getStringExtra(Helper.CHATKEY);
    }

    private void setFirebase() {
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();

        mChatsDatabaseReference = mFirebaseDatabase.getReference().child(getString(R.string.chats));
    }

    private void setLayout(){
        ((EditText) findViewById(R.id.tag_edit_text)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                listFiles();
            }
        });
        setTypeSpinner();
    }

    private void setTypeSpinner() {
        ArrayList<String> types = new ArrayList<>();
        types.add(getString(R.string.chose_type));
        types.add(Helper.IMAGE);
        types.add(Helper.VIDEO);
        types.add(Helper.GRAPHIC);
        types.add(Helper.FILE);

        mTypeSpinner = findViewById(R.id.type_spinner);

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, types);
        /*mTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                findViewById(R.id.response_layout).setVisibility(View.GONE);
                findViewById(R.id.button_bar).setVisibility(View.GONE);
                gridView.setVisibility(View.GONE);
                makeSpinner2(intent);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });*/

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTypeSpinner.setAdapter(dataAdapter);
        mTypeSpinner.setSelection(0);
    }

    private void listFiles() {
        detachDatabaseReadListener();
        attachDatabaseReadListener();
    }

    private void add(DataSnapshot tag) {
        try {
            for(DataSnapshot message : tag.getChildren()) {
                mChatsDatabaseReference.child(mChatKey + "/" + Helper.MESSAGES + "/" + message
                        + "/"+ Helper.URL).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot url) {
                        fileAdapter.add(new FilePointer(url.getValue().toString()));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            }
            //TODO: List files in the tag
        } catch (Exception e){
            Timber.e("url" + tag.child(Helper.URL).getValue());
        }
    }

    boolean isSpelling(String i, String j) {
        if (j.length() <= 3 || (float) abs(i.length() - j.length()) / j.length() > 0.3)
            return false;
        int count = 0;
        for (int a = 0; a < i.length() && a < j.length(); a++) {
            if (Character.toLowerCase(i.charAt(a)) == Character.toLowerCase(j.charAt(a))) {
                count++;
                Timber.v("Match for " + j.charAt(a) + "  count = " + count);
                continue;
            }
            Timber.d("Not a match");
            for (int b = -2; b < 3; b++) {
                Timber.v("" + b);
                if (a + b > 0 && a + b < i.length() && a + b < j.length()) {
                    if (i.charAt(a) == j.charAt(a + b)) {
                        count++;
                        Timber.v("match for " + j.charAt(a + b) + "  count = " + count);
                        break;
                    }
                }
            }
        }
        Timber.v("Count = " + count);
        return ((float) count / j.length()) > 0.6;
    }

    private void attachDatabaseReadListener() {
        if (fileSearchEventListener != null) return;
        fileSearchEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot tags) {
                String name = mEditText.getText().toString();
                if (name.equals("")) {  //All files (timeline)
                    //add(tags);        TODO: add all
                    return;
                }
                for (DataSnapshot tag : tags.getChildren()) {
                    if (tag.getValue().toString().equals(name)
                            || tag.getValue().toString().toLowerCase().contains(name)
                            || isSpelling(tag.getValue().toString(), name)) {
                        tagAdapter.addToAdapter(tag.getKey());
                        add(tag);
                        return;
                    }
                }
                //if (mFirebaseStorage.getReferenceFromUrl(
                  //      tags.child(Helper.URL).getValue().toString()).getName().equals(name))
                    //add(tags);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        mChatsDatabaseReference.child(mChatKey + "/" + Helper.TAGS).addListenerForSingleValueEvent(
                fileSearchEventListener);
    }

    private void detachDatabaseReadListener() {
        if (fileSearchEventListener != null) {
            mChatsDatabaseReference.child(mChatKey + "/" + Helper.MESSAGES).removeEventListener(
                    fileSearchEventListener);
            fileSearchEventListener = null;
        }
    }

    private class FilePointer {
        String url, name;

        FilePointer(String url) {
            this.url = url;
            this.name = mFirebaseStorage.getReferenceFromUrl(url).getName();
        }
    }

    private class FileAdapter extends ArrayAdapter<FilePointer> {

        public FileAdapter(ArrayList<FilePointer> fileList) {
            super(getApplicationContext(), 0, fileList);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.file_preview
                        , parent, false);
            }

            TextView textView = convertView.findViewById(R.id.file_preview_text_view);
            textView.setText(getItem(position).name);
            ImageView img = convertView.findViewById(R.id.file_preview_image_view);
            Glide.with(getApplicationContext())
                    .load(getItem(position).url)
                    .thumbnail(1 / 100)
                    .centerCrop()
                    .into(img);

            return convertView;
        }
    }

    private class TagAdapter extends RecyclerView.Adapter<TagAdapter.MyViewHolder> {
        ArrayList<String> tagList;

        public class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView textView;

            public MyViewHolder(View view) {
                super(view);
                textView = view.findViewById(R.id.bubble_text_view);
            }
        }

        public TagAdapter(ArrayList<String> tagList) {
            this.tagList = tagList;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.bubble_text_view, parent, false);

            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            String tag = tagList.get(position);
            holder.textView.setText(tag);
        }

        @Override
        public int getItemCount() {
            return tagList.size();
        }

        public void addToAdapter(String tag){
            tagList.add(tag);
        }
    }
}
//TODO: update adapter with files
//TODO: recent files
//TODO: search by name