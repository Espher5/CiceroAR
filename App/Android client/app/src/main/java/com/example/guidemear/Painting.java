package com.example.guidemear;

import java.io.Serializable;
import java.util.Map;

public class Painting implements Serializable {
    private static final long serialVersionUID = 1L;

    private String artist;
    private String title;
    private Map<String, String> paintingInfo;

    public Painting(String artist, String title, Map<String, String> paintingInfo) {
        this.artist = artist;
        this.title = title;
        this.paintingInfo = paintingInfo;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Map<String, String> getPaintingInfo() {
        return paintingInfo;
    }

    public void setPaintingInfo(Map<String, String> paintingInfo) {
        this.paintingInfo = paintingInfo;
    }
}
