package com.imagesearcher.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.imagesearcher.MainActivity;
import com.imagesearcher.R;
import com.imagesearcher.data.ImageItemsDbContract;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ImageSelectionRVAdapter extends RecyclerView.Adapter<ImageSelectionRVAdapter.ImageEntryViewHolder> {

    private Context mContext;
    final private ListItemClickHandler mOnClickHandler;
    private Cursor mImageEntryCursor;

    //RecyclerView Adapter methods
    public ImageSelectionRVAdapter(Context context, ListItemClickHandler listener) {
        this.mContext = context;
        this.mOnClickHandler = listener;
    }
    @NonNull @Override public ImageSelectionRVAdapter.ImageEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.image_entry_list_item, parent, false);
        view.setFocusable(true);
        return new ImageEntryViewHolder(view);
    }
    @Override public void onBindViewHolder(@NonNull ImageSelectionRVAdapter.ImageEntryViewHolder holder, int position) {

        //Moving the cursor to the desired row and skipping the next calls if there is no such position
        if (!mImageEntryCursor.moveToPosition(position)) return;

        // Determine the values of the wanted data
        int index = mImageEntryCursor.getColumnIndex(ImageItemsDbContract.ImageItemsDbEntry.COLUMN_LINK);
        String imagePathFromCursor = mImageEntryCursor.getString(index);

        index = mImageEntryCursor.getColumnIndex(ImageItemsDbContract.ImageItemsDbEntry.COLUMN_TITLE);
        String imageTitleFromCursor = mImageEntryCursor.getString(index);

        //Update the values in the layout
        updateImageTitleInGrid(imageTitleFromCursor, holder, position);
        updateImageInGrid(imagePathFromCursor, holder);
    }
    @Override public int getItemCount() {
        if (mImageEntryCursor == null) return 0;
        else return mImageEntryCursor.getCount();
    }
    public class ImageEntryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.image_description) TextView imageDescription;
        @BindView(R.id.image_in_list) ImageView imageInList;

        ImageEntryViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int clickedPosition = getAdapterPosition();
            mOnClickHandler.onListItemClick(clickedPosition);
        }
    }

    //Functional methods
    private void updateImageTitleInGrid(String imageTitleFromCursor, ImageEntryViewHolder holder, int position) {
        TextView textView = holder.imageDescription;
        textView.setText(imageTitleFromCursor);

    }
    private void updateImageInGrid(String path, final ImageEntryViewHolder holder) {
        Picasso.with(mContext)
                .load(path)
                .error(R.drawable.ic_warning_yellow_24dp)
                .into(holder.imageInList);
    }
    public void swapCursor(Cursor newCursor) {
        if (mImageEntryCursor != null) mImageEntryCursor.close();
        mImageEntryCursor = newCursor;
        if (newCursor != null) this.notifyDataSetChanged();
    }
    public interface ListItemClickHandler {
        void onListItemClick(int clickedItemIndex);
    }
}
