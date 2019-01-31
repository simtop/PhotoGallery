package com.example.simon.photogallery;

import java.util.List;

public class FlickrRequestResults {
    FlickrPhotosResults photos;
    String stat;

    List<GalleryItem> getResults() {
        return photos.getPhotolist();
    }
    int getPage(){
        return  photos.getPage();
    }

    int getMaxPages() {
        return photos.getMaxPages();
    }
    int getTotal() {
        return photos.getTotal();
    }
    int getPhotosPerPage() {
        return photos.getPhotosPerpage();
    }
}
