package com.sveloso.followtherecipe;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.woxthebox.draglistview.DragItem;
import com.woxthebox.draglistview.DragListView;
import com.woxthebox.draglistview.swipe.ListSwipeHelper;
import com.woxthebox.draglistview.swipe.ListSwipeItem;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/*  An Activity for the more detailed view at an existing individual Recipe,
    to create a new Recipe or to edit an existing Recipe
 */
public class RecipeActivity extends AppCompatActivity {

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final String TAG = "RecipeActivity.java";

    private RecipeDbHelper recipeDbHelper;
    private Recipe recipe;
    private EditText edTxtTitle;
    private DragListView listIngredients;
    private DragListView listDirections;

    private RelativeLayout rlNoIngredients;
    private RelativeLayout rlNoDirections;

    private ImageButton imgBtnAddIngredient;
    private ImageButton imgBtnAddDirection;
    private ImageButton imgBtnTakePic;
    private ImageView imgRecipe;

    private IngredientAdapter ingredientAdapter;
    private DirectionAdapter directionAdapter;

    private String recipeId;
    private boolean recipeChanged;

    /*  Receives a callback when user successfully takes a new picture
        of the dish associated with the Recipe.

        Loads the image taken by the user into the Activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            GlideApp.with(this).load(recipe.getImgPath()).centerCrop().into(imgRecipe);
            recipeChanged = true;
        }
    }

    /*  Initialize a SQLiteOpenHelper.
        Load a recipe's details if user tapped on existing recipe.
        If not, create a new recipe.
        Hook up UI elements to Activity.
        Initialize DragListViews.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        /* Initialize a SQLiteOpenHelper. */
        recipeDbHelper = new RecipeDbHelper(this);

        /* Load a recipe's details if user tapped on existing recipe. */
        recipeId = getIntent().getStringExtra(RecipeListActivity.RECIPE_ID);
        if (recipeId != null) {
            loadRecipe();
        } else { /* If not, create a new recipe. */
            recipe = new Recipe();
        }

