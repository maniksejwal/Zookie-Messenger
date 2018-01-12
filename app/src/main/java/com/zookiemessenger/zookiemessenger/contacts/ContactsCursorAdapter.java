package com.zookiemessenger.zookiemessenger.contacts;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import com.zookiemessenger.zookiemessenger.contacts.ContactContract.ContactEntry;

import com.zookiemessenger.zookiemessenger.R;

/**
 * Created by manik on 12/1/18.
 */

public class ContactsCursorAdapter extends CursorAdapter {
    public ContactsCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.contacts_list_item, parent,
                false);
    }

    @Override
    public void bindView(View listItemView, Context context, Cursor cursor) {
        TextView nameTextView = listItemView.findViewById(R.id.name);
        //.setText(getItem(position).displayName);
        TextView phoneNumberTextView = listItemView.findViewById(R.id.phone_number);
        //.setText(getItem(position).phoneNumber);

        int nameColumnIndex = cursor.getColumnIndex(ContactEntry.COLUMN_CONTACT_NAME);
        int phoneNumberColumnIndex = cursor.getColumnIndex(ContactEntry.COLUMN_CONTACT_PHONE_NUMBER);

        String contactName = cursor.getString(nameColumnIndex);
        String contactPhoneNumber = cursor.getString(phoneNumberColumnIndex);

        nameTextView.setText(contactName);
        phoneNumberTextView.setText(contactPhoneNumber);

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

    }
}
