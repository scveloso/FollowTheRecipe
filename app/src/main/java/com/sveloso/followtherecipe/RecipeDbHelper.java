package com.sveloso.followtherecipe;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by s.veloso on 7/10/2017.
 */

public class RecipeDbHelper extends SQLiteOpenHelper {

    private static final String SQL_CREATE_RECIPE_ENTRIES =
            "CREATE TABLE " + RecipeContract.RecipeEntry.TABLE_NAME + " (" +
                    RecipeContract.RecipeEntry._ID + " INTEGER PRIMARY KEY," +
                    RecipeContract.RecipeEntry.COLUMN_NAME_UUID + " TEXT," +
                    RecipeContract.RecipeEntry.COLUMN_NAME_TITLE + " TEXT NOT NULL," +
                    RecipeContract.RecipeEntry.COLUMN_NAME_INGREDIENTS + " TEXT NOT NULL," +
                    RecipeContract.RecipeEntry.COLUMN_NAME_DIRECTIONS + " TEXT NOT NULL," +
                    RecipeContract.RecipeEntry.COLUMN_NAME_IMG_PATH + " TEXT NOT NULL)";

    public static final String SQL_DELETE_RECIPE_ENTRIES =
            "DROP TABLE IF EXISTS " + RecipeContract.RecipeEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Recipes.db";

    public RecipeDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_RECIPE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_RECIPE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}