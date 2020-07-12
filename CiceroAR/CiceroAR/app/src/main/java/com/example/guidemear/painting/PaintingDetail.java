package com.example.guidemear.painting;

public class PaintingDetail {
    private String imagePath;
    private String description;

    public PaintingDetail (String imagePath, String description) {
        this.imagePath = imagePath;
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getDescription() {
        return description;
    }

    public String toString() {
        return "Path: " + imagePath + " Desc: " + description;
    }
}