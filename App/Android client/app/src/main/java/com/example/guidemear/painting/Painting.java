package com.example.guidemear.painting;

import java.io.Serializable;
import java.util.List;

public class Painting implements Serializable {
    private static final long serialVersionUID = 1L;

    private String artist;
    private String title;
    private List<PaintingDetail> paintingInfo;

    public Painting(String artist, String title, List<PaintingDetail> paintingInfo) {
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

    public List<PaintingDetail> getPaintingInfo() {
        return paintingInfo;
    }

    public void setPaintingInfo(List<PaintingDetail> paintingInfo) {
        this.paintingInfo = paintingInfo;
    }
}
