package com.zukimessenger.contacts;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.zukimessenger.contacts.ContactContract.ContactEntry;

/**
 * Created by manik on 12/1/18.
 */

public class ContactDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "contacts.db";
    private static final int DATABASE_VERSION = 3;

    public ContactDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_PETS_TABLE = "CREATE TABLE "
                + ContactEntry.TABLE_NAME + "("
                + ContactEntry._ID + " INTEGER, "
                + ContactEntry.COLUMN_CONTACT_PHONE_NUMBER + " TEXT PRIMARY KEY, "
                + ContactEntry.COLUMN_CONTACT_NAME + " TEXT);";

        db.execSQL(SQL_CREATE_PETS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }
}
