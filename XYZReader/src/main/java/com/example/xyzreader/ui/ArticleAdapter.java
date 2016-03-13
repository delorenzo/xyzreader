package com.example.xyzreader.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ViewHolder> {
    private Context mContext;
    private Cursor mCursor;
    private ItemClickListener itemClickListener;

    public ArticleAdapter(Cursor cursor, Context context, ItemClickListener itemClickListener) {
        mCursor = cursor;
        mContext = context;
        this.itemClickListener = itemClickListener;
        setHasStableIds(true);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.thumbnail) ImageView thumbnailView;
        @Bind(R.id.article_title) TextView titleView;
        @Bind(R.id.article_subtitle) TextView subtitleView;
        private final View parent;
        public ViewHolder(View view) {
            super(view);
            parent = view;
            ButterKnife.bind(this, view);
        }
        public void setOnClickListener(View.OnClickListener listener) {
            parent.setOnClickListener(listener);
        }
        public void SetSelected(Boolean isSelected) {
            titleView.setSelected(isSelected);
            subtitleView.setSelected(isSelected);
        }
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.list_item_article, parent, false);
        final ViewHolder vh = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListener.onClick(v, getItemId(vh.getAdapterPosition()));
            }
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
        holder.subtitleView.setText(String.format(
                        mContext.getString(R.string.subtitle_format),
                        DateUtils.getRelativeTimeSpanString(
                                mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                                System.currentTimeMillis(),
                                DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL),
                        mCursor.getString(ArticleLoader.Query.AUTHOR)
                )
        );
        Picasso.with(mContext)
                .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                .into(holder.thumbnailView);
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    public interface ItemClickListener {
        void onClick(View item, long itemId);
    }
}
