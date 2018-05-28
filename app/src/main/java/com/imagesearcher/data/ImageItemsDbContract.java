package com.imagesearcher.data;

import android.net.Uri;
import android.provider.BaseColumns;

public class ImageItemsDbContract {

    public static final String AUTHORITY = "com.imagesearcher";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final String PATH_IMAGE_ITEMS = "imageitems";

    public static final class ImageItemsDbEntry implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_IMAGE_ITEMS).build();

        public static final String TABLE_NAME = "imageItemsTable";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_LINK = "link";
        public static final String COLUMN_SNIPPET = "snippet";
        public static final String COLUMN_NEW = "newColumn";


    }
}
