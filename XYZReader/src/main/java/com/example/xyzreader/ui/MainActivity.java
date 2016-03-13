package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, ArticleAdapter.ItemClickListener {
    @Bind(R.id.toolbar) Toolbar mToolbar;
    @Bind(R.id.recycler_view) RecyclerView mRecyclerView;
    @Bind(R.id.swipe_refresh_layout) SwipeRefreshLayout mSwipeRefreshLayout;
    private Boolean mTwoPane = false;
    private long selectedItem = -1;
    public static final String ARTICLE_FRAGMENT_TAG = "articleFragmentTag";
    public static final String SELECTED_ITEM = "selectedItem";
    public static final String ARG_STARTING_POSITION = "startingPosition";
    public static final String ARG_CURRENT_POSITION = "currentPosition";
    private int currentPosition;
    private Bundle activityReenterData;
    private SharedElementCallback sharedElementCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            SetupSharedElementCallback();
            setExitSharedElementCallback(sharedElementCallback);
            setEnterSharedElementCallback(sharedElementCallback);
        }
        setContentView(R.layout.activity_article_list);
        ButterKnife.bind(this);
        getLoaderManager().initLoader(0, null, this);
        mTwoPane = findViewById(R.id.article_container) != null;
        if (savedInstanceState == null) {
            refresh();
        }
    }

    @Override
    public void onClick(View view, long itemId) {
        selectedItem = itemId;
        ArticleAdapter.ViewHolder viewHolder =
                (ArticleAdapter.ViewHolder) mRecyclerView.findViewHolderForItemId(itemId);
        currentPosition = viewHolder != null ? viewHolder.getAdapterPosition() : 0;
        if (mTwoPane) {
            loadFragmentFromItemId(itemId);
        }
        else {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    ItemsContract.Items.buildItemUri(itemId));
            intent.putExtra(ARG_STARTING_POSITION, currentPosition);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    viewHolder.thumbnailView,
                    getString(R.string.article_image_transition_name)
            );
            ActivityCompat.startActivity(this, intent, options.toBundle());
        }
    }

    private void loadFragmentFromItemId(long itemId) {
        ArticleDetailFragment fragment = ArticleDetailFragment.newInstance(itemId, true);
        FragmentManager fragmentManager = getFragmentManager();
        if (getFragmentManager().findFragmentByTag(ARTICLE_FRAGMENT_TAG) != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.article_container, fragment, ARTICLE_FRAGMENT_TAG)
                    .commit();
        }
        else {
            fragmentManager
                    .beginTransaction()
                    .add(R.id.article_container, fragment, ARTICLE_FRAGMENT_TAG)
                    .commit();
        }
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
        if (mTwoPane && mRecyclerView.getAdapter() != null) {
            onClick(mRecyclerView, mRecyclerView.getAdapter().getItemId(0));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(SELECTED_ITEM, selectedItem);
        outState.putInt(ARG_CURRENT_POSITION, currentPosition);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            selectedItem = savedInstanceState.getLong(SELECTED_ITEM, 0);
            currentPosition = savedInstanceState.getInt(ARG_CURRENT_POSITION, 0);
            if (selectedItem != -1) {
                if (mTwoPane) {
                    loadFragmentFromItemId(selectedItem);
                }
                //skip the transition animation on a screen rotation
                else {
                    Intent intent = new Intent(
                            Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(selectedItem));
                    intent.putExtra(ARG_STARTING_POSITION, currentPosition);
                    ActivityCompat.startActivity(this, intent, null);
                }
            }
        }
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        activityReenterData = data.getExtras();
        int startingPosition = activityReenterData.getInt(ARG_STARTING_POSITION);
        currentPosition = activityReenterData.getInt(ARG_CURRENT_POSITION);
        if (startingPosition != currentPosition) {
            mRecyclerView.scrollToPosition(currentPosition);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        final ArticleAdapter articleAdapter = new ArticleAdapter(cursor, this, this);
        mRecyclerView.setAdapter(articleAdapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        GridLayoutManager lm = new GridLayoutManager(this, columnCount);
        mRecyclerView.setLayoutManager(lm);
        //load the first item in the list, in two pane mode
        if (mTwoPane) {
            mRecyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onClick(mRecyclerView, mRecyclerView.getAdapter().getItemId(0));
                }
            }, 200);
        }
        mIsRefreshing = false;
        updateRefreshingUI();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @TargetApi(21)
    public void SetupSharedElementCallback() {
        sharedElementCallback = new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                if (activityReenterData != null) {
                    int startingPosition = activityReenterData.getInt(ARG_STARTING_POSITION);
                    int currentPosition = activityReenterData.getInt(ARG_CURRENT_POSITION);
                    if (currentPosition != startingPosition) {
                        ArticleAdapter.ViewHolder viewHolder =
                                (ArticleAdapter.ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(currentPosition);
                        ImageView sharedElement = viewHolder.thumbnailView;
                        if (sharedElement != null) {
                            names.clear();
                            names.add(sharedElement.getTransitionName());
                            sharedElements.clear();
                            sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                        }
                    }
                }
            }
        };
    }
}
