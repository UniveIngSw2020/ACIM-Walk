package com.acim.walk.Model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.acim.walk.R;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;



/*
 *
 * this class BINDS some data to the RECYCLERVIEW of the SHOWRANKING fragment
 *
 */

public class RankingRecyclerViewAdapter extends RecyclerView.Adapter<RankingRecyclerViewAdapter.ViewHolder> {

    // this arraylist will store all the users queried from Firebase
    private ArrayList<User> mData;
    private LayoutInflater mInflater;


    /**
     *
     * stores in mData the users queried from Firebase
     *
     * @param context
     * @param data users queried from Firebase
     */
    public RankingRecyclerViewAdapter(Context context, ArrayList<User> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }


    // factory method
    @NonNull
    @Override
    public RankingRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.ranking_recyclerview_row, parent, false);
        return new ViewHolder(view);
    }


    // binds data to the textviews of each row of the recycler
    @Override
    public void onBindViewHolder(@NonNull RankingRecyclerViewAdapter.ViewHolder holder, int position) {

        /*
         * the @position param, is the CURRENT INDEX of the mData arraylist.
         * mData[position] is basically users[i]
         */

        // getting some data
        String username = mData.get(position).getUsername();
        int steps = mData.get(position).getSteps();
        int rankingPosition = position+1;

        // displaying to the views
        holder.usernameTxt.setText(username);
        holder.stepsTxt.setText(String.valueOf(steps));
        holder.rankingTxt.setText(String.valueOf(rankingPosition) + '°');
    }


    // factory method
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView rankingTxt;
        TextView usernameTxt;
        TextView stepsTxt;

        ViewHolder(View itemView) {
            super(itemView);
            rankingTxt = itemView.findViewById(R.id.rankingPosition_txt);
            usernameTxt = itemView.findViewById(R.id.rankingUsername_txt);
            stepsTxt = itemView.findViewById(R.id.rankingSteps_txt);
            //myTextView = itemView.findViewById(R.id.tvAnimalName);
            //itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            //if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }


}
