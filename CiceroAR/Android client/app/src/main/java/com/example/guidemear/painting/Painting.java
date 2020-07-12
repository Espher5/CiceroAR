package com.example.guidemear.painting;


import java.io.Serializable;
import java.util.List;

public class Painting implements Serializable {
    private static final long serialVersionUID = 1L;

    private String artist;
    private String title;
    private List<PaintingDetail> paintingDetails;

    public Painting(String artist, String title, List<PaintingDetail> paintingInfo) {
        this.artist = artist;
        this.title = title;
        this.paintingDetails = paintingInfo;
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

    public List<PaintingDetail> getPaintingDetails() {
        return paintingDetails;
    }

    public void setPaintingDetails(List<PaintingDetail> paintingInfo) {
        this.paintingDetails = paintingInfo;
    }
}
