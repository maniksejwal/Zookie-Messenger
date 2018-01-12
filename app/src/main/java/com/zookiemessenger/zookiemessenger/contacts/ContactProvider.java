package com.zookiemessenger.zookiemessenger.contacts;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.zookiemessenger.zookiemessenger.contacts.ContactContract.ContactEntry;

import timber.log.Timber;

import static com.zookiemessenger.zookiemessenger.contacts.ContactContract.PATH_CONTACTS;

/**
 * Created by manik on 12/1/18.
 */

public class ContactProvider extends ContentProvider{

    private ContactDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new ContactDbHelper(getContext());
        return true;
    }

    private static final int CONTACTS = 100;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(ContactContract.CONTENT_AUTHORITY, PATH_CONTACTS, CONTACTS);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case CONTACTS:
                cursor = database.query(ContactEntry.TABLE_NAME, null, null, null, null, null, null);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CONTACTS:
                return insertContact(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private Uri insertContact(Uri uri, ContentValues values) {
        String name = values.getAsString(ContactEntry.COLUMN_CONTACT_NAME);
        if (name == null || name.equals("")) {
            //return null;
            throw new IllegalArgumentException("Contact requires a name");
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        long id = database.insert(ContactEntry.TABLE_NAME, null, values);

        if (id == -1) {
            Timber.e("Failed to insert row for " + uri);
            return null;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CONTACTS:
                return updateContact(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updateContact(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.containsKey(ContactEntry.COLUMN_CONTACT_NAME)) {
            String name = values.getAsString(ContactEntry.COLUMN_CONTACT_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Contact requires a name");
            }
        }

        if (values.containsKey(ContactEntry.COLUMN_CONTACT_PHONE_NUMBER)) {
            String phoneNumber = values.getAsString(ContactEntry.COLUMN_CONTACT_PHONE_NUMBER);
            if (phoneNumber == null) {
                throw new IllegalArgumentException("Contact requires valid gender");
            }
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        getContext().getContentResolver().notifyChange(uri, null);
        return database.update(ContactEntry.TABLE_NAME, values, selection, selectionArgs);
    }
                                                                 
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CONTACTS:
                getContext().getContentResolver().notifyChange(uri, null);
                return database.delete(ContactEntry.TABLE_NAME, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CONTACTS:
                return ContactEntry.CONTENT_LIST_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

}