        /* Hook up title field to Activity. */
        edTxtTitle = (EditText) findViewById(R.id.edTxtRecipeTitle);
        edTxtTitle.addTextChangedListener(new TextWatcher() {
            String originalTitle;
            String title;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                originalTitle = edTxtTitle.getText().toString();
                title = originalTitle;
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                title = s.toString();
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (!originalTitle.equals(title)) {
                    recipe.setTitle(title);
                    recipeChanged = true;
                }
            }
        });
        edTxtTitle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    hideKeyboard();
                }
                return true;
            }
        });
        edTxtTitle.setText(recipe.getTitle());
        hideKeyboard();

        /* Initialize views to show when lists are empty. */
        rlNoIngredients = (RelativeLayout) findViewById(R.id.noIngredients);
        rlNoDirections = (RelativeLayout) findViewById(R.id.noDirections);

        /* Load the dish image onto the ImageView, if it exists. */
        imgRecipe = (ImageView) findViewById(R.id.imgViewRecipeImage);
        if (!recipe.getImgPath().isEmpty()) {
            GlideApp.with(this).load(recipe.getImgPath()).centerCrop().into(imgRecipe);
        }

        /* Hook up Image Buttons for adding to the lists and taking a picture of the dish. */
        imgBtnAddIngredient = (ImageButton) findViewById(R.id.imgBtnAddIngredient);
        imgBtnAddIngredient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptAddIngredient();
            }
        });
        imgBtnAddDirection = (ImageButton) findViewById(R.id.imgBtnAddDirection);
        imgBtnAddDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptAddDirection();
            }
        });
        imgBtnTakePic = (ImageButton) findViewById(R.id.imgBtnTakeRecipePic);
        imgBtnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    /* Create the file where the photo should go. */
                    File photoFile = null;
                    try {
                        photoFile = createRecipeImageFile();
                    } catch (IOException ex) {
                        Toast.makeText(RecipeActivity.this, "Error in creating new image file. ", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error in creating new image file. ");
                    }
                    /* Continue only if the File was successfully created. */
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(RecipeActivity.this,
                                "com.example.android.fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                    }
                }
            }
        });

        /* Initialize Ingredients List. */
        listIngredients = (DragListView) findViewById(R.id.listIngredients);
        listIngredients.getRecyclerView().setVerticalScrollBarEnabled(true);
        listIngredients.setSwipeListener(new ListSwipeHelper.OnSwipeListenerAdapter() {
            TextView txtRight;
            TextView txtLeft;

            @Override
            public void onItemSwipeStarted(ListSwipeItem item) {
                txtRight = (TextView) item.getRootView().findViewById(R.id.item_right);
                txtLeft = (TextView) item.getRootView().findViewById(R.id.item_left);

                txtRight.setText(RecipeActivity.this.getString(R.string.question_delete));
                txtLeft.setText(RecipeActivity.this.getString(R.string.question_delete));
            }

            @Override
            public void onItemSwipeEnded(ListSwipeItem item, ListSwipeItem.SwipeDirection swipedDirection) {
                if (swipedDirection == ListSwipeItem.SwipeDirection.LEFT || swipedDirection == ListSwipeItem.SwipeDirection.RIGHT) {
                    Pair<Integer, String> adapterItem = (Pair<Integer, String>) item.getTag();
                    int pos = listIngredients.getAdapter().getPositionForItem(adapterItem);
                    recipe.removeIngredient(pos);
                    updateIngredientList();
                    recipeChanged = true;

                    txtRight.setText(RecipeActivity.this.getString(R.string.notify_deleted));
                    txtLeft.setText(RecipeActivity.this.getString(R.string.notify_deleted));
                }
            }
        });

        /* Initialize Direction List */
        listDirections = (DragListView) findViewById(R.id.listDirections);
        listDirections.getRecyclerView().setVerticalScrollBarEnabled(true);
        listDirections.setSwipeListener(new ListSwipeHelper.OnSwipeListenerAdapter() {
            TextView txtRight;
            TextView txtLeft;

            @Override
            public void onItemSwipeStarted(ListSwipeItem item) {
                txtRight = (TextView) item.getRootView().findViewById(R.id.item_right);
                txtLeft = (TextView) item.getRootView().findViewById(R.id.item_left);

                txtRight.setText(RecipeActivity.this.getString(R.string.question_delete));
                txtLeft.setText(RecipeActivity.this.getString(R.string.question_delete));
            }

            @Override
            public void onItemSwipeEnded(ListSwipeItem item, ListSwipeItem.SwipeDirection swipedDirection) {
                if (swipedDirection == ListSwipeItem.SwipeDirection.LEFT || swipedDirection == ListSwipeItem.SwipeDirection.RIGHT) {
                    Pair<Integer, String> adapterItem = (Pair<Integer, String>) item.getTag();
                    int pos = listDirections.getAdapter().getPositionForItem(adapterItem);
                    recipe.removeDirection(pos);
                    updateDirectionList();
                    recipeChanged = true;

                    txtRight.setText(RecipeActivity.this.getString(R.string.notify_deleted));
                    txtLeft.setText(RecipeActivity.this.getString(R.string.notify_deleted));
                }
            }
        });

        /* Implement DragListView lists */
        listIngredients.setLayoutManager(new LinearLayoutManager(this));
        ingredientAdapter = new IngredientAdapter(recipe.getIngredients(), this, R.layout.ingredient_list_item, R.id.item_layout);
        listIngredients.setAdapter(ingredientAdapter, false);
        listIngredients.setCanDragHorizontally(true);
        listIngredients.setCustomDragItem(new IngredientDragItem(this, R.layout.ingredient_list_item));
        listIngredients.setDragListListener(new DragListView.DragListListenerAdapter() {
            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {
                recipeChanged = true;
            }
        });
        updateIngredientList();


        listDirections.setLayoutManager(new LinearLayoutManager(this));
        directionAdapter = new DirectionAdapter(recipe.getDirections(), this, R.layout.direction_list_item,  R.id.item_layout);
        listDirections.setAdapter(directionAdapter, false);
        listDirections.setCanDragHorizontally(true);
        listDirections.setCustomDragItem(new DirectionDragitem(this, R.layout.direction_list_item));
        listDirections.setDragListListener(new DragListView.DragListListenerAdapter() {
            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {
                recipeChanged = true;
            }
        });
        updateDirectionList();

        recipeChanged = false;
    }

    /* Ask user if they want to save or discard Recipe changes */
    @Override
    public void onBackPressed() {
        if (recipeChanged) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_menu_save)
                    .setTitle("Save Recipe?")
                    .setMessage("Would you like to save or discard the recipe?")
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            storeRecipeInDb();
                            finish();
                        }

                    }).setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            }).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recipe_activity, menu);

        MenuItem miDelete = menu.findItem(R.id.action_delete);
        if (recipeId != null) {
            MenuItem item = menu.findItem(R.id.action_delete);
            item.setEnabled(true);
        } else { /* If not, create a new recipe. */
            MenuItem item = menu.findItem(R.id.action_delete);
            item.setEnabled(false);
        }
        return true;
    }

    /* Ask user if they want to save or discard Recipe changes */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();

                return true;
            case R.id.action_delete:
                Toast.makeText(this, "Delete", Toast.LENGTH_SHORT).show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*  Store current Recipe in the database.
        If the Recipe already exists, update the row inside the database.
        If not, insert a new row inside the database.
     */
    private void storeRecipeInDb() {
        SQLiteDatabase db = recipeDbHelper.getWritableDatabase();

        String query = "SELECT * FROM " + RecipeContract.RecipeEntry.TABLE_NAME + " WHERE " + RecipeContract.RecipeEntry.COLUMN_NAME_UUID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{recipe.getId().toString()});

        if (cursor.getCount() > 0) { /* If Recipe already exists, update it in the database */
            ContentValues values = new ContentValues();
            values.put(RecipeContract.RecipeEntry.COLUMN_NAME_TITLE, recipe.getTitle());
            values.put(RecipeContract.RecipeEntry.COLUMN_NAME_INGREDIENTS, recipe.getIngredientString());
            values.put(RecipeContract.RecipeEntry.COLUMN_NAME_DIRECTIONS, recipe.getDirectionString());
            values.put(RecipeContract.RecipeEntry.COLUMN_NAME_IMG_PATH, recipe.getImgPath());

            db.update(RecipeContract.RecipeEntry.TABLE_NAME, values, RecipeContract.RecipeEntry.COLUMN_NAME_UUID + " = ?", new String[]{recipe.getId().toString()});
            Toast.makeText(this, getString(R.string.toast_message_recipe_updated), Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Updated existing recipe");
        } else { /* If Recipe doesn't exist, make a new entry in the database */
            ContentValues contentValues = new ContentValues();
            contentValues.put(RecipeContract.RecipeEntry.COLUMN_NAME_UUID, recipe.getId().toString());
            contentValues.put(RecipeContract.RecipeEntry.COLUMN_NAME_TITLE, recipe.getTitle());
            contentValues.put(RecipeContract.RecipeEntry.COLUMN_NAME_DIRECTIONS, recipe.getDirectionString());
            contentValues.put(RecipeContract.RecipeEntry.COLUMN_NAME_INGREDIENTS, recipe.getIngredientString());
            contentValues.put(RecipeContract.RecipeEntry.COLUMN_NAME_IMG_PATH, recipe.getImgPath());

            db.insert(RecipeContract.RecipeEntry.TABLE_NAME, null, contentValues);
            Toast.makeText(this, getString(R.string.toast_message_recipe_created), Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Made new recipe");
        }
        cursor.close();
        db.close();
    }

    /*  Load an existing Recipe from the database with the recipe ID
        from RecipeListActivity.
     */
    private void loadRecipe() {
        SQLiteDatabase db = recipeDbHelper.getReadableDatabase();

        String queryRecipe = "SELECT * FROM " + RecipeContract.RecipeEntry.TABLE_NAME + " WHERE " + RecipeContract.RecipeEntry.COLUMN_NAME_UUID + " = ?";

        Cursor cursor = db.rawQuery(queryRecipe, new String[]{recipeId});

        if (cursor.moveToFirst()) { /* Load existing Recipe details */
            String title = cursor.getString(cursor.getColumnIndex(RecipeContract.RecipeEntry.COLUMN_NAME_TITLE));
            String strIngredients = cursor.getString(cursor.getColumnIndex(RecipeContract.RecipeEntry.COLUMN_NAME_INGREDIENTS));
            String strDirections = cursor.getString(cursor.getColumnIndex(RecipeContract.RecipeEntry.COLUMN_NAME_DIRECTIONS));
            String imgPath = cursor.getString(cursor.getColumnIndex(RecipeContract.RecipeEntry.COLUMN_NAME_IMG_PATH));

            recipe = new Recipe(UUID.fromString(recipeId), title, strIngredients, strDirections, imgPath);
            Log.i(TAG, "New recipe made.");

        } else { /* Recipe ID from RecipeListActivity should exist in the database, error if it does not */
            Toast.makeText(this, "Error: Recipe not found.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error: Recipe not found.");
            finish();
        }
        cursor.close();
        db.close();
    }

    private void promptAddIngredient() {
        promptAddIngredient("", "");
    }

    /*  Prompt the user for a new ingredient addition.
        Re-open the AlertDialog if user leaves any fields blank.

        If fields are filled in, add an ingredient to the Recipe
        and update the ingredient list to reflect addition.
     */
    private void promptAddIngredient(String ingredient, String quantity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add an ingredient");
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.alert_add_ingredient, (ViewGroup) findViewById(R.id.add_ingredient), false);

        final EditText editTxtIngredient = (EditText) viewInflated.findViewById(R.id.editTxtIngredient);
        editTxtIngredient.setText(ingredient);
        editTxtIngredient.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    hideKeyboard();
                }
                return true;
            }
        });

        final EditText editTxtQuantity = (EditText) viewInflated.findViewById(R.id.editTxtQuantity);
        editTxtQuantity.setText(quantity);
        editTxtQuantity.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    hideKeyboard();
                }
                return true;
            }
        });

        builder.setView(viewInflated);
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String ingredient = editTxtIngredient.getText().toString();
                String quantity = editTxtQuantity.getText().toString();

                if (ingredient.equals("") || quantity.equals("")) {
                    Toast.makeText(RecipeActivity.this, "Please don't leave any fields blank.", Toast.LENGTH_LONG).show();
                    dialog.cancel();
                    promptAddIngredient(ingredient, quantity);
                } else {
                    recipe.addIngredient(ingredient + ":" + quantity);
                    updateIngredientList();
                    recipeChanged = true;
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    /*  Prompt the user for a new direction addition.
        Re-open the AlertDialog if user leaves any fields blank.

        If fields are filled in, add a direction to the Recipe
        and update the direction list to reflect addition.
     */
    private void promptAddDirection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add a direction");
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.alert_add_direction, (ViewGroup) findViewById(R.id.add_direction), false);

        final EditText editTxtDirection = (EditText) viewInflated.findViewById(R.id.editTxtDirection);
        editTxtDirection.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    hideKeyboard();
                }
                return true;
            }
        });

        builder.setView(viewInflated);
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String direction = editTxtDirection.getText().toString();

                if (direction.equals("")) {
                    Toast.makeText(RecipeActivity.this, "Please don't leave the field blank.", Toast.LENGTH_LONG).show();
                    dialog.cancel();
                    promptAddDirection();
                } else {
                    recipe.addDirection(direction);
                    updateDirectionList();
                    recipeChanged = true;
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    /*  Simple utility method that hides the keyboard. */
    public void hideKeyboard() {
        if(getCurrentFocus()!= null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    /* Re-set the ingredient list to the ingredient adapter.
       If there are no ingredients, show the empty list View.
    */
    private void updateIngredientList() {
        ingredientAdapter.setItemList(recipe.getIngredients());

        if (ingredientAdapter.getItemCount() == 0) {
            rlNoIngredients.setVisibility(View.VISIBLE);
        } else {
            rlNoIngredients.setVisibility(View.INVISIBLE);
        }
    }

    /*  Re-set the direction list to the direction adapter.
        If there are no directions, show the empty list View.
     */
    private void updateDirectionList() {
        directionAdapter.setItemList(recipe.getDirections());

        if (directionAdapter.getItemCount() == 0) {
            rlNoDirections.setVisibility(View.VISIBLE);
        } else {
            rlNoDirections.setVisibility(View.INVISIBLE);
        }
    }

    /*  An item that represents an ingredient list item
        to visibly show the user which item is being dragged.
     */
    private static class IngredientDragItem extends DragItem {

        IngredientDragItem(Context context, int layoutId) {
            super(context, layoutId);
        }

        @Override
        public void onBindDragView(View clickedView, View dragView) {
            /* Copy the ingredient string from the selected view to the view being dragged.  */
            CharSequence text = ((TextView) clickedView.findViewById(R.id.txtIngredient)).getText();
            ((TextView) dragView.findViewById(R.id.txtIngredient)).setText(text);

            /* Copy the quantity string from the selected view to the view being dragged.  */
            CharSequence textQty = ((TextView) clickedView.findViewById(R.id.txtQuantity)).getText();
            ((TextView) dragView.findViewById(R.id.txtQuantity)).setText(textQty);

            /* Set the dragged view's background color to gray. */
            dragView.setBackgroundColor(Color.GRAY);
        }
    }

    /*  An item that represents a direction list item
        to visibly show the user which item is being dragged.
     */
    private static class DirectionDragitem extends DragItem {

        DirectionDragitem(Context context, int layoutId) {
            super(context, layoutId);
        }

        @Override
        public void onBindDragView(View clickedView, View dragView) {
            /* Copy the direction string from the selected view to the view being dragged. */
            CharSequence text = ((TextView) clickedView.findViewById(R.id.txtDirection)).getText();
            ((TextView) dragView.findViewById(R.id.txtDirection)).setText(text);

            /* Copy the direction number from the selected view to the view being dragged. */
            CharSequence textNumber = ((TextView) clickedView.findViewById(R.id.txtNumber)).getText();
            ((TextView) dragView.findViewById(R.id.txtNumber)).setText(textNumber);

            /* Set the dragged view's background color to gray. */
            dragView.setBackgroundColor(Color.GRAY);
        }
    }

    private File createRecipeImageFile() throws IOException {
        /* Create an image file name. */
        String imageFileName = "JPEG_" + recipe.getId().toString() + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        /* Save a file: path for use with ACTION_VIEW intents */
        recipe.setImgPath(image.getAbsolutePath());
        return image;
    }
}