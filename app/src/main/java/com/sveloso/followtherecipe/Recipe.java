package com.sveloso.followtherecipe;

import android.support.v4.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/* Represents a Recipe with:
    -A UUID identifier to find the Recipe in a SQLite database
    -A String title
    -A list of ingredients, with the ingredient represented as a Pair<Integer, String>
        -Needed for drag-sort feature (re-order ingredient list through dragging and dropping)
            -Drag-sort feature needs for each list item to have a unique ID. I cannot just use
             its position as the ID as there are times when it isn't unique. For example:
             if list item A was in position 7 and list item B was in position 6 and item A was
             dragged above item B, then the ID of 7 is now tied to item B and A at the same time,
             resulting in a bug
        -The integer is the ingredient's position and the String is the actual ingredient
    -A list of directions, with the direction represented as a Pair<Integer, String>
        -Needed for drag-sort feature (re-order ingredient list through dragging and dropping)
        -The integer is the ingredient's position and the String is the actual ingredient
    -A path to its bitmap, if it exists
 */
public class Recipe {

    private UUID id;
    private String title;
    private List<Pair<Integer, String>> ingredients;
    private List<Pair<Integer, String>> directions;
    private String imgPath;

    /* The base constructor when the user is creating a completely new Recipe. */
    public Recipe() {
        id = UUID.randomUUID();
        this.title = "Untitled Recipe";
        ingredients = new ArrayList<>();
        directions = new ArrayList<>();
        imgPath = "";
    }

    /*  The constructor used for an existing Recipe in RecipeActivity
        Ingredients and directions are then added from the database.
     */
    public Recipe(UUID id, String title, String imgPath) {
        this.title = title;
        this.id = id;
        ingredients = new ArrayList<>();
        directions = new ArrayList<>();
        this.imgPath = imgPath;
    }

    /*  The constructor used for the RecyclerView in RecipeListActivity.

        The recipe list item displays the title, number of ingredients and directions,
        as well as the recipe image.
     */
    public Recipe (UUID id, String title, String ingredientString, String directionString, String imgPath) {
        this.id = id;
        this.title = title;
        ingredients = new ArrayList<>();
        directions = new ArrayList<>();
        this.imgPath = imgPath;

        if (!ingredientString.equals("")) {
            String[] arrIngredients = ingredientString.split(",");
            if (arrIngredients.length > 1) {
                for (String ingredient : arrIngredients) {
                    addIngredient(ingredient);
                }
            } else {
                addIngredient(ingredientString);
            }
        }

        if (!directionString.equals("")) {
            String[] arrDirections = directionString.split(",");
            if (arrDirections.length > 1) {
                for (String direction : arrDirections) {
                    addDirection(direction);
                }
            } else {
                addDirection(directionString);
            }
        }
    }

    public String getImgPath() {
        return imgPath;
    }

    public void setImgPath(String newPath) {
        imgPath = newPath;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getIngredientCount() {
        return ingredients.size();
    }

    public int getDirectionCount() {
        return directions.size();
    }

    public void addIngredient (String ingredient) {
        ingredients.add(new Pair<>(ingredients.size(), ingredient));
    }

    public void addDirection (String direction) {
        directions.add(new Pair<>(directions.size(), direction));
    }

    /*  Since each ingredient come as a Pair<Integer, String>,

        When an ingredient is removed, the Integer in each Pair<Integer, String>
        will need to be shifted.
     */
    public void removeIngredient (int index) {
        ingredients.remove(index);
        realignIngredients();
    }

    /*  Go through the list of ingredients and ensure that
        the Integer in each Pair<Integer, String> reflects the
        position of each Ingredient.
     */
    private void realignIngredients() {
        List<Pair<Integer, String>> newIngredients = new ArrayList<>();
        for (int i = 0; i < ingredients.size(); i++) {
            newIngredients.add(new Pair<>(i, ingredients.get(i).second));
        }
        ingredients = newIngredients;
    }

    /*
        Since each direction come as a Pair<Integer, String>,

        When a direction is removed, the Direction in each Pair<Integer, String>
        will need to be shifted.
     */
    public void removeDirection (int index) {
        directions.remove(index);
        realignDirections();
    }

    /*
        Go through the list of directions and ensure that
        the Integer in each Pair<Integer, String> reflects the
        position of each Direction.
     */
    private void realignDirections() {
        List<Pair<Integer, String>> newDirections = new ArrayList<>();
        for (int i = 0; i < directions.size(); i++) {
            newDirections.add(new Pair<>(i, directions.get(i).second));
        }
        directions = newDirections;
    }

    public List<Pair<Integer, String>> getIngredients() {
        return ingredients;
    }

    public List<Pair<Integer, String>> getDirections() {
        return directions;
    }

    /*  Get a single string that represents all of the directions

        Format:
        direction1,direction2,direction3,direction4
     */
    public String getDirectionString () {
        StringBuilder directionString = new StringBuilder();

        for (int i = 0; i < directions.size(); i++) {
            if (i > 0) {
                directionString.append(",");
            }
            directionString.append(directions.get(i).second);
        }
        return directionString.toString();
    }

    /*  Get a single string that represents all of the ingredients

        Format:
        ingredient1,ingredient2

        Format of ingredients:
        ingredientString1:qty1
     */
    public String getIngredientString() {
        StringBuilder ingredientString = new StringBuilder();

        for (int i = 0; i < ingredients.size(); i++) {
            if (i > 0) {
                ingredientString.append(",");
            }
            ingredientString.append(ingredients.get(i).second);
        }
        return ingredientString.toString();
    }
}