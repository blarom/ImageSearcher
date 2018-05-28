package com.imagesearcher.data;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.SerializedName;

public class ImageSearchResult {

    public ImageSearchResult() {}

    @SerializedName("items")
    private List<Item> items = new ArrayList<>();
    public List<Item> getItems() { return items; }

    public class Item {

        @SerializedName("title")
        private String title;
        public String getTitleValue() { return title; }

        @SerializedName("link")
        private String link;
        public String getImageLink() { return link; }

        @SerializedName("snippet")
        private String snippet;
        public String getSnippet() { return snippet; }

    }
}
