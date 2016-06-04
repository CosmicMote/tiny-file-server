package com.fowler.tinyfileserver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Favorite {
    private File dir;
    private Calendar lastVisited;

    public Favorite(File dir) {
        this.dir = dir;
        this.lastVisited = Calendar.getInstance();
    }

    public Favorite(String dir, long lastVisited) {
        this.dir = new File(dir);
        this.lastVisited = Calendar.getInstance();
        this.lastVisited.setTimeInMillis(lastVisited);
    }

    public File getDir() {
        return dir;
    }

    public Calendar getLastVisited() {
        return lastVisited;
    }

    public JSONObject toJSON() throws JSONException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        JSONObject json = new JSONObject();
        json.put("dir", dir.getAbsolutePath());
        json.put("lastVisited", sdf.format(lastVisited.getTime()));
        return json;
    }

    public static String toJSON(List<Favorite> favorites) throws JSONException {
        List<JSONObject> jsonObjects = new ArrayList<>(favorites.size());
        for(Favorite favorite : favorites)
            jsonObjects.add(favorite.toJSON());
        JSONArray jsonArray = new JSONArray(jsonObjects);
        return jsonArray.toString(4);
    }
}
