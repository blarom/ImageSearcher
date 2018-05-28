package com.imagesearcher;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.imagesearcher.data.ImageItemsDbContract;
import com.imagesearcher.data.ImageSearchResult;
import com.imagesearcher.ui.ImageSelectionRVAdapter;
import com.imagesearcher.utilities.NetworkUtilities;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements
        ImageSelectionRVAdapter.ListItemClickHandler,
        LoaderManager.LoaderCallbacks<ImageSearchResult>{


    private static final int NUMBER_GRID_COLUMNS = 2;
    private static final int IMAGE_LIST_LOADER = 123;
    private static final String IMAGE_ITEMS_RV_POSITION = "image_items_RV_position";

    @BindView(R.id.search_keywords_TV) TextView mSearchKeywordsTV;
    @BindView(R.id.search_button) Button mSearchButton;
    @BindView(R.id.image_selecton_RV) RecyclerView mImageSelectionRV;
    @BindView(R.id.swipe_container) SwipeRefreshLayout mSwipeContainer;
    @BindView(R.id.loading_indicator) ProgressBar mLoadingIndicator;
    private ImageSelectionRVAdapter mImageSelectionRVAdapter;
    private List<ImageSearchResult.Item> mImageItems;
    public static final String[] IMAGE_ITEMS_TABLE_ELEMENTS_PROJECTION = {
            ImageItemsDbContract.ImageItemsDbEntry._ID,
            ImageItemsDbContract.ImageItemsDbEntry.COLUMN_TITLE,
            ImageItemsDbContract.ImageItemsDbEntry.COLUMN_LINK,
            ImageItemsDbContract.ImageItemsDbEntry.COLUMN_SNIPPET
    };
    private Cursor mImageItemsCursor;
    private int mStoredRecyclerViewPosition;

    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        //UI Initialization
        mSwipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadImagesGrid();
            }
        });
        showLoadingIndicatorInsteadOfRecycleView();

        //Data Initialization
        mImageItems = new ArrayList<>();
        setupImagesRecyclerView();
        loadImagesGrid();
    }
    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Adapted from: https://stackoverflow.com/questions/27816217/how-to-save-recyclerviews-scroll-position-using-recyclerview-state
        GridLayoutManager layoutManager = ((GridLayoutManager) mImageSelectionRV.getLayoutManager());
        mStoredRecyclerViewPosition = layoutManager.findFirstVisibleItemPosition();
        outState.putInt(IMAGE_ITEMS_RV_POSITION, mStoredRecyclerViewPosition);
    }
    @Override public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //Adapted from: https://stackoverflow.com/questions/27816217/how-to-save-recyclerviews-scroll-position-using-recyclerview-state
        if(savedInstanceState != null) {
            mStoredRecyclerViewPosition = savedInstanceState.getInt(IMAGE_ITEMS_RV_POSITION);
            mImageSelectionRV.scrollToPosition(mStoredRecyclerViewPosition);
        }
    }


    //Loader methods
    @NonNull @Override public Loader<ImageSearchResult> onCreateLoader(int id, @Nullable Bundle args) {

        showLoadingIndicatorInsteadOfRecycleView();
        return new ImageAsyncTaskLoader(this);
    }
    @Override public void onLoadFinished(@NonNull Loader<ImageSearchResult> loader, ImageSearchResult imageSearchResult) {
        if (imageSearchResult != null) {

            //Initial checks
            showRecycleViewInsteadOfLoadingIndicator();
            if (imageSearchResult.getItems().size() == 0) NetworkUtilities.tellUserInternetIsUnavailable(getApplicationContext());

            //Reset the image items list
            mImageItems.clear();
            mImageItems.addAll(imageSearchResult.getItems());

            //Update the data in the app
            updateLocalDatabaseWithData();
            updateImagesGridWithData();

            mSwipeContainer.setRefreshing(false);
        }
    }
    @Override public void onLoaderReset(@NonNull Loader<ImageSearchResult> loader) {

    }
    private static class ImageAsyncTaskLoader extends AsyncTaskLoader<ImageSearchResult> {
        //Inspired by: https://stackoverflow.com/questions/44309241/warning-this-asynctask-class-should-be-static-or-leaks-might-occur?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa

        private WeakReference<MainActivity> appReference;

        ImageAsyncTaskLoader(@NonNull MainActivity context) {
            super(context);
            appReference = new WeakReference<>(context);
        }

        @Override protected void onStartLoading() {
            //if (args == null) return;
            Context context = appReference.get().getApplicationContext();
            if (!NetworkUtilities.isInternetAvailable(getContext())) NetworkUtilities.tellUserInternetIsUnavailable(context);
            forceLoad();
        }

        @Nullable @Override public ImageSearchResult loadInBackground() {

            String websiteContentString = "";
            ImageSearchResult imageSearchResult = null;
            if (NetworkUtilities.isInternetAvailable(getContext())) {
                String requestUrl = "https://www.googleapis.com/customsearch/v1?key=AIzaSyCzREl07Bc_bk2hm65RtiaJ0hJ48R_nMfg&cx=005800383728131713214:5jocmduwqum&searchType=image&num=10&start=1&q=example";

                try {
                    websiteContentString = NetworkUtilities.getWebsiteContent(requestUrl);
                } catch (IOException e) {
                    NetworkUtilities.tellUserInternetIsUnavailable(getContext());
                    e.printStackTrace();
                }
                Gson recipesGson = new Gson();
                imageSearchResult = recipesGson.fromJson(websiteContentString, ImageSearchResult.class);
            }
            return imageSearchResult;
        }
    }

    //Functional methods
    private void setupImagesRecyclerView() {
        mImageSelectionRV.setLayoutManager(new GridLayoutManager(this, NUMBER_GRID_COLUMNS));
        mImageSelectionRVAdapter = new ImageSelectionRVAdapter(this, this);
        mImageSelectionRVAdapter.swapCursor(mImageItemsCursor);
        mImageSelectionRV.setAdapter(mImageSelectionRVAdapter);
        mImageSelectionRV.scrollToPosition(mStoredRecyclerViewPosition);
    }
    @Override public void onListItemClick(int clickedItemIndex) {
        if (mImageItemsCursor != null) {
            if (!mImageItemsCursor.moveToPosition(clickedItemIndex)) return;
            int index = mImageItemsCursor.getColumnIndex(ImageItemsDbContract.ImageItemsDbEntry.COLUMN_LINK);
            String imageLinkFromCursor = mImageItemsCursor.getString(index);
            blastToFullScreen(imageLinkFromCursor);
        }
    }
    private void blastToFullScreen(String imageLinkFromCursor) {
    }
    private void showRecycleViewInsteadOfLoadingIndicator() {
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mImageSelectionRV.setVisibility(View.VISIBLE);
    }
    private void showLoadingIndicatorInsteadOfRecycleView() {
        mLoadingIndicator.setVisibility(View.VISIBLE);
        mImageSelectionRV.setVisibility(View.INVISIBLE);
    }
    public void loadImagesGrid() {
        LoaderManager loaderManager = getSupportLoaderManager();
        Loader<String> WebSearchLoader = loaderManager.getLoader(IMAGE_LIST_LOADER);
        if (WebSearchLoader == null) loaderManager.initLoader(IMAGE_LIST_LOADER, null, this);
        else loaderManager.restartLoader(IMAGE_LIST_LOADER, null, this);
    }
    private void updateLocalDatabaseWithData() {

        for (int i=0; i<mImageItems.size(); i++) {

            //Query for image item in local database based on image link, since it is always unique
            String title = mImageItems.get(i).getTitleValue();
            String uniqueLink = mImageItems.get(i).getImageLink();
            String snippet = mImageItems.get(i).getSnippet();

            Cursor cursorImageItems = getContentResolver().query(
                    ImageItemsDbContract.ImageItemsDbEntry.CONTENT_URI,
                    MainActivity.IMAGE_ITEMS_TABLE_ELEMENTS_PROJECTION,
                    ImageItemsDbContract.ImageItemsDbEntry.COLUMN_LINK +" = ?",
                    new String[]{uniqueLink},
                    null);

            /*
            Used for debugging
            Log.w("Image Search Debug", DatabaseUtils.dumpCursorToString(cursorImageItems));
            */

            //If the image item exists in the local database, update its values, otherwise create a new entry
            ContentValues contentValues = new ContentValues();
            contentValues.put(ImageItemsDbContract.ImageItemsDbEntry.COLUMN_TITLE, title);
            contentValues.put(ImageItemsDbContract.ImageItemsDbEntry.COLUMN_LINK, uniqueLink);
            contentValues.put(ImageItemsDbContract.ImageItemsDbEntry.COLUMN_SNIPPET, snippet);

            if (cursorImageItems == null || cursorImageItems.getCount() < 1) {
                Uri uri = getContentResolver().insert(
                        ImageItemsDbContract.ImageItemsDbEntry.CONTENT_URI,
                        contentValues);
            }
            else {
                int updatedRows = getContentResolver().update(
                        ImageItemsDbContract.ImageItemsDbEntry.CONTENT_URI,
                        contentValues,
                        ImageItemsDbContract.ImageItemsDbEntry.COLUMN_LINK + " = ?",
                        new String[]{uniqueLink});
            }

            //Finally, close the cursor
            if (cursorImageItems != null) cursorImageItems.close();
        }
    }
    private void updateImagesGridWithData() {
        mImageItemsCursor = getContentResolver().query(
                ImageItemsDbContract.ImageItemsDbEntry.CONTENT_URI,
                MainActivity.IMAGE_ITEMS_TABLE_ELEMENTS_PROJECTION,
                null,
                null,
                ImageItemsDbContract.ImageItemsDbEntry.COLUMN_TITLE);
        mImageSelectionRVAdapter.swapCursor(mImageItemsCursor);
        mImageSelectionRVAdapter.notifyDataSetChanged();
        mImageSelectionRV.scrollToPosition(mStoredRecyclerViewPosition);
    }
}
