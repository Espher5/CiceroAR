package com.example.ciceroar.painting;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class PaintingDetailDeserializer implements JsonDeserializer<PaintingDetail> {
    @Override
    public PaintingDetail deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonElement paintingDetails = json.getAsJsonObject().get("paintingDetails");
        return new Gson().fromJson(paintingDetails, PaintingDetail.class);
    }
}
