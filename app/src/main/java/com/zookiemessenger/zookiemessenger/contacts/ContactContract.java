package com.zookiemessenger.zookiemessenger.contacts;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by manik on 12/1/18.
 */

public class ContactContract {
    public static final String CONTENT_AUTHORITY = "com.zookiemessenger.zookiemessenger";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_CONTACTS = "contacts";

    private ContactContract() {
    }

    public final static class ContactEntry implements BaseColumns {

        public final static String TABLE_NAME = "contacts";

        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_CONTACT_PHONE_NUMBER = "phoneNumber";
        public final static String COLUMN_CONTACT_NAME = "name";
        //public final static String COLUMN_CONTACT_GENDER = "gender";
        //public final static String COLUMN_CONTACT_WEIGHT = "weight";

        //public final static int GENDER_UNKNOWN_CONSTANT = 0;
        //public final static int GENDER_MALE_CONSTANT = 1;
        //public final static int GENDER_FEMALE_CONSTANT = 2;

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_CONTACTS);

        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_CONTACTS;
        //public static final String CONTENT_ITEM_TYPE =
          //      ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_CONTACTS;
    }
}
