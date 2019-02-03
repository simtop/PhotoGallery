package com.example.simon.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";
    private static int numberOfColumns = 3;   //default number
    private static final int COLUMN_CONSTANT = 360;

    private RecyclerView mRecyclerView;
    private GridLayoutManager mGridLayoutManager;
    private ProgressBar mProgressBar;

    private List<GalleryItem> mItems = new ArrayList<>();
    private int currentPage = 1;
    private int lastPosition = 0;
    private int firstPosition = 0;



    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        updateItems();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);

        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder,
                                                      Bitmap bitmap) {
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bindDrawable(drawable);
                    }
                }
        );
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mRecyclerView = v.findViewById(R.id.photo_recycler_view);

        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener
                (new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int width = mRecyclerView.getMeasuredWidth();
                        numberOfColumns = Math.round(width / COLUMN_CONSTANT);
                        mGridLayoutManager.setSpanCount(numberOfColumns);
                    }
                });

        mGridLayoutManager = new GridLayoutManager(getActivity(), numberOfColumns);

        mRecyclerView.setLayoutManager(mGridLayoutManager);

        //mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), numberOfColumns));

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1)) {
                    currentPage++;
                    LinearLayoutManager layoutManager = (LinearLayoutManager)mRecyclerView
                            .getLayoutManager();

                    firstPosition = layoutManager.findFirstVisibleItemPosition();
                    lastPosition = layoutManager.findLastVisibleItemPosition();
                    String query = QueryPreferences.getStoredQuery(getActivity());
                    new FetchItemsTask(query).execute(currentPage);
                }
                int top = (firstPosition > 10) ? firstPosition - 10 : 0;
                int bottom = lastPosition > mItems.size() - 11 ?
                        mItems.size() : lastPosition + 10;

                for (int i = top; i < bottom; i++) {
                    mThumbnailDownloader.queueThumbnailCache(mItems.get(i).getUrl());
                }
            }
        });
        mProgressBar = v.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);

        setupAdapter();

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);

        menuInflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override

            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "QueryTextSubmit: " + s);
                QueryPreferences.setStoredQuery(getActivity(), s);
                updateItems();

                return true;
            }
            @Override
            public boolean onQueryTextChange(String s) {

                Log.d(TAG, "QueryTextChange: " + s);
                return false;

            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());

                searchView.setQuery(query, false);
            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:

                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();

                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }


    private void updateItems() {
        currentPage=1;
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute(currentPage);
    }


    private void setupAdapter() {

        if (isAdded()) {
            mRecyclerView.setAdapter(new PhotoAdapter(mItems));
            mRecyclerView.scrollToPosition(lastPosition);
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        //private TextView mTitleTextView;
        private ImageView mItemsImageView;

        public PhotoHolder(View itemView) {
            super(itemView);
            //mTitleTextView = (TextView) itemView;
            mItemsImageView = itemView.findViewById(R.id.item_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mItemsImageView.setImageDrawable(drawable);
        }

    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;

        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            //TextView textView = new TextView(getActivity());
            //return new PhotoHolder(textView);
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_gallery, viewGroup, false);

            return new PhotoHolder(view);

        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {

            GalleryItem galleryItem = mGalleryItems.get(position);
            //photoHolder.bindGalleryItem(galleryItem);
            Drawable placeholder = getResources().getDrawable(R.drawable.sunrise);
            photoHolder.bindDrawable(placeholder);

            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
        }

        @Override

        public int getItemCount() {
            return mGalleryItems.size();

        }
    }

    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>> {

        private String mQuery;
        public FetchItemsTask(String query) {
            mQuery = query;
        }

        @Override
        protected void onPreExecute() {
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos(currentPage);
            } else {
                return new FlickrFetchr().searchPhotos(mQuery, currentPage);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            if(currentPage==1){
                mItems = new ArrayList<>();
            }

            mItems.addAll(items);
            mThumbnailDownloader.clearMemoryCache();
            setupAdapter();
            //currentPage++;
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.GONE);
            }
            Toast.makeText(getActivity(), "Page" + currentPage, Toast.LENGTH_SHORT).show();
        }


    }

}
