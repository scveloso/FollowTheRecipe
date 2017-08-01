package com.sveloso.followtherecipe;

import android.provider.BaseColumns;

/**
 * Created by s.veloso on 7/10/2017.
 */

public class RecipeContract {

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private RecipeContract() {}

    /* Inner class that defines the recipe table contents */
    public static class RecipeEntry implements BaseColumns {
        public static final String TABLE_NAME = "recipe_table";
        public static final String COLUMN_NAME_UUID = "uuid";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_INGREDIENTS = "ingredients";
        public static final String COLUMN_NAME_DIRECTIONS = "directions";
        public static final String COLUMN_NAME_IMG_PATH = "img_path";
    }
}