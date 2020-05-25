package com.example.guidemear;

import java.io.Serializable;

public class Painting implements Serializable {
    private static final long serialVersionUID = 1L;

    private String artist;
    private String title;
    private String description;

    public Painting(String artist, String title, String description) {
        this.artist = artist;
        this.title = title;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
