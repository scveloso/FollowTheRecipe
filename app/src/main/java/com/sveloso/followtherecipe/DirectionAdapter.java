package com.sveloso.followtherecipe;

import android.content.Context;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.woxthebox.draglistview.DragItemAdapter;

import java.util.List;

/*  An adapter for the RecyclerView's adapter for the list of
    directions in a recipe
 */

public class DirectionAdapter extends DragItemAdapter<Pair<Integer, String>, DirectionAdapter.DirectionViewHolder> {
    
    private int mLayoutId;
    private int mGrabHandleId;
    private Context context;

    /*  Takes in a list of directions, the layout of an item in a list of directions
        and the resource id of the draggable view
     */
    public DirectionAdapter(List<Pair<Integer, String>> list, Context context, int layoutId, int grabHandleId) {
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        this.context = context;
        setHasStableIds(true);
        setItemList(list);
    }

    /*  Return a DirectionViewHolder that holds the layout of each
        list item in the list of directions
     */
    @Override
    public DirectionAdapter.DirectionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new DirectionAdapter.DirectionViewHolder(view);
    }

    /*  For each DirectionViewHolder in the list of directions, set the direction's details

        The direction number is simply its position incremented as the position starts at 0
        but the steps of directions start at 1
     */
    @Override
    public void onBindViewHolder(DirectionAdapter.DirectionViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        String direction = mItemList.get(position).second;

        int number = position + 1;

        holder.strNumber.setText(context.getString(R.string.number_direction_marker, number));
        holder.strDirection.setText(context.getString(R.string.input_string, direction));
        holder.itemView.setTag(mItemList.get(position));
    }

    /*  The list of directions has a Pair of an Integer and a String,
        the Integer represents its position and the String
        represents the actual direction
     */
    @Override
    public long getItemId(int position) {
        return mItemList.get(position).first;
    }

    /*  A specific DirectionViewHolder that holds the direction number
        and actual direction string
     */
    class DirectionViewHolder extends DragItemAdapter.ViewHolder {
        TextView strNumber;
        TextView strDirection;

        DirectionViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, true);
            strNumber = (TextView) itemView.findViewById(R.id.txtNumber);
            strDirection = (TextView) itemView.findViewById(R.id.txtDirection);
        }
    }
}