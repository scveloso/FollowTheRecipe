package com.sveloso.followtherecipe;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.woxthebox.draglistview.DragItem;
import com.woxthebox.draglistview.DragListView;
import com.woxthebox.draglistview.swipe.ListSwipeHelper;
import com.woxthebox.draglistview.swipe.ListSwipeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*  The home activity that displays the user's list of recipes.
    Each recipe item shows the recipe title, number of ingredients,
    number of directions and the image of the final dish (if it exists).
 */
public class RecipeListActivity extends AppCompatActivity {

    private static final String TAG = "RecipeListActivity.java";
    public static final String RECIPE_ID = "id";

    private List<Pair<Integer, Recipe>> recipes;
    private DragListView recipeList;
    private RelativeLayout noRecipesContainer;
    private RecipeAdapter recipeAdapter;
    private RecipeDbHelper recipeDbHelper;

    /*  Hook up UI elements to the Activity.
        Create a SQLiteOpenHelper and load the recipes from the database.
        Initialize Recipe DragListView and enable item swiping and dragging.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_list);
        /* Hook up UI elements to the Activity. */
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        noRecipesContainer = (RelativeLayout) findViewById(R.id.noRecipesContainer);

        /* Create a SQLiteOpenHelper and load the recipes from the database. */
        recipeDbHelper = new RecipeDbHelper(this);
        loadRecipes();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add_recipe);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewRecipe();
            }
        });

        /* Initialize and enable Recipe DragListView item dragging and swiping. */
        recipeList = (DragListView) findViewById(R.id.recipe_list);
        recipeList.getRecyclerView().setVerticalScrollBarEnabled(true);
        recipeList.setSwipeListener(new ListSwipeHelper.OnSwipeListenerAdapter() {
            TextView txtRight;
            TextView txtLeft;

            @Override
            public void onItemSwipeStarted(ListSwipeItem item) {
                txtRight = (TextView) item.getRootView().findViewById(R.id.item_right);
                txtLeft = (TextView) item.getRootView().findViewById(R.id.item_left);

                txtRight.setText(getString(R.string.question_delete));
                txtLeft.setText(getString(R.string.question_delete));
            }

            @Override
            public void onItemSwipeEnded(ListSwipeItem item, ListSwipeItem.SwipeDirection swipedDirection) {
                super.onItemSwipeEnded(item, swipedDirection);
                final ListSwipeItem swipedItem = item;

                if (swipedDirection == ListSwipeItem.SwipeDirection.LEFT || swipedDirection == ListSwipeItem.SwipeDirection.RIGHT) {

                    new AlertDialog.Builder(RecipeListActivity.this)
                            .setIcon(android.R.drawable.ic_menu_delete)
                            .setTitle("Delete Recipe?")
                            .setMessage(getString(R.string.prompt_confirm_recipe_delete))
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Pair<Integer, String> adapterItem = (Pair<Integer, String>) swipedItem.getTag();
                                    int pos = recipeList.getAdapter().getPositionForItem(adapterItem);
                                    removeRecipe(recipes.get(pos).second);
                                    recipes.remove(pos);
                                    updateRecipeList();

                                    txtRight.setText(R.string.notify_deleted);
                                    txtLeft.setText(R.string.notify_deleted);
                                }

                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    recipeAdapter.setItemList(recipes);
                                }
                            }).show();
                }
            }
        });
        recipeList.setLayoutManager(new LinearLayoutManager(this));
        recipeAdapter = new RecipeAdapter(recipes, this, R.layout.recipe_list_item, R.id.recipe_list_item);
        recipeList.setAdapter(recipeAdapter, false);
        recipeList.setCanDragHorizontally(true);
        recipeList.setCustomDragItem(new RecipeListActivity.RecipeDragItem(this, R.layout.recipe_list_item));
    }

    /* Update the Recipe database to reflect user re-ordering. */
    private void updateRecipeDb() {
        SQLiteDatabase db = recipeDbHelper.getWritableDatabase();

        db.delete(RecipeContract.RecipeEntry.TABLE_NAME, null, null);

        ContentValues values = new ContentValues();
        try {
            db.beginTransaction();
            for (Pair<Integer, Recipe> recipePair : recipes) {
                Recipe recipe = recipePair.second;
                values.put(RecipeContract.RecipeEntry.COLUMN_NAME_UUID, recipe.getId().toString());
                values.put(RecipeContract.RecipeEntry.COLUMN_NAME_TITLE, recipe.getTitle());
                values.put(RecipeContract.RecipeEntry.COLUMN_NAME_INGREDIENTS, recipe.getIngredientString());
                values.put(RecipeContract.RecipeEntry.COLUMN_NAME_DIRECTIONS, recipe.getDirectionString());
                values.put(RecipeContract.RecipeEntry.COLUMN_NAME_IMG_PATH, recipe.getImgPath());

                db.insert(RecipeContract.RecipeEntry.TABLE_NAME, null, values);
            }
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            Log.e(TAG, "SQLiteException: " + e);
        } finally {
            db.endTransaction();
        }
    }

    /* Update the visible list of recipes upon entering the Activity. */
    @Override
    protected void onResume() {
        super.onResume();
        updateRecipeList();
    }

    /* Update the recipe database upon leaving the Activity. */
    @Override
    protected void onPause() {
        super.onPause();
        updateRecipeDb();
    }

    /* Load the recipes from the database and set the list to the recipe adapter. */
    private void updateRecipeList() {
        loadRecipes();
        recipeAdapter.setItemList(recipes);
    }

    /* Start a new RecipeActivity without an existing recipe. */
    private void createNewRecipe () {
        Intent deviceActivityIntent = new Intent(RecipeListActivity.this, RecipeActivity.class);
        RecipeListActivity.this.startActivity(deviceActivityIntent);
    }

    /* Remove the recipe from the database. */
    private void removeRecipe(Recipe recipe) {
        SQLiteDatabase db = recipeDbHelper.getWritableDatabase();

        db.delete(RecipeContract.RecipeEntry.TABLE_NAME, RecipeContract.RecipeEntry.COLUMN_NAME_UUID + " = ?", new String[]{recipe.getId().toString()});
        Toast.makeText(this, getString(R.string.toast_message_recipe_deleted), Toast.LENGTH_SHORT).show();
        db.close();
    }

    /* Load the recipes from the database. */
    private boolean loadRecipes() {
        recipes = new ArrayList<>();
        SQLiteDatabase deviceDb = recipeDbHelper.getReadableDatabase();

        String queryAll = "SELECT * FROM " + RecipeContract.RecipeEntry.TABLE_NAME;
        Cursor queryAllCursor = deviceDb.rawQuery(queryAll, null);

        /* If no recipes queried, show the user the empty list View. */
        if (queryAllCursor.getCount() <= 0) {
            queryAllCursor.close();
            noRecipesContainer.setVisibility(View.VISIBLE);
            deviceDb.close();
            return false;
        } else { /* If recipes were queried, create a Recipe and add it to the list */
            while (queryAllCursor.moveToNext()) {
                String id = queryAllCursor.getString(queryAllCursor.getColumnIndex(RecipeContract.RecipeEntry.COLUMN_NAME_UUID));
                String title = queryAllCursor.getString(queryAllCursor.getColumnIndex(RecipeContract.RecipeEntry.COLUMN_NAME_TITLE));
                String ingredients = queryAllCursor.getString(queryAllCursor.getColumnIndex(RecipeContract.RecipeEntry.COLUMN_NAME_INGREDIENTS));
                String directions = queryAllCursor.getString(queryAllCursor.getColumnIndex(RecipeContract.RecipeEntry.COLUMN_NAME_DIRECTIONS));
                String imgPath = queryAllCursor.getString(queryAllCursor.getColumnIndex(RecipeContract.RecipeEntry.COLUMN_NAME_IMG_PATH));

                Recipe recipe = new Recipe(UUID.fromString(id), title, ingredients, directions, imgPath);
                recipes.add(new Pair<>(recipes.size(), recipe));
            }
            queryAllCursor.close();
            deviceDb.close();
            noRecipesContainer.setVisibility(View.INVISIBLE);
            return true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recipe_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*  An item that represents a recipe list item
        to visibly show the user which item is being dragged.
     */
    private static class RecipeDragItem extends DragItem {

        private Context context;

        RecipeDragItem (Context context, int layoutId) {
            super(context, layoutId);
            this.context = context;
        }

        @Override
        public void onBindDragView(View clickedView, View dragView) {
            CharSequence textTitle = ((TextView) clickedView.findViewById(R.id.recipe_title)).getText();
            ((TextView) dragView.findViewById(R.id.recipe_title)).setText(textTitle);

            CharSequence numIngredients = ((TextView) clickedView.findViewById(R.id.recipe_num_ingredients)).getText();
            ((TextView) dragView.findViewById(R.id.recipe_num_ingredients)).setText(numIngredients);

            CharSequence numDirections = ((TextView) clickedView.findViewById(R.id.recipe_num_directions)).getText();
            ((TextView) dragView.findViewById(R.id.recipe_num_directions)).setText(numDirections);

            Drawable recipeImgDrawable = ((ImageView) clickedView.findViewById(R.id.list_item_recipe_image_view)).getDrawable();
            ((ImageView) dragView.findViewById(R.id.list_item_recipe_image_view)).setImageDrawable(recipeImgDrawable);

            dragView.setBackgroundColor(Color.GRAY);
        }
    }
}