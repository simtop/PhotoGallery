package com.example.simon.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";
    private static int numberOfColumns = 3;   //default number
    private static final int COLUMN_CONSTANT = 360;

    private RecyclerView mRecyclerView;
    private GridLayoutManager mGridLayoutManager;
    private List<GalleryItem> mItems = new ArrayList<>();
    private int currentPage = 1;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute(currentPage);
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
                    new FetchItemsTask().execute(currentPage);
                     currentPage++;
                }
            }
        });


        setupAdapter();

        return v;
    }

    private void setupAdapter() {

        if (isAdded()) {
            mRecyclerView.setAdapter(new PhotoAdapter(mItems));

        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private TextView mTitleTextView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mTitleTextView = (TextView) itemView;
        }

        public void bindGalleryItem(GalleryItem item) {
            mTitleTextView.setText(item.toString());
        }

    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;
        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;

        }
        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            TextView textView = new TextView(getActivity());

            return new PhotoHolder(textView);
        }
        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {

            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.bindGalleryItem(galleryItem);

        }

        @Override

        public int getItemCount() {
            return mGalleryItems.size();

        }
    }

    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
            return new FlickrFetchr().fetchItems(currentPage);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems.addAll(items);
            setupAdapter();
            //currentPage++;
            Toast.makeText(getActivity(), "Page" + currentPage, Toast.LENGTH_SHORT).show();
        }


    }

}
