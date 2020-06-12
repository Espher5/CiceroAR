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

    public String getDescription() {
        return description;
    }
}