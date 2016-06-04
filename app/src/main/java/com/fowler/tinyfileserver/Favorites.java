package com.fowler.tinyfileserver;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Favorites {

    private static final String TAG = Favorites.class.getName();

    private static final int FAVORITE_COUNT = 5;

    private File dbFile;

    public Favorites(Context context) {
        Log.d(TAG, "Creating favorites db if it does not exist");
        dbFile = new File(context.getFilesDir(), "favorites.db");
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            db.execSQL("create table if not exists favorites " +
                    "( " +
                    "  dir text primary key not null, " +
                    "  last_visited integer not null " +
                    ");");
        } finally {
            if(db != null)
                db.close();
        }
        Log.d(TAG, "Created favorites db if it did not exist");
    }

    public List<Favorite> getFavorites() {
        Log.d(TAG, "Getting favorites...");
        List<Favorite> favorites = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            cursor = db.rawQuery("select * from favorites order by last_visited desc", null);
            while (cursor.moveToNext()) {
                String dir = cursor.getString(0);
                long visited = cursor.getLong(1);
                favorites.add(new Favorite(dir, visited));
            }
            Log.d(TAG, String.format("Successfully retrieved %d favorites", favorites.size()));
        } catch(Exception e) {
            Log.e(TAG, "Failed to get favorites", e);
        } finally {
            if(cursor != null)
                cursor.close();
            if(db != null)
                db.close();
        }
        return favorites;
    }

    public void addFavorite(File dir) {

        Log.d(TAG, "Adding favorite: " + dir);

        Favorite favorite = new Favorite(dir);
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            db.beginTransaction();
            Object[] params =
                new Object[]{ favorite.getDir().getAbsolutePath(), favorite.getLastVisited().getTimeInMillis() };
            db.execSQL("insert or replace into favorites (dir, last_visited) values (?, ?)", params);
            // delete all but the most recent n records
            params = new Object[]{ FAVORITE_COUNT };
            db.execSQL("delete from favorites where dir not in ( " +
                       "  select dir from favorites order by last_visited desc limit ? " +
                       ")", params);
            db.setTransactionSuccessful();
            Log.d(TAG, "Successfully added favorite: " + dir);
        } catch(Exception e) {
            Log.e(TAG, "Failed to add favorite", e);
        } finally {
            if(db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    public void deleteFavorite(String dir) {
        Log.d(TAG, "Deleting favorite: " + dir);

        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            db.beginTransaction();
            db.execSQL("delete from favorites where dir = ?", new Object[]{ dir });
            db.setTransactionSuccessful();
            Log.d(TAG, "Successfully deleted favorite: " + dir);
        } catch(Exception e) {
            Log.e(TAG, "Failed to delete favorite", e);
        } finally {
            if(db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }
}
