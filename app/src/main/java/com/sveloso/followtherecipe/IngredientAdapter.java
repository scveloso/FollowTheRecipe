package com.sveloso.followtherecipe;

import android.content.Context;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.woxthebox.draglistview.DragItemAdapter;

import java.util.List;

/**
 * Created by s.veloso on 7/19/2017.
 */

/*  The RecyclerView's adapter for the list of
    ingredients in a recipe.
 */
public class IngredientAdapter extends DragItemAdapter<Pair<Integer, String>, IngredientAdapter.IngredientViewHolder> {

    private int mLayoutId;
    private int mGrabHandleId;
    private Context context;

    /*  Takes in a list of ingredients, the layout of an item in a list of ingredients
        and the resource id of the draggable view.
     */
    public IngredientAdapter(List<Pair<Integer, String>> listIngredients, Context context, int layoutId, int grabHandleId) {
        mLayoutId = layoutId;
        this.context = context;
        mGrabHandleId = grabHandleId;
        setHasStableIds(true);
        setItemList(listIngredients);
    }

    /*  Return an IngredientViewHolder that holds the layout of each
        list item in the list of ingredients.
     */
    @Override
    public IngredientViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new IngredientViewHolder(view);
    }

    /*  For each IngredientViewHolder in the list of ingredients, set the ingredient's details.

        The ingredient and its quantity are received in the form: "ingredientName:quantityString"
     */
    @Override
    public void onBindViewHolder(IngredientViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        String ingredient = mItemList.get(position).second;

        holder.strIngredient.setText(context.getString(R.string.input_string, ingredient.split(":")[0]));
        holder.strQuantity.setText(context.getString(R.string.input_string, ingredient.split(":")[1]));

        holder.itemView.setTag(mItemList.get(position));
    }

    /*  The list of ingredients has a Pair of an Integer and a String,
        the Integer represents its position and the String
        represents the actual direction.
     */
    @Override
    public long getItemId(int position) {
        return mItemList.get(position).first;
    }

    /*  A specific IngredientViewHolder that holds the ingredient
        and the quantity string.
     */
    class IngredientViewHolder extends DragItemAdapter.ViewHolder {
        TextView strIngredient;
        TextView strQuantity;

        IngredientViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, true);
            strIngredient = (TextView) itemView.findViewById(R.id.txtIngredient);
            strQuantity = (TextView) itemView.findViewById(R.id.txtQuantity);
        }
    }
}