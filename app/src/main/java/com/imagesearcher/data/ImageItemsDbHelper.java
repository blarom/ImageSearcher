package com.imagesearcher.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ImageItemsDbHelper extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "popularmovies.db";
    private static final int DATABASE_VERSION = 11;

    ImageItemsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase sqLiteDatabase) {

        final String SQL_CREATE_KEYWORDS_TABLE = "CREATE TABLE " + ImageItemsDbContract.ImageItemsDbEntry.TABLE_NAME +
                " (" +
                ImageItemsDbContract.ImageItemsDbEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ImageItemsDbContract.ImageItemsDbEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                ImageItemsDbContract.ImageItemsDbEntry.COLUMN_LINK + " TEXT NOT NULL, " +
                ImageItemsDbContract.ImageItemsDbEntry.COLUMN_SNIPPET + " TEXT NOT NULL " +
                "); ";


        sqLiteDatabase.execSQL(SQL_CREATE_KEYWORDS_TABLE);
    }

    @Override public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ImageItemsDbContract.ImageItemsDbEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

}
