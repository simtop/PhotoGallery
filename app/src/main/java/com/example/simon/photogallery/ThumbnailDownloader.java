package com.example.simon.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int MESSAGE_CACHE = 1;


    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    private LruCache<String, Bitmap> mMemoryCache;


    public interface ThumbnailDownloadListener<T> {

        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }
    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;

    }

    public ThumbnailDownloader(Handler responseHandler) {

        super(TAG);
        mResponseHandler = responseHandler;

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {

            @Override
            protected int sizeOf(String key, Bitmap value) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return value.getByteCount() / 1024;
            }
        };
    }


    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
                else if(msg.what == MESSAGE_CACHE){
                    T target = (T) msg.obj;
                    handleCacheRequest(target);
                }
            }
        };
    }


    @Override
    public boolean quit() {

        mHasQuit = true;
        return super.quit();

    }

    public void queueThumbnail(T target, String url) {

        Log.i(TAG, "Got a URL: " + url);
        if (url == null) {
            mRequestMap.remove(target);

        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)

                    .sendToTarget();
        }

    }

    public void queueThumbnailCache(String url) {
        Log.i(TAG, "Got a URL for cache: " + url);

        mRequestHandler.obtainMessage(MESSAGE_CACHE, url).sendToTarget();
    }


    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();

    }

    private void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);
            if (url == null) {
                return;
            }
            //byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            //final Bitmap bitmap = BitmapFactory
              //      .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            //Log.i(TAG, "Bitmap created");
            final Bitmap bitmapFromMemoryCache = getBitmapFromMemoryCache(url);
            if (bitmapFromMemoryCache != null) {
                Log.i(TAG, "Bitmap found in cache");
                mResponseHandler.post(new Runnable() {
                    public void run() {
                        if (!mRequestMap.get(target).equals(url) || mHasQuit) {
                            return;
                        }
                        mRequestMap.remove(target);
                        mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmapFromMemoryCache);
                    }
                });
            }
            else{
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                final Bitmap bitmap = BitmapFactory
                        .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                mResponseHandler.post(new Runnable() {
                    public void run() {
                        if (!mRequestMap.get(target).equals(url) || mHasQuit) {
                            return;
                        }
                        mRequestMap.remove(target);
                        //addBitmapToMemoryCache(url, bitmap);
                        mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                    }
                });
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }

    }

    private void handleCacheRequest(final T target) {
        final String url = (String)target;
        if (url == null) {
            return;
        }

        final Bitmap bitmapFromMemoryCache = getBitmapFromMemoryCache(url);
        try {
            if (bitmapFromMemoryCache == null) {
                byte[] bitmapBytes;
                bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                final Bitmap bitmap = BitmapFactory
                        .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                mResponseHandler.post(new Runnable() {
                    public void run() {
                        if (mHasQuit) {
                            return;
                        }
                        addBitmapToMemoryCache(url, bitmap);
                    }
                });
            }
        } catch (IOException e) {
            Log.e(TAG, "Error downloading image", e);
        }

    }


    public void clearMemoryCache(){
        mMemoryCache.evictAll();
    }
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }
}
