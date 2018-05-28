package com.imagesearcher.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ImageItemContentProvider extends ContentProvider {


    private ImageItemsDbHelper mDbHelper;
    public static final int IMAGE_ITEMS_KEY = 100;
    public static final int IMAGE_ITEMS_WITH_ID_KEY = 101;
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    public static UriMatcher buildUriMatcher() {

        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(ImageItemsDbContract.AUTHORITY, ImageItemsDbContract.PATH_IMAGE_ITEMS, IMAGE_ITEMS_KEY);
        uriMatcher.addURI(ImageItemsDbContract.AUTHORITY, ImageItemsDbContract.PATH_IMAGE_ITEMS + "/#", IMAGE_ITEMS_WITH_ID_KEY);

        return uriMatcher;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new ImageItemsDbHelper(getContext());
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri,
                        @Nullable String[] projection,
                        @Nullable String selection,
                        @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {

        //Get the database
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();

        //Get the matching Uri
        int match = sUriMatcher.match(uri);

        //Initialize the cursor that will be returned when the query is performed
        Cursor retCursor;

        //Query the database if the Uri matches the relevant table
        switch (match) {
            case IMAGE_ITEMS_KEY:
                retCursor =  db.query(ImageItemsDbContract.ImageItemsDbEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        //Set a notification URI on the Cursor
        if (getContext()!=null) retCursor.setNotificationUri(getContext().getContentResolver(), uri);

        return retCursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {

        //Get the database
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        //Get the matching Uri
        int match = sUriMatcher.match(uri);

        //Initialize the Uri that will be returned when the element is inserted into the database
        Uri returnUri;

        //Insert the element into the database if the Uri matches the relevant table
        long id;
        switch(match) {
            case IMAGE_ITEMS_KEY:
                id = db.insert(ImageItemsDbContract.ImageItemsDbEntry.TABLE_NAME, null, contentValues);
                if (id > 0) returnUri = ContentUris.withAppendedId(ImageItemsDbContract.ImageItemsDbEntry.CONTENT_URI, id);
                else throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);

        return returnUri;
    }

    @Override public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        switch (sUriMatcher.match(uri)) {
            case IMAGE_ITEMS_KEY:
                return insertValuesIntoTable(uri, db, values, ImageItemsDbContract.ImageItemsDbEntry.TABLE_NAME);
            default:
                return super.bulkInsert(uri, values);
        }
    }

    private int insertValuesIntoTable(Uri uri, SQLiteDatabase db, ContentValues[] values, String table) {
        db.beginTransaction();
        int rowsInserted = 0;
        try {
            for (ContentValues value : values) {
                long _id = db.insert(table, null, value);
                if (_id != -1)  rowsInserted++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (rowsInserted > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsInserted;
    }

    @Override public int delete(@NonNull Uri uri,
                                @Nullable String selection,
                                @Nullable String[] selectionArgs) {

        //Get the database
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        //Get the matching Uri
        int match = sUriMatcher.match(uri);

        //Number of deleted elements in the database
        int tasksDeleted;

        //Delete the element in the database with the relevant id if the Ur matches the relevant table
        String id;
        switch (match) {
            case IMAGE_ITEMS_WITH_ID_KEY:
                id = uri.getPathSegments().get(1);
                tasksDeleted = db.delete(ImageItemsDbContract.ImageItemsDbEntry.TABLE_NAME, "_id=?", new String[]{id});
                break;
            case IMAGE_ITEMS_KEY:
                tasksDeleted = db.delete(ImageItemsDbContract.ImageItemsDbEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        //Notify the resolver of a change
        if (tasksDeleted != 0 && getContext()!=null)  getContext().getContentResolver().notifyChange(uri, null);

        return tasksDeleted;
    }

    @Override public int update(@NonNull Uri uri,
                                @Nullable ContentValues contentValues,
                                @Nullable String selection,
                                @Nullable String[] selectionArgs) {

        //Get the database
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        //Get the matching Uri
        int match = sUriMatcher.match(uri);

        //Initialize the Uri that will be returned when the element is inserted into the database
        long hasBeenUpdated;

        //Insert the element into the database if the Uri matches the relevant table
        long id;
        switch(match) {
            case IMAGE_ITEMS_KEY:
                id = db.update(ImageItemsDbContract.ImageItemsDbEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                if (id > 0) hasBeenUpdated = id;
                else throw new android.database.SQLException("Failed to update row with uri " + uri);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (getContext()!=null) getContext().getContentResolver().notifyChange(uri, null);
        return (int) hasBeenUpdated;
    }
}
