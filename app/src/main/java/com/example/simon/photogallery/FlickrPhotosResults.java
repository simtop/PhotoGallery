package com.example.simon.photogallery;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FlickrPhotosResults {
    int page;
    int pages;
    int perpage;
    int total;
    @SerializedName("photo")
    List<GalleryItem> photolist;

    public int getPage() {
        return page;
    }

    public int getMaxPages() {
        return pages;
    }

    public int getPhotosPerpage() {
        return perpage;
    }

    public int getTotal() {
        return total;
    }

    public List<GalleryItem> getPhotolist() {
        return photolist;
    }

}
