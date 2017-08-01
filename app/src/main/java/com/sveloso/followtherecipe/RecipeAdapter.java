package com.sveloso.followtherecipe;

import android.content.Context;
import android.content.Intent;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.woxthebox.draglistview.DragItemAdapter;

import java.util.List;

import static com.sveloso.followtherecipe.RecipeListActivity.RECIPE_ID;

/**
 * Created by s.veloso on 7/24/2017.
 */

/*  The RecyclerView's adapter for the list of
    recipes in RecipeListActivity.
 */
public class RecipeAdapter extends DragItemAdapter<Pair<Integer, Recipe>, RecipeAdapter.RecipeViewHolder> {

    private Context context;
    private int mLayoutId;
    private int mGrabHandleId;

    /*  Takes in a list of ingredients, the layout of an item in a list of ingredients
        and the resource id of the draggable view.
     */
    public RecipeAdapter(List<Pair<Integer, Recipe>> list, Context context, int layoutId, int grabHandleId) {
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        this.context = context;
        setHasStableIds(true);
        setItemList(list);
    }

    /*  Return a RecipeViewHolder that holds the layout of each
        list item in the list of recipes.
    */
    @Override
    public RecipeAdapter.RecipeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new RecipeAdapter.RecipeViewHolder(view);
    }

    /*  For each RecipeViewHolder in the list of recipes, set the recipe's details. */
    @Override
    public void onBindViewHolder(RecipeAdapter.RecipeViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        Recipe recipe = mItemList.get(position).second;

        holder.title.setText(context.getString(R.string.input_string, recipe.getTitle()));
        holder.numIngredients.setText(context.getString(R.string.num_ingredients, recipe.getIngredientCount()));
        holder.numDirections.setText(context.getString(R.string.num_directions, recipe.getDirectionCount()));
        if (!recipe.getImgPath().equals("")) {
            GlideApp.with(context).load(recipe.getImgPath()).centerCrop().into(holder.imgRecipe);
        }
        holder.setRecipeId(recipe.getId().toString());

        holder.itemView.setTag(mItemList.get(position));
    }

    @Override
    public long getItemId(int position) {
        return mItemList.get(position).first;
    }

    /*  A specific RecipeViewHolder that holds the recipe details. */
    class RecipeViewHolder extends DragItemAdapter.ViewHolder {

        String recipeId;
        ImageView imgRecipe;
        TextView title;
        TextView numIngredients;
        TextView numDirections;

        RecipeViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, true);
            imgRecipe = (ImageView) itemView.findViewById(R.id.list_item_recipe_image_view);
            title = (TextView) itemView.findViewById(R.id.recipe_title);
            numIngredients = (TextView) itemView.findViewById(R.id.recipe_num_ingredients);
            numDirections = (TextView) itemView.findViewById(R.id.recipe_num_directions);
        }

        public void setRecipeId(String recipeId) {
            this.recipeId = recipeId;
        }

        /*  When a recipe in the list is tapped, open RecipeActivity with its details. */
        @Override
        public void onItemClicked(View view) {
            Intent deviceActivityIntent = new Intent(context, RecipeActivity.class);
            deviceActivityIntent.putExtra(RECIPE_ID, recipeId);
            RecipeAdapter.this.context.startActivity(deviceActivityIntent);
        }
    }
}