package com.imagesearcher;

import android.content.ContentValues;
import android.content.Intent;
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
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.imagesearcher.data.ImageItemsDbContract;
import com.imagesearcher.data.ImageSearchResult;
import com.imagesearcher.ui.ImageSelectionRVAdapter;
import com.imagesearcher.utilities.NetworkUtilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements
        ImageSelectionRVAdapter.ListItemClickHandler,
        LoaderManager.LoaderCallbacks<ImageSearchResult>{

    //region Locals
    public static final int NUMBER_GRID_COLUMNS = 2;
    private static final int IMAGE_LIST_LOADER = 123;
    private static final String IMAGE_ITEMS_RV_POSITION = "image_items_RV_position";
    public static final String CHOSEN_IMAGE_LINK = "chosen_image_link";
    private static final String USER_SEARCH_KEYWORDS = "user_search_keywords";

    @BindView(R.id.search_keywords_ET) EditText mSearchKeywordsET;
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
    private String mSearchKeywords;
    private boolean requestedSwipe;
    private ImageAsyncTaskLoader mImageAsyncTaskLoader;
    //endregion

    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        //UI Initialization
        requestedSwipe = false;
        mSwipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestedSwipe = true;
                loadImagesGrid();
            }
        });
        setupUserInputListeners();

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
        outState.putString(USER_SEARCH_KEYWORDS, mSearchKeywords);
    }
    @Override public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //Adapted from: https://stackoverflow.com/questions/27816217/how-to-save-recyclerviews-scroll-position-using-recyclerview-state
        if(savedInstanceState != null) {
            mStoredRecyclerViewPosition = savedInstanceState.getInt(IMAGE_ITEMS_RV_POSITION);
            mImageSelectionRV.scrollToPosition(mStoredRecyclerViewPosition);
            mSearchKeywords = savedInstanceState.getString(USER_SEARCH_KEYWORDS);
        }
        if (mImageAsyncTaskLoader!=null) mImageAsyncTaskLoader.setLoaderState(true);
    }
    @Override protected void onPause() {
        super.onPause();
        if (mImageAsyncTaskLoader!=null) mImageAsyncTaskLoader.setLoaderState(false);
    }

    //Loader methods
    @NonNull @Override public Loader<ImageSearchResult> onCreateLoader(int id, @Nullable Bundle args) {

        if (!requestedSwipe) showLoadingIndicatorInsteadOfRecycleView();
        mImageAsyncTaskLoader = new ImageAsyncTaskLoader(mSearchKeywords, this);
        mImageAsyncTaskLoader.setLoaderState(true);
        return mImageAsyncTaskLoader;
    }
    @Override public void onLoadFinished(@NonNull Loader<ImageSearchResult> loader, ImageSearchResult imageSearchResult) {

        updateImagesGridWithData();

        if (!NetworkUtilities.isInternetAvailable(getApplicationContext()) && imageSearchResult == null) {
            NetworkUtilities.tellUserInternetIsUnavailable(getApplicationContext());
        }
        else if (imageSearchResult != null) {

            //Initial checks
            if (imageSearchResult.getItems().size() == 0) {
                Toast.makeText(getApplicationContext(), getString(R.string.failed_find_images), Toast.LENGTH_SHORT).show();
            }

            //Reset the image items list
            mImageItems.clear();
            mImageItems.addAll(imageSearchResult.getItems());

            //Delete all the values in the app's data Content Provider Uri
            getContentResolver().delete(ImageItemsDbContract.ImageItemsDbEntry.CONTENT_URI, "1", null);

            //Update the data in the app's data Content Provider Uri
            updateLocalDatabaseWithData();

            //Update the grid
            updateImagesGridWithData();
        }

        showRecycleViewInsteadOfLoadingIndicator();
        mSwipeContainer.setRefreshing(false);
    }
    @Override public void onLoaderReset(@NonNull Loader<ImageSearchResult> loader) {

    }
    private static class ImageAsyncTaskLoader extends AsyncTaskLoader<ImageSearchResult> {
        //Inspired by: https://stackoverflow.com/questions/44309241/warning-this-asynctask-class-should-be-static-or-leaks-might-occur?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa

        //private WeakReference<MainActivity> appReference;
        private String mSearchKeyword;
        private boolean mAllowLoaderStart;

        ImageAsyncTaskLoader(String searchKeyword, @NonNull MainActivity context) {
            super(context);
            mSearchKeyword = searchKeyword;
        }

        @Override protected void onStartLoading() {
            //if (args == null) return;
            if (mAllowLoaderStart) forceLoad();
        }

        void setLoaderState(boolean state) {
            mAllowLoaderStart = state;
        }

        @Nullable @Override public ImageSearchResult loadInBackground() {

            String websiteContentString = "";
            ImageSearchResult imageSearchResult = null;
            if (NetworkUtilities.isInternetAvailable(getContext()) && !TextUtils.isEmpty(mSearchKeyword) ) {
                String basicPath = getContext().getString(R.string.basic_image_search_path);
                String requestUrl = basicPath + mSearchKeyword;

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
    private void setupUserInputListeners() {
        mSearchKeywordsET.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    performImageSearch();
                }
                return true;
            } } );
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performImageSearch();
            }
        });
    }
    private void performImageSearch() {
        mSearchKeywords = mSearchKeywordsET.getText().toString();
        List<String> searchWordsList = new ArrayList<>();
        String[] searchWordsSpace = mSearchKeywords.split(" ");
        for (String word : searchWordsSpace) {
            String[] searchWordsComma = word.split(",");
            searchWordsList.addAll(Arrays.asList(searchWordsComma));
        }
        String[] searchWordsArray = searchWordsList.toArray(new String[0]);
        mSearchKeywords = TextUtils.join("&", searchWordsArray);
        loadImagesGrid();
    }
    private void setupImagesRecyclerView() {
        mImageSelectionRV.setLayoutManager(new GridLayoutManager(this, NUMBER_GRID_COLUMNS));
        mImageSelectionRVAdapter = new ImageSelectionRVAdapter(this, this);
        mImageSelectionRVAdapter.swapCursor(mImageItemsCursor);
        mImageSelectionRV.setAdapter(mImageSelectionRVAdapter);
        mImageSelectionRV.addItemDecoration(new DividerItemDecoration(getApplicationContext(), GridLayoutManager.VERTICAL));
        mImageSelectionRV.addItemDecoration(new DividerItemDecoration(getApplicationContext(), GridLayoutManager.HORIZONTAL));
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
        Intent intent = new Intent(this, DisplayActivity.class);
        intent.putExtra(CHOSEN_IMAGE_LINK, imageLinkFromCursor);
        startActivity(intent);
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
        //mImageSelectionRV.scrollToPosition(mStoredRecyclerViewPosition);
    }
}
